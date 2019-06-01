package com.lastutf445.home2.loaders;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DataLoader {

    private static int DATABASE_VERSION = 24;
    private static SQLiteDatabase db;

    private static Context appContext;
    private static Resources appResources;
    private static final HashMap<String, Object> ops = new HashMap<>();
    private static final HashMap<String, Long> sync = new HashMap<>();

    static {
        // authentication
        ops.put("Session", null);
        ops.put("Username", null);
        ops.put("BasicAccount", false);
        ops.put("AESBytes", 16);
        // master server
        ops.put("MasterServer", false);
        ops.put("MasterServerAddress", null);
        ops.put("MasterServerPort", null);
        // proxy server
        ops.put("ExternalConnection", false);
        ops.put("ExternalAddress", null);
        ops.put("ExternalPort", null);
        // synchronization
        ops.put("SyncPersistentConnection", false);
        ops.put("SyncHomeNetwork", null);
        ops.put("lastSyncModules", 0);
        ops.put("lastSyncUser", 0);
        // sync behavior
        ops.put("SyncClientPort", 44501);
        ops.put("SyncPingAttempts", 3);
        ops.put("SyncPingInterval", 1000);
        ops.put("SyncDiscoveryPort", 44500);
        ops.put("SyncDiscoveryTimeout", 3);
    }

    public static void init(Context context, Resources resources) {
        appContext = context;
        appResources = resources;

        sync.clear();
        ops.clear();

        connect();
        load();
    }

    public static Context getAppContext() {
        return appContext;
    }

    public static Resources getAppResources() {
        return appResources;
    }

    private static void connect() {
        String path = appContext.getFilesDir().getAbsolutePath();
        String file = "app.db";

        db = SQLiteDatabase.openOrCreateDatabase(path + "/" + file, null);
        check();

        if (db.getVersion() < DATABASE_VERSION) {
            upgrade(db.getVersion(), DATABASE_VERSION);
        }
    }

    private static void check() {
        db.execSQL("CREATE TABLE IF NOT EXISTS core (id INTEGER PRIMARY KEY, options TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS modules (serial INTEGER PRIMARY KEY, type TEXT, ip TEXT, port INTEGER, title TEXT, ops TEXT, vals TEXT, syncing INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS dashboard (id INTEGER PRIMARY KEY, serial INTEGER, type TEXT, options TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS syncUserData (option TEXT PRIMARY KEY, time INTEGER)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS dashboard_serial ON dashboard (serial)");
    }

    private static void upgrade(int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS core");
        db.execSQL("DROP TABLE IF EXISTS nodes");
        db.execSQL("DROP TABLE IF EXISTS modules");
        db.execSQL("DROP TABLE IF EXISTS dashboard");
        db.setVersion(newVersion);
        check();
        save();
    }

    public static SQLiteDatabase getDb() {
        return db;
    }

    public static void kill() {
        db.close();
    }

    public static void load() {
        synchronized (ops) {
            SQLiteDatabase db = getDb();
            Cursor c = db.rawQuery("SELECT options FROM core WHERE id = 0", null);

            c.moveToFirst();
            if (c.isAfterLast()) return;

            try {
                JSONObject data = new JSONObject(
                        c.getString(c.getColumnIndex("options"))
                );

                Iterator<String> it = data.keys();

                while (it.hasNext()) {
                    String i = it.next();
                    ops.put(i, data.get(i));
                }

                Log.d("LOGTAG", data.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }

            c.close();
            loadSyncTable();
        }
    }

    private static void loadSyncTable() {
        synchronized (sync) {
            SQLiteDatabase db = getDb();
            Cursor c = db.rawQuery("SELECT * FROM syncUserData", null);

            c.moveToFirst();

            while (!c.isAfterLast()) {
                String option = c.getString(c.getColumnIndex("option"));
                long time = c.getLong(c.getColumnIndex("time"));
                sync.put(option, time);

                c.moveToNext();
            }

            c.close();

        }
    }

    public static void save() {
        synchronized (ops) {
            SQLiteDatabase db = getDb();
            JSONObject dump = new JSONObject();

            try {
                for (Map.Entry<String, Object> i: ops.entrySet()) {
                    dump.put(i.getKey(), i.getValue());
                }

                Log.d("LOGTAG", dump.toString());

                ContentValues cv = new ContentValues();
                cv.put("options", dump.toString());
                cv.put("id", 0);

                db.replace("core", null, cv);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            saveSyncTable();
        }
    }

    private static void saveSyncTable() {
        synchronized (sync) {
            SQLiteDatabase db = getDb();
            for (Map.Entry<String, Long> i: sync.entrySet()) {
                ContentValues cv = new ContentValues();

                cv.put("option", i.getKey());
                cv.put("time", i.getValue());

                db.replace("syncUserData", null, cv);
            }
        }
    }

    public static void flushTables() {
        db.execSQL("DROP TABLE IF EXISTS core");
        db.execSQL("DROP TABLE IF EXISTS syncUserData");
        kill();
        init(appContext, appResources);
    }

    public static Set<String> getKeys() {
        return ops.keySet();
    }

    public static void set(String key, Object value) {
        UserLoader.addToSyncUserDataQueue(key);
        sync.put(key, System.currentTimeMillis());
        setWithoutSync(key, value);
    }

    public static void setWithoutSync(String key, Object value) {
        synchronized (ops) {
            ops.put(key, value);
            Log.d("LOGTAG", "we are trying to set " + key + " by " + (value == null ? "null" : value.toString()));
        }
    }

    public static boolean setSynced(String key, Object value, long syncedAt) {
        synchronized (sync) {
            Object a = get(key);
            boolean isNull = (a == null || value == null);

            Log.d("LOGTAG", "SETSYNCED: " + key + " by " + (value == null ? "null" : value.toString()) + " " + syncedAt);
            Log.d("LOGTAG", getSyncTime(key) + " " + (isNull || a.getClass().getName().equals(value.getClass().getName())));
            if (getSyncTime(key) <= syncedAt && (isNull || a.getClass().getName().equals(value.getClass().getName()))) {
                UserLoader.removeFromSyncUserDataQueue(key);
                sync.put(key, syncedAt);
                setWithoutSync(key, value);
            }

            return true;
        }
    }

    public static boolean merge(@NonNull JSONObject ops) {
        try {
            Iterator<String> it = ops.keys();

            while (it.hasNext()) {
                String key = it.next();
                JSONArray pair = ops.getJSONArray(key);
                long time = pair.optLong(0, 0);
                Object value = pair.opt(1);

                time = Math.min(System.currentTimeMillis(), time);
                DataLoader.setSynced(key, value, time);
            }

            return true;

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static long getSyncTime(String key) {
        synchronized (sync) {
            return sync.containsKey(key) ? sync.get(key) : 0;
        }
    }

    public static Object get(String option) {
        synchronized (ops) {
            return ops.get(option);
        }
    }

    public static String getString(String option, String std) {
        synchronized (ops) {
            Object res = get(option);
            return res != null ? String.valueOf(res) : std;
        }
    }

    public static int getInt(String option, int std) {
        synchronized (ops) {
            String res = getString(option, null);
            return res != null ? Integer.valueOf(res) : std;
        }
    }

    public static long getLong(String option, long std) {
        synchronized (ops) {
            String res = getString(option, null);
            return res != null ? Long.valueOf(res) : std;
        }
    }

    public static boolean getBoolean(String option, boolean std) {
        synchronized (ops) {
            String res = getString(option, null);
            return res != null ? Boolean.valueOf(res) : std;
        }
    }

}
