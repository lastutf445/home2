package com.lastutf445.home2.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.WidgetsLoader;
import com.lastutf445.home2.network.Sender;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;

public class Dashboard extends NavigationFragment {

    private BottomSheetDialog bottomSheet;
    private View bottomSheetView;
    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        updater = new Updater(view);
        LinearLayout content = view.findViewById(R.id.dashboardContent);
        bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet, (ViewGroup) view, false);

        bottomSheet = new BottomSheetDialog(getActivity());
        bottomSheet.setContentView(bottomSheetView);

        WidgetsLoader.init(updater, getLayoutInflater(), content, bottomSheet, bottomSheetView);
    }

    @Override
    public void onDestroy() {
        try {
            Sync.removeTrigger(Sync.FRAGMENT_DASHBOARD_TRIGGER);
            Sender.unsubscribe();
        } catch (Exception e) {
            // lol
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        Sync.addTrigger(
                Sync.FRAGMENT_DASHBOARD_TRIGGER,
                new Runnable() {
                    @Override
                    public void run() {
                        updater.sendEmptyMessage(0);
                    }
                }
        );

        updater.sendEmptyMessage(0);
        Sender.subscribe(updater);
        super.onResume();
    }

    private static class Updater extends Handler {
        private WeakReference<View> weakView;

        public Updater(View view) {
            weakView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case -2:
                    updateStatus2(R.string.masterServer);
                    break;
                case -1:
                    updateStatus2(R.string.idle);
                    break;
                case 0:
                    updateNetworkState();
                    break;
                case 1:
                    updateWidget(msg.getData());
                    break;
            }
        }

        private void updateStatus2(int statusId) {
            View view = weakView.get();
            if (view == null) return;

            ((TextView) view.findViewById(R.id.dashboardStatus2)).setText(
                    DataLoader.getAppResources().getString(statusId)
            );
        }

        private void updateNetworkState() {
            View view = weakView.get();
            if (view == null) return;

            int state = Sync.getNetworkState();
            int title = R.string.notAvailable;

            switch (state) {
                case 0:
                    title = R.string.notAvailable;
                    break;
                case 1:
                    title = R.string.connectionMobile;
                    break;
                case 2:
                    title = R.string.connectionWiFi;
                    break;
            }

            ((TextView) view.findViewById(R.id.dashboardStatus)).setText(
                    DataLoader.getAppResources().getString(title)
            );
        }

        private void updateWidget(Bundle data) {
            if (data == null) return;
            WidgetsLoader.update(
                    data.getInt("id", Integer.MAX_VALUE)
            );
        }
    }
}
