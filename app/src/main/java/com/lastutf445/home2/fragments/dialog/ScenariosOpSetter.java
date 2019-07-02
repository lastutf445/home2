package com.lastutf445.home2.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.lastutf445.home2.R;

public class ScenariosOpSetter extends DialogFragment {

    private String key;
    private String value;
    private Object sample;
    private OnValueSet onValueSet;

    private EditText opValue;
    private TextView opKey;
    private View view;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.scenario_ops_setter, null, false);
        opValue = view.findViewById(R.id.opValue);
        opKey = view.findViewById(R.id.opKey);

        opKey.setText(key);
        opValue.setHint(
                sample.getClass().getSimpleName()
        );

        if (value.length() != 0) {
            opValue.setText(value);
        }

        builder.setTitle(R.string.scenarioOpSetterTitle);
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
                if (onValueSet != null) {
                    onValueSet.onSet(key, opValue.getText().toString());
                }
            }
        });

        return builder.create();
    }

    public void setup(@NonNull String key, @NonNull Object sample, @NonNull String value) {
        this.key = key;
        this.sample = sample;
        this.value = value;
    }

    public Object getSample() {
        return sample;
    }

    public void setOnValueSet(@NonNull OnValueSet onValueSet) {
        this.onValueSet = onValueSet;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (dialog != null) dialog.cancel();
        super.onDismiss(dialog);
    }

    public interface OnValueSet {
        void onSet(@NonNull String key, String value);
    }
}
