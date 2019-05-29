package com.lastutf445.home2.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.NotificationsAdapter;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.util.NavigationFragment;

import java.lang.ref.WeakReference;

public class Notifications extends NavigationFragment {

    private NotificationsLoader.Callback eventCallback;
    private NotificationsAdapter adapter;
    private RecyclerView content;
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

        eventCallback = new NotificationsLoader.Callback() {
            @Override
            public void removeAll(int oldSize) {
                Log.d("LOGTAG", "events deleted");
                send(0, oldSize);
            }

            @Override
            public void removeAt(int pos) {
                Log.d("LOGTAG", "event deleted");
                send(1, pos);
            }

            @Override
            public void insertedAt(int pos) {
                Log.d("LOGTAG", "event inserted");
                send(2, pos);
            }

            @Override
            public void updatedAt(int pos) {
                Log.d("LOGTAG", "event updated");
                send(3, pos);
            }

            private void send(int what, int extra) {
                Message msg = updater.obtainMessage(what);
                Bundle data = new Bundle();
                data.putInt("extra", extra);
                msg.setData(data);

                updater.sendMessage(msg);
            }
        };

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationsLoader.removeAll();
            }
        };

        view.findViewById(R.id.notificationsClearAll).setOnClickListener(c);

        adapter = new NotificationsAdapter(getLayoutInflater(), content, getActivity());
        adapter.setData(NotificationsLoader.getNotifications());
        NotificationsLoader.setCallback(eventCallback);
        updater = new Updater(view, adapter);
        content.setAdapter(adapter);
        adapter.initCallback();

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
        public void handleMessage(Message msg) {
            NotificationsAdapter adapter = weakAdapter.get();
            Bundle data = msg.getData();
            View view = weakView.get();
            if (adapter == null) return;

            if (data != null) {
                switch (msg.what) {
                    case 0:
                        adapter.notifyItemRangeRemoved(0, data.getInt("extra"));
                        break;
                    case 1:
                        adapter.notifyItemRemoved(data.getInt("extra"));
                        break;
                    case 2:
                        adapter.notifyItemInserted(data.getInt("extra"));
                        break;
                    case 3:
                        adapter.notifyItemChanged(data.getInt("extra"));
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
