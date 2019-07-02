package com.lastutf445.home2.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.lastutf445.home2.R;

import java.util.ArrayList;
import java.util.Collections;

public class ScenariosOpCreator extends DialogFragment {

    private OnClickListener c;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> data;

    public ScenariosOpCreator() {
        data = new ArrayList<>();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.scenariosOpCreatorTitle);

        adapter = new ArrayAdapter<>(
                getActivity(), R.layout.scenario_ops_item, R.id.opsItemTitle, data
        );

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

                if (c != null) {
                    c.onClick(data.get(which), which);
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        return builder.create();
    }

    public void setData(@NonNull ArrayList<String> data) {
        this.data.clear();
        this.data.addAll(data);
        Collections.sort(this.data);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void pushData(@NonNull String key) {
        this.data.add(key);
        Collections.sort(this.data);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void delete(@NonNull String key) {
        this.data.remove(key);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void deleteAll() {
        this.data.clear();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public int getDataSize() {
        return data.size();
    }

    public void setOnClickListener(@NonNull OnClickListener c) {
        this.c = c;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (dialog != null) dialog.cancel();
        super.onDismiss(dialog);
    }

    public interface OnClickListener {
        void onClick(@NonNull String key, int which);
    }
}
