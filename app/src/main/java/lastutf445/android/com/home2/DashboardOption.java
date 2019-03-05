package lastutf445.android.com.home2;

import java.util.ArrayList;

public class DashboardOption {
    int id;
    String type;
    ArrayList<Integer> modules;

    DashboardOption(int id, String type, ArrayList<Integer> modules) {
        this.id = id;
        this.type = type;
        this.modules = modules;
    }
}
