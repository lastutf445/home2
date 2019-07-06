package com.lastutf445.home2.fragments.scenarios;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lastutf445.home2.R;
import com.lastutf445.home2.activities.MainActivity;
import com.lastutf445.home2.adapters.ScenariosOpsAdapter;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.fragments.dialog.Processing;
import com.lastutf445.home2.fragments.dialog.ScenariosOpCreator;
import com.lastutf445.home2.fragments.dialog.ScenariosOpGetter;
import com.lastutf445.home2.fragments.dialog.ScenariosOpSetter;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SimpleAnimator;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ScenarioOpsEditor extends NavigationFragment {

    private FloatingActionButton opsEdit, opsReload, opsApply, opsAdd;
    private ScenariosOpsAdapter adapter;
    private boolean opsEditExpanded;
    private RecyclerView content;
    private TextView noContent;

    private Processing processing;
    private ScenariosOpCreator opCreator;
    private ScenariosOpSetter opSetter;
    private ScenariosOpGetter opGetter;

    private boolean hasUnsavedChanges;
    private boolean creatorMode, editMode;
    private Updater updater;
    private Module module;

    private LinearLayout invisibleLayer;
    private Connector connector;
    private JSONObject opsSample;
    private AsyncSave asyncSave;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.scenario_ops_editor, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        updater = new Updater();
        content = view.findViewById(R.id.opsContent);
        content.setLayoutManager(new LinearLayoutManager(getActivity()));
        noContent = view.findViewById(R.id.opsNoContent);

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.opsAdd:
                        opsAdd();
                        onOpsEditClicked();
                        break;
                    case R.id.opsApply:
                        opsApply();
                        onOpsEditClicked();
                        break;
                    case R.id.opsReload:
                        opsRefresh();
                        onOpsEditClicked();
                        break;
                    case R.id.opsEdit:
                        onOpsEditClicked();
                        break;
                    case R.id.opsInvisibleLayer:
                        onOpsEditClicked();
                        break;
                    case R.id.opsInfo:
                        opsInfo();
                        break;
                }
            }
        };

        opsEdit = view.findViewById(R.id.opsEdit);
        opsApply = view.findViewById(R.id.opsApply);
        opsReload = view.findViewById(R.id.opsReload);
        opsAdd = view.findViewById(R.id.opsAdd);

        invisibleLayer = view.findViewById(R.id.opsInvisibleLayer);
        invisibleLayer.setOnClickListener(c);
        invisibleLayer.setClickable(false);

        opsAdd.setClickable(false);
        opsApply.setClickable(false);
        opsReload.setClickable(false);

        view.findViewById(R.id.opsInfo).setOnClickListener(c);
        opsAdd.setOnClickListener(c);
        opsApply.setOnClickListener(c);
        opsReload.setOnClickListener(c);
        opsEdit.setOnClickListener(c);

        connector = new Connector() {
            @Override
            public void requestEdit(int pos) {
                String key = adapter.getKeyByPos(pos);
                Object value = adapter.getValueByKey(key);

                if (value == null) {
                    NotificationsLoader.makeToast("Unexpected error", true);
                    return;
                }
                if (!editMode && !creatorMode) {
                    opGetter.setup(key, value);
                    opGetter.show(getChildFragmentManager(), "opGetter");
                    return;
                }

                try {
                    opSetter.setup(key, value, value.toString());
                    opSetter.show(getChildFragmentManager(), "opSetter");

                } catch (Exception e) {
                    NotificationsLoader.makeToast("Unexpected error", true);
                    e.printStackTrace();
                }
            }

            @Override
            public void requestDelete(final int pos) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity()
                );

                builder.setTitle(R.string.scenarioOpsEditorDeleteTitle);
                builder.setMessage(R.string.scenarioOpsEditorDeleteMessage);

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String key = adapter.getKeyByPos(pos);
                        hasUnsavedChanges = true;
                        opCreator.pushData(key);
                        adapter.delete(key);
                        reload();
                    }
                });

                builder.create().show();
            }
        };

        adapter = new ScenariosOpsAdapter(getLayoutInflater(), content);
        adapter.setDeletable(editMode || creatorMode);
        adapter.setConnector(connector);
        content.setAdapter(adapter);
        adapter.initCallback();

        processing = new Processing();
        processing.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (asyncSave != null) {
                    asyncSave.cancel(true);
                }

                dialog.cancel();
            }
        });

        opCreator = new ScenariosOpCreator();
        opCreator.setOnClickListener(new ScenariosOpCreator.OnClickListener() {
            @Override
            public void onClick(@NonNull String key, int which) {
                try {
                    opSetter.setup(key, opsSample.get(key), "");
                    opSetter.show(getChildFragmentManager(), "opSetter");

                } catch (JSONException e) {
                    NotificationsLoader.makeToast("Unexpected error", true);
                    e.printStackTrace();
                }
            }
        });

        opGetter = new ScenariosOpGetter();

        opSetter = new ScenariosOpSetter();
        opSetter.setOnValueSet(new ScenariosOpSetter.OnValueSet() {
            @Override
            public void onSet(@NonNull String key, String raw_value) {
                Object value;

                try {
                    switch (opSetter.getSample().getClass().getSimpleName()) {
                        case "Integer":
                            value = Integer.valueOf(raw_value);
                            break;
                        case "Boolean":
                            if (raw_value.trim().toLowerCase().equals("true")) {
                                value = true;

                            } else if (raw_value.trim().toLowerCase().equals("false")) {
                                value = false;

                            } else {
                                throw new Exception();
                            }
                            break;
                        case "String":
                            raw_value = raw_value.trim();
                            value = raw_value;

                            if (raw_value.length() == 0) {
                                NotificationsLoader.makeToast("Strings can'\t be empty", true);
                                return;
                            }
                            break;
                        default:
                            NotificationsLoader.makeToast("Unknown data type", true);
                            return;
                    }

                    adapter.pushData(key, value);
                    hasUnsavedChanges = true;
                    opCreator.delete(key);
                    reload();

                    NotificationsLoader.makeToast("Success", true);

                } catch (Exception e) {
                    NotificationsLoader.makeToast("Type mismatch", true);
                    //e.printStackTrace();
                }
            }
        });

        reloadData();
        /*if (opCreator.getDataSize() == 0) {
            NotificationsLoader.makeToast("The module doesn\'t have options", true);
            getActivity().onBackPressed();
        }*/

        reload();
    }

    @Override
    protected void reload() {
        if (adapter != null) {
            SimpleAnimator.alpha2(
                    noContent,
                    50,
                    adapter.getItemCount() > 0 ? 0.0f : 1.0f
            );
        }

        if (creatorMode || editMode) {
            opsEdit.show();

        } else {
            opsEdit.hide();
        }
    }

    public void reloadData() {
        adapter.setData(module);

        if (opGetter != null && opGetter.isResumed()) {
            opGetter.dismiss();
        }

        Module moduleSample = ModulesLoader.getModule(module.getSerial());
        opsSample = new JSONObject();
        opCreator.deleteAll();

        if (moduleSample != null) {
            opsSample = moduleSample.getOps();
            Iterator<String> it = opsSample.keys();

            while (it.hasNext()) {
                String key = it.next();
                if (!key.equals("lastUpdated") && !key.equals("channel")) {
                    opCreator.pushData(key);
                }
            }
        }

        Iterator<String> it = module.getOps().keys();
        while (it.hasNext()) {
            opCreator.delete(it.next());
        }
    }

    private void opsAdd() {
        if (opCreator.getDataSize() == 0) {
            NotificationsLoader.makeToast("All options are used", true);

        } else {
            opCreator.show(getChildFragmentManager(), "opCreator");
        }
    }

    private void opsApply() {
        if (creatorMode && adapter.getItemCount() == 0) {
            NotificationsLoader.makeToast("Options can\'t be empty, aborted", true);
            getActivity().onBackPressed();
            return;

        } else {
            processing.setTitle(
                    DataLoader.getAppResources().getString(R.string.verifying)
            );

            processing.show(getChildFragmentManager(), "processing");
        }

        asyncSave = new AsyncSave(adapter.getVerifier(), module, this);
        asyncSave.execute();
    }

    private void opsRefresh() {
        if (!hasUnsavedChanges) {
            NotificationsLoader.makeToast("Nothing changed!", true);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        builder.setTitle(R.string.scenarioOpsEditorReloadTitle);
        builder.setMessage(R.string.scenarioOpsEditorReloadMessage);

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(R.string.reload, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                hasUnsavedChanges = false;
                reloadData();
                reload();

                NotificationsLoader.makeToast("Reloaded", true);
            }
        });

        builder.create().show();
    }

    private void onOpsEditClicked() {
        if (opsEditExpanded) {
            opsAdd.setClickable(false);
            opsApply.setClickable(false);
            opsReload.setClickable(false);
            invisibleLayer.setClickable(false);

            opsEdit.setImageResource(R.drawable.edit);
            opsEdit.setBackgroundTintList(
                    ColorStateList.valueOf(
                            DataLoader.getAppResources().getColor(R.color.colorPrimary)
                    )
            );

            opsAdd.hide();
            updater.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            opsApply.hide();
                        }
                    }, 25
            );
            updater.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            opsReload.hide();
                        }
                    }, 50
            );

        } else {
            opsAdd.setClickable(true);
            opsApply.setClickable(true);
            opsReload.setClickable(true);
            invisibleLayer.setClickable(true);

            opsEdit.setImageResource(R.drawable.clear);
            opsEdit.setBackgroundTintList(
                    ColorStateList.valueOf(Color.BLACK)
            );

            opsReload.show();
            updater.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            opsApply.show();
                        }
                    }, 25
            );
            updater.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            opsAdd.show();
                        }
                    }, 50
            );

        }

        opsEditExpanded = !opsEditExpanded;
    }

    private void opsInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        View v = getLayoutInflater().inflate(
                R.layout.scenario_ops_editor_info, null, false
        );

        v.findViewById(R.id.opsInfoCurrent1).setVisibility(
                (editMode || creatorMode) ? View.GONE : View.VISIBLE
        );

        v.findViewById(R.id.opsInfoCurrent2).setVisibility(
                (editMode || creatorMode) ? View.VISIBLE : View.GONE
        );

        builder.setView(v);
        builder.setPositiveButton(R.string.ok, null);

        builder.create().show();
    }

    public void setModule(@NonNull Module module) {
        this.module = module;
    }

    public void setCreatorMode(boolean creatorMode) {
        this.creatorMode = creatorMode;

        if (creatorMode || editMode) {
            if (opsEdit != null) {
                opsEdit.show();
            }
        }
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;

        if (editMode || creatorMode) {
            if (opsEdit != null) {
                opsEdit.show();
            }

        } else {
            if (opsEdit != null) {
                opsEdit.hide();
            }
        }
    }

    public int getSerial() {
        return module != null ? module.getSerial() : -1;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) {
            MainActivity.hideKeyboard();
        }
    }

    @Override
    public boolean onBackPressed() {
        if (hasUnsavedChanges) {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    getActivity()
            );

            builder.setTitle(R.string.scenariosUnsavedTitle);
            builder.setMessage(R.string.scenariosUnsavedTitle);

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
    public void onDestroy() {
        super.onDestroy();
    }

    public static class Updater extends Handler {
        public Updater() {

        }
    }

    public interface Connector {
        void requestEdit(int pos);
        void requestDelete(int pos);
    }

    public static class AsyncSave extends AsyncTask<Void, Void, JSONObject> {
        private WeakReference<HashMap<String, Object>> weakData;
        private WeakReference<ScenarioOpsEditor> weakEditor;
        private WeakReference<Module> weakModule;

        public AsyncSave(@NonNull HashMap<String, Object> data, @NonNull Module module, @NonNull ScenarioOpsEditor editor) {
            weakData = new WeakReference<>(data);
            weakEditor = new WeakReference<>(editor);
            weakModule = new WeakReference<>(module);
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            HashMap<String, Object> data = weakData.get();
            JSONObject ops = new JSONObject();
            Module raw_module = weakModule.get();

            if (data == null || raw_module == null) {
                return null;
            }

            try {
                for (Map.Entry<String, Object> i : data.entrySet()) {
                    //Log.d("LOGTAG", "key: " + i.getKey());
                    ops.put(i.getKey(), i.getValue());
                }

                if (!ModulesLoader.validateOps(raw_module.getType(), ops)) {
                    return null;
                }

                return ops;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject ops) {
            ScenarioOpsEditor editor = weakEditor.get();

            if (isCancelled()) {
                return;

            } else if (ops == null || editor == null) {
                if (editor != null && !editor.processing.isInactive()) {
                    editor.processing.dismiss();
                }

                NotificationsLoader.makeToast("Validation error", true);

            } else {
                if (!editor.processing.isInactive()) {
                    editor.processing.dismiss();
                }

                editor.toParent.putInt("serial", editor.module.getSerial());
                editor.toParent.putString("ops", ops.toString());
                editor.hasUnsavedChanges = false;
                editor.getActivity().onBackPressed();
            }
        }
    }
}
