package com.lastutf445.home2.containers;
import androidx.annotation.NonNull;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class Scenario {

    private int id;
    private String title;
    private ArrayList<Item> data;

    private boolean active = false;
    private long time = 0;
    private BitSet repeat = new BitSet(7);

    public static class Item {
        private int serial;
        private HashMap<String, Object> ops;

        public Item(int serial, @NonNull HashMap<String, Object> ops) {
            this.serial = serial;
            this.ops = ops;
        }

        public Item(Item item) {
            this.serial = item.getSerial();
            this.ops = new HashMap<>();

            for (Map.Entry<String, Object> i: item.ops.entrySet()) {
                this.ops.put(i.getKey(), i.getValue());
            }
        }

        public int getSerial() {
            return serial;
        }

        public HashMap<String, Object> getOps() {
            return ops;
        }
    }

    public Scenario(int id, @NonNull String title, @NonNull ArrayList<Item> data) {
        this.id = id;
        this.title = title;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title != null && title.length() != 0 ? title :
                DataLoader.getAppResources().getString(R.string.scenariosDefaultTitle);
    }

    public ArrayList<Item> getData() {
        return data;
    }

    public int getDataSize() {
        return data.size();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setRepeat(@NonNull BitSet repeat) {
        this.repeat = repeat;
    }

    public boolean getActive() {
        return active;
    }

    public long getTime() {
        return time;
    }

    public BitSet getRepeat() {
        return repeat;
    }
}
