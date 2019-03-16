package lastutf445.android.com.home2;

import android.icu.text.DateFormat;

import org.json.JSONObject;

abstract public class SyncProvider {

    protected int attempts = -1;
    protected JSONObject query;
    protected int id;

    SyncProvider(int id, JSONObject query) {
        this.id = id;
        this.query = query;
    }

    SyncProvider(int id, JSONObject query, int attempts) {
        this.id = id;
        this.query = query;
        this.attempts = attempts;
    }

    public void onReceive(JSONObject data, int nodeSerial) {}

    final public void onPublish(int statusCode) {
        if (statusCode != 1) {
            attempts -= (attempts > 0 ? 1 : 0);

            if (attempts == 0) {
                Sync.unpublish(id);
            }
        }

        onPublishCustomTrigger(statusCode);
    }

    public void onPublishCustomTrigger(int statusCode) {}

    public JSONObject getQuery() {
        return query;
    }
}
