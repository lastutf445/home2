package com.lastutf445.home2.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.lastutf445.home2.R;

import java.util.BitSet;

public class SchedulerRepeatCustom extends DialogFragment {

    private OnApply onApply;
    private BitSet repeat;
    private View view;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        view = inflater.inflate(R.layout.dialog_scheduler_custom, null, false);
        builder.setView(view);

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (onApply != null) {
                    onApply.onApply(repeat);
                }

                dialog.cancel();
            }
        });

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkbox = (CheckBox) ((ViewGroup) v).getChildAt(0);
                boolean state = !checkbox.isChecked();

                checkbox.setChecked(state);

                switch (v.getId()) {
                    case R.id.schedulerRepeatCustomMon:
                        repeat.set(0, state);
                        break;
                    case R.id.schedulerRepeatCustomTue:
                        repeat.set(1, state);
                        break;
                    case R.id.schedulerRepeatCustomWed:
                        repeat.set(2, state);
                        break;
                    case R.id.schedulerRepeatCustomThu:
                        repeat.set(3, state);
                        break;
                    case R.id.schedulerRepeatCustomFri:
                        repeat.set(4, state);
                        break;
                    case R.id.schedulerRepeatCustomSat:
                        repeat.set(5, state);
                        break;
                    case R.id.schedulerRepeatCustomSun:
                        repeat.set(6, state);
                        break;
                }
            }
        };

        View[] vv = new View[7];
        vv[0] = view.findViewById(R.id.schedulerRepeatCustomMon);
        vv[1] = view.findViewById(R.id.schedulerRepeatCustomTue);
        vv[2] = view.findViewById(R.id.schedulerRepeatCustomWed);
        vv[3] = view.findViewById(R.id.schedulerRepeatCustomThu);
        vv[4] = view.findViewById(R.id.schedulerRepeatCustomFri);
        vv[5] = view.findViewById(R.id.schedulerRepeatCustomSat);
        vv[6] = view.findViewById(R.id.schedulerRepeatCustomSun);

        for (int i = 0; i < vv.length; ++i) {
            ((CheckBox) ((ViewGroup) vv[i]).getChildAt(0)).setChecked(
                    repeat.get(i)
            );

            vv[i].setOnClickListener(c);
        }


        if (repeat == null) {
            repeat = new BitSet(7);
        }

        return builder.create();
    }

    public void setRepeat(@NonNull BitSet repeat) {
        this.repeat = (BitSet) repeat.clone();
    }

    public void setOnApply(OnApply onApply) {
        this.onApply = onApply;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        dialog.cancel();
        super.onDismiss(dialog);
    }

    public interface OnApply {
        void onApply(@NonNull BitSet repeat);
    }
}
