package com.lastutf445.home2.fragments.scenarios;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.ScenariosAdapter;
import com.lastutf445.home2.containers.Scenario;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.ScenariosLoader;
import com.lastutf445.home2.loaders.UserLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SimpleAnimator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class Scenarios extends NavigationFragment {

    private AppCompatButton contentRetry;
    private LinearLayout contentLoad;
    private ScenariosAdapter adapter;
    private FrameLayout spinner;
    private RecyclerView content;
    private Connector connector;
    private TextView noContent;
    private Updater updater;
    private Scenario blank;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.menu_scenarios, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        content = view.findViewById(R.id.scenariosContent);
        noContent = view.findViewById(R.id.scenariosNoContent);
        content.setLayoutManager(new LinearLayoutManager(DataLoader.getAppContext()));

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()) {
                    case R.id.scenarioViewer:
                        ScenarioViewer scenarioViewer = new ScenarioViewer();
                        scenarioViewer.setCreatorMode(true);
                        scenarioViewer.setConnector(connector);
                        FragmentsLoader.addChild(scenarioViewer, Scenarios.this);
                        break;
                    case R.id.scenariosRetry:
                        reload();
                        break;
                }
            }
        };

        connector = new Connector() {
            @Override
            public void onUpdate(int id, @NonNull String title, final int size) {
                Log.d("LOGTAG", "update item by scenarios.connector");
                adapter.pushData(
                        new Scenario(id, title, new ArrayList<Scenario.Item>()) {
                            @Override
                            public int getDataSize() {
                                return size;
                            }
                        }
                );

                if (adapter.getItemCount() == 1) {
                    noContent.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCreate(int id, @NonNull String title, int size) {
                onUpdate(id, title, size);
            }

            @Override
            public void onDelete(int id) {
                adapter.deleteData(id);

                if (adapter.getItemCount() == 0) {
                    noContent.setVisibility(View.VISIBLE);
                }
            }
        };

        ScenariosAdapter.OnClickListener d = new ScenariosAdapter.OnClickListener() {
            @Override
            public void onClick(Scenario scenario) {
                ScenarioViewer scenarioViewer = new ScenarioViewer();
                scenarioViewer.setScenario(scenario);
                scenarioViewer.setConnector(connector);
                FragmentsLoader.addChild(scenarioViewer, Scenarios.this);
            }
        };

        ScenariosAdapter.Loader e = new ScenariosAdapter.Loader() {
            @Override
            public void onThresholdReached() {
                ScenariosLoader.loadMoreScenarios(adapter.getLastItemId());
            }
        };

        spinner = view.findViewById(R.id.scenariosSpinner);
        contentLoad = view.findViewById(R.id.scenariosContentLoad);
        contentRetry = contentLoad.findViewById(R.id.scenariosRetry);
        noContent.setVisibility(View.GONE);

        if (!UserLoader.isAuthenticated()) {
            view.findViewById(R.id.scenariosAuthReq).setVisibility(View.VISIBLE);
            view.findViewById(R.id.scenarioViewer).setVisibility(View.GONE);
            spinner.setVisibility(View.GONE);
            return;
        }

        SimpleAnimator.alpha2(contentLoad, 0, 0.0f);
        view.findViewById(R.id.scenarioViewer).setOnClickListener(c);
        contentRetry.setOnClickListener(c);

        adapter = new ScenariosAdapter(content, getLayoutInflater());
        updater = new Updater(this);
        content.setAdapter(adapter);
        adapter.setListener(d);
        adapter.setLoader(e);
        reload();
    }

    @Override
    protected void reload() {
        adapter.clearData();
        contentLoad.setClickable(false);
        SimpleAnimator.alpha2(spinner, 150, 1.0f);
        SimpleAnimator.alpha2(contentLoad, 250, 0.0f);
        ScenariosLoader.loadScenarios(updater);
    }

    @Override
    public void onDestroy() {
        ScenariosLoader.removeLoader();
        super.onDestroy();
    }

    public static class Updater extends Handler {
        private WeakReference<Scenarios> weakScenarios;
        private boolean firstLoad = true;

        public Updater(@NonNull Scenarios scenarios) {
            weakScenarios = new WeakReference<>(scenarios);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    fail(msg.getData().getInt(
                            "status", R.string.unexpectedError
                    ));
                    break;
                case 1:
                    load(msg.getData());
                    break;
            }
        }

        private void load(Bundle data) {
            Scenarios scenarios = weakScenarios.get();
            if (scenarios == null) return;

            if (data.getInt("status") != Sync.OK) {
                fail(R.string.unexpectedError);
                return;
            }

            String raw_json = data.getString("json");
            ArrayList<Scenario> list = convertPoly(raw_json);

            if (list == null) {
                fail(R.string.unexpectedError);
                return;
            }

            if (firstLoad) {
                    scenarios.adapter.setData(list);
                    SimpleAnimator.alpha2(scenarios.spinner, 250, 0.0f);
                    scenarios.contentLoad.setVisibility(View.GONE);
                    firstLoad = false;

                    if (list.size() > 0) {
                        scenarios.adapter.allowLoaderItem();
                    }

                } else {
                    for (Scenario i : list) {
                        scenarios.adapter.pushData(i);
                    }
                }

            Log.d("LOGTAG", "itemCount: " + scenarios.adapter.getItemCount());

            if (scenarios.adapter.getItemCount() == 0) {
                scenarios.noContent.setVisibility(View.VISIBLE);

            } else {
                scenarios.noContent.setVisibility(View.GONE);
            }

            ScenariosLoader.setLoaded();
            scenarios.adapter.setLoaded(
                    data.getBoolean("end", false)
            );
        }

        @Nullable
        private ArrayList<Scenario> convertPoly(@Nullable String raw_json) {
            if (raw_json == null) return null;
            ArrayList<Scenario> list = new ArrayList<>();

            try {
                JSONArray json = new JSONArray(raw_json);

                for (int i = 0; i < json.length(); ++i) {
                    try {
                        JSONObject obj = json.optJSONObject(i);

                        if (obj == null) {
                            return null;
                        }

                        final int dataSize = obj.getInt("size");

                        list.add(new Scenario(
                                obj.getInt("id"),
                                obj.getString("title"),
                                new ArrayList<Scenario.Item>()
                        ) {

                            @Override
                            public int getDataSize() {
                                return dataSize;
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                return list;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private void fail(int status) {
            final Scenarios scenarios = weakScenarios.get();
            if (scenarios == null) return;

            ScenariosLoader.setLoaded();

            if (firstLoad) {
                SimpleAnimator.alpha2(scenarios.spinner, 150, 0.0f);
                SimpleAnimator.alpha2(scenarios.contentLoad, 250, 1.0f);
                scenarios.contentLoad.setClickable(true);

            } else {
                // todo: manage this
                ScenariosAdapter.ScenarioLoaderViewHolder vh = scenarios.adapter.getLoaderItem();
                Log.d("LOGTAG", "can\'t loadMore...");

                if (vh != null) {
                    vh.stopped();
                }
            }

            NotificationsLoader.makeToast(
                    DataLoader.getAppResources().getString(status),
                    true
            );
        }
    }

    public interface Connector {
        void onUpdate(int id, @NonNull String title, int size);
        void onCreate(int id, @NonNull String title, int size);
        void onDelete(int id);
    }
}
