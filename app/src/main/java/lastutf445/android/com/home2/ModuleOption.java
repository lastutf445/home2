package lastutf445.android.com.home2;

public class ModuleOption {
    private int serial;
    private String type, ip, mac, title, state;

    ModuleOption(int serial, String type, String ip, String mac, String title, String state) {
        this.serial = serial;
        this.type = type;
        this.ip = ip;
        this.mac = mac;
        this.title = title;
        this.state = state;
    }

    public int getSerial() {
        return serial;
    }

    public String getType() {
        return type;
    }

    public String getIp() {
        return ip;
    }

    public String getMac() {
        return mac;
    }

    public String getTitle() {
        return title;
    }

    public String getState() {
        return state;
    }
}
