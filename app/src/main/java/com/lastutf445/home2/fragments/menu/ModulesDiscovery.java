package com.lastutf445.home2.fragments.menu;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.ModulesAdapter;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SimpleAnimator;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class ModulesDiscovery extends NavigationFragment {

    private View hint, noContent;
    private Discoverer discoverer;
    private ModulesAdapter adapter;
    private RecyclerView content;
    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.modules_discovery, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        updater = new Updater(view);
        content = view.findViewById(R.id.modulesDiscoveryContent);
        content.setLayoutManager(new LinearLayoutManager(DataLoader.getAppContext()));
        noContent = view.findViewById(R.id.modulesDiscoveryNoContent);
        hint = view.findViewById(R.id.modulesDiscoveryHint);

        try {
            discoverer = new Discoverer(updater);
            discoverer.setBroadcast(true);

        } catch (JSONException e) {
            //e.printStackTrace();
            NotificationsLoader.makeToast("Unexpected error", true);
            getActivity().onBackPressed();
        }

        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.modulesDiscoverySwipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Sync.addSyncProvider(discoverer);

                        if (noContent.getVisibility() != View.GONE) {
                            SimpleAnimator.fadeOut(noContent, 200, new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {}

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    noContent.setVisibility(View.GONE);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {}
                            });
                        }

                        adapter.deleteAll();

                        if (hint.getVisibility() != View.GONE) {
                            SimpleAnimator.fadeOut(hint, 200, new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {}

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    hint.setVisibility(View.GONE);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {}
                            });
                        }
                    }
                }
        );

        View.OnClickListener d = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = content.getChildLayoutPosition(v);
                Module module = adapter.getModule(pos);

                if (module == null) {
                    NotificationsLoader.makeToast("Unexpected error", true);
                    return;
                }

                addModule(module, pos);
            }
        };

        adapter = new ModulesAdapter(
                getLayoutInflater(),
                d
        );

        adapter.setShowSerials(true);
        updater.setAdapter(adapter);
        content.setAdapter(adapter);
    }

    private void addModule(final @NonNull Module module, final int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        Resources res = DataLoader.getAppResources();
        boolean match = false;

        if (ModulesLoader.getModule(module.getSerial()) != null) {
            builder.setMessage(res.getString(R.string.modulesAddModuleOverrideMessage));
            builder.setTitle(res.getString(R.string.modulesAddModuleOverrideTitle));
            match = true;

        } else {
            builder.setMessage(res.getString(R.string.modulesAddModuleMessage));
            builder.setTitle(res.getString(R.string.modulesAddModuleTitle));
        }

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(match ? R.string.override : R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!ModulesLoader.addModule(module, true)) {
                    NotificationsLoader.makeToast("Unexpected error", true);

                } else {
                    NotificationsLoader.makeToast("Added", true);
                    toParent.putInt("reload", pos);
                    adapter.update(pos);
                }
            }
        });

        builder.create().show();
    }

    private void merge() {
        // TODO: PROCESSING MODAL WINDOW, CODE WRAPPED INTO A NEW THREAD
/*
        try {
            if (NodesLoader.getNode(node.getSerial()) == null) {
                Node original = new Node(node, true);
                NodesLoader.addNode(original, false);
            }

        } catch (Exception e) {
            //e.printStackTrace();
        }

        if (NodesLoader.getNode(node.getSerial()) == null) {
            NotificationsLoader.makeToast("Unexpected error", true);
            return;
        }

        boolean override = ((CheckBox) view.findViewById(R.id.nodesImportOverrideCheckBox)).isChecked();
        SparseArray<Module> modules = adapter.getData();
        int added = 0;

        for (int i = 0; i < modules.size(); ++i) {
            if (ModulesLoader.addModule(modules.valueAt(i), override)) ++added;
        }

        NotificationsLoader.makeToast("Added " + added + " modules", true);
        toParent.putInt("updated", pos);
        getActivity().onBackPressed();*/
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
        private WeakReference<ModulesAdapter> weakAdapter;
        private WeakReference<View> weakView;

        Updater(@NonNull View view) {
            weakView = new WeakReference<>(view);
        }

        public void setAdapter(@NonNull ModulesAdapter adapter) {
            this.weakAdapter = new WeakReference<>(adapter);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    stopRefreshing(msg.getData());
                    break;
                case 1:
                    pushModule(msg.getData());
                    break;
            }
        }

        private void stopRefreshing(@Nullable Bundle data) {
            ModulesAdapter adapter = weakAdapter.get();
            View view = weakView.get();
            if (view == null) return;

            ((SwipeRefreshLayout) view.findViewById(R.id.modulesDiscoverySwipeRefreshLayout)).setRefreshing(false);

            if (adapter != null && adapter.getItemCount() == 0) {
                SimpleAnimator.fadeIn(view.findViewById(R.id.modulesDiscoveryNoContent), 200);
            }

            if (data != null) {
                int status = data.getInt("status", 0);

                if (status != 0) {
                    NotificationsLoader.makeToast(
                            DataLoader.getAppResources().getString(status),
                            true
                    );
                }
            }
        }

        private synchronized void pushModule(Bundle data) {
            ModulesAdapter adapter = weakAdapter.get();
            if (adapter == null) return;

            try {
                int serial = data.getInt("serial");
                String type = data.getString("type");
                String ip = data.getString("ip");
                int port = data.getInt("port");
                String title = data.getString("title", null);
                JSONObject ops = new JSONObject(data.getString("ops"));
                JSONObject values = new JSONObject(data.getString("values"));

                Module module = new Module(serial, type, ip, port, title, ops, values, false);
                adapter.pushData(module);

                if (adapter.getItemCount() == 1) {
                    View view = weakView.get();

                    if (view != null) {
                        final View noContent = view.findViewById(R.id.modulesDiscoveryNoContent);

                        if (noContent.getVisibility() != View.GONE) {
                            SimpleAnimator.fadeOut(noContent, 200, new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {}

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    noContent.setVisibility(View.GONE);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {}
                            });
                        }
                    }
                }

            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    private static class Discoverer extends SyncProvider {
        private WeakReference<Handler> weakHandler;
        private int maxAttempts;
        private int attempts;

        Discoverer(@NonNull Handler handler) throws JSONException {
            super(
                    Sync.PROVIDER_DISCOVERER,
                    "discovery",
                    new JSONObject(),
                    null,
                    DataLoader.getInt("SyncDiscoveryPort", Sync.DEFAULT_PORT)
            );

            weakHandler = new WeakReference<>(handler);
            maxAttempts = DataLoader.getInt("SyncDiscoveryAttempts", 3);
            attempts = maxAttempts;
        }

        @Override
        public void onPostPublish(int statusCode) {
            switch (statusCode) {
                case 0:
                    finish(R.string.disconnected);
                    break;
                case 1:
                    if (attempts-- <= 1) {
                        finish(0);
                    }
                    break;
                case 2:
                    //finish(R.string.masterServerRequired);
                    break;
                case 3:
                    finish(R.string.disconnected);
                    break;
                case 4:
                    finish(R.string.encryptionError);
                    break;
            }
        }

        @Override
        public void onReceive(JSONObject data) {
            Log.d("LOGTAG", data.toString());

            Handler handler = weakHandler.get();
            if (handler == null) return;

            try {
                Message msg = handler.obtainMessage(1);
                Bundle msgData = new Bundle();

                msgData.putInt("serial", data.getInt("serial"));
                msgData.putString("type", data.getString("type"));
                msgData.putString("ip", data.getString("ip"));
                msgData.putInt("port", data.getInt("port"));
                msgData.putString("ops", data.getJSONObject("ops").toString());
                msgData.putString("values", data.getJSONObject("values").toString());

                if (data.has("title")) {
                    msgData.putString("title", data.getString("title"));
                }

                msg.setData(msgData);
                handler.sendMessage(msg);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void finish(int status) {
            Sync.removeSyncProvider(Sync.PROVIDER_DISCOVERER);
            Handler handler = weakHandler.get();
            if (handler == null) return;

            Message msg = handler.obtainMessage(0);
            Bundle msgData = new Bundle();
            msgData.putInt("status", status);

            msg.setData(msgData);
            handler.sendMessage(msg);

            attempts = maxAttempts;
        }
    }
}