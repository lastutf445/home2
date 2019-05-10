package com.lastutf445.home2.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lastutf445.home2.R;

public class Processing extends DialogFragment {

    private DialogInterface.OnDismissListener d;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_processing, null, false);
        builder.setView(view);

        return builder.create();
    }

    public void setOnDismissListener(@NonNull DialogInterface.OnDismissListener d) {
        this.d = d;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (d != null) d.onDismiss(dialog);
        super.dismiss();
    }
}
