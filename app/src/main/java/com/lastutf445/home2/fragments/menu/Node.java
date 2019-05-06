package com.lastutf445.home2.fragments.menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.NodesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;

public class Node extends NavigationFragment {

    private com.lastutf445.home2.containers.Node node;
    private int pos;

    //private Thread ping;
    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.node, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        if (node == null) return;

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.nodeEditTitle:
                        Rename rename = new Rename();
                        Resources res = DataLoader.getAppResources();
                        rename.setTitle(res.getString(R.string.nodeTitle));
                        rename.setHint(res.getString(R.string.nodeHint));
                        rename.setOld(node.getTitle());

                        rename.setCallback(new Rename.Callback() {
                            @Override
                            public boolean onApply(String s) {
                                String t = s.trim();

                                if (t.length() == 0) {
                                    NotificationsLoader.makeToast("Invalid node title", true);
                                    return false;

                                } else {
                                    NotificationsLoader.makeToast("Success", true);
                                    node.setTitle(t);
                                    return true;
                                }
                            }
                        });

                        FragmentsLoader.addChild(rename, Node.this);
                        break;
                    case R.id.nodeModules:
                        Modules modules = new Modules();
                        modules.setForceDelete(true);
                        modules.setModules(node.getModules(), false);
                        FragmentsLoader.addChild(modules, Node.this);
                        break;
                    case R.id.nodeGetModules:
                        NodesImport nodesImport = new NodesImport();
                        nodesImport.setNode(node, pos);
                        FragmentsLoader.addChild(nodesImport, Node.this);
                        break;
                    case R.id.nodeDisableSync:
                        disableSync();
                        break;
                    case R.id.nodeDelete:
                        delete();
                        break;
                }
            }
        };

        view.findViewById(R.id.nodeEditTitle).setOnClickListener(c);
        view.findViewById(R.id.nodeModules).setOnClickListener(c);
        view.findViewById(R.id.nodeGetModules).setOnClickListener(c);
        view.findViewById(R.id.nodeDisableSync).setOnClickListener(c);
        view.findViewById(R.id.nodeDelete).setOnClickListener(c);

        updater = new Updater(view);
        reload();
    }

    @Override
    protected void reload() {
        ((TextView) view.findViewById(R.id.nodeTitle)).setText(
                node.getTitle()
        );

        ((TextView) view.findViewById(R.id.nodeSyncingCount)).setText(
                String.valueOf(node.getSyncingCount())
        );

        if (node.getIp() != null) {
            ((TextView) view.findViewById(R.id.nodeIPAddress)).setText(
                    node.getIp().getHostAddress()
            );
        }

        ((TextView) view.findViewById(R.id.nodeSerial)).setText(
                String.valueOf(node.getSerial())
        );
    }

    private void disableSync() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        Resources res = DataLoader.getAppResources();
        builder.setTitle(res.getString(R.string.nodeSyncTitle));
        builder.setMessage(res.getString(R.string.nodeSyncMessage));

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(R.string.disable, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                node.wipeSyncing();
                ((TextView) view.findViewById(R.id.nodeSyncingCount)).setText("0");
                NotificationsLoader.makeToast("Sync disabled", true);
            }
        });

        builder.create().show();
    }

    private void delete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        Resources res = DataLoader.getAppResources();
        builder.setTitle(res.getString(R.string.nodeDeleteTitle));
        builder.setMessage(res.getString(R.string.nodeDeleteMessage));

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                NodesLoader.removeNode(node);
                toParent.putInt("deleted", pos);
                NotificationsLoader.makeToast("Node deleted", true);
                getActivity().onBackPressed();
            }
        });

        builder.create().show();

    }

    public void setNode(com.lastutf445.home2.containers.Node node, int pos) {
        this.node = node;
        this.pos = pos;
    }

    @Override
    public void onResult(Bundle data) {
        if (data.containsKey("syncStateChanged")) {
            reload();
        }
        if (data.containsKey("deleted")) {
            toParent.putInt("updated", pos);
            reload();
        }
        if (data.containsKey("updated")) {
            toParent.putInt("updated", pos);
        }
        if (data.containsKey("reload")) {
            toParent.putInt("updated", pos);
            reload();
        }
    }

    private static class Updater extends Handler {
        private WeakReference<View> weakView;

        Updater(View view) {
            this.weakView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {

        }
    }
}
