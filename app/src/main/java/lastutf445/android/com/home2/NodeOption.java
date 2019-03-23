package lastutf445.android.com.home2;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class NodeOption {

    private int serial;
    //private InetAddress ip;
    private String ip;
    private String title;
    private ArrayList<Integer> modules;

    NodeOption(int serial, String ip, String title) throws UnknownHostException {
        configure(serial, ip, title);
    }

    NodeOption(JSONObject json) throws UnknownHostException, JSONException {
        configure(
                json.getInt("serial"),
                json.getString("ip"),
                json.getString("title")
        );
    }

    private void configure(int serial, String ip, String title) throws UnknownHostException {
        this.serial = serial;
        //this.ip = InetAddress.getByName(ip);
        this.ip = ip;
        this.title = title;
        modules = new ArrayList<>();
    }

    public void addModule(int serial) {
        modules.add(serial);
    }

    public int getSerial() {
        return serial;
    }

    //public InetAddress getIp() {
    //    return ip;
    //}

    public int getModulesCount() {
        return modules.size();
    }

    public ArrayList<Integer> getModules() {
        return modules;
    }

    public String getIp() {
        return ip;
    }

    public String getTitle() {
        return title;
    }

    public String toJSON() {
        JSONObject json = new JSONObject();

        try {
            json.put("serial", serial);
            json.put("ip", ip);
            json.put("title", title);
            json.put("modules", modules);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString();
    }

    @Override
    public int hashCode() {
        return Integer.valueOf(serial).hashCode();
        //return super.hashCode();
    }
}
