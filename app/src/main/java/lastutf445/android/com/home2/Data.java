package lastutf445.android.com.home2;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.SparseArray;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class Data {

    private static HashMap<String, Object> ops;

    static {
        ops = new HashMap<>();

        // Authorization
        ops.put("Session", null);
        ops.put("GuestMode", false);
        ops.put("AccountName", null);
        // Master server
        ops.put("MasterServer", false);
        ops.put("MasterServerMacAddress", null);
        ops.put("MasterServerIP", null);
        // Proxy server
        ops.put("ProxyServer", false);
        ops.put("ProxyServerIP", null);
        // Synchronisation
        ops.put("AutoSyncDashboard", true);
        ops.put("AutoSyncMessages", true); // requires master
        ops.put("AutoSyncNotifications", true); // semi-requires master
        // Notifications
        ops.put("NotificationsEnabled", true); // use notifications
        ops.put("NotificationsCounter", true);
        ops.put("OutAppNotifications", true); // show notifications in outer space
        // Messages
        ops.put("MessagesEnabled", true);
        ops.put("MessagesCounter", true);
        // User Interface
        ops.put("ActiveFragment", "dashboard");
    }

    public static boolean refreshOptions() {
        try {
            HashMap<String, Object> h = Database.getOptions();
            Notifications.makeToast("count of ops: " + h.size());

            for (Map.Entry<String, Object> i: h.entrySet()) {
                ops.put(i.getKey(), i.getValue());
            }

            return true;

        } catch (SQLiteException e) {
            return false;
        }
    }

    public static void set(String option, Object value) {
        ops.put(option, value);
    }

    private static Object get(String option) {
        return ops.get(option);
    }

    public static String getString(String option, String std) {
        Object res = get(option);
        return res != null ? String.valueOf(res) : std;
    }

    public static int getInt(String option, int std) {
        String res = getString(option, null);
        return res != null ? Integer.valueOf(res) : std;
    }

    public static boolean getBoolean(String option, boolean std) {
        String res = getString(option, null);
        return res != null ? Boolean.valueOf(res) : std;
    }

    public static boolean recordOptions() {
        try {
            Database.setOptions(ops);
            return true;

        } catch (SQLiteException e) {
            return false;
        }
    }
}
