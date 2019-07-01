package com.lastutf445.home2.fragments.dialog;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.lastutf445.home2.R;

import java.util.BitSet;

public class SchedulerRepeat extends BottomSheetDialogFragment {

    private DialogInterface.OnDismissListener d;
    private SchedulerRepeatCustom custom;
    private OnRepeatModeChanged r;
    private int active = 0;
    private BitSet repeat;
    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.dialog_scheduler, container, false);
        init();
        return view;
    }

    public void init() {
        custom = new SchedulerRepeatCustom();
        custom.setOnApply(new SchedulerRepeatCustom.OnApply() {
            @Override
            public void onApply(@NonNull BitSet repeat) {
                SchedulerRepeat.this.repeat = repeat;
                sendToParent(3);
            }
        });

        int clearBitIndex = repeat.nextClearBit(0);
        boolean isClear = true;

        for (int i = 0; i < 7; ++i) {
            isClear &= !repeat.get(i);
        }

        if (isClear) {
            active = 0;

        } else if (clearBitIndex == 7) {
            active = 1;

        } else if (clearBitIndex == 5 && !repeat.get(6)) {
            active = 2;

        } else {
            active = 3;
        }

        AppCompatButton activeButton;

        switch (active) {
            case 1:
                activeButton = view.findViewById(R.id.schedulerEveryday);
                break;
            case 2:
                activeButton = view.findViewById(R.id.schedulerMon2Fri);
                break;
            case 3:
                activeButton = view.findViewById(R.id.schedulerCustom);
                break;
            default:
                activeButton = view.findViewById(R.id.schedulerOnce);
                break;
        }

        activeButton.setTextColor(
                Color.parseColor("#00796B")
        );

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int active;

                switch (v.getId()) {
                    case R.id.schedulerEveryday:
                        active = 1;
                        break;
                    case R.id.schedulerMon2Fri:
                        active = 2;
                        break;
                    case R.id.schedulerCustom:
                        active = 3;
                        break;
                    default:
                        active = 0;
                        break;
                }

                if (active == 3) {
                    custom.setRepeat(repeat);
                    custom.show(getActivity().getSupportFragmentManager(), "repeatCustom");
                    getDialog().cancel();

                } else {
                    sendToParent(active);
                }
            }
        };

        view.findViewById(R.id.schedulerOnce).setOnClickListener(c);
        view.findViewById(R.id.schedulerEveryday).setOnClickListener(c);
        view.findViewById(R.id.schedulerMon2Fri).setOnClickListener(c);
        view.findViewById(R.id.schedulerCustom).setOnClickListener(c);
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener d) {
        this.d = d;
    }

    public void setActive(@NonNull BitSet repeat) {
        this.repeat = (BitSet) repeat.clone();
    }

    public void setOnRepeatModeListener(@NonNull OnRepeatModeChanged r) {
        this.r = r;
    }

    private void sendToParent(int active) {
        switch (active) {
            case 1:
                repeat.set(0, 7, true);
                break;
            case 2:
                repeat.set(0, 5, true);
                repeat.set(5, false);
                repeat.set(6, false);
            case 3:
                break;
            default:
                repeat.set(0, 7, false);
                break;
        }

        if (r != null) {
            r.onRepeatModeChanged(repeat);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (d != null) d.onDismiss(dialog);
        super.dismiss();
    }

    public interface OnRepeatModeChanged {
        void onRepeatModeChanged(@NonNull BitSet repeat);
    }
}
