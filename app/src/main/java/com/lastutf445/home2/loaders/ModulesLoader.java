package com.lastutf445.home2.loaders;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.configure.Humidity;
import com.lastutf445.home2.configure.Light;
import com.lastutf445.home2.configure.Socket;
import com.lastutf445.home2.configure.Temperature;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.Configure;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashSet;

public class ModulesLoader {

    private static final int MODULES_SERIAL = 0;
    private static final int MODULES_TYPE = 1;
    private static final int MODULES_IP = 2;
    private static final int MODULES_PORT = 3;
    private static final int MODULES_TITLE = 4;
    private static final int MODULES_OPTIONS = 5;
    private static final int MODULES_VALUES = 6;
    private static final int MODULES_SYNCING = 7;

    private static final SparseArray<Module> modules = new SparseArray<>();
    private static SyncModulesStateRequest syncModulesState;
    private static WeakReference<Handler> moduleUpdater;
    private static int moduleUpdaterSerial;
    private static ModuleUpdater updater;

    public static void init() {
        Log.d("LOGTAG", "loading modules");

        try {
            updater = new ModuleUpdater();
            syncModulesState = new SyncModulesStateRequest();
            Sync.addSyncProvider(syncModulesState);
            Sync.addSyncProvider(updater);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        load();
    }

    public static void load() {
        SQLiteDatabase db = DataLoader.getDb();
        Cursor cursor = db.rawQuery("SELECT * FROM modules", null);
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            try {
                int serial = cursor.getInt(MODULES_SERIAL);
                String type = cursor.getString(MODULES_TYPE);
                String ip = cursor.getString(MODULES_IP);
                int port = cursor.getInt(MODULES_PORT);
                String title = cursor.getString(MODULES_TITLE);
                String ops = cursor.getString(MODULES_OPTIONS);
                String values = cursor.getString(MODULES_VALUES);
                int syncing = cursor.getInt(MODULES_SYNCING);

                Module module = new Module(
                        serial, type, ip, port, title,
                        new JSONObject(ops),
                        new JSONObject(values),
                        syncing > 0
                );

                //if (module.getSyncing()) onModuleSyncingChanged(module, true);
                WidgetsLoader.onModuleLinkChanged(module, true);
                modules.put(serial, module);

            } catch (Exception e) {
                e.printStackTrace();
            }

            cursor.moveToNext();
        }

        cursor.close();
    }

    @Nullable
    public static Module getModule(int serial) {
        return modules.get(serial);
    }

    @NonNull
    public static SparseArray<Module> getModules() {
        return modules;
    }

