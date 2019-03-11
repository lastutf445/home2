package lastutf445.android.com.home2;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NodeOption {

    private int serial;
    private InetAddress ip;
    private String title;

    NodeOption(int serial, String ip, String title) throws UnknownHostException {
        this.serial = serial;
        this.title = title;
        this.ip = InetAddress.getByName(ip);
    }

    public int getSerial() {
        return serial;
    }

    public InetAddress getIp() {
        return ip;
    }

    public String getTitle() {
        return title;
    }
}
