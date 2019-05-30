package com.lastutf445.home2.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.containers.Widget;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.WidgetsLoader;

public class WidgetsAdapter extends RecyclerView.Adapter<WidgetsAdapter.ViewHolder> {

    private SparseArray<Widget> data = new SparseArray<>();
    private ItemTouchHelper itemTouchHelper;
    private LayoutInflater inflater;
    private RecyclerView content;
    private Activity activity;

    public WidgetsAdapter(LayoutInflater inflater, RecyclerView content, Activity activity) {
        this.inflater = inflater;
        this.activity = activity;
        this.content = content;

        WidgetsLoader.setWidgetsAdapterRemover(new Remover());
    }

    public void initCallback() {
        itemTouchHelper = new ItemTouchHelper(new Callback());
        itemTouchHelper.attachToRecyclerView(content);
    }

    public class Callback extends ItemTouchHelper.Callback {
        private int from, to;
        private boolean drag = false;

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT);
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            if (viewHolder != null) {
                if (actionState == 2) {
                    //Log.d("LOGTAG", "drag from = " + viewHolder.getLayoutPosition());
                    from = viewHolder.getLayoutPosition();
                    drag = true;
                    to = from;
                }
            } else if (drag) {
                //Log.d("LOGTAG", "drop to = " + to);
                if (from != to) {
                    WidgetsLoader.swapViews(from, to);
                }

                drag = false;
            }
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
            WidgetsLoader.swapWidgets(viewHolder.getAdapterPosition(), viewHolder1.getAdapterPosition());
            notifyItemMoved(viewHolder.getAdapterPosition(), viewHolder1.getAdapterPosition());
            to = viewHolder.getAdapterPosition();
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
        private TextView title;

        public ViewHolder(@NonNull View view) {
            super(view);

            title = view.findViewById(R.id.widgetTitle);
            icon = view.findViewById(R.id.widgetIcon);
        }

        public void bind(Widget op) {
            Module module = ModulesLoader.getModule(op.getSerial());
            icon.setImageResource(op.getIcon());

            switch (op.getType()) {
                case "title":
                    title.setText(op.getString("title", DataLoader.getAppResources().getString(R.string.unknownTitle)));
                    break;
                default:
                    title.setText(module != null ? module.getTitle() : Module.getDefaultTitle(op.getType()));
                    break;
            }
        }
    }

    public void setData(SparseArray<Widget> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    private void removeItem(int pos) {
        if (pos < 0 || pos >= data.size()) return;

        WidgetsLoader.remove(data.valueAt(pos));
        data.removeAt(pos);
        notifyItemRemoved(pos);
    }

    @NonNull
    @Override
    public WidgetsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = inflater.inflate(R.layout.widgets_item, viewGroup, false);
        final ViewHolder viewHolder = new WidgetsAdapter.ViewHolder(view);

        view.findViewById(R.id.widgetDragHandle).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(viewHolder);
                }

                return false;
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull WidgetsAdapter.ViewHolder viewHolder, int i) {
        viewHolder.bind(data.valueAt(i));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class Remover {
        public void remove(int pos) {
            removeItem(pos);
        }
    }
}
