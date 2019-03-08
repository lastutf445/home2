package lastutf445.android.com.home2;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NodeOption {

    private int id;
    private InetAddress ip;
    private String title;

    NodeOption(int id, String ip, String title) throws UnknownHostException {
        this.id = id;
        this.title = title;
        this.ip = InetAddress.getByName(ip);
    }

    public int getId() {
        return id;
    }

    public InetAddress getIp() {
        return ip;
    }

    public String getTitle() {
        return title;
    }
}
