package lastutf445.android.com.home2;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.SparseArray;

import java.util.HashMap;

public final class Modules {

    private static HashMap<Integer, ModuleOption> modules;


    public static boolean refreshModules(Context context) {

        try {
            modules = Database.getModules();
            return true;

        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static ModuleOption getModule(int serial) {
        return modules.get(serial);
    }

    public static HashMap<Integer, ModuleOption> getModules() {
        return modules;
    }
}
