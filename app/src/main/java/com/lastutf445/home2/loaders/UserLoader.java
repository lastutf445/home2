package com.lastutf445.home2.loaders;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class UserLoader {

    private static WeakReference<Handler> settingsHandler;
    private static UserDataSync userDataSync;
    private static UserDataSyncTransport userDataSyncTransport;
    private static Authenticator authenticator;
    private static boolean allowUserDataSync;
    private static GetPublicKey getPublicKey;
    private static long lastSync;

    public static void init() {
        lastSync = DataLoader.getLong("lastSyncUser", 0);

        try {
            userDataSyncTransport = new UserDataSyncTransport();
            userDataSync = new UserDataSync();
            Sync.addSyncProvider(userDataSyncTransport);
            Sync.addSyncProvider(userDataSync);
            scanForSync();

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

    public static void scanForSync() {

        long lastSync = DataLoader.getLong("lastSyncUser", 0);
        Log.d("LOGTAG", "current syncTime: " + lastSync);

        Set<String> ops = DataLoader.getKeys();

        for (String key: ops) {
            if (!DataLoader.isSyncable(key)) continue;
            if (DataLoader.getSyncTime(key) > lastSync) {
                Log.d("LOGTAG", "should be synced: " + key + " " + DataLoader.getSyncTime(key));
                UserLoader.addToSyncUserDataQueue(key);
            } else {
                Log.d("LOGTAG", "not needed to be synced: " + key + " " + DataLoader.getSyncTime(key));
            }
        }
    }

    public static boolean isAuthenticated() {
        return DataLoader.getString("Session", null) != null;
    }

    @NonNull
    public static String getUsername() {
        Resources res = DataLoader.getAppResources();
        String name = DataLoader.getString("Username", res.getString(R.string.usernameError));
        return isAuthenticated() ? name : res.getString(R.string.notAuthenticated);
    }

    public static void getPublicKey(@NonNull GetPublicKey.Callback callback) {
        Sync.removeSyncProvider(Sync.PROVIDER_GET_PUBLIC_KEY);

        try {
            getPublicKey = new GetPublicKey(callback);
            Sync.addSyncProvider(getPublicKey);

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

    public static void setAuthKey(@NonNull String authKey) {
        if (getPublicKey != null) {
            new AsyncTask<String, Void, Void>() {
                @Override
                protected Void doInBackground(String... strings) {
                    getPublicKey.keyIntegrity(strings[0]);
                    return null;
                }

            }.execute(authKey);
        }
    }

    public static void startAuth(@NonNull String login, @NonNull String password, @NonNull Handler handler) {
        authenticator = new Authenticator(
                login,
                password,
                handler
        );

        if (!CryptoLoader.isPublicKeyValid()) {
            handler.sendEmptyMessage(1);
            getPublicKey(authenticator);

        } else {
            authenticator.sendCredentials();
        }
    }

    public static void cancelAuth() {
        Log.d("LOGTAG", "auth canceled");

        Sync.removeSyncProvider(Sync.PROVIDER_GET_PUBLIC_KEY);
        Sync.removeSyncProvider(Sync.PROVIDER_CREDENTIALS);
        Sync.removeSyncProvider(Sync.PROVIDER_ENTER_BY_EMAIL);

        if (authenticator != null) {
            authenticator.weakHandler = new WeakReference<>(null);
        }
    }

    public static void logout() {
        //ModulesLoader.wipeModules();
        //DataLoader.flushTables();
        DataLoader.flushSyncable();
        flushSyncUserDataQueue();
        lastSync = 0;
        DataLoader.save();
        CryptoLoader.init();
    }

    public static void setSettingsHandler(Handler handler) {
        settingsHandler = new WeakReference<>(handler);
    }

    private static void callSettingsHandler() {
        if (settingsHandler != null) {
            Handler handler = settingsHandler.get();
            if (handler != null) {
                handler.sendEmptyMessage(-1);
            }
        }
    }

    @Nullable
    public static String getSession() {
        return DataLoader.getString("Session", null);
    }

    @Nullable
    public static String getAESKey() {
        return DataLoader.getString("AESKey", null);
    }

    public static void onReconnect() {
        allowUserDataSync = false;
        if (userDataSync != null) {
            userDataSync.reconnected = true;
        }
    }

    public static void addToSyncUserDataQueue(String option) {
        if (userDataSyncTransport != null) {
            userDataSyncTransport.queue.add(option);
            if (userDataSyncTransport.request.has(option)) {
                userDataSyncTransport.update(option);
            }
        }
    }

    public static void removeFromSyncUserDataQueue(String option) {
        if (userDataSyncTransport != null) {
            userDataSyncTransport.queue.remove(option);
            userDataSyncTransport.request.remove(option);
        }
    }

    public static void flushSyncUserDataQueue() {
        if (userDataSyncTransport != null) {
            synchronized (userDataSyncTransport.queue) {
                synchronized (userDataSyncTransport.request) {
                    while (userDataSyncTransport.request.length() > 0) {
                        Iterator<String> it = userDataSyncTransport.request.keys();
                        userDataSyncTransport.request.remove(it.next());
                    }
                    userDataSyncTransport.queue.clear();
                }
            }
        }
    }

    public final static class GetPublicKey extends SyncProvider {
        String modulus, pubExp, salt, mac;
        private Callback callback;

        public GetPublicKey(@NonNull Callback callback) throws JSONException {
            super(
                    Sync.PROVIDER_GET_PUBLIC_KEY,
                    "getPublicKey",
                    new JSONObject(),
                    null,
                    Sync.DEFAULT_PORT,
                    true);

            encrypted = false;
            this.callback = callback;
            setGroup(Sync.SYNC_GET_PUBLIC_KEY);
        }

        @Override
        public void onPostPublish(int statusCode) {
            callback.onPostPublish(statusCode);
        }

        @Override
        public void onReceive(JSONObject data) {
            Sync.removeSyncProvider(Sync.PROVIDER_GET_PUBLIC_KEY);

            try {
                modulus = data.getString("modulus");
                pubExp = data.getString("pubExp");
                salt = data.getString("salt");
                mac = data.getString("mac");

                if (!modulus.equals(DataLoader.getString("PublicKeyModulus", "")) ||
                        !pubExp.equals(DataLoader.getString("PublicKeyExp", ""))
                ) {
                    callback.onKeyMismatch();
                    return;
                }

                callback.onValid(modulus, pubExp);

            } catch (Exception e) {
                e.printStackTrace();
                callback.onInvalid();
            }
        }

        private void keyIntegrity(@NonNull String authKey) {
            callback.onKeyCheck();

            Log.d("LOGTAG", "got AuthKey: " + authKey);

            if(!CryptoLoader.compareMAC(modulus.concat(pubExp), authKey, mac, salt)) {
                callback.onKeyMismatch();
                return;
            }

            keyValidity();
        }

        private void keyValidity() {
            Log.d("LOGTAG", "key validating...");

            if (CryptoLoader.isPublicKeyValid(modulus, pubExp)) {
                DataLoader.setWithoutSync("PublicKeyModulus", modulus);
                DataLoader.setWithoutSync("PublicKeyExp", pubExp);
                DataLoader.save();

                Log.d("LOGTAG", "its ok");
                Log.d("LOGTAG", "new RSA keys is set");

                CryptoLoader.setPublicKey(modulus, pubExp);
                callback.onValid(modulus, pubExp);

            } else {
                callback.onInvalid();
            }
        }

        public interface Callback {
            void onValid(@NonNull String modulus, @NonNull String pubExp);
            void onInvalid();
            void onPostPublish(int statusCode);
            void onKeyCheck();
            void onKeyMismatch();
        }
    }

    private final static class CredentialsProvider extends SyncProvider {

        interface Callback {
            void onStatusReceived(JSONObject data);
            void onPostPublish(int statusCode);
        }

        private Callback callback;

        public CredentialsProvider(@NonNull JSONObject data, @NonNull Callback callback) throws JSONException {
            super(
                    Sync.PROVIDER_CREDENTIALS,
                    "auth",
                    data,
                    null,
                    Sync.DEFAULT_PORT,
                    false);

            this.callback = callback;
        }

        @Override
        public void onPostPublish(int statusCode) {
            callback.onPostPublish(statusCode);
        }

        @Override
        public void onReceive(JSONObject data) {
            if (!data.has("status")) return;
            Sync.removeSyncProvider(Sync.PROVIDER_CREDENTIALS);
            callback.onStatusReceived(data);
        }
    }

    public final static class KeyChanger extends SyncProvider {
        private WeakReference<Handler> weakHandler;

        public KeyChanger(@NonNull Handler handler, @NonNull String key) throws JSONException {
            super(
                    Sync.PROVIDER_KEY_CHANGER,
                    "keyChange",
                    new JSONObject(),
                    null,
                    Sync.DEFAULT_PORT,
                    true
            );

            weakHandler = new WeakReference<>(handler);
            JSONObject data = new JSONObject();
            data.put("key", key);
            query.put("data", data);

            Log.d("LOGTAG", "!!! i want to update AES key from: " + CryptoLoader.getInstalledAESKeyLength());
        }

        @Override
        public void onPostPublish(int statusCode) {
            if (statusCode < 0 || statusCode == 1) return;
            Handler handler = weakHandler.get();
            Bundle data = new Bundle();
            int status;

            if (handler == null) return;

            switch (statusCode) {
                case 2:
                    status = R.string.masterServerRequired;
                    break;
                case 3:
                    status = R.string.disconnected;
                    break;
                case 4:
                    status = R.string.encryptionError;
                    break;
                default:
                    status = R.string.unexpectedError;
                    break;
            }

            data.putInt("status", status);

            Message msg = handler.obtainMessage(0);
            msg.setData(data);
            handler.sendMessage(msg);
        }

        @Override
        public void onReceive(JSONObject data) {
            if (!data.has("status")) return;
            Handler handler = weakHandler.get();

            try {
                if (data.getInt("status") == Sync.OK) {
                    DataLoader.setWithoutSync("AESKey", data.getString("key"));
                    CryptoLoader.setAESKey(data.getString("key"));
                    DataLoader.save();

                    Log.d("LOGTAG", "!!! new AES key was set: " + CryptoLoader.getInstalledAESKeyLength());
                    Sync.removeSyncProvider(Sync.PROVIDER_KEY_CHANGER);
                    callSettingsHandler();

                    if (handler != null) {
                        handler.sendEmptyMessage(1);
                    }

                } else if (handler != null) {
                    handler.sendEmptyMessage(0);
                }

            } catch (JSONException e) {
                //e.printStackTrace();

                if (handler != null) {
                    handler.sendEmptyMessage(0);
                }
            }
        }
    }

    public final static class CloseSessions extends SyncProvider {
        private WeakReference<Handler> weakHandler;

        public CloseSessions(@NonNull Handler handler) throws JSONException {
            super(
                    Sync.PROVIDER_TERMINATE_SESSIONS,
                    "closeSessions",
                    new JSONObject(),
                    null,
                    Sync.DEFAULT_PORT,
                    false
            );

            weakHandler = new WeakReference<>(handler);
        }

        @Override
        public void onPostPublish(int statusCode) {
            if (statusCode < 0 || statusCode == 1) return;
            Handler handler = weakHandler.get();
            Bundle data = new Bundle();
            int status;

            if (handler == null) return;

            switch (statusCode) {
                case 2:
                    status = R.string.masterServerRequired;
                    break;
                case 3:
                    status = R.string.disconnected;
                    break;
                case 4:
                    status = R.string.encryptionError;
                    break;
                default:
                    status = R.string.unexpectedError;
                    break;
            }

            data.putInt("status", status);

            Message msg = handler.obtainMessage(0);
            msg.setData(data);
            handler.sendMessage(msg);
        }

        @Override
        public void onReceive(JSONObject data) {
            if (!data.has("status")) return;
            Handler handler = weakHandler.get();

            try {
                if (data.getInt("status") == Sync.OK) {
                    if (handler != null) {
                        handler.sendEmptyMessage(2);
                    }

                } else if (handler != null) {
                    handler.sendEmptyMessage(0);
                }

            } catch (JSONException e) {
                //e.printStackTrace();

                if (handler != null) {
                    handler.sendEmptyMessage(0);
                }
            }
        }
    }

    private final static class Authenticator implements GetPublicKey.Callback, CredentialsProvider.Callback {

        private WeakReference<Handler> weakHandler;
        private String login, password, key;
        private int stage;

        public Authenticator(@NonNull String login, @NonNull String password, @NonNull Handler handler) {
            weakHandler = new WeakReference<>(handler);
            this.password = password;
            this.login = login;
            stage = 5;
        }

        /**
         * HANDLER RETURN CODES:
         * 5 - requesting publicKey
         * 6 - invalid publicKey
         * 7 - sending credentials
         * 8 - auth failed
         * 9 - ok
         * 10 - key check (integrity - mac verifying)
         * 11 - key mismatch (need auth key)
         */

        @Override
        public void onValid(@NonNull String modulus, @NonNull String pubExp) {
            try {
                if (weakHandler.get() == null) return;
                sendCredentials();

            } catch (NumberFormatException e) {
                //e.printStackTrace();
                onInvalid();
            }
        }

        @Override
        public void onInvalid() {
            Handler handler = weakHandler.get();
            if (handler == null) return;

            handler.sendEmptyMessage(6);
        }

        @Override
        public void onKeyCheck() {
            Handler handler = weakHandler.get();
            if (handler == null) return;

            handler.sendEmptyMessage(10);
        }

        @Override
        public void onKeyMismatch() {
            Handler handler = weakHandler.get();
            if (handler == null) return;

            handler.sendEmptyMessage(11);
        }

        public void sendCredentials() {
            Handler handler = weakHandler.get();
            stage = 7;

            if (handler != null) {
                handler.sendEmptyMessage(7);

            } else {
                return;
            }

            JSONObject data = new JSONObject();
            key = CryptoLoader.createMaxAESKey();

            try {
                data.put("login", login);
                data.put("password", password);
                data.put("key", key);

                Sync.addSyncProvider(new CredentialsProvider(data, this));

            } catch (JSONException e) {
                //e.printStackTrace();
                handler.sendEmptyMessage(0);
            }
        }

        @Override
        public void onPostPublish(int statusCode) {
            Handler handler = weakHandler.get();

            if (handler != null) {
                if (statusCode != 1) {
                    handler.sendEmptyMessage(statusCode);
                } else {
                    handler.sendEmptyMessage(stage);
                }
            }
        }

        @Override
        public void onStatusReceived(JSONObject data) {
            Handler handler = weakHandler.get();

            try {
                int status = data.getInt("status");

                if (handler != null) {
                    switch (status) {
                        case Sync.UNKNOWN_USER:
                            handler.sendEmptyMessage(8);
                            return;
                        case Sync.UNEXPECTED_ERROR:
                            handler.sendEmptyMessage(0);
                            return;
                    }
                }

                if (status == Sync.OK) {
                    NotificationsLoader.removeById(Sync.UNAUTHORIZED);
                    DataLoader.setWithoutSync("Username", DataLoader.getAppResources().getString(R.string.usernameDefault));
                    DataLoader.setWithoutSync("Session", data.getString("session"));
                    DataLoader.setWithoutSync("AESKey", key);
                    CryptoLoader.setAESKey(key);
                    DataLoader.save();

                    if (handler != null) {
                        handler.sendEmptyMessage(9);
                    }

                } else if (handler != null) {
                    handler.sendEmptyMessage(0);
                }

            } catch (JSONException e) {
                //e.printStackTrace();

                if (handler != null) {
                    handler.sendEmptyMessage(0);
                }
            }
        }
    }

    private final static class UserDataSync extends SyncProvider {
        private boolean reconnected;

        public UserDataSync() throws JSONException {
            super(Sync.PROVIDER_USER_DATA_STARTER, "syncUserData", new JSONObject(), null, 0, false);
            lastSync = DataLoader.getLong("lastSyncUser", 0);
            group = Sync.SYNC_USER_DATA;
        }

        @Override
        public boolean isWaiting() {
            return !UserLoader.isAuthenticated() || !reconnected;
        }

        @Override
        public void onReceive(JSONObject data) {
            Log.d("LOGTAG", data.toString());

            if (!UserLoader.isAuthenticated()) {
                Log.d("LOGTAG", "SYNCUSERDATA ERROR: data received, but user isn\'t authenticated");
                return;
            }

            try {
                int status = data.getInt("status");
                long lastUpdated = data.getLong("lastUpdated");

                if (status != Sync.UPDATE) {
                    Log.d("LOGTAG", "SYNCUSERDATA ERROR: " + data.toString());
                    NotificationsLoader.makeStatusNotification(
                            Sync.SYNC_USER_DATA_FAILED_EVENT,
                            true
                    );
                    return;
                }

                JSONObject ops = data.getJSONObject("ops");

                if (!DataLoader.merge(ops)) {
                    Log.d("LOGTAG", "SYNCUSERDATA ERROR: MERGE ERROR");
                    NotificationsLoader.makeStatusNotification(
                            Sync.SYNC_USER_DATA_FAILED_EVENT,
                            true
                    );
                    callSettingsHandler();
                    return;
                }

                callSettingsHandler();

                lastSync = Math.max(lastSync, Math.min(System.currentTimeMillis(), lastUpdated));
                DataLoader.setWithoutSync("lastSyncUser", lastSync);
                DataLoader.save();

                Log.d("LOGTAG", "SyncUserData finished");
                NotificationsLoader.removeById(Sync.SYNC_USER_DATA_EVENT);
                NotificationsLoader.removeById(Sync.SYNC_USER_DATA_FAILED_EVENT);

                allowUserDataSync = true;
                reconnected = false;

            } catch (JSONException e) {
                e.printStackTrace();
                NotificationsLoader.makeStatusNotification(
                        Sync.SYNC_USER_DATA_FAILED_EVENT,
                        true
                );
            }
        }

        @Override
        public JSONObject getQuery() {
            NotificationsLoader.makeStatusNotification(Sync.SYNC_USER_DATA_EVENT, false);
            allowUserDataSync = false;

            try {
                JSONObject data = new JSONObject();
                data.put("lastSync", lastSync);
                query.put("data", data);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return query;
        }
    }

    private final static class UserDataSyncTransport extends SyncProvider {
        private final HashSet<String> queue = new HashSet<>();
        private final JSONObject request = new JSONObject();

        public UserDataSyncTransport() throws JSONException {
            super(Sync.PROVIDER_USER_DATA_TRANSPORT, "syncUserDataTransport", new JSONObject(), null, 0, false);
            group = Sync.SYNC_USER_DATA;
        }

        @Override
        public boolean isWaiting() {
            return !UserLoader.isAuthenticated() || queue.isEmpty() || !allowUserDataSync;
        }

        @Override
        public void onPostPublish(int statusCode) {
            if (statusCode < 0) return;
            Log.d("LOGTAG", "SYNCUSERDATATRANSPORT: " + statusCode);
        }

        @Override
        public void onReceive(JSONObject data) {
            try {
                int status = data.getInt("status");

                if (status != Sync.OK) {
                    Log.d("LOGTAG", "SYNCUSERDATATRANSPORT ERROR: " + data.toString());
                    NotificationsLoader.makeStatusNotification(
                            Sync.SYNC_USER_DATA_FAILED_EVENT,
                            true
                    );
                }

                // we don't update lastSync here !

                synchronized (queue) {
                    synchronized (request) {
                        while (request.length() > 0) {
                            Iterator<String> it = request.keys();
                            String key = it.next();
                            lastSync = Math.max(lastSync, DataLoader.getSyncTime(key));
                            request.remove(key);
                            queue.remove(key);
                        }
                    }
                }

                lastSync = Math.min(lastSync, System.currentTimeMillis());
                DataLoader.setWithoutSync("lastSyncUser", lastSync);

                Log.d("LOGTAG", "SYNCUSERDATATRANSPORT finished");
                Log.d("LOGTAG", "Current lastSyncUser is " + lastSync);

                NotificationsLoader.removeById(Sync.SYNC_USER_DATA_EVENT);
                NotificationsLoader.removeById(Sync.SYNC_USER_DATA_FAILED_EVENT);

            } catch (JSONException e) {
                e.printStackTrace();
                NotificationsLoader.makeStatusNotification(
                        Sync.SYNC_USER_DATA_FAILED_EVENT,
                        true
                );
            }
        }

        @Override
        public JSONObject getQuery() {
            NotificationsLoader.makeStatusNotification(Sync.SYNC_USER_DATA_EVENT, false);

            if (request.length() == 0 && queue.size() > 0) {
                try {
                    synchronized (queue) {
                        for (String option: queue) {
                            update(option);
                        }
                    }

                    JSONObject data = new JSONObject();
                    data.put("ops", request);
                    query.put("data", data);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return query;
        }

        public void update(String option) {
            synchronized (request) {
                try {
                    JSONArray load = new JSONArray();
                    load.put(DataLoader.getSyncTime(option));
                    load.put(DataLoader.get(option));
                    request.put(option, load);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
