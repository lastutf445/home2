package com.lastutf445.home2.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.NotificationsAdapter;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;

public class Notifications extends NavigationFragment {

    private NotificationsLoader.QueueInterface queueInterface;
    @Nullable
    private NotificationsAdapter adapter;
    private RecyclerView content;
    @Nullable
    private Updater updater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_notifications, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        content = view.findViewById(R.id.notificationsContent);
        content.setLayoutManager(new LinearLayoutManager(DataLoader.getAppContext()));

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationsLoader.removeAll();
            }
        };

        view.findViewById(R.id.notificationsClearAll).setOnClickListener(c);

        adapter = new NotificationsAdapter(getLayoutInflater(), content, getActivity());
        adapter.setData(NotificationsLoader.getNotifications());
        updater = new Updater(view, adapter);
        content.setAdapter(adapter);
        adapter.initCallback();

        queueInterface = new NotificationsLoader.QueueInterface() {
            @Override
            public void makeStatusNotification(int status, boolean update) {
                Message msg = updater.obtainMessage(0);
                Bundle data = new Bundle();
                data.putInt("status", status);
                data.putBoolean("update", update);
                msg.setData(data);
                updater.sendMessage(msg);
            }

            @Override
            public void removeAll() {
                updater.sendEmptyMessage(1);
            }

            @Override
            public void removeById(int id) {
                Message msg = updater.obtainMessage(2);
                Bundle data = new Bundle();
                data.putInt("id", id);
                msg.setData(data);
                updater.sendMessage(msg);
            }
        };

        NotificationsLoader.setCallback(queueInterface);
        updater.sendEmptyMessage(-1);
    }

    private static class Updater extends Handler {
        private WeakReference<NotificationsAdapter> weakAdapter;
        private WeakReference<View> weakView;

        public Updater(View view, NotificationsAdapter adapter) {
            weakAdapter = new WeakReference<>(adapter);
            weakView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            NotificationsAdapter adapter = weakAdapter.get();
            Bundle data = msg.getData();
            View view = weakView.get();
            if (adapter == null) return;

            if (data != null) {
                switch (msg.what) {
                    case 0:
                        int status = NotificationsLoader.nativeMakeStatusNotification(
                                data.getInt("status"),
                                data.getBoolean("update")
                        );

                        if (status == 0) break;

                        int pos = NotificationsLoader.getNotifications().indexOfKey(
                                data.getInt("status")
                        );

                        if (status == 1) {
                            Log.d("LOGTAG", "inserted " + data.getInt("status"));
                            adapter.notifyItemInserted(pos);

                        } else if (status == 2) {
                            Log.d("LOGTAG", "updated " + data.getInt("status"));
                            if (adapter.getSelectedId() != status) {
                                adapter.notifyItemChanged(pos);
                            } // temporary fix
                        }
                        break;

                    case 1:
                        int oldSize = NotificationsLoader.getNotifications().size();
                        NotificationsLoader.nativeRemoveAll();
                        adapter.notifyItemRangeRemoved(0, oldSize);
                        break;

                    case 2:
                        pos = NotificationsLoader.nativeRemoveById(
                                data.getInt("id")
                        );
                        if (pos >= 0) {
                            Log.d("LOGTAG", "remove " + data.getInt("id"));
                            adapter.notifyItemRemoved(pos);
                        }
                        break;
                }
            }

            if (view != null) {
                view.findViewById(R.id.notificationsNoContent).setVisibility(
                        adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE
                );
            }
        }
    }
}
