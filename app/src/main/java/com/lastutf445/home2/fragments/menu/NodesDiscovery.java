package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.NodesAdapter;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.containers.Node;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class NodesDiscovery extends NavigationFragment {

    private Discoverer discoverer;

    private NodesAdapter adapter;
    private RecyclerView content;
    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.nodes_discovery, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        updater = new Updater(view);
        content = view.findViewById(R.id.nodesDiscoveryContent);
        content.setLayoutManager(new LinearLayoutManager(DataLoader.getAppContext()));

        try {
            discoverer = new Discoverer(updater);
            discoverer.setBroadcast(true);

        } catch (JSONException e) {
            //e.printStackTrace();
            NotificationsLoader.makeToast("Unexpected error", true);
            getActivity().onBackPressed();
        }

        ((SwipeRefreshLayout) view.findViewById(R.id.nodesDiscoverySwipeRefreshLayout)).setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Sync.addSyncProvider(discoverer);
                    }
                }
        );

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = content.getChildLayoutPosition(v);
                Node node = adapter.getNode(pos);

                if (node != null) {
                    NodesImport nodesImport = new NodesImport();
                    nodesImport.setNode(node, pos);
                    FragmentsLoader.addChild(nodesImport, NodesDiscovery.this);
                }

            }
        };

        adapter = new NodesAdapter(
                getLayoutInflater(),
                c
        );

        updater.setAdapter(adapter);
        content.setAdapter(adapter);
    }

    @Override
    public void onDestroy() {
        Sync.removeSyncProvider(Sync.PROVIDER_DISCOVERER);
        super.onDestroy();
    }

    @Override
    public void onResult(Bundle data) {
        if (data.containsKey("updated")) {
            toParent.putBoolean("reload", true);
            reload();
        }
    }

    private static class Updater extends Handler {
        private WeakReference<NodesAdapter> weakAdapter;
        private WeakReference<View> weakView;

        Updater(@NonNull View view) {
            weakView = new WeakReference<>(view);
        }

        public void setAdapter(@NonNull NodesAdapter adapter) {
            this.weakAdapter = new WeakReference<>(adapter);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    stopRefreshing();
                    break;
                case 1:
                    pushNode(msg.getData());
                    break;
            }
        }

        private void stopRefreshing() {
            View view = weakView.get();
            if (view == null) return;
            ((SwipeRefreshLayout) view.findViewById(R.id.nodesDiscoverySwipeRefreshLayout)).setRefreshing(false);
        }

        private void pushNode(Bundle data) {
            NodesAdapter adapter = weakAdapter.get();
            if (adapter == null) return;

            try {
                int id = data.getInt("serial");
                String ip = data.getString("ip");
                int port = data.getInt("port");
                String title = data.getString("title");
                final int modules = data.getInt("modules");

                Node node = new Node(id, ip, port, title) {
                    @Override
                    public int getModulesCount() {
                        return modules;
                    }
                };

                adapter.pushData(node);

            } catch (Exception e) {
                e.printStackTrace();

            }
        }
    }

    private static class Discoverer extends SyncProvider {
        private int attempts = 0;
        private Handler handler;

        Discoverer(@NonNull Handler handler) throws JSONException {
            super(
                    Sync.PROVIDER_DISCOVERER,
                    "discovery",
                    new JSONObject(),
                    null,
                    DataLoader.getInt("SyncDiscoveryPort", Sync.DEFAULT_PORT)
            );

            this.handler = handler;
        }

        @Override
        public JSONObject getQuery() {
            if (++attempts >= DataLoader.getInt("SyncDiscoveryAttempts", 5)) stop();
            return super.getQuery();
        }

        @Override
        public void onReceive(JSONObject data) {
            Log.d("LOGTAG", data.toString());

            try {
                Message msg = handler.obtainMessage(1);
                Bundle msgData = new Bundle();

                msgData.putInt("serial", data.getInt("serial"));
                msgData.putString("ip", data.getString("ip"));
                msgData.putInt("port", data.getInt("port"));
                msgData.putString("title", data.getString("title"));
                msgData.putInt("modules", data.getInt("modules"));

                msg.setData(msgData);
                handler.sendMessage(msg);
                //stop();

            } catch (JSONException e) {
                //e.printStackTrace();
            }
        }

        private void stop() {
            handler.sendEmptyMessage(0);
            Sync.removeSyncProvider(source);
            attempts = 0;
        }
    }
}
