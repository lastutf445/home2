package com.lastutf445.home2.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.WidgetsLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;

public class Dashboard extends NavigationFragment {

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
        LinearLayout sheet = view.findViewById(R.id.bottomSheet);

        BottomSheetBehavior behavior = BottomSheetBehavior.from(sheet);
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        WidgetsLoader.init(updater, getLayoutInflater(), content, behavior);

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
    }

    @Override
    public void onDestroy() {
        // Sync.removeTrigger(Sync.FRAGMENT_DASHBOARD_TRIGGER);
        // it causes crashes
        super.onDestroy();
    }

    private static class Updater extends Handler {
        private WeakReference<View> weakView;

        public Updater(View view) {
            weakView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    updateNetworkState();
                    break;
                case 1:
                    updateWidget(msg.getData());
                    break;
            }
        }

        private void updateNetworkState() {
            View view = weakView.get();
            if (view == null) return;

            int state = Sync.getNetworkState();
            int title = R.string.notAvailable;

            switch (state) {
                case 0:
                    title = R.string.disconnected;
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
            WidgetsLoader.update(data.getInt("id"));
        }
    }
}
