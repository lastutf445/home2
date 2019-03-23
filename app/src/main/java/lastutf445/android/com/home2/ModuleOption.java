package lastutf445.android.com.home2;

import java.net.InetAddress;

public class ModuleOption {
    private boolean syncing;
    private int serial, nodeSerial;
    private String type, title, state;

    ModuleOption(int serial, String type, int nodeSerial, String title, String state, int syncing) {
        this.serial = serial;
        this.type = type;
        this.nodeSerial = nodeSerial;
        this.title = title;
        this.state = state;
        this.syncing = syncing > 0;
    }

    public int getSerial() {
        return serial;
    }

    public String getType() {
        return type;
    }

    public int getNodeSerial() {
        return nodeSerial;
    }

    public String getIp() {
        NodeOption node = Modules.getNode(nodeSerial);
        return node != null ? node.getIp() : null;
    }

    public boolean getSyncing() {
        return syncing;
    }

    public String getTitle() {
        return title;
    }

    public String getState() {
        return state;
    }

    public void setSyncing(boolean syncing) {
        Database.setModuleSyncing(serial, syncing);
        this.syncing = syncing;
    }

    public void setTitle(String title) {

    }
}
