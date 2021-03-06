package com.lastutf445.home2.adapters;

import android.app.Activity;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Event;
import com.lastutf445.home2.loaders.NotificationsLoader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    private SparseArray<Event> data = new SparseArray<>();
    private ItemTouchHelper itemTouchHelper;
    private SimpleDateFormat dateFormat;
    private LayoutInflater inflater;
    private RecyclerView content;
    private Activity activity;

    private int selectedId;

    public NotificationsAdapter(LayoutInflater inflater, RecyclerView content, Activity activity) {
        this.dateFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        this.inflater = inflater;
        this.activity = activity;
        this.content = content;
    }

    public void initCallback() {
        itemTouchHelper = new ItemTouchHelper(new Callback());
        itemTouchHelper.attachToRecyclerView(content);
    }

    public class Callback extends ItemTouchHelper.Callback {
        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
            return true;
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            if (viewHolder != null && actionState == 1) {
                //Log.d("LOGTAG", "onselchanged:" + actionState);
                selectedId = data.valueAt(viewHolder.getAdapterPosition()).getId();
            }
        }

        @Override
        public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, final int i) {
            selectedId = -1;

            NotificationsLoader.removeById(
                    data.valueAt(viewHolder.getAdapterPosition()).getId()
            );
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        private TextView title, subtitle, timestamp;
        private ImageView icon;

        public ViewHolder(@NonNull View view) {
            super(view);

            title = view.findViewById(R.id.notificationTitle);
            subtitle = view.findViewById(R.id.notificationSubtitle);
            timestamp = view.findViewById(R.id.notificationTimestamp);
            icon = view.findViewById(R.id.notificationIcon);
        }

        public void bind(@NonNull Event ev) {
            title.setText(ev.getTitle());
            subtitle.setText(ev.getSubtitle());
            icon.setImageResource(ev.getIcon());

            timestamp.setText(
                    dateFormat.format(new Date(ev.getTimestamp()))
            );
        }
    }

    public void setData(SparseArray<Event> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public int getSelectedId() {
        return selectedId;
    }

    @NonNull
    @Override
    public NotificationsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = inflater.inflate(R.layout.notifications_item, viewGroup, false);
        return new NotificationsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationsAdapter.ViewHolder viewHolder, int i) {
        viewHolder.bind(data.valueAt(i));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
