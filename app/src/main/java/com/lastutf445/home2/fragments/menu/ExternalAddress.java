package com.lastutf445.home2.fragments.menu;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.network.Sync;
import com.lastutf445.home2.util.GlobalPing;
import com.lastutf445.home2.util.NavigationFragment;
import com.lastutf445.home2.util.SimpleAnimator;

import java.lang.ref.WeakReference;
import java.net.InetAddress;

public class ExternalAddress extends NavigationFragment {

    private GlobalPing ping;
    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.sync_externaladdress, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        InputFilter[] filters = new InputFilter[1];

        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       android.text.Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart)
                            + source.subSequence(start, end)
                            + destTxt.substring(dend);
                    if (!resultingTxt
                            .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i = 0; i < splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }

        };

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.externalAddressCheck:
                        globalPing();
                        break;
                    case R.id.externalAddressDisable:
                        disable();
                        break;
                    case R.id.externalAddressEnable:
                        enable();
                        break;
                }
            }
        };

        ((EditText) view.findViewById(R.id.externalAddress)).setFilters(filters);
        view.findViewById(R.id.externalAddressCheck).setOnClickListener(c);
        view.findViewById(R.id.externalAddressDisable).setOnClickListener(c);
        view.findViewById(R.id.externalAddressEnable).setOnClickListener(c);
        updater = new Updater(view);
        reload();
    }

    @Override
    protected void reload() {
        Resources res = DataLoader.getAppResources();

        ((TextView) view.findViewById(R.id.externalAddressStatus)).setText(
                DataLoader.getBoolean("ExternalConnection", false) ? res.getString(R.string.enabled) : res.getString(R.string.disabled)
        );

        String raw_address = DataLoader.getString("ExternalAddress", null);
        int port = DataLoader.getInt("ExternalPort", 0);

        if (raw_address != null) {
            ((EditText) view.findViewById(R.id.externalAddress)).setText(raw_address);
        }

        if (port != 0) {
            ((EditText) view.findViewById(R.id.externalPort)).setText(
                    String.valueOf(port)
            );
        }
    }

    private void globalPing() {
        String raw_address = ((EditText) view.findViewById(R.id.externalAddress)).getText().toString().trim();
        String raw_port = ((EditText) view.findViewById(R.id.externalPort)).getText().toString().trim();

        try {
            switch (Sync.validateAddress(raw_address, raw_port)) {
                case 0:
                    NotificationsLoader.makeToast("Unexpected error", true);
                    break;
                case 1:
                    ping = new GlobalPing(updater, InetAddress.getByName(raw_address), Integer.valueOf(raw_port));
                    new Thread(ping).start();
                    break;
                case 2:
                    NotificationsLoader.makeToast("Invalid address", true);
                    break;
                case 3:
                    NotificationsLoader.makeToast("Invalid port", true);
                    break;
            }
        } catch (Exception e) {
            //e.printStackTrace();
            NotificationsLoader.makeToast("Unexpected error", true);
        }

    }

    private void disable() {
        if (!DataLoader.getBoolean("ExternalConnection", false)) {
            return;
        }

        DataLoader.set("ExternalConnection", false);
        DataLoader.save();
        reload();

        Sync.restart();
    }

    private void enable() {
        String raw_address = ((EditText) view.findViewById(R.id.externalAddress)).getText().toString().trim();
        String raw_port = ((EditText) view.findViewById(R.id.externalPort)).getText().toString().trim();

        try {
            switch (Sync.validateAddress(raw_address, raw_port)) {
                case 0:
                    NotificationsLoader.makeToast("Unexpected error", true);
                    break;
                case 1:
                    DataLoader.set("ExternalAddress", InetAddress.getByName(raw_address).getHostAddress());
                    DataLoader.set("ExternalPort", Integer.valueOf(raw_port));
                    DataLoader.set("ExternalConnection", true);
                    DataLoader.save();
                    Sync.restart();
                    reload();
                    break;
                case 2:
                    NotificationsLoader.makeToast("Invalid address", true);
                    break;
                case 3:
                    NotificationsLoader.makeToast("Invalid port", true);
                    break;

            }

        } catch (Exception e) {
            //e.printStackTrace();
            NotificationsLoader.makeToast("Unexpected error", true);
        }
    }

    @Override
    public void onDestroy() {
        if (ping != null) {
            ping.abort();
        }

        super.onDestroy();
    }

    private static class Updater extends Handler {
        private WeakReference<View> weakView;

        Updater(View view) {
            this.weakView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case -2:
                    beginPing();
                    break;
                case -1:
                    updateConnectionStatus(msg.getData());
                    break;
            }
        }

        private void beginPing() {
            View view = weakView.get();
            if (view == null) return;

            SimpleAnimator.fadeIn(view.findViewById(R.id.externalAddressSpinner), 300);
            TextView connection = view.findViewById(R.id.externalAddressConnection);
            View check = view.findViewById(R.id.externalAddressCheck);

            SimpleAnimator.alpha(check, 1f, 0.6f, 100);

            connection.setText(
                    DataLoader.getAppResources().getString(R.string.pending)
            );

            check.setClickable(false);
        }

        private void updateConnectionStatus(Bundle data) {
            View view = weakView.get();
            if (view == null) return;

            TextView connection = view.findViewById(R.id.externalAddressConnection);
            View check = view.findViewById(R.id.externalAddressCheck);

            if (data == null || !data.containsKey("success") || !data.getBoolean("success", false)) {
                connection.setText(
                        DataLoader.getAppResources().getString(R.string.unreachable)
                );

            } else {
                connection.setText(
                        DataLoader.getAppResources().getString(R.string.reachable)
                );
            }

            final View spinner = view.findViewById(R.id.externalAddressSpinner);
            SimpleAnimator.alpha(check, 0.6f, 1f, 100);
            check.setClickable(true);

            SimpleAnimator.fadeOut(spinner, 300, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    spinner.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }
    }
}
