package com.lastutf445.home2.containers;

import android.content.res.Resources;

import com.lastutf445.home2.loaders.DataLoader;

public class Event {
    private int icon, id;
    private long timestamp;
    private String title, subtitle;

    public Event(int id, int title, int subtitle, int icon, long timestamp) {
        Resources res = DataLoader.getAppResources();
        this.id = id;
        this.title = res.getString(title);
        this.subtitle = res.getString(subtitle);
        this.icon = icon;
        this.timestamp = timestamp;
    }

    public int getIcon() {
        return icon;
    }

    public int getId() {
        return id;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getTitle() {
        return title;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
