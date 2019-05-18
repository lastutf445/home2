package com.lastutf445.home2.fragments.menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.CheckBox;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.ModulesAdapter;
import com.lastutf445.home2.adapters.NodesAdapter;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.containers.Node;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NodesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SimpleAnimator;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.Iterator;

public class NodesImport extends NavigationFragment {

    private DetailsProvider provider;
    private Node node;
    private int pos;

    private ModulesAdapter adapter;
    private RecyclerView content;
    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.nodes_import, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        if (node == null) return;
        content = view.findViewById(R.id.nodesImportContent);
        content.setLayoutManager(new LinearLayoutManager(DataLoader.getAppContext()));
        content.setNestedScrollingEnabled(false);

        updater = new Updater(view, node.getSerial());

        try {
            provider = new DetailsProvider(updater, node);
            Sync.addSyncProvider(provider);

        } catch (JSONException e) {
            //e.printStackTrace();
            NotificationsLoader.makeToast("Unexpected error", true);
            getActivity().onBackPressed();
        }

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //NotificationsLoader.makeToast("okay", true);
                int pos = content.getChildLayoutPosition(v);
                addModule(adapter.getModule(pos), pos);
            }
        };

        View.OnClickListener d = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setClickable(false);

                switch (v.getId()) {
                    case R.id.nodesImportOverride:
                        switchOverride(v);
                        break;
                    case R.id.nodesImportMerge:
                        merge();
                        break;
                    case R.id.nodesImportAddNode:
                        addNode();
                        break;
                }

                v.setClickable(true);
            }
        };

        adapter = new ModulesAdapter(
                getLayoutInflater(),
                c
        );

        view.findViewById(R.id.nodesImportOverride).setOnClickListener(d);
        view.findViewById(R.id.nodesImportAddNode).setOnClickListener(d);
        view.findViewById(R.id.nodesImportMerge).setOnClickListener(d);
        adapter.setData(new SparseArray<Module>(), true);
        updater.setAdapter(adapter);
        content.setAdapter(adapter);
        reload();
    }

    @Override
    protected void reload() {
        ((TextView) view.findViewById(R.id.nodesImportTitle)).setText(node.getTitle());
        SimpleAnimator.fadeIn(view.findViewById(R.id.nodesImportSpinner), 500);
    }

    public void setNode(@NonNull Node node, int pos) {
        this.node = node;
        this.pos = pos;
    }

    private void switchOverride(View v) {
        CheckBox checkBox = v.findViewById(R.id.nodesImportOverrideCheckBox);
        checkBox.setChecked(!checkBox.isChecked());
    }

    private void addModule(@Nullable final Module module, final int pos) {
        if (module == null) {
            NotificationsLoader.makeToast("Unexpected error", true);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        Resources res = DataLoader.getAppResources();
        boolean match = false;

        if (ModulesLoader.getModule(module.getSerial()) != null) {
            builder.setMessage(res.getString(R.string.nodesImportAddModuleOverrideMessage));
            builder.setTitle(res.getString(R.string.nodesImportAddModuleOverrideTitle));
            match = true;

        } else {
            builder.setMessage(res.getString(R.string.nodesImportAddModuleMessage));
            builder.setTitle(res.getString(R.string.nodesImportAddModuleTitle));
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
                if (!ModulesLoader.addModule(module, true)) {
                    NotificationsLoader.makeToast("Unexpected error", true);

                } else {
                    NotificationsLoader.makeToast("Added", true);
                    toParent.putInt("updated", pos);
                    adapter.update(pos);
                }
            }
        });

        builder.create().show();
    }

    private void merge() {
        // TODO: PROCESSING MODAL WINDOW, CODE WRAPPED INTO A NEW THREAD

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
        getActivity().onBackPressed();
    }

    private void addNode() {
        if (NodesLoader.getNode(node.getSerial()) != null) {
            NotificationsLoader.makeToast("Node has been already added", true);
            return;
        }

        try {
            Node original = new Node(node, true);
            if (NodesLoader.addNode(original, true)) {
                NotificationsLoader.makeToast("Added", true);
                toParent.putInt("updated", pos);
                return;
            }

        } catch (Exception e) {
            //e.printStackTrace();
        }

        NotificationsLoader.makeToast("Unexpected error", true);
    }

    @Override
    public void onDestroy() {
        Sync.removeSyncProvider(Sync.PROVIDER_NODE_IMPORT);
        super.onDestroy();
    }

    private static class Updater extends Handler {
        private WeakReference<ModulesAdapter> weakAdapter;
        private WeakReference<View> weakView;
        private int nodeSerial;

        public Updater(@NonNull View view, int serial) {
            weakView = new WeakReference<>(view);
            nodeSerial = serial;
        }

        public void setAdapter(@NonNull ModulesAdapter adapter) {
            this.weakAdapter = new WeakReference<>(adapter);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    pushData(msg.getData());
                    break;
            }
        }

        private void pushData(Bundle data) {
            View view = weakView.get();
            ModulesAdapter adapter = weakAdapter.get();
            if (data == null || view == null) return;

            try {
                JSONObject json = new JSONObject(
                        data.getString("json")
                );

                Iterator<String> it = json.keys();

                while(it.hasNext()) {
                    String key = it.next();

                    try {
                        JSONObject details = json.getJSONObject(key);
                        int serial = details.getInt("serial");
                        String type = details.getString("type");
                        String title = details.has("title") ? details.getString("title") : null;
                        String ops = details.getString("ops");

                        if (serial < 0) continue;
                        Module module = new Module(serial, type, nodeSerial, title, ops, 0);
                        adapter.pushData(module);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            Sync.removeSyncProvider(Sync.PROVIDER_NODE_IMPORT);

            final View spinner = view.findViewById(R.id.nodesImportSpinner);

            SimpleAnimator.fadeOut(spinner, 500, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    spinner.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }
    }

    private static class DetailsProvider extends SyncProvider {
        private WeakReference<Handler> weakHandler;

        public DetailsProvider(@NonNull Handler handler, @NonNull Node node) throws JSONException {
            super(
                    Sync.PROVIDER_NODE_IMPORT,
                    "details",
                    new JSONObject(),
                    node.getIp(),
                    node.getPort()
            );

            this.weakHandler = new WeakReference<>(handler);
        }

        @Override
        public void onReceive(JSONObject data) {
            Handler handler = weakHandler.get();
            if (handler == null) return;

            Message msg = handler.obtainMessage(0);
            Bundle msgData = new Bundle();

            msgData.putString("json", data.toString());
            msg.setData(msgData);

            handler.sendMessage(msg);
        }
    }
}
