package com.lastutf445.home2.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Event;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.containers.Widget;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.WidgetsLoader;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    private SparseArray<Event> data = new SparseArray<>();
    private ItemTouchHelper itemTouchHelper;
    private LayoutInflater inflater;
    private RecyclerView content;
    private Activity activity;

    public NotificationsAdapter(LayoutInflater inflater, RecyclerView content, Activity activity) {
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
            return makeMovementFlags(0, ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
            return true;
        }

        @Override
        public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, final int i) {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    activity
            );

            Resources res = DataLoader.getAppResources();
            builder.setTitle(res.getString(R.string.widgetRemoveTitle));
            builder.setMessage(res.getString(R.string.widgetRemoveMessages));

            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    notifyItemChanged(viewHolder.getAdapterPosition());
                }
            });

            builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    removeItem(viewHolder.getAdapterPosition());
                }
            });

            builder.create().show();
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
        private ImageView icon;
        private TextView title, subtitle;

        public ViewHolder(@NonNull View view) {
            super(view);

            title = view.findViewById(R.id.notificationTitle);
            subtitle = view.findViewById(R.id.notificationSubtitle);
            icon = view.findViewById(R.id.notificationIcon);
        }

        public void bind(Event ev) {
            title.setText(ev.getTitle());
            subtitle.setText(ev.getSubtitle());
            icon.setImageResource(ev.getIcon());
        }
    }

    public void setData(SparseArray<Event> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    private void removeItem(int pos) {
        if (pos < 0 || pos >= data.size()) return;

        synchronized (data) {
            //data.removeAt(pos);
            notifyItemRemoved(pos);
        }
    }

    @NonNull
    @Override
    public NotificationsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = inflater.inflate(R.layout.widgets_item, viewGroup, false);
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
