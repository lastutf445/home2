package com.lastutf445.home2.loaders;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.lastutf445.home2.configure.LightRGB;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.configure.Socket;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.Configure;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.Iterator;

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
    private static ModuleUpdater updater;

    public static void init() {
        Log.d("LOGTAG", "loading modules");

        try {
            updater = new ModuleUpdater();
            updater.setGroup(Sync.SYNC_DASHBOARD);

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

                if (module.getSyncing()) onModuleSyncingChanged(module, true);
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

        if (oldModule != null) {
            removeModule(oldModule);
        }

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

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (module.getSyncing()) {
            onModuleSyncingChanged(module, true);
        }

        modules.put(module.getSerial(), module);
        WidgetsLoader.onModuleLinkChanged(module, true);
        return true;
    }

    public static boolean saveState(@NonNull Module module) {
        module.set("lastUpdated", System.currentTimeMillis());

        SQLiteDatabase db = DataLoader.getDb();
        ContentValues cv = new ContentValues();

        cv.put("serial", module.getSerial());
        cv.put("type", module.getType());
        cv.put("ip", module.getIp().toString());
        cv.put("port", module.getPort());
        cv.put("title", module.getTitle());
        cv.put("ops", module.getOps().toString());
        cv.put("vals", module.getVals().toString());
        cv.put("syncing", module.getSyncing() ? 1 : 0);

        try {
            db.replaceOrThrow("modules", null, cv);
            return true;

        } catch (SQLiteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void removeModule(@NonNull Module module) {
        SQLiteDatabase db = DataLoader.getDb();

        String[] args = {
                String.valueOf(module.getSerial())
        };

        db.delete("modules", "serial=?", args);

        if (module.getSyncing()) {
            onModuleSyncingChanged(module, false);
        }

        module.setOps(new JSONObject());
        WidgetsLoader.onModuleLinkChanged(module, false);
        modules.remove(module.getSerial());
    }

    public static void onModuleSyncingChanged(@NonNull Module module, boolean syncing) {
        if (updater != null) {
            updater.onModuleSyncingChanged(module, syncing);
        }
    }

    public static boolean hasSpecial(@NonNull Module module) {
        switch (module.getType()) {
            case "lightrgb":
            case "socket":
                return true;
            default:
                return false;
        }
    }

    public static boolean callSpecial(int id, @NonNull Module module, @Nullable NavigationFragment base) {
        if (id != 1 && id != 2 || base == null) return false;
        Configure child = null;

        switch (module.getType()) {
            case "lightrgb":
                child = new LightRGB();
                break;
            case "socket":
                child = new Socket();
                break;
        }

        if (child == null) return false;

        child.setModule(module);
        child.setConnectorId(id);
        FragmentsLoader.addChild(child, base);
        return true;
    }

    private static class ModuleUpdater extends SyncProvider {
        private boolean waiting;

        public ModuleUpdater() throws JSONException {
            super(Sync.PROVIDER_DASHBOARD, "update", new JSONObject(), null, 0);
            waiting = false;
        }

        @Override
        public boolean isWaiting() {
            if (waiting) return true;
            waiting = true;
            return false;
        }

        public void onModuleSyncingChanged(@NonNull Module module, boolean syncing) {

        }
/*
        @Override
        public void onPostPublish(int statusCode) {
            Log.d("LOGTAG", "statusCode - " + statusCode);
        }

        @Override
        public void onReceive(JSONObject data) {
            Iterator<String> it = data.keys();

            while (it.hasNext()) {
                String s = it.next();

                try {
                    int serial = Integer.valueOf(s);
                    if (syncing.get(serial) == null) continue;
                    Module module = ModulesLoader.getModule(serial);
                    if (module == null) continue;

                    JSONObject state = data.getJSONObject(s);
                    if (state.isNull("type") && state.isNull("ops")) module.wipe();
                    module.mergeStates(state.getString("type"), state.getJSONObject("ops"));

                } catch (NumberFormatException e) {
                    //e.printStackTrace();

                } catch (JSONException e) {
                    //e.printStackTrace();
                }
            }
        }

        @Override
        public JSONObject getQuery() {
            if (syncTainted) {
                try {
                    JSONObject data = new JSONObject();
                    JSONArray modules = new JSONArray();

                    for (int i = 0; i < syncing.size(); ++i) {
                        modules.put(syncing.valueAt(i).getSerial());
                    }

                    data.put("modules", modules);
                    query.put("data", data);
                    syncTainted = false;

                    if (getSyncingCount() == 0) {
                        Sync.removeSyncProvider(serial);
                    }

                } catch (JSONException e) {
                    //e.printStackTrace();
                    Log.d("LOGTAG", "can't update node syncProvider");
                }
            }

            return query;
        }*/
    }

    public static class SyncSwitch implements Runnable {
        @Override
        public void run() {
            boolean state = DataLoader.getBoolean("SyncDashboard", true);
            DataLoader.set("SyncDashboard", !state);

            // TODO: save optimization
            DataLoader.save();
        }
    }
}
