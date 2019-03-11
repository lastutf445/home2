package lastutf445.android.com.home2;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

public class Dashboard extends NavigationFragment {

    private LinearLayout content;
    private LayoutInflater inflater;
    private View view;

    private ArrayList<DashboardOption> dashboardOptions;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        content = view.findViewById(R.id.dashboard);
        refreshDashboard();

        this.inflater = inflater;

        for (DashboardOption i: dashboardOptions) {
            createDashboardBlock(i);
        }

        return view;
    }

    public void createDashboardBlock(DashboardOption op) {
        ArrayList<Integer> modules = op.modules;
        String type = op.type;
        View view = null;

        switch(type) {
            case "row_temp":
                view = createRowTemp(modules);
                break;
            case "row_humidity":
                view = createRowHumidity(modules);
                break;
            case "row_space":
                view = createRowSpace(modules);
                break;
            case "table_icons":
                view = createTableIcons(modules);
                break;
        }

        if (view != null) {
            content.addView(view);
        }
    }

    public static String getSubtitleByType(String type) {
        Resources r = MainActivity.getAppResources();

        switch (type) {
            case "temp":
                return r.getString(R.string.dashboard_temp_sensor);
            case "humidity":
                return r.getString(R.string.dashboard_humidity_sensor);
            default:
                return r.getString(R.string.dashboard_unknown_sensor);
        }
    }

    public static int getIconByType(String type) {
        switch (type) {
            case "temp":
                return R.drawable.thermometer;
            default:
                return R.drawable.warning;
        }
    }

    private View createRowTemp(ArrayList<Integer> modules) {
        View v = inflater.inflate(R.layout.dashboard_row_temp, content, false);

        if (modules.size() == 0) {
            return null;
        }

        ModuleOption m = Modules.getModule(modules.get(0));

        ((TextView) v.findViewById(R.id.dashboardRowTempValue)).setText(
                String.format("%s %s", m.getState(), "C")
                // TODO: Celsius degrees
        );

        ((TextView) v.findViewById(R.id.dashboardRowTempTitle)).setText(m.getTitle());
        ((TextView) v.findViewById(R.id.dashboardRowTempSubtitle)).setText(getSubtitleByType(m.getType()));

        return v;
    }

    private View createRowHumidity(ArrayList<Integer> modules) {
        View v = inflater.inflate(R.layout.dashboard_row_humidity, content, false);

        if (modules.size() == 0) {
            return null;
        }

        ModuleOption m = Modules.getModule(modules.get(0));

        ((TextView) v.findViewById(R.id.dashboardRowHumidityValue)).setText(
                String.format("%s %s", m.getState(), "%")
                // TODO: Percentages
        );

        ((TextView) v.findViewById(R.id.dashboardRowHumidityTitle)).setText(m.getTitle());
        ((TextView) v.findViewById(R.id.dashboardRowHumiditySubtitle)).setText(getSubtitleByType(m.getType()));

        return v;
    }

    private View createRowSpace(ArrayList<Integer> modules) {
        View v = inflater.inflate(R.layout.dashboard_row_space, content, false);
        return v;
    }

    private View createTableIcons(ArrayList<Integer> modules) {
        View v = inflater.inflate(R.layout.dashboard_table_icons, content, false);
        LinearLayout w = v.findViewById(R.id.dashboardTableIconsWrapper);
        LinearLayout row = null;

        TypedValue out = new TypedValue();

        getContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, out, true
        );

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
        );

        for (int i = 0; i < modules.size(); ++i) {
            if (i % 3 == 0) {

                if (w != null && row != null) {
                    w.addView(row);
                }

                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
            }

            ImageButton button = new ImageButton(getContext());
            button.setBackgroundResource(out.resourceId);

            button.setImageResource(getIconByType(Modules.getModule(modules.get(i)).getType()));
            button.setLayoutParams(params);
            row.addView(button);
        }

        if (w != null && row != null && row.getChildCount() > 0) {
            w.addView(row);
        }

        return v;
    }

    public boolean refreshDashboard() {

        try {
            ArrayList<Integer> a = new ArrayList<>();
            ArrayList<Integer> b = new ArrayList<>();
            ArrayList<Integer> c = new ArrayList<>();
            ArrayList<Integer> d = new ArrayList<>();
            ArrayList<Integer> e = new ArrayList<>();

            a.add(0);
            b.add(1);
            c.add(2);

            e.add(3);
            e.add(4);
            e.add(5);

            dashboardOptions = Database.getDashboard();
            dashboardOptions.add(new DashboardOption(0, "row_temp", a));
            dashboardOptions.add(new DashboardOption(1, "row_temp", b));
            dashboardOptions.add(new DashboardOption(2, "row_humidity", c));
            dashboardOptions.add(new DashboardOption(3, "row_space", d));
            dashboardOptions.add(new DashboardOption(4, "table_icons", e));
            return true;

        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return false;
    }

    public synchronized static void setSync(boolean checked) {

    }
}
