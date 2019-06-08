package com.lastutf445.home2.adapters;

import android.graphics.Color;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.util.SimpleAnimator;

import java.util.HashSet;

public class ModulesAdapter extends RecyclerView.Adapter<ModulesAdapter.ViewHolder>  {

    private boolean showSerials = false;
    private boolean selectMode = false;
    private boolean removable = false;

    private onItemSelectedCallback onItemSelectedCallback;
    private SparseArray<Module> data = new SparseArray<>();
    @NonNull
    private HashSet<Integer> selected = new HashSet<>();
    private View.OnClickListener listener;
    private LayoutInflater inflater;
    private RecyclerView content;

    @NonNull
    private View.OnLongClickListener longListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(@NonNull View v) {
            ViewHolder viewHolder = (ViewHolder) content.getChildViewHolder(v);
            boolean selected = viewHolder.invertSelection();

            int pos = viewHolder.getLayoutPosition();
            notifyItemChanged(pos);

            if (onItemSelectedCallback != null) {
                onItemSelectedCallback.onSelectionChanged(selected);
            }

            Log.d("LOGTAG", "on_long: pos = " + pos);
            return true;
        }
    };

    @NonNull
    private View.OnClickListener wrapperListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (selectMode) {
                longListener.onLongClick(v);

            } else if (listener != null) {
                listener.onClick(v);
            }
        }
    };

    public ModulesAdapter(LayoutInflater inflater, View.OnClickListener listener) {
        this.inflater = inflater;
        this.listener = listener;
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkbox;
        private TextView serial;
        private TextView title;
        private ImageView icon;
        private Module module;

        public ViewHolder(@NonNull View view) {
            super(view);

            checkbox = view.findViewById(R.id.modulesItemCheckBox);
            serial = view.findViewById(R.id.modulesItemSerial);
            title = view.findViewById(R.id.modulesItemTitle);
            icon = view.findViewById(R.id.modulesItemIcon);

            if (!showSerials) serial.setVisibility(View.GONE);

            if (removable) {
                view.setOnLongClickListener(longListener);
            }

            view.setOnClickListener(wrapperListener);
        }

        public void bind(@NonNull Module module) {
            this.module = module;

            if (showSerials) {
                serial.setText(String.valueOf(module.getSerial()));
                Module oldModule = ModulesLoader.getModule(module.getSerial());

                if (oldModule != null) {
                    serial.setTextColor(DataLoader.getAppResources().getColor(R.color.colorPrimary));

                } else {
                    serial.setTextColor(Color.parseColor("#999999"));
                }
            }

            boolean checked = selected.contains(module.getSerial());

            if ((checkbox.getVisibility() == View.VISIBLE) != selectMode) {
                if (checkbox.getVisibility() != View.VISIBLE) {

                    checkbox.setEnabled(true);
                    SimpleAnimator.fadeIn(checkbox, 200);
                    checkbox.setVisibility(View.VISIBLE);

                } else {
                    SimpleAnimator.fadeOut(checkbox, 200, new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            checkbox.setVisibility(View.GONE);
                            checkbox.setEnabled(false);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                }
            }

            //checkbox.setVisibility(selectMode ? View.VISIBLE : View.GONE);
            checkbox.setChecked(checked);
            icon.setImageResource(module.getIcon());
            title.setText(module.getTitle());
        }

        public boolean invertSelection() {
            if (selected.contains(module.getSerial())) {
                selected.remove(module.getSerial());
                return false;

            } else {
                selected.add(module.getSerial());
                return true;
            }
        }
    }

    public void setData(SparseArray<Module> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public void setShowSerials(boolean showSerials) {
        this.showSerials = showSerials;
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    public void setOnItemSelectedCallback(ModulesAdapter.onItemSelectedCallback onItemSelectedCallback) {
        this.onItemSelectedCallback = onItemSelectedCallback;
    }

    public void setContent(RecyclerView content) {
        this.content = content;
    }

    public void setSelectMode(boolean selectMode) {
        this.selectMode = selectMode;
        notifyDataSetChanged();
    }

    public void selectAll() {
        for (int i = 0; i < data.size(); ++i) {
            selected.add(data.valueAt(i).getSerial());
        }

        notifyDataSetChanged();
    }

    public void deselectAll() {
        selected.clear();
        selectMode = false;

        if (onItemSelectedCallback != null) {
            onItemSelectedCallback.onSelectionChanged(false);
        }

        notifyDataSetChanged();
    }

    public void pushData(@NonNull Module module) {
        if (data.get(module.getSerial()) != null) return;
        data.put(module.getSerial(), module);
        notifyItemInserted(data.indexOfKey(module.getSerial()));
    }

    public SparseArray<Module> getData() {
        return data;
    }

    @Nullable
    public Module getModule(int pos) {
        return pos >= 0 && pos < data.size() ? data.valueAt(pos) : null;
    }

    @NonNull
    public HashSet<Integer> getSelected() {
        return selected;
    }

    public void delete(int pos) {
        if (pos < 0 || pos > data.size()) return;
        //data.removeAt(pos);
        notifyItemRemoved(pos);
    }

    public void deleteAll() {
        int oldSize = data.size();
        data.clear();
        notifyItemRangeRemoved(0, oldSize);
    }

    public void update(int pos) {
        if (pos < 0 || pos >= data.size()) return;
        notifyItemChanged(pos);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = inflater.inflate(R.layout.modules_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bind(data.valueAt(i));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public interface onItemSelectedCallback {
        void onSelectionChanged(boolean selected);
    }
}
