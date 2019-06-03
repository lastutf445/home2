package com.lastutf445.home2.loaders;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.lastutf445.home2.R;
import com.lastutf445.home2.activities.MainActivity;
import com.lastutf445.home2.adapters.WidgetsAdapter;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.containers.Widget;
import com.lastutf445.home2.util.Configure;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WidgetsLoader {

    private static final int WIDGETS_ID = 0;
    private static final int WIDGETS_SERIAL = 1;
    private static final int WIDGETS_TYPE = 2;
    private static final int WIDGETS_OPS = 3;

    private static WeakReference<LayoutInflater> weakInflater;
    private static WeakReference<LinearLayout> weakContent;
    private static Handler handler;

    private static BottomSheetClickListener bottomSheetClickListener;
    private static WeakReference<BottomSheetDialog> weakBottomSheetDialog;
    private static WeakReference<View> weakBottomSheetView;
    private static int bottomSheetWidgetSerial;

    private static final SparseArray<Widget> widgets = new SparseArray<>();
    private static final SparseArray<Module> free = new SparseArray<>();
    private static final SparseIntArray serials = new SparseIntArray();
    @Nullable
    private static Configure.Connector bottomSheetConnect1, bottomSheetConnect2;
    private static WeakReference<WidgetsAdapter.Remover> weakRemover;
    private static int headId = 0, headSerial = 0;
    private static boolean unsaved = false;

    private static NumberFormat tempFormatter;
    private static NumberFormat humidityFormatter;

    public static void init(Handler handler, LayoutInflater inflater, LinearLayout content, BottomSheetDialog dialog, @NonNull View bottomSheetView) {
        weakBottomSheetView = new WeakReference<>(bottomSheetView);
        weakBottomSheetDialog = new WeakReference<>(dialog);
        weakInflater = new WeakReference<>(inflater);
        weakContent = new WeakReference<>(content);

        bottomSheetClickListener = new BottomSheetClickListener();
        BottomSheetButtonClickListener c = new BottomSheetButtonClickListener();

        bottomSheetView.findViewById(R.id.bottomSheetConfigure).setOnClickListener(c);
        bottomSheetView.findViewById(R.id.bottomSheetDelete).setOnClickListener(c);

        WidgetsLoader.handler = handler;
        reload();
    }

    public static void reload() {
        tempFormatter = new DecimalFormat("#0.0 °C");
        humidityFormatter = new DecimalFormat("#0.0 '%'");

        load();
        render();
        updateAll();
    }

    public static void load() {
        SQLiteDatabase db = DataLoader.getDb();
        Cursor cursor = db.rawQuery("SELECT * FROM dashboard", null);
        cursor.moveToFirst();

        headId = 0;
        headSerial = 0;
        unsaved = false;

        serials.clear();
        widgets.clear();
        free.clear();

        while (!cursor.isAfterLast()) {
            try {
                int id = cursor.getInt(WIDGETS_ID);
                int serial = cursor.getInt(WIDGETS_SERIAL);

                headSerial = Math.min(headSerial, serial);
                headId = Math.min(headId, id);

                String type = cursor.getString(WIDGETS_TYPE);
                JSONObject ops = new JSONObject(cursor.getString(WIDGETS_OPS));

                widgets.append(
                        id,
                        new Widget(id, serial, type, ops)
                );

                serials.put(serial, id);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            cursor.moveToNext();
        }

        cursor.close();

        SparseArray<Module> modules = ModulesLoader.getModules();

        for (int i = 0; i < modules.size(); ++i) {
            Module module = modules.valueAt(i);
            if (serials.get(module.getSerial(), Integer.MAX_VALUE) == Integer.MAX_VALUE) {
                free.put(module.getSerial(), module);
            }
        }
    }

    public static void render() {
        LayoutInflater inflater = weakInflater.get();
        LinearLayout content = weakContent.get();

        if (inflater == null || content == null) return;
        content.removeAllViews();

        synchronized (widgets) {
            for (int i = 0; i < widgets.size(); ++i) {
                renderWidget(widgets.valueAt(i), inflater, content, content.getChildCount());
            }
        }
    }

    private static void renderWidget(@NonNull Widget widget, @NonNull LayoutInflater inflater, @NonNull LinearLayout content, int pos) {
        Log.d("LOGTAG", "render: " + widget.getType());

        switch (widget.getType()) {
            case "temperature":
                renderTemperature(widget, inflater, content);
                break;
            case "humidity":
                renderHumidity(widget, inflater, content);
                break;
            case "lightrgb":
                renderLightRGB(widget, inflater, content);
                break;
            case "socket":
                renderSocket(widget, inflater, content);
                break;
            case "title":
                renderTitle(widget, inflater, content);
                break;
        }

        if (widget.getView() != null) {
            View view = widget.getView();
            content.addView(view, pos);

            view.setTag(widget.getSerial());
            view.setOnClickListener(bottomSheetClickListener);
        }
    }

    private static void renderSimpleWidget(@NonNull Widget widget, int layoutId, @NonNull LayoutInflater inflater, @NonNull LinearLayout content) {
        View view = inflater.inflate(layoutId, content, false);
        Module module = ModulesLoader.getModule(widget.getSerial());

        if (module != null) {
            ((TextView) view.findViewById(R.id.widgetTitle)).setText(
                    module.getTitle()
            );
        }

        widget.setView(view);
    }

    private static void renderTemperature(@NonNull Widget widget, @NonNull LayoutInflater inflater, @NonNull LinearLayout content) {
        renderSimpleWidget(widget, R.layout.widget_temperature, inflater, content);
    }

    private static void renderHumidity(@NonNull Widget widget, @NonNull LayoutInflater inflater, @NonNull LinearLayout content) {
        renderSimpleWidget(widget, R.layout.widget_humidity, inflater, content);
    }

    private static void renderLightRGB(@NonNull Widget widget, @NonNull LayoutInflater inflater, @NonNull LinearLayout content) {
        renderSimpleWidget(widget, R.layout.widget_lightrgb, inflater, content);
    }

    private static void renderSocket(@NonNull Widget widget, @NonNull LayoutInflater inflater, @NonNull LinearLayout content) {
        renderSimpleWidget(widget, R.layout.widget_socket, inflater, content);
    }

    private static void renderTitle(@NonNull Widget widget, @NonNull LayoutInflater inflater, @NonNull LinearLayout content) {
        renderSimpleWidget(widget, R.layout.widget_title, inflater, content);

        String title = widget.getString(
                "title",
                DataLoader.getAppResources().getString(R.string.defaultTitleTitle)
        );

        ((TextView) widget.getView().findViewById(R.id.widgetTitle)).setText(title);
    }

    @NonNull
    public static SparseArray<Widget> getWidgets() {
        return widgets;
    }

    @NonNull
    public static SparseArray<Module> getFree() {
        return free;
    }

    public static boolean isUnsaved() {
        return unsaved;
    }

    public static void updateAll() {
        for (int i = 0; i < widgets.size(); ++i)  {
            Widget widget = widgets.valueAt(i);
            createUpdateEvent(widget.getId());
        }
    }

    public static void createUpdateEvent(int id) {
        Message msg = handler.obtainMessage(1);
        Bundle data = new Bundle();
        data.putInt("id", id);
        msg.setData(data);
        handler.sendMessage(msg);
    }

    public static void update(int id) {
        Log.d("LOGTAG", "update request to " + id);

        Widget widget = widgets.get(id);
        if (widget == null) return;

        Module module = ModulesLoader.getModule(widget.getSerial());

        if (module != null && module.getSerial() == bottomSheetWidgetSerial) {
            bottomSheetClickListener.lastUpdated(module);
        }

        switch (widget.getType()) {
            case "temperature":
                updateTemperature(widget, module);
                break;
            case "humidity":
                updateHumidity(widget, module);
                break;
            case "lightrgb":
                updateLightRGB(widget, module);
                break;
            case "socket":
                updateSocket(widget, module);
                break;
            case "title":
                updateTitle(widget);
                break;
        }
    }

    private static void updateSimpleWidget(@NonNull View view, @Nullable String title, @Nullable String format, @Nullable String value) {
        ((TextView) view.findViewById(R.id.widgetTitle)).setText(title);
        View widgetValue = view.findViewById(R.id.widgetValue);

        if (widgetValue instanceof TextView && format != null) {
            ((TextView) widgetValue).setText(
                    value != null ? String.format(format, value) :
                            DataLoader.getAppResources().getString(R.string.notAvailableShort)
            );
        }
    }

    private static void updateTemperature(@NonNull Widget widget, @Nullable Module module) {
        if (widget.getView() == null || module == null) return;

        updateSimpleWidget(
                widget.getView(),
                module.getTitle(),
                null,
                null
        );

        try {
            ((TextView) widget.getView().findViewById(R.id.widgetValue)).setText(
                    tempFormatter.format(module.getVals().getDouble("temp"))
            );

        } catch (JSONException e) {
            ((TextView) widget.getView().findViewById(R.id.widgetValue)).setText(
                    R.string.notAvailableShort
            );

            //e.printStackTrace();
        }
    }

    private static void updateHumidity(@NonNull Widget widget, @Nullable Module module) {
        if (widget.getView() == null || module == null) return;

        updateSimpleWidget(
                widget.getView(),
                module.getTitle(),
                null,
                null
        );

        try {
            ((TextView) widget.getView().findViewById(R.id.widgetValue)).setText(
                    humidityFormatter.format(module.getVals().getDouble("humidity"))
            );

        } catch (JSONException e) {
            ((TextView) widget.getView().findViewById(R.id.widgetValue)).setText(
                    R.string.notAvailableShort
            );

            //e.printStackTrace();
        }
    }

    private static void updateLightRGB(@NonNull Widget widget, @Nullable Module module) {
        if (widget.getView() == null || module == null) return;
        updateSimpleWidget(
                widget.getView(),
                module.getTitle(),
                null,
                null
        );

        try {
            int color = Color.parseColor("#333333");

            if (module.getBoolean("lit", false)) {
                color = Color.parseColor(module.getString("color", "#008577"));
            }

            ((ImageView) widget.getView().findViewById(R.id.widgetValue)).setColorFilter(color, PorterDuff.Mode.SRC_IN);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateSocket(@NonNull Widget widget, @Nullable Module module) {
        if (widget.getView() == null || module == null) return;

        updateSimpleWidget(
                widget.getView(),
                module.getTitle(),
                null,
                null
        );

        try {
            int color = Color.parseColor("#aaaaaa");

            if (module.getBoolean("enabled", false)) {
                color = DataLoader.getAppResources().getColor(R.color.colorPrimary);
            }

            ((ImageView) widget.getView().findViewById(R.id.widgetValue)).setColorFilter(color, PorterDuff.Mode.SRC_IN);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateTitle(@NonNull Widget widget) {
        if (widget.getView() == null) return;

        updateSimpleWidget(
                widget.getView(),
                widget.getString("title", DataLoader.getAppResources().getString(R.string.unknownTitle)),
                null,
                null
        );
    }

    @Nullable
    public static Widget createWidget(@NonNull Module module) {
        if (serials.get(module.getSerial(), Integer.MAX_VALUE) != Integer.MAX_VALUE) return null;
        return new Widget(--headId, module.getSerial(), module.getType(), new JSONObject());
    }

    @NonNull
    public static Widget createUtilWidget(@NonNull String type, JSONObject ops) {
        return new Widget(--headId, --headSerial, type, ops);
    }

    public static boolean addWidget(@NonNull Widget widget) {
        if (serials.get(widget.getSerial(), Integer.MAX_VALUE) != Integer.MAX_VALUE) return false;

        SQLiteDatabase db = DataLoader.getDb();
        ContentValues cv = new ContentValues();

        cv.put("id", widget.getId());
        cv.put("serial", widget.getSerial());
        cv.put("type", widget.getType());
        cv.put("options", widget.getOps().toString());

        try {
            db.insertOrThrow("dashboard", null, cv);

        } catch (SQLiteException e) {
            e.printStackTrace();
            return false;
        }

        LayoutInflater inflater = weakInflater.get();
        LinearLayout content = weakContent.get();

        synchronized (widgets) {
            synchronized (serials) {
                if (content != null && inflater != null) {
                    renderWidget(widget, inflater, content, 0);
                    serials.put(widget.getSerial(), widget.getId());
                    widgets.put(widget.getId(), widget);
                    createUpdateEvent(widget.getId());
                    free.delete(widget.getSerial());
                    return true;
                }

                return false;
            }
        }
    }

    public static boolean replaceWidget(@NonNull Widget widget) {
        if (widgets.get(widget.getId()) == null) return false;

        LayoutInflater inflater = weakInflater.get();
        LinearLayout content = weakContent.get();

        if (content == null || inflater == null) return false;

        synchronized (widgets) {
            synchronized (serials) {
                int pos = content.indexOfChild(widgets.get(widget.getId()).getView());
                if (pos == -1) return false;

                widgets.put(widget.getId(), widget);
                content.removeViewAt(pos);

                renderWidget(widget, inflater, content, pos);
                createUpdateEvent(widget.getId());
                saveWidget(widget);

                return true;
            }
        }
    }

    public static void swapWidgets(int from, int to) {
        if (from == to) return;

        if (from < to) {
            for (int i = from; i < to; ++i) {
                swapAdjacentWidgets(i, i + 1);
            }
        } else {
            for (int i = from; i > to; --i) {
                swapAdjacentWidgets(i, i - 1);
            }
        }

        unsaved = true;
    }

    public static void swapAdjacentWidgets(int i, int j) {
        if (i == j) return;

        synchronized (widgets) {
            synchronized (serials) {
                Widget w1 = widgets.valueAt(i);
                Widget w2 = widgets.valueAt(j);

                int id1 = w1.getId();
                w1.setId(w2.getId());
                w2.setId(id1);

                serials.put(w1.getSerial(), w1.getId());
                serials.put(w2.getSerial(), w2.getId());
                widgets.put(w1.getId(), w1);
                widgets.put(w2.getId(), w2);
            }
        }
    }

    public static void swapViews(int i, int j) {
        if (i == j) return;

        synchronized (widgets) {
            synchronized (serials) {
                LayoutInflater inflater = weakInflater.get();
                LinearLayout content = weakContent.get();

                if (inflater != null && content != null) {
                    View y = content.getChildAt(i);
                    content.removeViewAt(i);
                    content.addView(y, j);
                }
            }
        }
    }

    public static void remove(@NonNull Widget widget) {
        synchronized (widgets) {
            synchronized (serials) {
                LinearLayout content = weakContent.get();

                if (content != null) {
                    content.removeView(widget.getView());
                }

                widgets.remove(widget.getId());
                serials.delete(widget.getSerial());
                unsaved = true;

                Module module = ModulesLoader.getModule(widget.getSerial());

                if (module != null) {
                    free.put(module.getSerial(), module);
                }
            }
        }
    }

    public static void removeAll() {
        LinearLayout content = weakContent.get();
        if (content == null) return;

        synchronized (widgets) {
            synchronized (serials) {
                content.removeAllViews();
                SQLiteDatabase db = DataLoader.getDb();
                db.delete("dashboard", null, null);

                for (int i = 0; i < widgets.size(); ++i) {
                    Widget widget = widgets.valueAt(i);
                    if (widget == null) continue;

                    Module module = ModulesLoader.getModule(widget.getSerial());

                    if (module != null) {
                        free.put(module.getSerial(), module);
                    }
                }

                widgets.clear();
                serials.clear();

                unsaved = false;
                headSerial = 0;
                headId = 0;
            }
        }
    }

    public static boolean saveWidget(@NonNull Widget widget) {
        SQLiteDatabase db = DataLoader.getDb();

        String[] args = {
                String.valueOf(widget.getSerial())
        };

        ContentValues cv = new ContentValues();
        cv.put("type", widget.getType());
        cv.put("options", widget.getOps().toString());

        try {
            db.update("dashboard", cv, "serial = ?", args);
            return true;

        } catch (SQLiteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean save() {
        SQLiteDatabase db = DataLoader.getDb();
        db.beginTransaction();

        try {
            db.delete("dashboard", null, null);
            int id = 0;
            int serial = 0;

            for (int i = 0; i < widgets.size(); ++i) {
                Widget widget = widgets.valueAt(i);

                ContentValues cv = new ContentValues();
                cv.put("id", id++);

                switch (widget.getType()) {
                    case "title":
                        cv.put("serial", --serial);
                        break;
                    default:
                        cv.put("serial", widget.getSerial());
                        break;
                }

                cv.put("type", widget.getType());
                cv.put("options", widget.getOps().toString());
                db.insertOrThrow("dashboard", null, cv);
            }

            db.setTransactionSuccessful();
            db.endTransaction();
            load();

            return true;

        } catch (SQLiteException e) {
            db.endTransaction();
            return false;
        }
    }

    public static void onModuleLinkChanged(@NonNull Module module, boolean linked) {
        int id = serials.get(module.getSerial(), Integer.MAX_VALUE);

        if (!linked) {
            callBottomSheetConnector(module, false);
            free.remove(module.getSerial());
            update(id);
            return;
        }

        if (id != Integer.MAX_VALUE) {
            Widget widget = widgets.get(id);

            if (widget.getType().equals(module.getType())) {
                createUpdateEvent(id);

            } else {
                replaceWidget(new Widget(
                        id,
                        module.getSerial(),
                        module.getType(),
                        new JSONObject()
                ));
            }

            return;
        }

        free.put(module.getSerial(), module);
        Log.d("LOGTAG", "addded to free");
    }

    public static void onModuleTitleUpdated(@NonNull Module module) {
        int id = serials.get(module.getSerial(), Integer.MAX_VALUE);

        if (id != Integer.MAX_VALUE) {
            createUpdateEvent(id);
        }

        callBottomSheetConnector(module, true);
    }

    public static void onModuleStateUpdated(@NonNull Module module) {
        int id = serials.get(module.getSerial(), Integer.MAX_VALUE);

        if (id != Integer.MAX_VALUE) {
            createUpdateEvent(id);
        }

        callBottomSheetConnector(module, true);
    }

    public static void callBottomSheetConnector(@NonNull Module module, boolean linked) {
        if (bottomSheetConnect1 != null && module.getSerial() == bottomSheetConnect1.getSerial()) {
            if (linked) bottomSheetConnect1.onModuleStateUpdated();
            else bottomSheetConnect1.onModuleRemoved();
        }

        if (bottomSheetConnect2 != null && module.getSerial() == bottomSheetConnect2.getSerial()) {
            if (linked) bottomSheetConnect2.onModuleStateUpdated();
            else bottomSheetConnect2.onModuleRemoved();
        }
    }

    public static void setBottomSheetConnector(Configure.Connector connector, int id) {
        if (id == 1) bottomSheetConnect1 = connector;
        else if (id == 2) bottomSheetConnect2 = connector;
    }

    public static void delBottomSheetConnector(int id) {
        if (id == 1) bottomSheetConnect1 = null;
        else if (id == 2) bottomSheetConnect2 = null;
    }

    public static void setWidgetsAdapterRemover(WidgetsAdapter.Remover remover) {
        weakRemover = new WeakReference<>(remover);
    }

    public static boolean callWidgetsAdapterRemover(int id) {
        WidgetsAdapter.Remover remover = weakRemover != null ? weakRemover.get() : null;
        Widget widget = widgets.get(id);

        if (remover != null) {
            remover.remove(widgets.indexOfKey(id));

        } else if (widget != null) {
            remove(widget);

        } else {
            return false;
        }

        return true;
    }

    private static class BottomSheetClickListener implements View.OnClickListener {
        @Override
        public void onClick(@NonNull View v) {
            if (!(v.getTag() instanceof Integer)) {
                return;
            }

            BottomSheetDialog dialog = weakBottomSheetDialog.get();
            View bottomSheet = weakBottomSheetView.get();
            bottomSheetWidgetSerial = (int) v.getTag();

            if (bottomSheet != null) {
                ((TextView) bottomSheet.findViewById(R.id.bottomSheetTitle)).setText(
                        String.format(Locale.ENGLISH, "%s %d",
                                DataLoader.getAppResources().getString(R.string.serial),
                                bottomSheetWidgetSerial
                        )
                );

                Module module = ModulesLoader.getModule(bottomSheetWidgetSerial);
                lastUpdated(module);

                if (module != null) {
                    bottomSheet.findViewById(R.id.bottomSheetConfigure).setClickable(true);

                    ((Button) bottomSheet.findViewById(R.id.bottomSheetConfigureButton)).setTextColor(
                            Color.parseColor("#333333")
                    );

                    ((ImageView) bottomSheet.findViewById(R.id.bottomSheetConfigureIcon)).setColorFilter(
                            Color.parseColor("#333333")
                    );

                } else {
                    bottomSheet.findViewById(R.id.bottomSheetConfigure).setClickable(false);

                    ((Button) bottomSheet.findViewById(R.id.bottomSheetConfigureButton)).setTextColor(
                            Color.parseColor("#999999")
                    );

                    ((ImageView) bottomSheet.findViewById(R.id.bottomSheetConfigureIcon)).setColorFilter(
                            Color.parseColor("#999999")
                    );
                }
            }

            if (dialog != null) {
                dialog.show();
            }

            return;
        }

        public void lastUpdated(@Nullable Module module) {
            View bottomSheet = weakBottomSheetView.get();

            if (bottomSheet != null) {
                TextView textView = bottomSheet.findViewById(R.id.bottomSheetLastUpdate);
                String formatted = DataLoader.getAppResources().getString(R.string.undefined);

                if (module != null) {
                    long lastUpdated = module.getLong("lastUpdated", 0);

                    if (lastUpdated != 0) {
                        Date date = new Date(lastUpdated);
                        formatted = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.ENGLISH).format(date);
                    }
                }

                textView.setText(
                        String.format(Locale.ENGLISH, "%s %s",
                                DataLoader.getAppResources().getString(R.string.lastUpdated),
                                formatted
                        )
                );
            }

        }
    }

    private static class BottomSheetButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(@NonNull View v) {
            switch (v.getId()) {
                case R.id.bottomSheetConfigure:
                    configure();
                    break;

                case R.id.bottomSheetDelete:
                    deleteWidget();
                    break;
            }
        }

        private void configure() {
            Module module = ModulesLoader.getModule(bottomSheetWidgetSerial);
            if (module == null) return;

            if (ModulesLoader.configure(2, module, FragmentsLoader.getPrimaryNavigationFragment())) {
                BottomSheetDialog dialog = weakBottomSheetDialog.get();
                if (dialog != null) dialog.cancel();
            }
        }

        private void deleteWidget() {
            Activity activity = MainActivity.getInstance();

            if (activity == null) {
                NotificationsLoader.makeToast("Unexpected error", true);
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    activity
            );

            Resources res = DataLoader.getAppResources();
            builder.setTitle(res.getString(R.string.widgetRemoveTitle));
            builder.setMessage(res.getString(R.string.widgetRemoveMessages));

            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(@NonNull DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteWidgetCallback();
                }
            });

            builder.create().show();
        }

        private void deleteWidgetCallback() {
            synchronized (serials) {
                synchronized (widgets) {
                    int id = serials.get(bottomSheetWidgetSerial, Integer.MAX_VALUE);

                    if (!callWidgetsAdapterRemover(id)) {
                        NotificationsLoader.makeToast("Unexpected error", true);

                    } else {
                        NotificationsLoader.makeToast("Deleted", true);
                        BottomSheetDialog dialog = weakBottomSheetDialog.get();
                        if (dialog != null) dialog.cancel();
                    }
                }
            }
        }
    }
}
