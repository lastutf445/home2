package com.lastutf445.home2.loaders;

import android.support.design.widget.BottomNavigationView;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.widget.Toast;

import com.lastutf445.home2.R;
import com.lastutf445.home2.activities.MainActivity;
import com.lastutf445.home2.containers.Event;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

public final class NotificationsLoader {

    private static HashSet<Integer> toasts = new HashSet<>();
    private static SparseArray<Event> notifications = new SparseArray<>();
    private static WeakReference<BottomNavigationView> weakBottomNav;
    private static Callback callback;

    private static final int LENGTH_SHORT = 2000;
    private static final int LENGTH_LONG = 3500;

    public static void init(BottomNavigationView bottomNav) {
        weakBottomNav = new WeakReference<>(bottomNav);

        try {
            Notifier notifier = new Notifier();
            Sync.addSyncProvider(notifier);

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

    public static SparseArray<Event> getNotifications() {
        return notifications;
    }

    public static void setCallback(Callback callback) {
        NotificationsLoader.callback = callback;
    }

    private static void attract() {
        BottomNavigationView nav = weakBottomNav.get();
        if (nav == null) return;

        final MenuItem navNotificationItem = nav.getMenu().findItem(R.id.nav_notifications);
        MainActivity mainActivity = MainActivity.getInstance();

        if (mainActivity != null) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    navNotificationItem.setIcon(
                        notifications.size() == 0 ? R.drawable.notifications_none :
                                R.drawable.notifications

                    );
                }
            });
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

    public static void makeStatusNotification(int status, boolean update) {
        if (notifications.get(status) != null) {
            if (update) {
                Event ev = notifications.get(status);
                ev.setTimestamp(System.currentTimeMillis());

                if (callback != null) {
                    callback.updatedAt(
                            notifications.indexOfKey(status)
                    );
                    attract();
                }
            }
            return;
        }

        int title = -1, subtitle = -1, icon = -1;

        switch (status) {
            case Sync.ENCODE_ERROR:
                title = R.string.notificationEncodeErrorTitle;
                subtitle = R.string.notificationEncodeErrorSubtitle;
                icon = R.drawable.world;
                break;
            case Sync.TOO_MANY_TASKS:
                title = R.string.notificationTooManyTasksTitle;
                subtitle = R.string.notificationTooManyTasksSubtitle;
                icon = R.drawable.dnd;
                break;
            case Sync.MALFORMED_PACKET:
                title = R.string.notificationMalformedPacketTitle;
                subtitle = R.string.notificationMalformedPacketSubtitle;
                icon = R.drawable.world;
                break;
            case Sync.UNEXPECTED_ERROR:
                title = R.string.notificationUnexpectedErrorTitle;
                subtitle = R.string.notificationUnexpectedErrorSubtitle;
                icon = R.drawable.about;
                break;
            case Sync.UNAUTHORIZED:
                title = R.string.notificationUnauthorizedTitle;
                subtitle = R.string.notificationUnauthorizedSubtitle;
                icon = R.drawable.account;
                break;
            case Sync.ENCRYPT_ERROR:
                title = R.string.notificationEncryptErrorTitle;
                subtitle = R.string.notificationEncryptErrorSubtitle;
                icon = R.drawable.vpn_key;
                break;
            case Sync.MALFORMED_AES:
                title = R.string.notificationMalformedAESTitle;
                subtitle = R.string.notificationMalformedAESSubtitle;
                icon = R.drawable.vpn_key;
                break;
            case Sync.MALFORMED_RSA:
                title = R.string.notificationMalformedRSATitle;
                subtitle = R.string.notificationMalformedRSASubtitle;
                icon = R.drawable.vpn_key;
                break;
            case Sync.UNSUPPORTED:
                title = R.string.notificationUnsupportedTitle;
                subtitle = R.string.notificationUnsupportedSubtitle;
                icon = R.drawable.bug;
                break;
            case Sync.TOO_MANY_CLIENTS:
                title = R.string.notificationTooManyClientsTitle;
                subtitle = R.string.notificationTooManyClientsSubtitle;
                icon = R.drawable.dnd;
                break;
            case Sync.SYNC_USER_DATA_EVENT:
                title = R.string.notificationSyncUserDataTitle;
                subtitle = R.string.notificationSyncUserDataSubtitle;
                icon = R.drawable.sync;
                break;
            case Sync.SYNC_MODULES_STATE_EVENT:
                title = R.string.notificationSyncModulesStateTitle;
                subtitle = R.string.notificationSyncModulesStateSubtitle;
                icon = R.drawable.sync;
                break;
            case Sync.SYNC_USER_DATA_FAILED_EVENT:
                title = R.string.notificationSyncUserDataTitle;
                subtitle = R.string.notificationSyncUserDataFailedSubtitle;
                icon = R.drawable.sync_problem;
                break;
            case Sync.SYNC_MODULES_STATE_FAILED_EVENT:
                title = R.string.notificationSyncModulesStateTitle;
                subtitle = R.string.notificationSyncModulesStateFailedSubtitle;
                icon = R.drawable.sync_problem;
        }

        if (title == -1) {
            return;
        }

        Event ev = new Event(
                status,
                title,
                subtitle,
                icon,
                System.currentTimeMillis()
        );

        notifications.put(status, ev);

        if (callback != null) {
            callback.insertedAt(
                    notifications.indexOfKey(status)
            );
            attract();
        }
    }

    public static void removeAll() {
        int oldSize = notifications.size();
        notifications.clear();

        if (callback != null) {
            callback.removeAll(oldSize);
            attract();
        }
    }

    public static void removeById(int id) {
        int index = notifications.indexOfKey(id);
        if (index < 0) return;
        removeAt(index);
    }

    public static void removeAt(int pos) {
        notifications.removeAt(pos);

        if (callback != null) {
            callback.removeAt(pos);
            attract();
        }
    }

    public static class Notifier extends SyncProvider {
        public Notifier() throws JSONException {
            super(
                    Sync.PROVIDER_NOTIFICATIONS,
                    "notifier",
                    new JSONObject(),
                    null,
                    0
            );
        }

        @Override
        public boolean isWaiting() {
            return true;
        }

        @Override
        public void onReceive(JSONObject data) {
            Log.d("LOGTAG", "notification received");
            if (!data.has("status")) return;

            try {
                int status = data.getInt("status");
                makeStatusNotification(status, true);

            } catch (JSONException e) {
                //e.printStackTrace();
            }
        }
    }

    public interface Callback {
        void removeAll(int oldSize);
        void removeAt(int oldSize);
        void insertedAt(int pos);
        void updatedAt(int pos);
    }

    public static class SyncSwitch implements Runnable {
        @Override
        public void run() {
            boolean state = DataLoader.getBoolean("SyncNotifications", true);
            DataLoader.set("SyncNotifications", !state);

            // TODO: save optimization
            DataLoader.save();
        }
    }
}
