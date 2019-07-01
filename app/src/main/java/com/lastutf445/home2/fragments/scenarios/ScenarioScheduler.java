package com.lastutf445.home2.fragments.scenarios;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.fragments.dialog.SchedulerRepeat;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.util.NavigationFragment;

import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Locale;

public class ScenarioScheduler extends NavigationFragment {

    private long time;
    private boolean active;
    private BitSet repeat;

    private SimpleDateFormat dateFormat;
    private Calendar calendar;
    private int repeatMode;

    private TextView etTime;
    private TextView etRepeat;

    private ScenarioViewer.SchedulerSettingsProvider p;
    private SchedulerRepeat schedulerRepeat;
    private TimePickerDialog timePicker;

    /**
     * REPEAT MODES:
     * 0 - Once
     * 1 - Everyday
     * 2 - Mon to Fri
     * 3 - Custom
     */

    public ScenarioScheduler() {
        calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.scenario_scheduler, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        dateFormat = new SimpleDateFormat("HH:mm", Locale.UK);

        timePicker = new TimePickerDialog(
                getActivity(),
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        timePicker.updateTime(hourOfDay, minute);
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        time = calendar.getTimeInMillis();
                        sendToParent();
                        reload();
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );

        schedulerRepeat = new SchedulerRepeat();
        schedulerRepeat.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialog.cancel();
            }
        });

        schedulerRepeat.setOnRepeatModeListener(new SchedulerRepeat.OnRepeatModeChanged() {
            @Override
            public void onRepeatModeChanged(@NonNull BitSet repeat) {
                ScenarioScheduler.this.repeat = repeat;
                schedulerRepeat.dismiss();
                sendToParent();
                checkData();
                reload();
            }
        });

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.schedulerActive:
                        Switch enabled = ((Switch) ((ViewGroup) v).getChildAt(0));
                        enabled.setChecked(!enabled.isChecked());
                        active = enabled.isChecked();
                        sendToParent();
                        break;
                    case R.id.schedulerTime:
                    case R.id.schedulerTimePicker:
                        timePicker.show();
                        break;
                    case R.id.schedulerRepeat:
                    case R.id.schedulerRepeatPicker:
                        schedulerRepeat.setActive(repeat);
                        schedulerRepeat.show(getChildFragmentManager(), "schedulerRepeat");
                        break;
                }
            }
        };

        etTime = view.findViewById(R.id.schedulerTime);
        etRepeat = view.findViewById(R.id.schedulerRepeat);

        view.findViewById(R.id.schedulerActive).setOnClickListener(c);
        view.findViewById(R.id.schedulerTimePicker).setOnClickListener(c);
        view.findViewById(R.id.schedulerRepeatPicker).setOnClickListener(c);
        view.findViewById(R.id.schedulerRepeat).setOnClickListener(c);
        view.findViewById(R.id.schedulerTime).setOnClickListener(c);

        checkData();
        reload();
    }

    @Override
    protected void reload() {
        etTime.setText(dateFormat.format(calendar.getTime()));
        ((Switch) view.findViewById(R.id.schedulerActiveSwitcher)).setChecked(active);
        String repeatFormat;

        switch (repeatMode) {
            case 0:
                repeatFormat = DataLoader.getAppResources().getString(R.string.once);
                break;
            case 1:
                repeatFormat = DataLoader.getAppResources().getString(R.string.everyday);
                break;
            case 2:
                repeatFormat = DataLoader.getAppResources().getString(R.string.mon2fri);
                break;
            default:
                repeatFormat = DataLoader.getAppResources().getString(R.string.custom);
                break;
        }

        etRepeat.setText(repeatFormat);
    }

    private void checkData() {
        if (time <= 0) {
            time = System.currentTimeMillis();
        }

        if (repeat == null) {
            repeat = new BitSet(7);
            repeat.set(0, 6, false);
            sendToParent();
        }

        int clearBitIndex = repeat.nextClearBit(0);
        boolean isClear = true;

        for (int i = 0; i < 7; ++i) {
            isClear &= !repeat.get(i);
        }

        if (isClear) {
            repeatMode = 0;

        } else if (clearBitIndex == 7) {
            repeatMode = 1;

        } else if (clearBitIndex == 5 && !repeat.get(6)) {
            repeatMode = 2;

        } else {
            repeatMode = 3;
        }
    }

    public void setup(boolean active, long time, @NonNull BitSet repeat) {
        this.time = time;
        this.active = active;
        this.repeat = repeat;

        checkData();

        if (time != 0) {
            calendar.setTimeInMillis(time);
        }

        if (view != null) {
            reload();
        }
    }

    public void deactivated() {
        active = false;

        if (view != null) {
            ((Switch) view.findViewById(R.id.schedulerActiveSwitcher)).setChecked(false);
        }
    }

    public void sendToParent() {
        if (p != null) {
            p.apply(active, time, repeat);
        }
    }

    public void setProvider(@NonNull ScenarioViewer.SchedulerSettingsProvider p) {
        this.p = p;
    }
}
