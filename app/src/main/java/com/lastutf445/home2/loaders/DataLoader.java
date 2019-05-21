package com.lastutf445.home2.loaders;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DataLoader {

    private static int DATABASE_VERSION = 17;
    private static SQLiteDatabase db;

    private static Context appContext;
    private static Resources appResources;
    private static final HashMap<String, Object> ops = new HashMap<>();

    static {
        // authorization
        ops.put("Session", null);
        ops.put("Username", null);
        ops.put("BasicAccount", false);
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
        ops.put("SyncDashboard", true);
        ops.put("SyncMessages", false);
        ops.put("SyncNotifications", false);
        ops.put("SyncHomeNetwork", null);
        // sync behavior
        ops.put("SyncClientPort", 44501);
        ops.put("SyncPingAttempts", 3);
        ops.put("SyncPingInterval", 1000);
        ops.put("SyncDiscoveryPort", 44500);
        ops.put("SyncDiscoveryAttempts", 3);
        ops.put("SyncDashboardInterval", 5000);
        ops.put("SyncMessagesInterval", 500);
        ops.put("SyncNotificationsInterval", 2000);
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
        db.execSQL("CREATE TABLE IF NOT EXISTS nodes (serial INTEGER PRIMARY KEY, ip TEXT, port INTEGER, title TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS modules (serial INTEGER PRIMARY KEY, type TEXT, node INTEGER, title TEXT, options TEXT, syncing INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS dashboard (id INTEGER PRIMARY KEY, serial INTEGER, type TEXT, options TEXT)");
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
        }
    }

    public synchronized static void save() {
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

    public synchronized static void set(String key, Object value) {
        synchronized (ops) {
            ops.put(key, value);
        }
    }

    public synchronized static boolean has(String key) {
        synchronized (ops) {
            return ops.containsKey(key);
        }
    }

    public synchronized static Object get(String option) {
        synchronized (ops) {
            return ops.get(option);
        }
    }

    public synchronized static String getString(String option, String std) {
        synchronized (ops) {
            Object res = get(option);
            return res != null ? String.valueOf(res) : std;
        }
    }

    public synchronized static int getInt(String option, int std) {
        synchronized (ops) {
            String res = getString(option, null);
            return res != null ? Integer.valueOf(res) : std;
        }
    }

    public synchronized static boolean getBoolean(String option, boolean std) {
        synchronized (ops) {
            String res = getString(option, null);
            return res != null ? Boolean.valueOf(res) : std;
        }
    }
}
