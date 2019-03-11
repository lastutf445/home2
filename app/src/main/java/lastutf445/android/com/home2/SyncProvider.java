package lastutf445.android.com.home2;

import android.icu.text.DateFormat;

import org.json.JSONObject;

abstract public class SyncProvider {

    private int attempts = -1;
    private JSONObject query;
    private int id;

    SyncProvider(int id, JSONObject query) {
        this.id = id;
        this.query = query;
    }

    SyncProvider(int id, JSONObject query, int attempts) {
        this.id = id;
        this.query = query;
        this.attempts = attempts;
    }

    public void onReceive(JSONObject data, int id) {}

    final public void onPublish(int statusCode) {
        if (statusCode == 0) {
            // TODO: fail status code
            attempts -= Integer.valueOf(Boolean.valueOf(attempts > 0).toString());

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
