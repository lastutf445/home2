package com.lastutf445.home2.loaders;

import android.util.SparseArray;
import android.widget.Toast;

import com.lastutf445.home2.containers.Event;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

public final class NotificationsLoader {

    private static HashSet<Integer> toasts = new HashSet<>();
    private static SparseArray<Event> notifcations;
    private static Notifier notifier;

    private static final int LENGTH_SHORT = 2000;
    private static final int LENGTH_LONG = 3500;

    public static void init() {
        try {
            notifier = new Notifier();
            Sync.addSyncProvider(notifier);

        } catch (JSONException e) {
            //e.printStackTrace();
            // do something
        }
    }

    public static void makeToast(final String msg, boolean lengthShort) {
        if (toasts.contains(msg.hashCode())) return;
        toasts.add(msg.hashCode());

        Toast.makeText(DataLoader.getAppContext(), msg, lengthShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                toasts.remove(msg.hashCode());
            }
        }, lengthShort ? LENGTH_SHORT : LENGTH_LONG);
    }

    public static class Notifier extends SyncProvider {
        public Notifier() throws JSONException {
            super(
                    Sync.PROVIDER_NOTIFICATIONS,
                    "notifier",
                    new JSONObject(),
                    null,
                    Sync.DEFAULT_PORT
            );
        }

        @Override
        public void onReceive(JSONObject data) {
            try {
                if (!data.has("status") || data.get("status") == null || (data.get("status") instanceof Integer))
                    return;

            } catch (JSONException e) {
                //e.printStackTrace();
                return;
            }

            //do smth
        }
    }

    public static class SyncSwitch implements Runnable {
        @Override
        public void run() {
            boolean state = DataLoader.getBoolean("SyncNotifications", true);
            DataLoader.set("SyncNotifications", !state);

            // TODO: save optimization
            DataLoader.save();
/*
            if (state) wipeSyncing();
            else reloadSyncing();
*/
        }
    }
}
