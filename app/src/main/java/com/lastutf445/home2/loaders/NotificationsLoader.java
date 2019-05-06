package com.lastutf445.home2.loaders;

import android.widget.Toast;

import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

public final class NotificationsLoader {

    private static HashSet<Integer> toasts = new HashSet<>();

    private static final int LENGTH_SHORT = 2000;
    private static final int LENGTH_LONG = 3500;

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
