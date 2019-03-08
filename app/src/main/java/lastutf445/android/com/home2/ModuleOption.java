package lastutf445.android.com.home2;

import java.net.InetAddress;

public class ModuleOption {
    private int serial, nodeId;
    private String type, title, state;

    ModuleOption(int serial, String type, int nodeId, String title, String state) {
        this.serial = serial;
        this.type = type;
        this.nodeId = nodeId;
        this.title = title;
        this.state = state;
    }

    public int getSerial() {
        return serial;
    }

    public String getType() {
        return type;
    }

    public int getNodeId() {
        return nodeId;
    }

    public InetAddress getIp() {
        NodeOption node = Modules.getNode(nodeId);
        return node != null ? node.getIp() : null;
    }

    public String getTitle() {
        return title;
    }

    public String getState() {
        return state;
    }
}
