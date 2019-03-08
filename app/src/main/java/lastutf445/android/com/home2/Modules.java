package lastutf445.android.com.home2;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.SparseArray;

import org.w3c.dom.Node;

import java.util.HashMap;

public final class Modules {

    private static HashMap<Integer, ModuleOption> modules;
    private static HashMap<Integer, NodeOption> nodes;

    public static boolean refreshNodes() {

        try {
            nodes = Database.getNodes();
            return true;

        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean refreshModules() {

        try {
            modules = Database.getModules();
            return true;

        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static NodeOption getNode(int id) {
        return nodes.get(id);
    }

    public static HashMap<Integer, NodeOption> getNodes() {
        return nodes;
    }

    public static ModuleOption getModule(int serial) {
        return modules.get(serial);
    }

    public static HashMap<Integer, ModuleOption> getModules() {
        return modules;
    }
}