    public static boolean addModule(@NonNull Module module, boolean override) {
        if (modules.get(module.getSerial()) != null && !override) return false;

        Module oldModule = modules.get(module.getSerial());
        int syncing = 0;

        if (oldModule != null) {
            syncing = oldModule.getSyncing() ? 1 : 0;
            removeModule(oldModule);
        }

        module.set("lastUpdated", System.currentTimeMillis());
        module.setSyncing(syncing > 0);

        SQLiteDatabase db = DataLoader.getDb();
        ContentValues cv = new ContentValues();

        cv.put("serial", module.getSerial());
        cv.put("type", module.getType());
        cv.put("ip", module.getIp().getHostAddress());
        cv.put("port", module.getPort());
        cv.put("title", module.getTitle());
        cv.put("ops", module.getOps().toString());
        cv.put("vals", module.getVals().toString());
        cv.put("syncing", syncing);

        try {
            db.replaceOrThrow("modules", null, cv);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        modules.put(module.getSerial(), module);
        WidgetsLoader.onModuleLinkChanged(module, true);
        addToSyncModulesStateQueue(module);
        return true;
    }

    public static boolean saveState(@NonNull Module module) {
        module.set("lastUpdated", System.currentTimeMillis());

        SQLiteDatabase db = DataLoader.getDb();
        ContentValues cv = new ContentValues();

        cv.put("serial", module.getSerial());
        cv.put("type", module.getType());
        cv.put("ip", module.getIp().getHostAddress());
        cv.put("port", module.getPort());
        cv.put("title", module.getTitle());
        cv.put("ops", module.getOps().toString());
        cv.put("vals", module.getVals().toString());
        cv.put("syncing", module.getSyncing() ? 1 : 0);

        try {
            db.replaceOrThrow("modules", null, cv);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void removeModule(@NonNull Module module) {
        SQLiteDatabase db = DataLoader.getDb();
        module.wipe();

        String[] args = {
                String.valueOf(module.getSerial())
        };

        db.delete("modules", "serial=?", args);
        WidgetsLoader.onModuleLinkChanged(module, false);
        modules.remove(module.getSerial());
    }

    public static void onModuleSyncingChanged(@NonNull Module module, boolean syncing, Handler handler) {
        if (updater != null) {
            updater.setHandler(handler);
            updater.onModuleSyncingChanged(module, syncing);
        } else {
            Log.d("LOGTAG", "onModuleSyncingChanged error: updater is null");
        }
    }

    public static void resetUpdater() {
        if (updater != null) {
            updater.serial = -1;
        }
    }

    public static void setModuleUpdater(int serial, Handler handler) {
        moduleUpdater = new WeakReference<>(handler);
        moduleUpdaterSerial = serial;
    }

    private static void callModuleUpdater(int serial) {
        if (serial == moduleUpdaterSerial && moduleUpdater != null) {
            Handler handler = moduleUpdater.get();

            if (handler != null) {
                handler.sendEmptyMessage(-3);
            }
        }
    }

    public static boolean validateState(@NonNull Module module, @NonNull String type, @NonNull JSONObject ops, @NonNull JSONObject values) {
        if (!module.getType().equals(type)) return false;

        switch (module.getType()) {
            case "temperature":
                return Temperature.validateState(ops, values);
            case "humidity":
                return Humidity.validateState(ops, values);
            case "light":
                return Light.validateState(ops, values);
            case "socket":
                return Socket.validateState(ops, values);
            default:
                return false;
        }
    }

    public static void setSyncing(JSONArray request, @NonNull JSONArray subs) {
        synchronized (modules) {
            for (int i = 0; i < request.length(); ++i) {
                try {
                    Module module = modules.get(request.getInt(i));

                    if (module != null) {
                        module.setSyncing(false);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < subs.length(); ++i) {
                try {
                    Module module = modules.get(subs.getInt(i));

                    if (module != null) {
                        module.setSyncing(true);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean configure(int id, @NonNull Module module, @Nullable NavigationFragment base) {
        if (id != 1 && id != 2 || base == null) return false;
        Configure child = null;

        switch (module.getType()) {
            case "temperature":
                child = new Temperature();
                break;
            case "humidity":
                child = new Humidity();
                break;
            case "light":
                child = new Light();
                break;
            case "socket":
                child = new Socket();
                break;
        }

        if (child == null) {
            NotificationsLoader.makeToast(
                    DataLoader.getAppResources().getString(R.string.notificationUnsupportedTitle),
                    true
            );
        }

        child.setModule(module);
        child.setConnectorId(id);
        FragmentsLoader.addChild(child, base);
        return true;
    }

    public static void onReconnect() {
        addToSyncModulesStateQueue(modules);
    }

    public static void addToSyncModulesStateQueue(@NonNull SparseArray<Module> data) {
        if (syncModulesState != null) {
            synchronized (syncModulesState.queue) {
                for (int i = 0; i < data.size(); ++i) {
                    Module module = data.valueAt(i);
                    if (module != null) {
                        syncModulesState.queue.add(module.getSerial());
                    }
                }
            }
        }
    }

    public static void addToSyncModulesStateQueue(@NonNull Module module) {
        if (syncModulesState != null) {
            synchronized (syncModulesState.queue) {
                syncModulesState.queue.add(module.getSerial());
            }
        }
    }

    public static void wipeModules() {
        synchronized (modules) {
            for (int i = 0; i < modules.size(); ++i) {
                Module module = modules.valueAt(i);
                if (module != null) {
                    module.setSyncingWithoutSave(false);
                    module.wipe();
                    saveState(module);
                    WidgetsLoader.defaceWithoutRemoveFromFree(module);
                }
            }
        }

        if (syncModulesState != null) {
            syncModulesState.lastSync = 0;
        }
        if (updater != null) {
            updater.serial = -1;
        }
    }

    private static class ModuleUpdater extends SyncProvider {
        private volatile WeakReference<Handler> weakHandler;
        private volatile boolean subscribe, syncTainted;
        private volatile int serial;

        public ModuleUpdater() throws JSONException {
            super(Sync.PROVIDER_DASHBOARD, "nothing", new JSONObject(), null, 0);
            serial = -1;
        }

        public void setHandler(Handler handler) {
            weakHandler = new WeakReference<>(handler);
        }

        @Override
        public boolean isWaiting() {
            return !UserLoader.isAuthenticated() || serial == -1;
        }

        public void onModuleSyncingChanged(@NonNull Module module, boolean syncing) {
            subscribe = syncing;
            serial = module.getSerial();
            syncTainted = true;
            Log.d("LOGTAG", "onModulesSyncingChanged: serial set");
        }

        @Override
        public void onPostPublish(int statusCode) {
            if (statusCode < 0) return;

            Bundle data = new Bundle();
            int code = -1;

            switch (statusCode) {
                case 0:
                    code = R.string.unexpectedError;
                    break;
                case 2:
                    code = R.string.masterServerRequired;
                    break;
                case 3:
                    code = R.string.disconnected;
                    break;
                case 4:
                    code = R.string.encryptionError;
                    break;
            }

            if (code != -1) {
                serial = -1;
                Handler handler = weakHandler.get();
                if (handler == null) {
                    return;
                }

                data.putInt("status", code);
                Message msg = handler.obtainMessage(0);
                msg.setData(data);
                handler.sendMessage(msg);
            }
        }

        @Override
        public void onReceive(@NonNull JSONObject data) {
            try {
                int status = data.getInt("status");

                if (status == Sync.OK) {
                    Handler handler = weakHandler.get();
                    if (handler != null) handler.sendEmptyMessage(0);
                    //Module module = ModulesLoader.getModule(serial); todo: edit this weak approach
                    //if (module != null) module.setSyncing(!module.getSyncing());
                    serial = -1;
                    return;
                }

                if (status == Sync.SYNC_SUBSCRIBE) {
                    JSONObject msg = data.getJSONObject("msg");
                    Module module = modules.get(msg.getInt("serial"));
                    if (module != null) {
                        module.setSyncing(true);
                        callModuleUpdater(module.getSerial());
                    }
                    return;
                }

                if (status == Sync.SYNC_UNSUBSCRIBE) {
                    JSONObject msg = data.getJSONObject("msg");
                    Module module = modules.get(msg.getInt("serial"));
                    if (module != null) {
                        module.setSyncing(false);
                        callModuleUpdater(module.getSerial());
                    }
                    return;
                }

                switch (status) {
                    case Sync.ENCODE_ERROR:
                    case Sync.ENCRYPT_ERROR:
                    case Sync.MALFORMED_PACKET:
                    case Sync.UNEXPECTED_ERROR:
                    case Sync.MALFORMED_AES:
                    case Sync.UNAUTHORIZED:
                        Handler handler = weakHandler.get();
                        Bundle msgData = new Bundle();
                        msgData.putInt("status", status);
                        if (handler != null && serial != -1) {
                            Message msg = handler.obtainMessage(0);
                            msg.setData(msgData);
                            handler.sendMessage(msg);
                        }
                        serial = -1;
                        return;
                }

                if (status != Sync.UPDATE) {
                    Log.d("LOGTAG", "unsupported msg");
                    Log.d("LOGTAG", data.toString());
                    return;
                }

                JSONObject msg = data.getJSONObject("msg");

                int serial = msg.getInt("serial");
                String type = msg.getString("type");
                String act = msg.getString("act");

                if (act.equals("update")) {
                    JSONObject ops = msg.getJSONObject("ops");
                    JSONObject values = msg.getJSONObject("values");
                    Module module = ModulesLoader.getModule(serial);

                    if (module != null) {
                        module.mergeStates(type, ops, values);

                    } else {
                        Log.d("LOGTAG", "module doesn\'t exist");
                    }

                } else {
                    Log.d("LOGTAG", "unsupported act");
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public JSONObject getQuery() {
            Log.d("LOGTAG", "onModulesSyncingChanged: get query");
            if (syncTainted) {
                try {
                    if (subscribe) {
                        query.put("act", "subscribe");
                    } else {
                        query.put("act", "unsubscribe");
                    }

                    JSONObject data = new JSONObject();
                    data.put("serial", serial);
                    query.put("data", data);
                    syncTainted = false;

                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d("LOGTAG", "unknown error: ML subscribers list");
                }
            }

            return query;
        }
    }

    private static class SyncModulesStateRequest extends SyncProvider {
        private final HashSet<Integer> queue = new HashSet<>();
        private final JSONArray request = new JSONArray();
        private long lastSync;

        public SyncModulesStateRequest() throws JSONException {
            super(
                    Sync.PROVIDER_SYNC_MODULES_STATE,
                    "syncModulesState",
                    new JSONObject(),
                    null,
                    0
            );

            lastSync = DataLoader.getLong("lastSyncModules", 0);
            group = Sync.SYNC_MODULES_STATE;
        }

        @Override
        public boolean isWaiting() {
            return !UserLoader.isAuthenticated() || queue.isEmpty();
        }

        @Override
        public void onReceive(@NonNull JSONObject data) {
            try {
                int status = data.getInt("status");

                if (status == Sync.OK) {
                    long currentTime = System.currentTimeMillis();
                    lastSync = Math.max(data.getLong("lastUpdated"), lastSync);
                    setSyncing(request, data.getJSONArray("subs"));

                    synchronized (queue) {
                        while (request.length() > 0) {
                            queue.remove(request.getInt(0));
                            request.remove(0);
                        }

                        if (queue.isEmpty()) {
                            NotificationsLoader.removeById(Sync.SYNC_MODULES_STATE_EVENT);
                        }
                    }

                    NotificationsLoader.removeById(Sync.SYNC_MODULES_STATE_FAILED_EVENT);

                    DataLoader.setWithoutSync("lastSyncModules", Math.min(currentTime, lastSync));
                    DataLoader.save();
                    return;
                }

                switch (status) {
                    case Sync.ENCODE_ERROR:
                    case Sync.ENCRYPT_ERROR:
                    case Sync.MALFORMED_PACKET:
                    case Sync.UNEXPECTED_ERROR:
                        Log.d("LOGTAG", "syncModulesState error: " + status);
                        NotificationsLoader.makeStatusNotification(
                                Sync.SYNC_MODULES_STATE_FAILED_EVENT,
                                true
                        );
                }

            } catch (JSONException e) {
                e.printStackTrace();
                NotificationsLoader.makeStatusNotification(
                        Sync.SYNC_MODULES_STATE_FAILED_EVENT,
                        true
                );
            }
        }

        @Override
        public JSONObject getQuery() {
            NotificationsLoader.makeStatusNotification(Sync.SYNC_MODULES_STATE_EVENT, false);
            if (request.length() == 0 && queue.size() > 0) {
                try {
                    synchronized (queue) {
                        for (Integer serial: queue) {
                            request.put(serial);
                        }
                    }

                    JSONObject data = new JSONObject();
                    data.put("lastSync", lastSync);
                    data.put("serials", request);
                    query.put("data", data);

                } catch (JSONException e) {
                    //e.printStackTrace();
                }
            }

            return query;
        }
    }
}
