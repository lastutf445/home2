package com.lastutf445.home2.fragments.scenarios;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.ModulesAdapter;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.containers.Scenario;
import com.lastutf445.home2.fragments.dialog.Processing;
import com.lastutf445.home2.fragments.menu.Modules;
import com.lastutf445.home2.fragments.menu.Rename;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.ScenariosLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SimpleAnimator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ScenarioViewer extends NavigationFragment {

    private boolean editMode = false, creatorMode = false, isExpanded = false;
    private boolean pendingLock = false, hasUnsavedChanges = false;
    private SparseArray<Module> scenarioData;
    private boolean hasBeenLoaded = false;
    private String scenarioTitle;
    private boolean isLocked;
    private int scenarioId;

    private long schedulerTime = 0;
    private boolean schedulerActive = false;
    private BitSet schedulerRepeat = new BitSet();

    private Rename rename;
    private Processing processing;
    private AppCompatImageButton scenarioSave;
    private AppCompatImageButton scenarioSwitchBar;
    private FloatingActionButton scenarioAddModule;
    private LinearLayout scenarioNoContent;
    private ScrollView scenarioButtons;
    private LinearLayout scenarioShadow;
    private ScenarioScheduler scheduler;
    private ModulesAdapter adapter;
    private RecyclerView content;

    private Scenarios.Connector connector;
    private SchedulerSettingsProvider provider;
    private AsyncSaveState saveState;
    private Scenario scenarioSource;
    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.scenario, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        updater = new Updater(this);
        content = view.findViewById(R.id.scenarioContent);
        content.setLayoutManager(new LinearLayoutManager(DataLoader.getAppContext()));

        rename = new Rename();
        rename.setCallback(new Rename.Callback() {
            @Override
            public boolean onApply(String s) {
                String t = s.trim();

                if (t.length() == 0) {
                    NotificationsLoader.makeToast("Invalid title", true);
                    return false;
                }

                scenarioTitle = t;
                hasUnsavedChanges = true;
                reload();

                return true;
            }
        });

        processing = new Processing();
        processing.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (!hasBeenLoaded) {
                            getActivity().onBackPressed();

                        } else {
                            cancelSaving();
                        }

                        dialog.cancel();
                    }
                }
        );

        scheduler = new ScenarioScheduler();
        scheduler.setProvider(
                provider = new SchedulerSettingsProvider() {
                    @Override
                    public void apply(boolean active, long time, @NonNull BitSet repeat) {
                        if (schedulerActive != active) {
                            hasUnsavedChanges = true;
                        }

                        if (schedulerTime != time) {
                            hasUnsavedChanges = true;
                        }

                        if (!schedulerRepeat.equals(repeat)) {
                            hasUnsavedChanges = true;
                        }

                        schedulerActive = active;
                        schedulerTime = time;
                        schedulerRepeat = repeat;
                    }
                }
        );

        if (scenarioData == null) {
            scenarioId = -1;
            scenarioData = new SparseArray<>();
        }

        if (scenarioTitle == null || scenarioTitle.length() == 0) {
            scenarioTitle = DataLoader.getAppResources().getString(R.string.scenariosDefaultTitle);
        }

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOpsEditor(v);
            }
        };

        View.OnClickListener d = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.scenarioSwitchBar:
                    case R.id.scenarioShadow:
                        switchBar();
                        break;
                    case R.id.scenarioAddModule:
                        addModule();
                        break;
                    case R.id.scenarioSave:
                        lockOrSave();
                        break;
                    case R.id.scenarioExecute:
                        execute();
                        break;
                    case R.id.scenarioRename:
                        rename();
                        break;
                    case R.id.scenarioSchedule:
                        schedule();
                        break;
                    case R.id.scenarioDelete:
                        delete();
                        break;
                }
            }
        };

        scenarioSwitchBar = view.findViewById(R.id.scenarioSwitchBar);
        scenarioAddModule = view.findViewById(R.id.scenarioAddModule);
        scenarioNoContent = view.findViewById(R.id.scenarioNoContent);
        scenarioButtons = view.findViewById(R.id.scenarioButtons);
        scenarioShadow = view.findViewById(R.id.scenarioShadow);
        scenarioSave = view.findViewById(R.id.scenarioSave);

        SimpleAnimator.collapse(scenarioButtons, 0);
        scenarioSwitchBar.setOnClickListener(d);
        scenarioAddModule.setOnClickListener(d);
        scenarioSave.setOnClickListener(d);

        scenarioButtons.findViewById(R.id.scenarioExecute).setOnClickListener(d);
        scenarioButtons.findViewById(R.id.scenarioRename).setOnClickListener(d);
        scenarioButtons.findViewById(R.id.scenarioSchedule).setOnClickListener(d);
        scenarioButtons.findViewById(R.id.scenarioDelete).setOnClickListener(d);

        ScenariosLoader.createVerifier(
                scenarioId, updater
        );

        if (creatorMode) {
            ScenariosLoader.lock(true);
            //ScenariosLoader.lockingEnded();
            hasBeenLoaded = true;
        }

        content.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy < -10 && !scenarioAddModule.isShown() && (editMode || creatorMode)) {
                    scenarioAddModule.show();

                } else if (dy > 10 && scenarioAddModule.isShown()) {
                    scenarioAddModule.hide();
                }
            }
        });

        scenarioSave.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                //Log.d("LOGTAG", isLocked + " " + !editMode + " " + creatorMode);
                if (isLocked || !editMode || creatorMode) {
                    return false;
                }

                if (hasUnsavedChanges) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            getActivity()
                    );

                    builder.setTitle(R.string.scenariosUnsavedTitle);
                    builder.setMessage(R.string.scenariosUnsavedMessage);

                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(@NonNull DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.setPositiveButton(R.string.unlock, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            hasUnsavedChanges = false;
                            reloadScenario(scenarioSource);
                            onLongClick(v);
                        }
                    });

                    builder.create().show();

                } else {
                    unlock();
                    reloadUi();
                    NotificationsLoader.makeToast("Unlocked", true);
                }

                return true;
            }
        });

        scenarioShadow.setOnClickListener(d);
        scenarioShadow.setClickable(false);

        adapter = new ModulesAdapter(
                getLayoutInflater(),
                c
        );

        adapter.setData(scenarioData);
        adapter.setShowSerials(true);
        content.setAdapter(adapter);
        reload();
    }

    @Override
    protected void reload() {
        ((TextView) view.findViewById(R.id.scenarioTitle)).setText(
                scenarioTitle
        );

        adapter.setData(scenarioData);
        reloadUi();

        if (!hasBeenLoaded && processing.isInactive()) {
            processing.show(getChildFragmentManager(), "processing");
        }
    }

    private void reloadUi() {
        boolean edit = (editMode || creatorMode);

        if (!edit) {
            scenarioAddModule.hide();

            scenarioSave.setImageResource(
                    isLocked ? R.drawable.lock :
                            R.drawable.lock_open
            );

        } else {
            scenarioAddModule.show();

            scenarioSave.setImageResource(
                    R.drawable.save
            );
        }

        if (scenarioData.size() == 0) {
            scenarioNoContent.setVisibility(View.VISIBLE);

        } else {
            scenarioNoContent.setVisibility(View.GONE);
        }
    }

    public void setCreatorMode(boolean creatorMode) {
        this.creatorMode = creatorMode;
    }

    public void setConnector(Scenarios.Connector connector) {
        this.connector = connector;
    }

    public void setScenario(@NonNull Scenario scenario) {
        scenarioTitle = scenario.getTitle();
        scenarioId = scenario.getId();
        scenarioSource = scenario;

        schedulerActive = scenario.getActive();
        schedulerTime = scenario.getTime();
        schedulerRepeat = scenario.getRepeat();

        if (scenarioTitle == null || scenarioTitle.length() == 0) {
            scenarioTitle = DataLoader.getAppResources().getString(R.string.scenariosDefaultTitle);
        }

        if (scenarioData == null) {
            scenarioData = new SparseArray<>();
        }

        if (schedulerTime <= 0) {
            schedulerTime = System.currentTimeMillis();
        }

        ArrayList<Scenario.Item> data = scenario.getData();
        scenarioData.clear();

        for (int i = 0; i < data.size(); ++i) {
            scenarioData.put(
                    i, ScenarioItem2Module(data.get(i))
            );
        }
    }

    public void reloadScenario(@NonNull Scenario scenario) {
        adapter.deleteAll();
        setScenario(scenario);
        reload();
    }

    private Module ScenarioItem2Module(@NonNull Scenario.Item item) {
        Module module = ModulesLoader.getModule(item.getSerial());

        HashMap<String, Object> itemOps = item.getOps();
        JSONObject moduleOps = new JSONObject();

        for (Map.Entry<String, Object> i: itemOps.entrySet()) {
            try {
                moduleOps.put(i.getKey(), i.getValue());

            } catch (JSONException e) {
                NotificationsLoader.makeToast("Unexpected error", true);
                getActivity().onBackPressed();
                e.printStackTrace();
            }
        }

        try {
            return new Module(
                    item.getSerial(),
                    module == null ? "unknown" : module.getType(),
                    "",
                    Sync.DEFAULT_PORT,
                    module == null ? null : module.getTitle(),
                    moduleOps,
                    new JSONObject(),
                    false
            );

        } catch (IOException e) {
            NotificationsLoader.makeToast("Unexpected error", true);
            getActivity().onBackPressed();
            e.printStackTrace();
            return null;
        }
    }

    private void openOpsEditor(View v) {
        int pos = content.getChildLayoutPosition(v);
        Log.d("LOGTAG", "ops pos: " + pos);

        ScenarioOpsEditor editor = new ScenarioOpsEditor();
        editor.setModule(scenarioData.valueAt(pos));
        FragmentsLoader.addChild(editor, this);
    }

    private void switchBar() {
        if (isExpanded) {
            //Log.d("LOGTAG", "collapse");
            SimpleAnimator.collapse(scenarioButtons, 250);
            scenarioSwitchBar.setImageResource(R.drawable.expand_more);

            scenarioShadow.setClickable(false);
            SimpleAnimator.alpha2(scenarioShadow, 250, 0.0f);

        } else {
            //Log.d("LOGTAG", "expand");
            SimpleAnimator.expand(scenarioButtons, 150);
            scenarioSwitchBar.setImageResource(R.drawable.expand_less);

            scenarioShadow.setClickable(true);
            SimpleAnimator.alpha2(scenarioShadow, 250, 1.0f);
        }

        isExpanded = !isExpanded;
    }

    private void addModule() {
        Modules modules = new Modules();
        modules.setModules(ModulesLoader.getModules(), true);
        FragmentsLoader.addChild(modules, this);
    }

    private void lockOrSave() {
        if (isExpanded) switchBar();

        if (!(editMode || creatorMode)) {
            lock();
        } else {
            save();
        }

        reloadUi();
    }

    private void lock() {
        Log.d("LOGTAG", "REQUEST LOCK");

        processing.setTitle(
                DataLoader.getAppResources().getString(R.string.scenarioLocking)
        );

        //if (processing.isInactive()) {
            processing.show(getChildFragmentManager(), "processing");
        //}

        ScenariosLoader.lock(creatorMode);
        pendingLock = true;
    }

    private void unlock() {
        ScenariosLoader.unlock();
        creatorMode = false;
        pendingLock = false;
        editMode = false;
    }

    private void save() {
        if (scenarioData.size() == 0) {
            NotificationsLoader.makeToast("Scenario shouldn't be empty", true);
            return;
        }

        Log.d("LOGTAG", "REQUEST SAVE");
        processing.setTitle(
                DataLoader.getAppResources().getString(R.string.scenarioSaving)
        );

        processing.show(getChildFragmentManager(), "processing");
        saveState = new AsyncSaveState(this);
        saveState.execute();
    }

    private void cancelSaving() {
        if (saveState != null) {
            saveState.cancel(true);
            ScenariosLoader.savingEnded();
        }

        saveState = null;
    }

    // execute if no locks
    private void execute() {
        if (isLocked || editMode || creatorMode) {
            NotificationsLoader.makeToast("The scenario is locked", true);
            return;
        }

        processing.setTitle(
                DataLoader.getAppResources().getString(R.string.executing)
        );

        if (processing.isInactive()) {
            processing.show(getChildFragmentManager(), "processing");
        }

        ScenariosLoader.execute(true);
        switchBar();
    }

    private void rename() {
        if (!editMode && !creatorMode) {
            NotificationsLoader.makeToast("You should lock the scenario", true);
            return;
        }

        rename.setOld(scenarioTitle);
        rename.setTitle(
                DataLoader.getAppResources().getString(R.string.scenarioRenameTitle)
        );

        rename.setHint(
                DataLoader.getAppResources().getString(R.string.moduleHint)
        );

        FragmentsLoader.addChild(rename, this);
        switchBar();
    }

    private void schedule() {
        if (!editMode && !creatorMode) {
            NotificationsLoader.makeToast("You should lock the scenario", true);
            return;
        }

        scheduler.setup(schedulerActive, schedulerTime, schedulerRepeat);
        FragmentsLoader.addChild(scheduler, this);
        switchBar();
    }

    // delete if locked by other guys
    private void delete() {
        if (isLocked && !(editMode || creatorMode)) {
            NotificationsLoader.makeToast("The scenario is locked", true);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        builder.setTitle(R.string.scenariosDeleteTitle);
        builder.setMessage(R.string.scenariosDeleteMessage);

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (creatorMode) {
                    onDelete();
                    NotificationsLoader.makeToast("Success", true);
                    getActivity().onBackPressed();
                    return;
                }

                processing.setTitle(
                        DataLoader.getAppResources().getString(R.string.deleting)
                );

                if (processing.isInactive()) {
                    processing.show(getChildFragmentManager(), "processing");
                }

                ScenariosLoader.delete(true);
            }
        });

        builder.create().show();
        switchBar();
    }

    private void onUpdate() {
        if (connector != null) {
            connector.onUpdate(
                    scenarioId,
                    scenarioTitle,
                    scenarioData.size()
            );
        }
    }

    private void onCreate() {
        if (connector != null) {
            connector.onCreate(
                    scenarioId,
                    scenarioTitle,
                    scenarioData.size()
            );
        }
    }

    private void onDelete() {
        if (connector != null) {
            connector.onDelete(
                    scenarioId
            );
        }
    }

    @Override
    public void onDestroy() {
        ScenariosLoader.freeScenario();
        super.onDestroy();
    }

    @Override
    public boolean onBackPressed() {
        if (hasUnsavedChanges) {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    getActivity()
            );

            builder.setTitle(R.string.scenariosUnsavedTitle);
            builder.setMessage(R.string.scenariosUnsavedMessage);

            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(@NonNull DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    hasUnsavedChanges = false;
                    getActivity().onBackPressed();
                }
            });

            builder.create().show();
            return false;

        } else {
            return true;
        }
    }

    @Override
    public void onResult(Bundle data) {
        if (data.containsKey("serial")) {
            Log.d("LOGTAG", "serial: " + data.getInt("serial"));
            Module module = ModulesLoader.getModule(data.getInt("serial"));

            if (module == null) {
                NotificationsLoader.makeToast("Unexpected error", true);
                return;
            }

            try {
                Module dummyModule = new Module(
                        module.getSerial(),
                        module.getType(),
                        "",
                        Sync.DEFAULT_PORT,
                        module.getTitle(),
                        new JSONObject(),
                        new JSONObject(),
                        false
                );

                scenarioData.put(
                        scenarioData.size(),
                        dummyModule
                );

                adapter.notifyItemInserted(scenarioData.size() - 1);
                scenarioNoContent.setVisibility(View.GONE);
                hasUnsavedChanges = true;

            } catch (IOException e) {
                NotificationsLoader.makeToast("Unexpected error", true);
                e.printStackTrace();
            }
        }
    }

    public static class Updater extends Handler {
        private WeakReference<ScenarioViewer> weakViewer;
        private boolean deleted = false;

        public Updater(ScenarioViewer viewer) {
            weakViewer = new WeakReference<>(viewer);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    end(msg.getData().getInt(
                            "status", R.string.unexpectedError
                    ));
                    break;
                case 1:
                    verifier(msg.getData());
                    break;
            }
        }

        private void verifier(Bundle data) {
            ScenarioViewer viewer = weakViewer.get();
            if (viewer == null || deleted) return;

            if (data.containsKey("isLocked")) {
                viewer.isLocked = data.getBoolean("isLocked");
                viewer.reloadUi();
            }

            if (data.getInt("id", -1) != -1) {
                int id = data.getInt("id", -1);
                viewer.scenarioId = id;
                ScenariosLoader.setId(id);
            }

            if (data.containsKey("saveSuccess")) {
                if (!data.getBoolean("saveSuccess")) {
                    end(
                            data.getInt("status") == Sync.UNKNOWN_MODULE ?
                            R.string.scenarioUnknownModuleError : R.string.unexpectedError
                    );

                } else {
                    if (viewer.creatorMode) {
                        viewer.onCreate();

                    } else {
                        viewer.onUpdate();
                    }

                    viewer.hasUnsavedChanges = false;
                    viewer.creatorMode = false;
                    viewer.editMode = true;
                    end(R.string.success);
                }

                ScenariosLoader.savingEnded();
            }

            if (data.containsKey("lockSuccess")) {
                if (!data.getBoolean("lockSuccess")) {
                    end(R.string.unableToLock);
                    viewer.unlock();

                } else if (viewer.pendingLock) {
                    end(R.string.success);
                    viewer.editMode = true;
                    viewer.pendingLock = false;
                    viewer.reloadUi();
                }
            }

            if (data.getInt("status") == Sync.SCENARIO_DELETED) {
                if (viewer.hasBeenLoaded) {
                    viewer.onDelete();
                    viewer.editMode = false;
                    viewer.creatorMode = true;
                    viewer.scenarioId = -1;
                    ScenariosLoader.setId(-1);
                    ScenariosLoader.lock(true);
                    viewer.hasUnsavedChanges = true;
                    viewer.reloadUi();
                    NotificationsLoader.makeToast("Source has been deleted, creator mode loaded", true);
                    end(-1);

                } else {
                    end(R.string.unexpectedError);
                }
            }

            // possible (or not) tricky case: deactivated by executor and activated by user at the same time
            if (data.getInt("status") == Sync.SCENARIO_EDITED && data.containsKey("deactivatedSID")) {
                if (viewer.scenarioId == data.getInt("deactivatedSID") && viewer.schedulerActive && (viewer.editMode || viewer.creatorMode)) {
                    viewer.scenarioSource.setActive(false);
                    viewer.schedulerActive = false;
                    viewer.scheduler.deactivated();
                    NotificationsLoader.makeToast("Scenario has been executed, deactivated", true);
                }
            }

            if (data.containsKey("execSuccess")) {
                end(
                        data.getBoolean("execSuccess") ?
                                R.string.success : R.string.unexpectedError
                );
            }

            if (data.containsKey("deleteSuccess")) {
                end(
                        data.getBoolean("deleteSuccess") ?
                                R.string.success : R.string.unexpectedError
                );

                if (data.getBoolean("deleteSuccess")) {
                    deleted = true;
                    ScenariosLoader.freeScenario();
                    this.removeMessages(0);
                    this.removeMessages(1);
                    viewer.onDelete();
                    viewer.getActivity().onBackPressed();
                }
            }

            // i do not trust you and check everything
            if (data.containsKey("scenario") && !viewer.editMode && !viewer.creatorMode) {
                try {
                    JSONObject json = new JSONObject(
                            data.getString("scenario")
                    );

                    ArrayList<Scenario.Item> body = new ArrayList<>();
                    JSONArray array = json.getJSONArray("data");

                    for (int i = 0; i < array.length(); ++i) {
                        JSONObject obj = array.getJSONObject(i);
                        JSONObject raw_ops = obj.getJSONObject("ops");
                        HashMap<String, Object> ops = new HashMap<>();

                        Iterator<String> it = raw_ops.keys();

                        while (it.hasNext()) {
                            String key = it.next();
                            ops.put(key, raw_ops.get("ops"));
                        }

                        body.add(new Scenario.Item(
                                obj.getInt("serial"), ops
                        ));
                    }

                    Scenario scenario = new Scenario(
                            json.getInt("id"),
                            json.getString("title"),
                            body
                    );

                    BitSet repeat = new BitSet(7);
                    JSONArray raw_repeat = json.getJSONArray("repeat");

                    for (int i = 0; i < 7; ++i) {
                        repeat.set(i, raw_repeat.getBoolean(i));
                    }

                    scenario.setActive(json.getBoolean("active"));
                    scenario.setTime(json.getLong("time"));
                    scenario.setRepeat(repeat);

                    ScenariosLoader.setLastUpdateTime(json.getLong("lastUpdateTime"));
                    viewer.reloadScenario(scenario);
                    viewer.hasBeenLoaded = true;
                    end(-1);

                } catch (JSONException e) {
                    end(
                            viewer.hasBeenLoaded ? -1 :
                            R.string.unexpectedError
                    );
                    e.printStackTrace();
                }
            }
        }

        private void end(int stringId) {
            ScenarioViewer viewer = weakViewer.get();

            if (stringId != -1) {
                NotificationsLoader.makeToast(
                        DataLoader.getAppResources().getString(stringId),
                        true
                );
            }

            ScenariosLoader.execute(false);
            ScenariosLoader.delete(false);

            if (viewer != null && viewer.processing != null) {
                if (!viewer.processing.isInactive()) {
                    viewer.processing.dismiss();
                }
            }
        }
    }

    public static class AsyncSaveState extends AsyncTask<Void, Void, Scenario> {
        private WeakReference<ScenarioViewer> weakViewer;

        public AsyncSaveState(@NonNull ScenarioViewer viewer) {
            weakViewer = new WeakReference<>(viewer);
        }

        @Override
        protected Scenario doInBackground(Void... voids) {
            try {
                ScenarioViewer viewer = weakViewer.get();
                ArrayList<Scenario.Item> data = new ArrayList<>();

                for (int i = 0; i < viewer.scenarioData.size(); ++i) {
                    Module module = viewer.scenarioData.valueAt(i);
                    HashMap<String, Object> ops = new HashMap<>();
                    Iterator<String> it = module.getOps().keys();

                    while (it.hasNext()) {
                        String key = it.next();
                        ops.put(key, module.getOps().get(key));
                    }

                    data.add(new Scenario.Item(
                            module.getSerial(), ops
                    ));

                    if (isCancelled()) {
                        return null;
                    }
                }

                Scenario scenario = new Scenario(
                        viewer.scenarioId,
                        viewer.scenarioTitle,
                        data
                );

                scenario.setActive(viewer.schedulerActive);
                scenario.setTime(viewer.schedulerTime);
                scenario.setRepeat(viewer.schedulerRepeat);

                return scenario;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Scenario scenario) {
            ScenarioViewer viewer = weakViewer.get();

            if (isCancelled()) {
                NotificationsLoader.makeToast("Canceled", true);

            } else if (scenario == null) {
                Log.d("LOGTAG", "Unable to save!");
                NotificationsLoader.makeToast("Unable to save", true);

                if (viewer != null && viewer.processing != null) {
                    if (viewer.processing.isVisible()) {
                        viewer.processing.dismiss();
                    }
                }

            } else {
                ScenariosLoader.save(
                        scenario.getData(),
                        scenario.getTitle(),
                        scenario.getActive(),
                        scenario.getTime(),
                        scenario.getRepeat()
                );
            }
        }
    }

    public interface SchedulerSettingsProvider {
        void apply(boolean active, long time, @NonNull BitSet repeat);
    }
}
