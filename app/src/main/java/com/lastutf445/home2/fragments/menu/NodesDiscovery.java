package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.InterpolatorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;

import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.NodesAdapter;
import com.lastutf445.home2.containers.Node;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SimpleAnimator;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class NodesDiscovery extends NavigationFragment {

    private View hint, noContent;
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
        noContent = view.findViewById(R.id.nodesDiscoveryNoContent);
        hint = view.findViewById(R.id.nodesDiscoveryHint);

        try {
            discoverer = new Discoverer(updater);
            discoverer.setBroadcast(true);

        } catch (JSONException e) {
            //e.printStackTrace();
            NotificationsLoader.makeToast("Unexpected error", true);
            getActivity().onBackPressed();
        }

        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.nodesDiscoverySwipeRefreshLayout);
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
                    stopRefreshing(msg.getData());
                    break;
                case 1:
                    pushNode(msg.getData());
                    break;
            }
        }

        private void stopRefreshing(@Nullable Bundle data) {
            NodesAdapter adapter = weakAdapter.get();
            View view = weakView.get();
            if (view == null) return;

            ((SwipeRefreshLayout) view.findViewById(R.id.nodesDiscoverySwipeRefreshLayout)).setRefreshing(false);

            if (adapter != null && adapter.getItemCount() == 0) {
                SimpleAnimator.fadeIn(view.findViewById(R.id.nodesDiscoveryNoContent), 200);
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

        private synchronized void pushNode(Bundle data) {
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

                if (adapter.getItemCount() == 1) {
                    View view = weakView.get();

                    if (view != null) {
                        final View noContent = view.findViewById(R.id.nodesDiscoveryNoContent);

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
                e.printStackTrace();

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
                msgData.putString("ip", data.getString("ip"));
                msgData.putInt("port", data.getInt("port"));
                msgData.putString("title", data.getString("title"));
                msgData.putInt("modules", data.getInt("modules"));

                msg.setData(msgData);
                handler.sendMessage(msg);

            } catch (JSONException e) {
                //e.printStackTrace();
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
