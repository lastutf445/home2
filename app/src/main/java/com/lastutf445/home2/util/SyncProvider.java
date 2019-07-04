package com.lastutf445.home2.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;

public class SyncProvider {

    protected long lastAccess = 0;
    protected boolean broadcast = false;
    protected int group = -1, source, port;
    protected boolean encrypted = true;
    protected JSONObject query;
    protected InetAddress ip;
    private boolean emergency;
    protected String act;

    public SyncProvider(int source, String act, JSONObject data, InetAddress ip, int port, boolean emergency) throws JSONException {
        this.source = source;
        this.act = act;
        this.ip = ip;
        this.port = port;
        this.query = new JSONObject();
        this.emergency = emergency;

        query.put("id", source);
        query.put("act", act);
        setData(data);
    }

    public boolean isWaiting() {
        return false;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public boolean getBroadcast() {
        return broadcast;
    }

    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public void updateLastAccess(long time) {
        lastAccess = time;
    }

    public int getSource() {
        return source;
    }

    public InetAddress getIP() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void setData(JSONObject data) throws JSONException {
        query.put("data", data);
    }

    final public boolean getEmergencyStatus() {
        return emergency;
    }

    public boolean getEncrypted() {
        return encrypted;
    }

    public void onReceive(JSONObject data) {}

    public void onPostPublish(int statusCode) {}

    public JSONObject getQuery() {
        return query;
    }

}
