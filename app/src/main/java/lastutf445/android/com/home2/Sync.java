package lastutf445.android.com.home2;

import android.os.Handler;
import android.os.Message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static java.lang.Thread.sleep;

public class Sync {

    private static Thread sync;

    private static DatagramSocket socket;
    private static int port = 44501;
    private static int sleep = 5;

    private static HashMap<String, SyncListener> subs = new HashMap<>();

    private static class wHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    private static Runnable worker = new Runnable() {
        @Override
        public void run() {
            try {
                socket = new DatagramSocket(port);

            } catch (SocketException e) {
                e.printStackTrace();
            }

            while (!socket.isClosed()) {
                try {
                    byte[] buf = new byte[2048];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    JSONObject obj = new JSONObject(new String(buf));
                    JSONObject data = obj.getJSONObject("data");

                    Iterator<String> i = data.keys();

                    while (i.hasNext()) {
                        String id = i.next();
                        if (Sync.hasSyncListener(id)) {
                            Sync.getSyncListener(id).onRecieve(
                                    data.getJSONArray(id),
                                    obj.getInt("node")
                            );
                        }
                    }

                    sleep(sleep);

                } catch (IOException e) {
                    e.printStackTrace();

                } catch (JSONException e) {
                    e.printStackTrace();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public static SyncListener setSyncListener(String id, SyncListener listener) {
        return Sync.subs.put(id, listener);
    }

    public static boolean hasSyncListener(String id) {
        return Sync.subs.containsKey(id);
    }

    public static SyncListener getSyncListener(String id) {
        return Sync.subs.get(id);
    }

    public static boolean start() {
        if (sync != null && sync.isAlive()) {
            return false;
        }

        sync = new Thread(worker);
        sync.start();

        return sync.isAlive();
    }

    public static boolean reset() {
        if (stop() && start()) {
            return true;
        }

        return false;
    }

    public static boolean stop() {
        Thread dummy = sync;
        sync = null;

        if (dummy != null) {
            dummy.interrupt();
        }

        return true;
    }

}
