package com.lastutf445.home2.loaders;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.lastutf445.home2.R;
import com.lastutf445.home2.fragments.menu.Auth;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;

public final class UserLoader {

    public synchronized static boolean isAuthenticated() {
        return DataLoader.getString("Session", null) != null || DataLoader.getBoolean("BasicAccount", false);
    }

    @NonNull
    public synchronized static String getUsername() {
        Resources res = DataLoader.getAppResources();
        String name = DataLoader.getString("Username", res.getString(R.string.usernameError));
        return isAuthenticated() ? name : res.getString(R.string.notAuthenticated);
    }

    public synchronized static void setOnlineUsername(@NonNull String s, @NonNull Handler handler) {
        try {
            JSONObject data = new JSONObject();
            data.put("Username", s);

            Sync.addSyncProvider(
                    new Editor(data, handler, false)
            );

        } catch (JSONException e) {
            //e.printStackTrace();
            handler.sendEmptyMessage(0);
        }
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
        CryptoLoader.init();
        DataLoader.save();
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

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean getUseMasterConnectionOnly() {
            return true;
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

        public Authenticator(@NonNull String login, @NonNull String password, @NonNull Handler handler) {
            weakHandler = new WeakReference<>(handler);
            this.password = password;
            this.login = login;
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

            if (handler != null) {
                handler.sendEmptyMessage(7);
            }

            //Log.d("LOGTAG", "lets auth");

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
                handler.sendEmptyMessage(statusCode);
            }
        }

        @Override
        public void onStatusReceived(JSONObject data) {
            Handler handler = weakHandler.get();

            try {
                String status = data.getString("status");

                if (handler != null) {
                    switch (status) {
                        case "unknown_user":
                            handler.sendEmptyMessage(8);
                            return;
                        case "unexpected_error":
                            handler.sendEmptyMessage(0);
                            return;
                    }
                }

                if (status.equals("ok")) {
                    if (data.has("username") && data.get("username") instanceof String) {
                        DataLoader.set("Username", data.getString("username"));

                    } else {
                        DataLoader.set("Username", DataLoader.getAppResources().getString(R.string.usernameDefault));
                    }

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

    private final static class Editor extends SyncProvider {
        private WeakReference<Handler> weakHandler;
        private boolean sessionParams;

        /**
         * HANDLER RETURN CODES:
         * 5 - done
         */

        public Editor(@NonNull JSONObject data, @NonNull Handler handler, boolean sessionParams) throws JSONException {
            super(
                    Sync.PROVIDER_EDITOR,
                    sessionParams ? "editSession" : "editProfile",
                    data,
                    null,
                    Sync.DEFAULT_PORT
            );

            weakHandler = new WeakReference<>(handler);
            this.sessionParams = sessionParams;
        }

        @Override
        public boolean getUseMasterConnectionOnly() {
            return true;
        }

        @Override
        public void onPostPublish(int statusCode) {
            Handler handler = weakHandler.get();

            if (handler != null) {
                handler.sendEmptyMessage(statusCode);
            }
        }

        @Override
        public void onReceive(JSONObject data) {
            Handler handler = weakHandler.get();
            Sync.removeSyncProvider(Sync.PROVIDER_EDITOR);
            //Log.d("LOGTAG", data.toString());

            try {
                String status = data.getString("status");
                if (status != null && status.equals("ok")) {
                    if (!sessionParams) {
                        JSONObject user = query.getJSONObject("data");
                        //Log.d("LOGTAG", user.toString());
                        Iterator<String> it = user.keys();

                        while (it.hasNext()) {
                            String key = it.next();
                            if (!DataLoader.has(key)) continue;

                            Object a = DataLoader.get(key);
                            Object b = user.get(key);

                            if (a == null || (b != null && a.getClass().getName().equals(b.getClass().getName()))) {
                                //Log.d("LOGTAG", "set: " + key);
                                DataLoader.set(key, b);
                            }
                        }

                        DataLoader.save();
                    }

                    handler.sendEmptyMessage(5);
                    return;
                }

            } catch (JSONException e) {
                //e.printStackTrace();
            }
        }
    }
}
