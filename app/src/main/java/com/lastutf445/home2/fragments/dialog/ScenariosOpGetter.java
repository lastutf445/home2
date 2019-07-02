package com.lastutf445.home2.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.lastutf445.home2.R;

public class ScenariosOpGetter extends DialogFragment {

    private String key;
    private Object value;

    private TextView opValue;
    private TextView opType;
    private TextView opKey;
    private View view;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.scenario_ops_getter, null, false);
        opValue = view.findViewById(R.id.opValue);
        opType = view.findViewById(R.id.opType);
        opKey = view.findViewById(R.id.opKey);

        opKey.setText(key);
        opType.setText(value.getClass().getSimpleName());
        opValue.setText(value.toString());

        builder.setTitle(R.string.scenarioOpGetterTitle);
        builder.setView(view);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        return builder.create();
    }

    public void setup(@NonNull String key, @NonNull Object value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (dialog != null) dialog.cancel();
        super.onDismiss(dialog);
    }
}
