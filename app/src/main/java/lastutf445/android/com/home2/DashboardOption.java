package lastutf445.android.com.home2;

import org.json.JSONObject;

import java.util.ArrayList;

public class DashboardOption {
    private int id;
    private String type;
    private JSONObject ops;

    DashboardOption(int id, String type, JSONObject ops) {
        this.id = id;
        this.type = type;
        this.ops = ops;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public JSONObject getOps() {
        return ops;
    }
}
