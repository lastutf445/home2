package com.lastutf445.home2.util;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.View;

import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.fragments.dialog.Processing;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.WidgetsLoader;
import com.lastutf445.home2.network.Sync;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public abstract class Configure extends NavigationFragment {

    protected Render render;
    protected Module module;
    protected Updater updater;
    protected String type;

    private int connectorId = 0;
    private Connector connector;

    public final void setModule(@NonNull Module module) {
        type = module.getType();
        this.module = module;
    }

    public final void setConnectorId(int id) {
        if (id == 1 || id == 2) {
            connectorId = id;
        }
    }

    public final void setRender(@NonNull Render render) {
        this.render = render;

        if (updater != null) {
            updater.setRender(render);

            if (module != null) {
                WidgetsLoader.setBottomSheetConnector(new Connector(), connectorId);
            }
        }
    }

    @Override
    protected void reload() {
        if (render != null) {
            render.reload(view, module);
        }
    }

    @Override
    public final void onDestroy() {
        WidgetsLoader.delBottomSheetConnector(connectorId);
        super.onDestroy();
    }

    protected void makeEditRequest(JSONObject ops) {
        Processing dialog = new Processing();

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Sync.removeSyncProvider(Sync.PROVIDER_MODULE_EDIT_REQUEST);
            }
        });
/*
        try {
            Node node = NodesLoader.getNode(module.getNode());

            if (node == null) {
                NotificationsLoader.makeToast("Node is lost, aborted", true);
                return;
            }

            ModuleEditRequest request = new ModuleEditRequest(node, updater);
            request.setSerial(module.getSerial());
            request.setOps(ops);

            dialog.show(getChildFragmentManager(), "processing");
            Sync.addSyncProvider(request);
            updater.setDialog(dialog);

        } catch (JSONException e) {
            e.printStackTrace();
        }*/
    }

    protected static class Updater extends Handler {
        private WeakReference<View> weakView;
        private WeakReference<Module> weakModule;
        private WeakReference<Render> weakRender;
        private WeakReference<Processing> weakDialog;

        public Updater(@NonNull View view, @NonNull Module module) {
            weakView = new WeakReference<>(view);
            weakModule = new WeakReference<>(module);
        }

        public void setDialog(@NonNull Processing dialog) {
            weakDialog = new WeakReference<>(dialog);
        }

        public void setRender(@NonNull Render render) {
            weakRender = new WeakReference<>(render);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    fail();
                    break;
                case 1:
                    success(msg.getData());
                    break;
                case 2:
                    reload();
                    break;
            }
        }

        private void fail() {
            Sync.removeSyncProvider(Sync.PROVIDER_MODULE_EDIT_REQUEST);
            Processing dialog = weakDialog.get();

            if (dialog != null) {
                dialog.dismiss();
            }

            NotificationsLoader.makeToast("Unexpected error", true);
        }

        private void success(Bundle data) {
            Sync.removeSyncProvider(Sync.PROVIDER_MODULE_EDIT_REQUEST);

            View view = weakView.get();
            Module module = weakModule.get();
            Processing dialog = weakDialog.get();

            if (view != null && module != null) {
                try {
                    module.mergeStates(module.getType(), new JSONObject(data.getString("ops")));

                } catch (JSONException e) {
                    e.printStackTrace();
                    fail();
                    return;
                }

                reload();
            }

            if (dialog != null) {
                dialog.dismiss();
            }
        }

        private void reload() {
            View view = weakView.get();
            Module module = weakModule.get();
            Render render = weakRender.get();

            if (view != null && module != null && render != null) {
                render.reload(view, module);
            }
        }
    }

    public interface Render {
        void reload(@NonNull View view, @NonNull Module module);
    }

    public class Connector {
        public void onModuleStateUpdated() {
            if (updater != null) {
                Module newModule = ModulesLoader.getModule(module.getSerial());

                if (newModule == null || !newModule.getType().equals(module.getType())) {
                    FragmentsLoader.pop(Configure.this.getParent());
                    return;
                }

                updater.sendEmptyMessage(2);
            }
        }

        public void onModuleRemoved() {
            FragmentsLoader.pop(Configure.this.getParent());
        }

        public int getSerial() {
            return (module == null ? -1 : module.getSerial());
        }
    }
}
