package com.lastutf445.home2.containers;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.lastutf445.home2.loaders.DataLoader;

public class Event {
    private int icon, id;
    private String title, subtitle;

    public Event(int title, int subtitle, int icon) {
        Resources res = DataLoader.getAppResources();
        this.title = res.getString(title);
        this.subtitle = res.getString(subtitle);
        this.icon = icon;
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
}
