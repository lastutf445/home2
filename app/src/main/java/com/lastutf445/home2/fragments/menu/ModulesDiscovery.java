package com.lastutf445.home2.fragments.menu;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.CheckBox;

import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.ModulesAdapter;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.UserLoader;
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
        content.setLayoutManager(new LinearLayoutManager(DataLoader.getAppContext()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });

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

        final SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.modulesDiscoverySwipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        if (!UserLoader.isAuthenticated()) {
                            swipeRefreshLayout.setRefreshing(false);
                            NotificationsLoader.makeToast(
                                    DataLoader.getAppResources().getString(R.string.authenticationRequired),
                                    true
                            );
                            return;
                        }

                        adapter.deleteAll();
                        discoverer.setup();

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
            public void onClick(@NonNull View v) {
                int pos = content.getChildLayoutPosition(v);
                Module module = adapter.getModule(pos);

                if (module == null) {
                    NotificationsLoader.makeToast("Unexpected error", true);
                    return;
                }

                addModule(module, pos);
            }
        };

        View.OnClickListener e = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.modulesDiscoveryOverride:
                        CheckBox checkBox = (CheckBox) ((ViewGroup) v).getChildAt(0);
                        checkBox.setChecked(!checkBox.isChecked());
                        break;
                    case R.id.modulesDiscoveryMerge:
                        merge();
                        break;
                }
            }
        };

        view.findViewById(R.id.modulesDiscoveryOverride).setOnClickListener(e);
        view.findViewById(R.id.modulesDiscoveryMerge).setOnClickListener(e);

        AppCompatButton button = view.findViewById(R.id.modulesDiscoveryMerge);
        button.setCompoundDrawableTintList(ColorStateList.valueOf(Color.parseColor("#999999")));
        button.setTextColor(Color.parseColor("#999999"));
        button.setClickable(false);

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

        boolean match = false;

        if (ModulesLoader.getModule(module.getSerial()) != null) {
            builder.setMessage(R.string.modulesAddModuleOverrideMessage);
            builder.setTitle(R.string.modulesAddModuleOverrideTitle);
            match = true;

        } else {
            builder.setMessage(R.string.modulesAddModuleMessage);
            builder.setTitle(R.string.modulesAddModuleTitle);
        }

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
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
                    toParent.putBoolean("reload", true);
                    adapter.update(pos);
                }
            }
        });

        builder.create().show();
    }

    private void merge() {
        // TODO: PROCESSING MODAL WINDOW, CODE WRAPPED INTO A NEW THREAD

        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        builder.setTitle(R.string.modulesMergeTitle);
        builder.setMessage(R.string.modulesMergeMessage);

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(R.string.merge, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean override = ((CheckBox) view.findViewById(R.id.modulesDiscoveryOverrideCheckbox)).isChecked();
                SparseArray<Module> modules = adapter.getData();
                int added = 0;

                for (int i = 0; i < modules.size(); ++i) {
                    if (ModulesLoader.addModule(modules.valueAt(i), override)) ++added;
                }

                NotificationsLoader.makeToast("Added " + added + " modules", true);
                toParent.putBoolean("reload", true);
                getActivity().onBackPressed();
            }
        });

        builder.create().show();
    }

    @Override
    public void onDestroy() {
        try {
            Sync.removeSyncProvider(Sync.PROVIDER_DISCOVERER);
        } catch (Exception e) {
            // lol
        }
        super.onDestroy();
    }

    @Override
    public void onResult(@NonNull Bundle data) {
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
        public void handleMessage(@NonNull Message msg) {
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

        private synchronized void pushModule(@NonNull Bundle data) {
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

                        AppCompatButton button = view.findViewById(R.id.modulesDiscoveryMerge);
                        button.setCompoundDrawableTintList(ColorStateList.valueOf(Color.parseColor("#333333")));
                        button.setTextColor(Color.parseColor("#333333"));
                        button.setClickable(true);
                    }
                }

            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    private static class Discoverer extends SyncProvider {
        private WeakReference<Handler> weakHandler;
        private int timeout = 0;
        private boolean waiting;

        Discoverer(@NonNull Handler handler) throws JSONException {
            super(
                    Sync.PROVIDER_DISCOVERER,
                    "discovery",
                    new JSONObject(),
                    null,
                    DataLoader.getInt("SyncDiscoveryPort", Sync.DEFAULT_PORT)
            );

            weakHandler = new WeakReference<>(handler);
        }

        public void setup() {
            timeout = DataLoader.getInt("SyncDiscoveryTimeout", 3);
            waiting = false;
        }

        @Override
        public boolean isWaiting() {
            if (waiting) {
                if (timeout-- <= 1) {
                    finish(0);
                }
                return true;

            } else {
                waiting = true;
                return false;
            }
        }

        @Override
        public void onPostPublish(int statusCode) {
            switch (statusCode) {
                case 0:
                    finish(R.string.disconnected);
                    break;
                case 1:
                    if (timeout-- <= 1) {
                        finish(0);
                    }

                    waiting = true;
                    break;
                case 2:
                    finish(R.string.masterServerRequired);
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
        public void onReceive(@NonNull JSONObject data) {
            //Log.d("LOGTAG", data.toString());

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
        }
    }
}
