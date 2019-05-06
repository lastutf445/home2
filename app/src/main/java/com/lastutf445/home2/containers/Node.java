package com.lastutf445.home2.containers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NodesLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class Node {

    private final SparseArray<Module> syncing = new SparseArray<>();
    private final SparseArray<Module> idle = new SparseArray<>();
    private NodeUpdater updater;
    private boolean syncTainted;

    private String title;
    private InetAddress ip;
    private int serial, port;

    public Node(int serial, String ip, int port, String title) throws Exception {
        configure(serial, ip, port, title);
    }

    public Node(@NonNull Node node, boolean rescan) throws Exception {
        configure(node.getSerial(), node.getIp() == null ? null : node.getIp().getHostAddress(), node.getPort(), node.getTitle());

        if (rescan) {
            SparseArray<Module> modules = ModulesLoader.getModules();

            for (int i = 0; i < modules.size(); ++i) {
                Module module = modules.valueAt(i);

                if (module != null && module.getNode() == serial) {
                    NodesLoader.onModuleLinkChanged(module, true);
                }
            }
        }
    }

    private void configure(int serial, String ip, int port, String title) throws Exception {
        this.ip = InetAddress.getByName(ip);
        this.syncTainted = true;
        this.serial = serial;
        this.title = title;
        this.port = port;

        this.updater = new NodeUpdater(
                serial, "update", new JSONObject(), this.ip, port
        );

        updater.setGroup(Sync.SYNC_DASHBOARD);
    }

    public int getSerial() {
        return serial;
    }

    @Nullable
    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @NonNull
    public String getTitle() {
        return title == null ? DataLoader.getAppResources().getString(R.string.unknownNode) : title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @NonNull
    public SparseArray<Module> getModules() {
        SparseArray<Module> modules = idle.clone();

        for (int i = 0; i < syncing.size(); ++i) {
            Module module = syncing.valueAt(i);
            modules.put(module.getSerial(), module);
        }

        return modules;
    }

    @NonNull
    public SparseArray<Module> getSyncing() {
        return syncing;
    }

    public int getModulesCount() {
        return syncing.size() + idle.size();
    }

    public int getSyncingCount() {
        return syncing.size();
    }

    public void addModule(@NonNull Module module) {
        if (module.getSyncing()) {
            syncing.put(module.getSerial(), module);
            Sync.addSyncProvider(updater);
            syncTainted = true;
        }
        else {
            idle.put(module.getSerial(), module);
        }
    }

    public void removeModule(@NonNull Module module) {
        if (syncing.get(module.getSerial()) != null) {
            syncing.remove(module.getSerial());
            syncTainted = true;
        }

        idle.remove(module.getSerial());
    }

    public void updateSyncing(@NonNull Module module) {
        if (syncing.get(module.getSerial()) != null && !module.getSyncing()) {
            idle.put(module.getSerial(), module);
            syncing.remove(module.getSerial());
            syncTainted = true;
        }

        else if (idle.get(module.getSerial()) != null && module.getSyncing()) {
            syncing.put(module.getSerial(), module);
            idle.remove(module.getSerial());
            Sync.addSyncProvider(updater);
            syncTainted = true;
        }
    }

    public void wipeSyncing() {
        SparseArray<Module> buf = syncing.clone();

        for (int i = 0; i < buf.size(); ++i) {
            Module module = buf.valueAt(i);
            module.setSyncing(false);
        }

        syncTainted = true;
    }

    public void reloadSyncing() {
        SparseArray<Module> buf = idle.clone();

        for (int i = 0; i < buf.size(); ++i) {
            Module module = buf.valueAt(i);
            updateSyncing(module);
        }

        syncTainted = true;
    }

    private class NodeUpdater extends SyncProvider {
        public NodeUpdater(int source, String act, JSONObject data, InetAddress ip, int port) throws JSONException {
            super(source, act, data, ip, port);
        }

        @Override
        public void onPostPublish(int statusCode) {
            Log.d("LOGTAG", "statusCode - " + statusCode);
        }

        @Override
        public void onReceive(JSONObject data) {
            Iterator<String> it = data.keys();

            while (it.hasNext()) {
                String s = it.next();

                try {
                    int serial = Integer.valueOf(s);
                    if (syncing.get(serial) == null) continue;
                    Module module = ModulesLoader.getModule(serial);
                    if (module == null) continue;

                    module.updateState(data.getJSONObject(s));

                } catch (NumberFormatException e) {
                    //e.printStackTrace();

                } catch (JSONException e) {
                    //e.printStackTrace();
                }
            }
        }

        @Override
        public JSONObject getQuery() {
            if (syncTainted) {
                try {
                    JSONObject data = new JSONObject();
                    JSONArray modules = new JSONArray();

                    for (int i = 0; i < syncing.size(); ++i) {
                        modules.put(syncing.valueAt(i).getSerial());
                    }

                    data.put("modules", modules);
                    query.put("data", data);
                    syncTainted = false;

                    if (getSyncingCount() == 0) {
                        Sync.removeSyncProvider(serial);
                    }

                } catch (JSONException e) {
                    //e.printStackTrace();
                    Log.d("LOGTAG", "can't update node syncProvider");
                }
            }

            return query;
        }
    }
}
