package com.lastutf445.home2.util;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.lastutf445.home2.R;
import com.lastutf445.home2.activities.MainActivity;
import com.lastutf445.home2.loaders.DataLoader;

public class IntroFragment extends Fragment {

    private int iconId, titleId, descId, layoutId;
    private TextView title, desc;
    private ImageView icon;
    private Button enter;
    private View view;

    public IntroFragment(@DrawableRes int iconId, @StringRes int titleId, @StringRes int descId) {
        this.iconId = iconId;
        this.titleId = titleId;
        this.descId = descId;
        this.layoutId = R.layout.introduce_fragment;
    }

    public IntroFragment(@DrawableRes int iconId, @StringRes int titleId, @StringRes int descId, @LayoutRes int layoutId) {
        this.iconId = iconId;
        this.titleId = titleId;
        this.descId = descId;
        this.layoutId = layoutId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(layoutId, container, false);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        icon = view.findViewById(R.id.introIcon);
        title = view.findViewById(R.id.introTitle);
        desc = view.findViewById(R.id.introDesc);
        enter = view.findViewById(R.id.introEnd);

        icon.setImageResource(iconId);
        title.setText(titleId);

        if (desc != null) {
            desc.setText(descId);

        } else {
            enter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DataLoader.setWithoutSync("FirstStart", false);
                    DataLoader.save();

                    Intent i = new Intent(getActivity(), MainActivity.class);
                    startActivity(i);
                    getActivity().finish();
                }
            });
        }
    }
}
