package lastutf445.android.com.home2;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.SparseArray;

import org.w3c.dom.Node;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class Database {

    private static int DATABASE_VERSION = 2;
    private static String DATABASE_NAME = "app.db";
    private static String DATABASE_PATH;

    private static SQLiteDatabase db;

    public static void init() {
        DATABASE_PATH = MainActivity.getAppContext().getFilesDir().getAbsolutePath();
        db = SQLiteDatabase.openOrCreateDatabase(DATABASE_PATH + "/" + DATABASE_NAME, null);
        onCreate();

        if (db.getVersion() > DATABASE_VERSION) {
            upgrade(DATABASE_VERSION, db.getVersion());
        }
    }

    public static void kill() {
        db.close();
    }

    public static void onCreate() {
        db.execSQL("CREATE TABLE IF NOT EXISTS core (option TEXT PRIMARY KEY, value TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS nodes (id INTEGER PRIMARY KEY AUTOINCREMENT, ip TEXT, title TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS modules (serial INTEGER PRIMARY KEY, type TEXT, nodeId INTEGER, title TEXT, state TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS dashboard (id INTEGER PRIMARY KEY, type TEXT, modules string)");
    }

    public static void upgrade(int oldVersion, int newVersion) {
        // TODO: you know
        db.execSQL("DROP TABLE IF EXISTS core");
        db.execSQL("DROP TABLE IF EXISTS nodes");
        db.execSQL("DROP TABLE IF EXISTS modules");
        db.execSQL("DROP TABLE IF EXISTS dashboard");
        db.setVersion(DATABASE_VERSION);
        onCreate();

        Data.recordOptions();
    }

    public static HashMap<String, Object> getOptions() {
        Cursor cursor = db.rawQuery("SELECT * FROM core", null);

        HashMap<String, Object> ops = new HashMap<>();
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            ops.put(cursor.getString(cursor.getColumnIndex("option")),
                    cursor.getString(cursor.getColumnIndex("value"))
            );

            cursor.moveToNext();
        }

        cursor.close();
        return ops;
    }

    public static void setOptions(HashMap<String, Object> d) {
        String nil = null;

        for (Map.Entry<String, Object> i: d.entrySet()) {
            ContentValues cv = new ContentValues();

            cv.put("option", i.getKey());
            cv.put("value", i.getValue() != null ? String.valueOf(i.getValue()) : nil);

            db.replace("core", null, cv);
        }
    }

    public static HashMap<Integer, NodeOption> getNodes() {
        Cursor cursor = db.rawQuery("SELECT * FROM nodes", null);

        HashMap<Integer, NodeOption> ops = new HashMap<>();
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {

            try {
                ops.put(cursor.getInt(cursor.getColumnIndex("id")),
                        new NodeOption(
                                cursor.getInt(cursor.getColumnIndex("id")),
                                cursor.getString(cursor.getColumnIndex("ip")),
                                cursor.getString(cursor.getColumnIndex("title"))
                        )
                );

            } catch (UnknownHostException e) {
                e.printStackTrace();

            } catch (SecurityException e) {
                e.printStackTrace();
            }

            cursor.moveToNext();
        }

        try {
            ops.put(0, new NodeOption(0, "192.168.0.102", "title"));

        } catch(Exception e) {
            e.printStackTrace();
        }

        cursor.close();
        return ops;
    }

    public static HashMap<Integer, ModuleOption> getModules() {
        Cursor cursor = db.rawQuery("SELECT * FROM modules", null);

        HashMap<Integer, ModuleOption> ops = new HashMap<>();
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {

            ops.put(cursor.getInt(cursor.getColumnIndex("serial")),
                    new ModuleOption(
                    cursor.getInt(cursor.getColumnIndex("serial")),
                    cursor.getString(cursor.getColumnIndex("type")),
                    cursor.getInt(cursor.getColumnIndex("nodeId")),
                    cursor.getString(cursor.getColumnIndex("title")),
                    cursor.getString(cursor.getColumnIndex("state"))
            ));

            cursor.moveToNext();
        }

        ops.put(0, new ModuleOption(0, "temp", 0, "Temperature inside", "24"));
        ops.put(1, new ModuleOption(1, "temp", 0, "Temperature outside", "-3"));
        ops.put(2, new ModuleOption(2, "humidity", 0, "Humidity inside", "57"));
        ops.put(3, new ModuleOption(3, "temp", 0, "Temperature inside near gate", "-3"));
        ops.put(4, new ModuleOption(4, "temp", 0, "Temperature outside near gate", "-4"));
        ops.put(5, new ModuleOption(5, "temp", 0, "Temperature outside the garden", "-5"));

        cursor.close();
        return ops;
    }

    public static ArrayList<DashboardOption> getDashboard() {
        Cursor cursor = db.rawQuery("SELECT * FROM dashboard", null);

        ArrayList<DashboardOption> ops = new ArrayList<>();
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            ArrayList<Integer> modules = new ArrayList<>();
            // TODO: serialization

            ops.add(new DashboardOption(
                            cursor.getInt(cursor.getColumnIndex("id")),
                            cursor.getString(cursor.getColumnIndex("type")),
                            modules
                    ));

            cursor.moveToNext();
        }

        cursor.close();
        return ops;
    }
}
