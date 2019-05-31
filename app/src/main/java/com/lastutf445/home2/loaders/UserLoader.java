package com.lastutf445.home2.loaders;

import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.lastutf445.home2.R;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public final class UserLoader {

    private static UserDataSyncStarter userDataSyncStarter;
    private static UserDataSync userDataSync;
    private static boolean allowUserDataSync;
    private static long lastSync;

    public static void init() {
        lastSync = DataLoader.getLong("lastSyncUser", 0);

        try {
            userDataSync = new UserDataSync();
            userDataSyncStarter = new UserDataSyncStarter();
            Sync.addSyncProvider(userDataSync);
            Sync.addSyncProvider(userDataSyncStarter);

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

    public static boolean isAuthenticated() {
        return DataLoader.getString("Session", null) != null || DataLoader.getBoolean("BasicAccount", false);
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
            Sync.addSyncProvider(new GetPublicKey(callback));

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

    public static void startAuth(@NonNull String login, @NonNull String password, @NonNull Handler handler) {
        Authenticator auth = new Authenticator(
                login,
                password,
                handler
        );

        //Sender.killConnection();

        if (!CryptoLoader.isPublicKeyValid()) {
            handler.sendEmptyMessage(1);
            getPublicKey(auth);

        } else {
            auth.sendCredentials();
        }
    }

    public static void authBasic() {
        DataLoader.setWithoutSync("BasicAccount", true);

        DataLoader.setWithoutSync(
                "Username",
                DataLoader.getAppResources().getString(R.string.usernameDefault)
        );

        DataLoader.save();
    }

    public static void logout() {
        DataLoader.setWithoutSync("Session", null);
        DataLoader.setWithoutSync("Username", null);
        DataLoader.setWithoutSync("BasicAccount", null);
        DataLoader.setWithoutSync("AESKey", null);
        DataLoader.setWithoutSync("lastSyncModules", 0);
        DataLoader.setWithoutSync("lastSyncUser", 0);
        DataLoader.flushSyncTable();
        flushSyncUserDataQueue();
        DataLoader.save();
        CryptoLoader.init();
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
        if (userDataSyncStarter != null) {
            userDataSyncStarter.reconnected = true;
        }
    }

    public static void addToSyncUserDataQueue(String option) {
        if (userDataSync != null) {
            userDataSync.queue.add(option);
            if (userDataSync.request.has(option)) {
                userDataSync.update(option);
            }
        }
    }

    public static void removeFromSyncUserDataQueue(String option) {
        if (userDataSync != null) {
            userDataSync.queue.remove(option);
            userDataSync.request.remove(option);
        }
    }

    public static void flushSyncUserDataQueue() {
        if (userDataSync != null) {
            synchronized (userDataSync.queue) {
                synchronized (userDataSync.request) {
                    while (userDataSync.request.length() > 0) {
                        Iterator<String> it = userDataSync.request.keys();
                        userDataSync.request.remove(it.next());
                    }
                    userDataSync.queue.clear();
                }
            }
        }

    }

    private final static class GetPublicKey extends SyncProvider {
        private Callback callback;

        public GetPublicKey(@NonNull Callback callback) throws JSONException {
            super(
                    Sync.PROVIDER_GET_PUBLIC_KEY,
                    "getPublicKey",
                    new JSONObject(),
                    null,
                    Sync.DEFAULT_PORT
            );

            encrypted = false;
            this.callback = callback;
        }

        @Override
        public void onPostPublish(int statusCode) {
            callback.onPostPublish(statusCode);
        }

        @Override
        public void onReceive(JSONObject data) {
            if (!data.has("modulus") || !data.has("pubExp")) return;

            try {
                String modulus = data.getString("modulus");
                String pubExp = data.getString("pubExp");

                if (CryptoLoader.isPublicKeyValid(modulus, pubExp)) {
                    callback.onValid(modulus, pubExp);

                } else {
                    callback.onInvalid();
                }

                Sync.removeSyncProvider(Sync.PROVIDER_GET_PUBLIC_KEY);

            } catch (Exception e) {
                e.printStackTrace();
                callback.onInvalid();
            }
        }

        interface Callback {
            void onValid(@NonNull String modulus, @NonNull String pubExp);
            void onInvalid();
            void onPostPublish(int statusCode);
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
                    Sync.DEFAULT_PORT
            );

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
         */

        @Override
        public void onValid(@NonNull String modulus, @NonNull String pubExp) {
            try {
                CryptoLoader.setPublicKey(modulus, pubExp);
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

        public void sendCredentials() {
            Handler handler = weakHandler.get();
            stage = 7;

            if (handler != null) {
                handler.sendEmptyMessage(7);
            }

            JSONObject data = new JSONObject();
            key = CryptoLoader.createAESKey();

            try {
                data.put("login", login);
                data.put("password", password);
                data.put("key", key);

                Sync.addSyncProvider(new CredentialsProvider(data, this));

            } catch (JSONException e) {
                //e.printStackTrace();
                if (handler != null) {
                    handler.sendEmptyMessage(0);
                }
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

    private final static class UserDataSyncStarter extends SyncProvider {
        private boolean reconnected;

        public UserDataSyncStarter() throws JSONException {
            super(Sync.PROVIDER_USER_DATA_STARTER, "syncUserDataStarter", new JSONObject(), null, 0);
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

            try {
                int status = data.getInt("status");
                long lastUpdated = data.getLong("lastUpdated");

                if (status != Sync.UPDATE) {
                    Log.d("LOGTAG", "SYNCUSERDATASTARTER ERROR: " + data.toString());
                    NotificationsLoader.makeStatusNotification(
                            Sync.SYNC_USER_DATA_FAILED_EVENT,
                            true
                    );
                    return;
                }

                JSONObject ops = data.getJSONObject("ops");

                if (!DataLoader.merge(ops)) {
                    Log.d("LOGTAG", "SYNCUSERDATASTARTER ERROR: MERGE ERROR");
                    NotificationsLoader.makeStatusNotification(
                            Sync.SYNC_USER_DATA_FAILED_EVENT,
                            true
                    );
                    return;
                }

                lastSync = Math.max(lastSync, Math.min(System.currentTimeMillis(), lastUpdated));
                DataLoader.setWithoutSync("lastSyncUser", lastSync);
                DataLoader.save();

                Log.d("LOGTAG", "SyncUserDataStarter finished");
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

    private final static class UserDataSync extends SyncProvider {
        private final HashSet<String> queue = new HashSet<>();
        private final JSONObject request = new JSONObject();

        public UserDataSync() throws JSONException {
            super(Sync.PROVIDER_USER_DATA, "syncUserData", new JSONObject(), null, 0);
            group = Sync.SYNC_USER_DATA;
        }

        @Override
        public boolean isWaiting() {
            return !UserLoader.isAuthenticated() || queue.isEmpty() || !allowUserDataSync;
        }

        @Override
        public void onPostPublish(int statusCode) {
            if (statusCode < 0) return;
            Log.d("LOGTAG", "SYNCUSERDATA: " + statusCode);
        }

        @Override
        public void onReceive(JSONObject data) {
            try {
                int status = data.getInt("status");

                if (status != Sync.OK) {
                    Log.d("LOGTAG", "SYNCUSERDATA ERROR: " + data.toString());
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
                            request.remove(key);
                            queue.remove(key);
                        }
                    }
                }

                Log.d("LOGTAG", "SyncUserData finished");
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
