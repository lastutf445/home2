package com.lastutf445.home2.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;

public class Processing extends DialogFragment {

    private String title = DataLoader.getAppResources().getString(R.string.processing);
    private DialogInterface.OnDismissListener d;
    private View view;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        view = inflater.inflate(R.layout.dialog_processing, null, false);
        builder.setView(view);
        setTitle();

        return builder.create();
    }

    public void setOnDismissListener(@NonNull DialogInterface.OnDismissListener d) {
        this.d = d;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
        setTitle();
    }

    private void setTitle() {
        if (view != null) {
            ((TextView) view.findViewById(R.id.processingTitle)).setText(title);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (d != null) d.onDismiss(dialog);
        super.dismiss();
    }
}
