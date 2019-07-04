package com.lastutf445.home2.loaders;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Scenario;
import com.lastutf445.home2.fragments.scenarios.ScenarioViewer;
import com.lastutf445.home2.fragments.scenarios.Scenarios;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.SyncProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;

public class ScenariosLoader {

    private static Loader scenarioLoader;
    private static Verifier scenarioVerifier;

    public static void createVerifier(int scenarioId, @NonNull ScenarioViewer.Updater updater) {
        try {
            scenarioVerifier = new ScenariosLoader.Verifier(scenarioId, updater);
            Sync.addSyncProvider(scenarioVerifier);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void setId(int id) {
        if (scenarioVerifier != null) {
            scenarioVerifier.setId(id);
        }
    }

    public static void setLastUpdateTime(long lastUpdateTime) {
        if (scenarioVerifier != null) {
            scenarioVerifier.setLastUpdateTime(lastUpdateTime);
        }
    }

    public static void lock(boolean creatorMode) {
        if (scenarioVerifier != null) {
            scenarioVerifier.setEditMode(true, creatorMode);
        }
    }

    public static void unlock() {
        if (scenarioVerifier != null) {
            scenarioVerifier.setEditMode(false, false);
        }
    }

    public static void save(@NonNull ArrayList<Scenario.Item> body, @NonNull String title, boolean active, long time, @NonNull BitSet repeat) {
        if (scenarioVerifier != null) {
            scenarioVerifier.save(
                    body,
                    title,
                    active,
                    time,
                    repeat
            );
        }
    }

    public static void savingEnded() {
        if (scenarioVerifier != null) {
            scenarioVerifier.cancelSaving();
        }
    }

    public static void execute(boolean execute) {
        if (scenarioVerifier != null) {
            scenarioVerifier.setExecute(execute);
        }
    }

    public static void delete(boolean delete) {
        if (scenarioVerifier != null) {
            scenarioVerifier.setDelete(delete);
        }
    }

    public static void loadScenarios(@NonNull Scenarios.Updater updater) {
        Log.d("LOGTAG", "scenariosLoader.load....");

        try {
            scenarioLoader = new Loader(updater);
            Sync.addSyncProvider(scenarioLoader);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void loadMoreScenarios(int lastScenarioId) {
        Log.d("LOGTAG", "scenariosLoader.loadMore....");

        if (scenarioLoader != null) {
            scenarioLoader.loadMoreFrom(lastScenarioId);
        }
    }

    public static void removeLoader() {
        if (scenarioLoader != null) {
            Sync.removeSyncProvider(Sync.PROVIDER_SCENARIOS_LOADER);
            scenarioLoader = null;
        }
    }

    public static void setLoaded() {
        if (scenarioLoader != null) {
            scenarioLoader.waiting = true;
        }
    }

    public static void freeScenario() {
        if (scenarioVerifier != null) {
            Sync.removeSyncProvider(Sync.PROVIDER_SCENARIOS_VERIFIER);
            scenarioVerifier = null;
        }
    }

    public static class Loader extends SyncProvider {
        private WeakReference<Scenarios.Updater> weakUpdater;
        private boolean tainted, waiting;
        private int lastScenarioId;

        public Loader(@NonNull Scenarios.Updater updater) throws JSONException {
            super(
                    Sync.PROVIDER_SCENARIOS_LOADER,
                    "scenarios",
                    new JSONObject(),
                    null,
                    Sync.DEFAULT_PORT,
                    false);

            weakUpdater = new WeakReference<>(updater);
            lastScenarioId = -1;
            waiting = false;
            tainted = true;
        }

        @Override
        public boolean isWaiting() {
            return waiting;
            //return true;
        }

        public void loadMoreFrom(int lastScenarioId) {
            this.lastScenarioId = lastScenarioId;
            waiting = false;
            tainted = true;
        }

        @Override
        public void onPostPublish(int statusCode) {
            if (statusCode < 0 || statusCode == 1) return;
            Scenarios.Updater updater = weakUpdater.get();
            Bundle data = new Bundle();
            int status;

            if (updater == null) return;
            Log.d("LOGTAG", "scenariosLoader.onPostPublish()");

            switch (statusCode) {
                case 2:
                    status = R.string.masterServerRequired;
                    break;
                case 3:
                    status = R.string.disconnected;
                    break;
                case 4:
                    status = R.string.encryptionError;
                    break;
                default:
                    status = R.string.unexpectedError;
                    break;
            }

            data.putInt("status", status);

            Message msg = updater.obtainMessage(0);
            msg.setData(data);
            updater.sendMessage(msg);
        }

        @Override
        public void onReceive(JSONObject data) {
            Scenarios.Updater updater = weakUpdater.get();
            Log.d("LOGTAG", "scenariosLoader.onReceive()");

            if (updater == null) {
                Sync.removeSyncProvider(source);
                return;
            }

            if (data == null) {
                updater.sendEmptyMessage(0);
                return;
            }

            Bundle msgData = new Bundle();

            try {
                msgData.putInt("status", data.getInt("status"));
                msgData.putString("json", data.getJSONArray("scenarios").toString());

                if (data.has("end")) {
                    msgData.putBoolean("end", data.getBoolean("end"));
                }

            } catch (JSONException e) {
                updater.sendEmptyMessage(0);
                e.printStackTrace();
                return;
            }

            Message msg = updater.obtainMessage(1);
            msg.setData(msgData);
            updater.sendMessage(msg);
        }

        @Override
        public JSONObject getQuery() {
            if (tainted) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("lastScenarioId", lastScenarioId);
                    query.put("data", data);
                    tainted = false;

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return query;
        }
    }

    public static class Verifier extends SyncProvider {
        private WeakReference<ScenarioViewer.Updater> weakUpdater;
        private boolean tainted = true, editMode = false, creatorMode = false;
        private boolean active, execute = false, delete = false;
        private ArrayList<Scenario.Item> state;
        private long lastUpdateTime = 0, time;
        private BitSet repeat;
        private String title;
        private int id;

        public Verifier(int id, ScenarioViewer.Updater updater) throws JSONException {
            super(
                    Sync.PROVIDER_SCENARIOS_VERIFIER,
                    "scenarioVerifier",
                    new JSONObject(),
                    null,
                    Sync.DEFAULT_PORT,
                    false);

            this.weakUpdater = new WeakReference<>(updater);
            this.state = null;
            this.id = id;
        }

        @Override
        public boolean isWaiting() {
            return state == null && id == -1;
        }

        public synchronized void setId(int id) {
            this.id = id;
            this.tainted = true;
        }

        public synchronized void setLastUpdateTime(long lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
            tainted = true;
        }

        public synchronized void setEditMode(boolean editMode, boolean creatorMode) {
            this.creatorMode = creatorMode;
            this.editMode = editMode;
            tainted = true;
        }

        public synchronized void save(@NonNull ArrayList<Scenario.Item> state, @NonNull String title, boolean active, long time, @NonNull BitSet repeat) {
            Log.d("LOGTAG", "verifier: save");

            this.state = new ArrayList<>();

            for (Scenario.Item i: state) {
                this.state.add(new Scenario.Item(i));
            }

            this.title = title;
            this.active = active;
            this.time = time;
            this.repeat = (BitSet) repeat.clone();
            tainted = true;
        }

        public synchronized void cancelSaving() {
            this.state = null;
            tainted = true;
        }

        public synchronized void setExecute(boolean execute) {
            this.execute = execute;
            tainted = true;
        }

        public synchronized void setDelete(boolean delete) {
            this.delete = delete;
            tainted = true;
        }

        @Override
        public void onPostPublish(int statusCode) {
            if (statusCode < 0 || statusCode == 1) return;
            ScenarioViewer.Updater updater = weakUpdater.get();
            Bundle data = new Bundle();
            int status;

            if (updater == null) return;
            Log.d("LOGTAG", "scenarioVerifier.onPostPublish()");

            switch (statusCode) {
                case 2:
                    status = R.string.masterServerRequired;
                    break;
                case 3:
                    status = R.string.disconnected;
                    break;
                case 4:
                    status = R.string.encryptionError;
                    break;
                default:
                    status = R.string.unexpectedError;
                    break;
            }

            data.putInt("status", status);

            Message msg = updater.obtainMessage(0);
            msg.setData(data);
            updater.sendMessage(msg);
        }

        @Override
        public void onReceive(JSONObject data) {
            ScenarioViewer.Updater updater = weakUpdater.get();
            Log.d("LOGTAG", "scenarioVerifier.onReceive()");

            if (updater == null) {
                Sync.removeSyncProvider(source);
                return;
            }

            if (data == null) {
                updater.sendEmptyMessage(0);
                return;
            }

            Bundle msgData = new Bundle();

            try {
                msgData.putInt("status", data.getInt("status"));

                if (data.has("scenario")) {
                    msgData.putString("scenario", data.getJSONObject("scenario").toString());
                }

                if (data.has("isLocked")) {
                    msgData.putBoolean("isLocked", data.getBoolean("isLocked"));
                }

                if (data.has("lockSuccess")) {
                    msgData.putBoolean("lockSuccess", data.getBoolean("lockSuccess"));
                }

                if (data.has("saveSuccess")) {
                    msgData.putBoolean("saveSuccess", data.getBoolean("saveSuccess"));
                }

                if (data.has("execSuccess")) {
                    msgData.putBoolean("execSuccess", data.getBoolean("execSuccess"));
                }

                if (data.has("deleteSuccess")) {
                    msgData.putBoolean("deleteSuccess", data.getBoolean("deleteSuccess"));
                }

                if (data.has("deactivatedSID")) {
                    msgData.putInt("deactivatedSID", data.getInt("deactivatedSID"));
                }

                if (data.has("id")) {
                    msgData.putInt("id", data.getInt("id"));
                }

            } catch (JSONException e) {
                updater.sendEmptyMessage(0);
                e.printStackTrace();
                return;
            }

            Message msg = updater.obtainMessage(1);
            msg.setData(msgData);
            updater.sendMessage(msg);
        }

        @Override
        public JSONObject getQuery() {
            if (tainted) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("id", id);

                    if (editMode || creatorMode) {
                        data.put("lock", true);

                    } else {
                        data.put("lastUpdateTime", lastUpdateTime);
                    }

                    Log.d("LOGTAG", "rebuild query for verifier");

                    if (state != null) {
                        JSONArray array = new JSONArray();

                        for (Scenario.Item i: state) {
                            JSONObject obj = new JSONObject();
                            JSONObject ops = new JSONObject();
                            obj.put("serial", i.getSerial());

                            for (Map.Entry<String, Object> j: i.getOps().entrySet()) {
                                ops.put(j.getKey(), j.getValue());
                            }
                            obj.put("ops", ops);
                            array.put(obj);
                        }

                        JSONArray raw_repeat = new JSONArray();

                        for (int i = 0; i < 7; ++i) {
                            raw_repeat.put(repeat.get(i));
                        }

                        data.put("state", array);
                        data.put("title", title);
                        data.put("active", active);
                        data.put("time", time);
                        data.put("repeat", raw_repeat);
                    }

                    if (execute) {
                        data.put("execute", true);
                    }

                    if (delete) {
                        data.put("delete", true);
                    }

                    query.put("data", data);
                    tainted = false;

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return query;
        }
    }
}
