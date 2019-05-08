package com.lastutf445.home2.fragments.menu;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.network.Sender;
import com.lastutf445.home2.util.NavigationFragment;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ExternalAddress extends NavigationFragment {

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
                        check();
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

    private void check() {
        // TODO: PING
    }

    private void disable() {
        if (!DataLoader.getBoolean("ExternalConnection", false)) {
            NotificationsLoader.makeToast("Feature has been already disabled", true);
            return;
        }

        DataLoader.set("ExternalConnection", false);
        DataLoader.save();
        reload();

        Sender.killConnection();
        NotificationsLoader.makeToast("Disabled", true);
    }

    private void enable() {
        String raw_address = ((EditText) view.findViewById(R.id.externalAddress)).getText().toString().trim();
        String raw_port = ((EditText) view.findViewById(R.id.externalPort)).getText().toString().trim();
        // TODO: processing

        try {
            if (raw_address.length() == 0) {
                throw new UnknownHostException("Null-length address");
            }

            InetAddress ip = InetAddress.getByName(raw_address);
            int port = Integer.valueOf(raw_port);

            DataLoader.set("ExternalAddress", ip.getHostAddress());
            DataLoader.set("ExternalPort", port);
            DataLoader.set("ExternalConnection", true);
            DataLoader.save();
            reload();

            NotificationsLoader.makeToast("Success", true);

        } catch (UnknownHostException e) {
            //e.printStackTrace();
            NotificationsLoader.makeToast("Invalid address", true);

        } catch (NumberFormatException e) {
            //e.printStackTrace();
            NotificationsLoader.makeToast("Invalid port", true);

        } catch (Exception e) {
            //e.printStackTrace();
            NotificationsLoader.makeToast("Unexpected error", true);

        }
    }

}
