package lastutf445.android.com.home2;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.MissingFormatArgumentException;
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
                if (i.getSyncing()) enableSyncModule(i);

                if (nodes.containsKey(i.getNodeSerial())) {
                    nodes.get(i.getNodeSerial()).addModule(i.getSerial());
                }
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
                        return super.getQuery();
                    }

                    @Override
                    public void onPublishCustomTrigger(int statusCode) {
                        super.onPublishCustomTrigger(statusCode);
                        Log.d("LOGTAG", "statusCode - " + statusCode);
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

                //Log.d("LOGTAG", "successfully installed " + i.getKey());

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

    public synchronized static void enableNodesSearching(final UniversalViewer uv, final View view) {
        try {

            JSONObject query = new JSONObject();
            query.put("nodesSearch", 1);
            query.put("broadcast", 1);


            Sync.publish(-3, new SyncProvider(-3, query) {
                @Override
                public void onPublishCustomTrigger(int statusCode) {
                    Log.d("LOGTAG", "try to search nodes, status code - " + statusCode);
                }
            });

            Sync.subscribe(-3, new SyncProvider(-3, query) {
                @Override
                public void onReceive(JSONObject data, int nodeSerial) {
                    Log.d("LOGTAG", "received nodeSerial - " + nodeSerial);
                    Log.d("LOGTAG", data.toString());

                    Message msg = MainActivity.getMainHandler().obtainMessage();
                    Bundle msgData = new Bundle();

                    msgData.putString("data", data.toString());
                    msg.setData(msgData);
                    msg.what = -3;

                    MainActivity.getMainHandler().sendMessage(msg);
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void disableNodesSearching() {
        Sync.unsubscribe(-3);
        Sync.unpublish(-3);
    }

    public static void onNodesSearchUpdate(Bundle data) {
        View view = MainActivity.getUVView();

        try {
            //JSONObject modules = data.getJSONObject("modules");
            JSONObject node = (new JSONObject(data.getString("data"))).getJSONObject("node");
            NodeOption nodeOption = new NodeOption(node);

            ArrayList<NodeOption> arrayList = new ArrayList<>();
            arrayList.add(nodeOption);

            RecyclerView recyclerView = view.findViewById(R.id.nodesSearchContent);
            NodesAdapter adapter = (NodesAdapter) recyclerView.getAdapter();
            adapter.addItems(arrayList);

            Log.d("LOGTAG", "recycler updated");

        } catch (JSONException e) {
            e.printStackTrace();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        view.invalidate();
    }
}
