package com.lastutf445.home2.loaders;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.containers.Node;
import com.lastutf445.home2.fragments.menu.ExternalAddress;
import com.lastutf445.home2.fragments.menu.Sync;

import java.util.concurrent.ExecutionException;

public class NodesLoader {

    private static final int NODES_SERIAL = 0;
    private static final int NODES_IP = 1;
    private static final int NODES_PORT = 2;
    private static final int NODES_TITLE = 3;

    private static final SparseArray<Node> nodes = new SparseArray<>();

    public static void init() {
        Log.d("LOGTAG", "reloading nodes list...");
        load();
    }

    public static void load() {
        SQLiteDatabase db = DataLoader.getDb();
        Cursor cursor = db.rawQuery("SELECT * FROM nodes", null);
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            int serial = cursor.getInt(NODES_SERIAL);
            String ip = cursor.getString(NODES_IP);
            int port = cursor.getInt(NODES_PORT);
            String title = cursor.getString(NODES_TITLE);

            try {
                nodes.put(
                        serial,
                        new Node(serial, ip, port, title)
                );

            } catch (Exception e) {
                //e.printStackTrace();
                Log.d("LOGTAG", "error in trying to init node " + serial);
            }

            cursor.moveToNext();
        }

        cursor.close();
    }

    @Nullable
    public static Node getNode(int serial) {
        return nodes.get(serial);
    }

    @NonNull
    public static SparseArray<Node> getNodes() {
        return nodes;
    }

    public static boolean addNode(@NonNull Node node, boolean override) {
        if (nodes.get(node.getSerial()) != null && !override) return false;

        SQLiteDatabase db = DataLoader.getDb();
        ContentValues cv = new ContentValues();

        cv.put("serial", node.getSerial());
        cv.put("ip", node.getIp() != null ? node.getIp().getHostAddress() : null);
        cv.put("port", node.getPort());
        cv.put("title", node.getTitle());

        try {
            db.replaceOrThrow("nodes", null, cv);

        } catch (SQLiteException e) {
            //e.printStackTrace();
            return false;
        }

        nodes.put(node.getSerial(), node);
        return true;
    }

    public static void removeNode(@NonNull Node node) {
        SQLiteDatabase db = DataLoader.getDb();

        String[] args = new String[] {
                String.valueOf(node.getSerial())
        };

        db.delete("nodes", "serial=?", args);
        nodes.remove(node.getSerial());
        node.wipeSyncing();
    }

    public static void onModuleLinkChanged(@NonNull Module module, boolean linked) {
        Node node = getNode(module.getNode());

        if (node != null) {
            if (linked) {
                WidgetsLoader.onModuleLinkChanged(module, true);
                node.addModule(module);
            }
            else {
                module.getOps().remove("value");
                WidgetsLoader.onModuleLinkChanged(module, false);
                node.removeModule(module);
            }
        }
    }

    public static void onModuleSyncingChanged(@NonNull Module module) {
        Node node = getNode(module.getNode());

        if (node != null) {
            node.updateSyncing(module);
        }
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
