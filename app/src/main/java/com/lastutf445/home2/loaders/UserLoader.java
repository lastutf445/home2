package com.lastutf445.home2.loaders;

import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public final class UserLoader {

    private UserDataSync userDataSync;

    public void init() {
        try {
            userDataSync = new UserDataSync();

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

    public synchronized static boolean isAuthenticated() {
        return DataLoader.getString("Session", null) != null || DataLoader.getBoolean("BasicAccount", false);
    }

    @NonNull
    public synchronized static String getUsername() {
        Resources res = DataLoader.getAppResources();
        String name = DataLoader.getString("Username", res.getString(R.string.usernameError));
        return isAuthenticated() ? name : res.getString(R.string.notAuthenticated);
    }

    public synchronized static void getPublicKey(@NonNull GetPublicKey.Callback callback) {
        Sync.removeSyncProvider(Sync.PROVIDER_GET_PUBLIC_KEY);

        try {
            Sync.addSyncProvider(new GetPublicKey(callback));

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

    public synchronized static void startAuth(@NonNull String login, @NonNull String password, @NonNull Handler handler) {
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

    public synchronized static void authBasic() {
        DataLoader.set("BasicAccount", true);

        DataLoader.set(
                "Username",
                DataLoader.getAppResources().getString(R.string.usernameDefault)
        );

        DataLoader.save();
    }

    public synchronized static void logout() {
        DataLoader.set("Session", null);
        DataLoader.set("Username", null);
        DataLoader.set("BasicAccount", null);
        DataLoader.set("AESKey", null);
        DataLoader.save();
        CryptoLoader.init();
    }

    @Nullable
    public synchronized static String getSession() {
        return DataLoader.getString("Session", null);
    }

    @Nullable
    public synchronized static String getAESKey() {
        return DataLoader.getString("AESKey", null);
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
                    DataLoader.set("Username", DataLoader.getAppResources().getString(R.string.usernameDefault));
                    DataLoader.set("Session", data.getString("session"));
                    DataLoader.set("AESKey", key);
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
        private boolean syncTainted;
        private long lastSync;

        public UserDataSync() throws JSONException {
            super(Sync.SYNC_USER_DATA, "sync", new JSONObject(), null, 0);
            lastSync = DataLoader.getLong("lastSyncUser", 0);
            group = Sync.SYNC_USER_DATA;
        }

        @Override
        public boolean isWaiting() {
            return !syncTainted;
        }

        @Override
        public void onPostPublish(int statusCode) {
            super.onPostPublish(statusCode);
        }

        @Override
        public void onReceive(JSONObject data) {
            super.onReceive(data);
        }

        @Override
        public JSONObject getQuery() {
            return super.getQuery();
        }
    }
}
