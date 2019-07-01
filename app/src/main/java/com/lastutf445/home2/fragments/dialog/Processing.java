package com.lastutf445.home2.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;

public class Processing extends DialogFragment {

    @NonNull
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

    @Override
    public void onResume() {
        super.onResume();
        WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = DataLoader.getAppResources().getDimensionPixelSize(R.dimen.processingDialogWidth);
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getDialog().getWindow().setAttributes(params);
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

    public boolean isInactive() {
        return !(getDialog() != null && getDialog().isShowing() && !isRemoving());
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (d != null) d.onDismiss(dialog);
        super.dismiss();
    }
}
