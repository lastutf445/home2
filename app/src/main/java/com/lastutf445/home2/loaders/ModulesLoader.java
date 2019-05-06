package com.lastutf445.home2.loaders;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.containers.Node;
import com.lastutf445.home2.fragments.menu.Sync;

import org.json.JSONException;

public class ModulesLoader {

    private static final int MODULES_SERIAL = 0;
    private static final int MODULES_TYPE = 1;
    private static final int MODULES_NODE = 2;
    private static final int MODULES_TITLE = 3;
    private static final int MODULES_OPTIONS = 4;
    private static final int MODULES_SYNCING = 5;

    private static final SparseArray<Module> modules = new SparseArray<>();

    public static void init() {
        Log.d("LOGTAG", "reloading moduels list...");
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
                int node = cursor.getInt(MODULES_NODE);
                String title = cursor.getString(MODULES_TITLE);
                String options = cursor.getString(MODULES_OPTIONS);
                int syncing = cursor.getInt(MODULES_SYNCING);

                Module module = new Module(serial, type, node, title, options, syncing);
                NodesLoader.onModuleLinkChanged(module, true);
                modules.put(serial, module);

            } catch (JSONException e) {
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

        SQLiteDatabase db = DataLoader.getDb();
        ContentValues cv = new ContentValues();

        cv.put("serial", module.getSerial());
        cv.put("type", module.getType());
        cv.put("node", module.getNode());
        cv.put("title", module.getTitle());
        cv.put("options", module.getOps().toString());
        cv.put("syncing", module.getSyncing() ? 1 : 0);

        try {
            db.replaceOrThrow("modules", null, cv);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        NodesLoader.onModuleLinkChanged(module, true);
        modules.put(module.getSerial(), module);
        return true;
    }

    public static void removeModule(@NonNull Module module) {
        SQLiteDatabase db = DataLoader.getDb();

        String[] args = {
                String.valueOf(module.getSerial())
        };

        db.delete("modules", "serial=?", args);
        NodesLoader.onModuleLinkChanged(module, false);
        modules.remove(module.getSerial());
    }
}
