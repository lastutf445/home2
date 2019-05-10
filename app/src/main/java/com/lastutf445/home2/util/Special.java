package com.lastutf445.home2.util;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.containers.Node;
import com.lastutf445.home2.fragments.dialog.Processing;
import com.lastutf445.home2.loaders.NodesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.network.Sync;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class Special extends NavigationFragment {

    protected Render render;
    protected Module module;
    protected Updater updater;

    public final void setModule(@NonNull Module module) {
        this.module = module;
    }

    public final void setRender(@NonNull Render render) {
        this.render = render;

        if (updater != null) {
            updater.setRender(render);
        }
    }

    @Override
    protected void reload() {
        if (render != null) {
            render.reload(view, module);
        }
    }

    protected void makeEditRequest(JSONObject ops) {
        Processing dialog = new Processing();

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Sync.removeSyncProvider(Sync.PROVIDER_MODULE_EDIT_REQUEST);
            }
        });

        try {
            Node node = NodesLoader.getNode(module.getNode());

            if (node == null) {
                NotificationsLoader.makeToast("Node is null", true);
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
        }
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
            Render render = weakRender.get();
            Processing dialog = weakDialog.get();

            if (view != null && module != null) {
                try {
                    module.mergeStates(module.getType(), new JSONObject(data.getString("ops")));

                } catch (JSONException e) {
                    e.printStackTrace();
                    fail();
                    return;
                }

                if (render != null) {
                    render.reload(view, module);
                }
            }

            if (dialog != null) {
                dialog.dismiss();
            }
        }
    }

    public interface Render {
        void reload(@NonNull View view, @NonNull Module module);
    }
}
