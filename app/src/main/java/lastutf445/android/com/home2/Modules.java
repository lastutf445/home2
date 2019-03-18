package lastutf445.android.com.home2;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Modules {

    private static HashMap<Integer, ModuleOption> modules;
    private static HashMap<Integer, NodeOption> nodes;

    private static HashMap<Integer, HashSet<Integer>> syncing = new HashMap<>();

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

            for (ModuleOption i: modules.values()) {
                enableSyncModule(i);
            }

            return true;

        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static void enableSyncModule(int id) {
        enableSyncModule(getModule(id));
    }

    private static void enableSyncModule(ModuleOption i) {
        if (i == null) return;

        int id = i.getSerial();
        int nodeId = i.getNodeSerial();

        if (!syncing.containsKey(nodeId)) {
            syncing.put(nodeId, new HashSet<Integer>());
        }

        syncing.get(nodeId).add(id);
    }

    public static void disableSyncModule(int id) {
        disableSyncModule(getModule(id));
    }

    private static void disableSyncModule(ModuleOption i) {
        if (i == null) return;

        int id = i.getSerial();
        int nodeId = i.getNodeSerial();

        if (syncing.containsKey(nodeId)) {
            syncing.get(nodeId).remove(id);

            if (syncing.get(nodeId).size() == 0) {
                syncing.remove(nodeId);
            }
        }
    }

    public static NodeOption getNode(int serial) {
        return nodes.get(serial);
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

    public synchronized static void setSync(boolean enable) {
        if (enable) enableSync();
        else disableSync();
    }

    private synchronized static void enableSync() {

        disableSync();
        Log.d("LOGTAG", "try to enable sync...");

        for (Map.Entry<Integer, HashSet<Integer>> i: syncing.entrySet()) {
            try {
                JSONObject query = new JSONObject();
                JSONArray modules = new JSONArray();

                for (Integer j : i.getValue()) {
                    modules.put(j);
                }

                query.put("address", nodes.get(i.getKey()).getIp());
                query.put("getState", modules);

                Sync.publish(i.getKey(), new SyncProvider(i.getKey(), query) {
                    @Override
                    public JSONObject getQuery() {
                        Log.d("LOGTAG", "try publish with id " + id);
                        return super.getQuery();
                    }

                    @Override
                    public void onPublishCustomTrigger(int statusCode) {
                        super.onPublishCustomTrigger(statusCode);
                        Log.d("LOGTAG", "statusCode - " + statusCode);


                        try {
                            Message msg = MainActivity.getMainHandler().obtainMessage();
                            Bundle msgData = new Bundle();

                            msgData.putString("msg", "statusCode - " + statusCode);
                            msg.setData(msgData);
                            msg.what = 999;

                            MainActivity.getMainHandler().sendMessage(msg);

                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }

                    }
                });
                Sync.subscribe(i.getKey(), new SyncProvider(i.getKey(), query) {
                    @Override
                    public void onReceive(JSONObject data, int nodeSerial) {
                        super.onReceive(data, id);

                        try {
                            JSONObject results = data.getJSONObject("states");
                            JSONArray modules = query.getJSONArray("getState");

                            for (int i = 0; i < modules.length(); ++i) {
                                int id = modules.getInt(i);
                                String state = results.getString(String.valueOf(id));

                                if (state != null) {
                                    Database.setModuleState(id, state);
                                    Message msg = MainActivity.getMainHandler().obtainMessage();
                                    Bundle msgData = new Bundle();

                                    msgData.putInt("serial", id);
                                    msgData.putString("state", state);
                                    msg.setData(msgData);
                                    msg.what = 0;

                                    MainActivity.getMainHandler().sendMessage(msg);
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                Log.d("LOGTAG", "successfully installed " + i.getKey());

            } catch (JSONException e) {
                e.printStackTrace();

            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        Sync.restart();
    }

    private synchronized static void disableSync() {
        for (Integer i: syncing.keySet()) {
            Sync.unsubscribe(i);
            Sync.unpublish(i);
        }
    }
}
