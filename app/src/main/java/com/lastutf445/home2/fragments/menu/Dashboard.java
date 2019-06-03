package com.lastutf445.home2.fragments.menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lastutf445.home2.R;
import com.lastutf445.home2.fragments.dashboard.CreateWidget;
import com.lastutf445.home2.fragments.dashboard.Widgets;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.loaders.WidgetsLoader;
import com.lastutf445.home2.util.NavigationFragment;

public class Dashboard extends NavigationFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.menu_dashboard, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.dashboardCreateWidget:
                        FragmentsLoader.addChild(new CreateWidget(), Dashboard.this);
                        break;
                    case R.id.dashboardMoveWidgets:
                        FragmentsLoader.addChild(new Widgets(), Dashboard.this);
                        break;
                    case R.id.dashboardRemoveWidgets:
                        removeAll();
                        break;
                }
            }
        };

        view.findViewById(R.id.dashboardCreateWidget).setOnClickListener(c);
        view.findViewById(R.id.dashboardMoveWidgets).setOnClickListener(c);
        view.findViewById(R.id.dashboardRemoveWidgets).setOnClickListener(c);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) reload();
    }

    @Override
    protected void reload() {
        View unsaved = view.findViewById(R.id.dashboardUnsaved);
        View saved = view.findViewById(R.id.dashboardSaved);

        if (WidgetsLoader.isUnsaved()) {
            unsaved.setVisibility(View.VISIBLE);
            saved.setVisibility(View.GONE);

        } else {
            unsaved.setVisibility(View.GONE);
            saved.setVisibility(View.VISIBLE);
        }
    }

    private void removeAll() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        Resources res = DataLoader.getAppResources();
        builder.setTitle(res.getString(R.string.widgetsRemoveTitle));
        builder.setMessage(res.getString(R.string.widgetsRemoveMessages));

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                WidgetsLoader.removeAll();
                NotificationsLoader.makeToast("Removed", true);
            }
        });

        builder.create().show();
    }

    @Override
    public void onDestroy() {
        try {
            //Sync.removeSyncProvider(Sync.PROVIDER_DASHBOARD);
        } catch (Exception e) {
            // lol
        }
        super.onDestroy();
    }

    @Override
    public void onResult(Bundle data) {
        reload();
    }
}
