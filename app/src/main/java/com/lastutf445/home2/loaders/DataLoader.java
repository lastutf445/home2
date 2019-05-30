package com.lastutf445.home2.loaders;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DataLoader {

    private static int DATABASE_VERSION = 20;
    private static SQLiteDatabase db;

    private static Context appContext;
    private static Resources appResources;
    private static final HashMap<String, Object> ops = new HashMap<>();

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
        ops.put("MasterServerSoTimeout", 2000);
        // proxy server
        ops.put("ExternalConnection", false);
        ops.put("ExternalAddress", null);
        ops.put("ExternalPort", null);
        // synchronization
        ops.put("SyncDashboard", false);
        ops.put("SyncMessages", false);
        //ops.put("SyncUserDataInterval", 5);
        //ops.put("SyncModulesStateOnReconnect", true);
        ops.put("SyncHomeNetwork", null);
        // sync behavior
        ops.put("SyncClientPort", 44501);
        ops.put("SyncPingAttempts", 3);
        ops.put("SyncPingInterval", 1000);
        ops.put("SyncDiscoveryPort", 44500);
        ops.put("SyncDiscoveryTimeout", 3);
        //ops.put("SyncMessagesInterval", 500);
    }

    public static void init(Context context, Resources resources) {
        appContext = context;
        appResources = resources;

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
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS dashboard_serial ON dashboard (serial)");
    }

    public static void merge(@NonNull JSONObject data) {
        Iterator<String> it = data.keys();

        while (it.hasNext()) {
            String key = it.next();
            if (!DataLoader.has(key)) continue;

            try {
                Object a = DataLoader.get(key);
                Object b = data.get(key);

                if (a == null || (b != null && a.getClass().getName().equals(b.getClass().getName()))) {
                    DataLoader.set(key, b);
                }

            } catch (JSONException e) {
                //e.printStackTrace();
            }
        }
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
        }
    }

    public static void save() {
        synchronized (ops) {
            SQLiteDatabase db = getDb();
            JSONObject dump = new JSONObject();

            try {
                for (Map.Entry<String, Object> i : ops.entrySet()) {
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
        }
    }

    public static void set(String key, Object value) {
        synchronized (ops) {
            ops.put(key, value);
        }
    }

    public static boolean has(String key) {
        synchronized (ops) {
            return ops.containsKey(key);
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
