package com.lastutf445.home2.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.lastutf445.home2.R;

public class ColorPicker extends DialogFragment {

    private View view;
    private OnColorPicked c;
    private int red = 0, green = 0, blue = 0;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        view = inflater.inflate(R.layout.dialog_pickcolor, null, false);

        SeekBar.OnSeekBarChangeListener s = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar v, int progress, boolean fromUser) {
                switch (v.getId()) {
                    case R.id.pickRed:
                        red = progress;
                        break;
                    case R.id.pickGreen:
                        green = progress;
                        break;
                    case R.id.pickBlue:
                        blue = progress;
                        break;
                }

                render();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

        };

        ((SeekBar) view.findViewById(R.id.pickRed)).setProgress(red);
        ((SeekBar) view.findViewById(R.id.pickGreen)).setProgress(green);
        ((SeekBar) view.findViewById(R.id.pickBlue)).setProgress(blue);

        ((SeekBar) view.findViewById(R.id.pickRed)).setOnSeekBarChangeListener(s);
        ((SeekBar) view.findViewById(R.id.pickGreen)).setOnSeekBarChangeListener(s);
        ((SeekBar) view.findViewById(R.id.pickBlue)).setOnSeekBarChangeListener(s);

        render();

        view.findViewById(R.id.pickApply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (c != null) c.onColorPicked(getColor());
                dismiss();
            }
        });

        builder.setView(view);
        return builder.create();
    }

    public void setColor(String raw_color) {
        try {
            int color = Color.parseColor(raw_color);

            red = Color.red(color);
            green = Color.green(color);
            blue = Color.blue(color);

            //Log.d("LOGTAG", "color is set up: " + getColor());

        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private void render() {
        ((ImageView) view.findViewById(R.id.pickColor)).setColorFilter(
                Color.rgb(red, green, blue)
        );
    }

    private String getColor() {
        return "#" + Integer.toHexString(Color.rgb(red, green, blue)).substring(2);
    }

    public void setOnColorPicked(OnColorPicked c) {
        this.c = c;
    }

    public interface OnColorPicked {
        void onColorPicked(String color);
    }
}
