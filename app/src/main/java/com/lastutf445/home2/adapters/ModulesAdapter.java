package com.lastutf445.home2.adapters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.ModulesLoader;

import java.util.Locale;

import static android.view.View.GONE;

public class ModulesAdapter extends RecyclerView.Adapter<ModulesAdapter.ViewHolder>  {

    private boolean serials = false;
    private boolean forceDelete = false;
    private SparseArray<Module> data = new SparseArray<>();
    private View.OnClickListener listener;
    private LayoutInflater inflater;

    public ModulesAdapter(LayoutInflater inflater, View.OnClickListener listener) {
        this.inflater = inflater;
        this.listener = listener;
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        private TextView serial;
        private TextView title;
        private ImageView icon;

        public ViewHolder(@NonNull View view, View.OnClickListener listener) {
            super(view);

            serial = view.findViewById(R.id.modulesItemSerial);
            title = view.findViewById(R.id.modulesItemTitle);
            icon = view.findViewById(R.id.modulesItemIcon);

            if (!serials) serial.setVisibility(GONE);
            view.setOnClickListener(listener);
        }

        public void bind(@NonNull Module module) {
            if (serials) {
                serial.setText(
                        String.format(
                                Locale.getDefault(),
                                "%s %d",
                                DataLoader.getAppResources().getString(R.string.serial),
                                module.getSerial()
                        )
                );

                if (ModulesLoader.getModule(module.getSerial()) != null) {
                    serial.setTextColor(DataLoader.getAppResources().getColor(R.color.colorPrimary));
                }
            }

            icon.setImageResource(module.getIcon());
            title.setText(module.getTitle());
        }
    }

    public void setData(SparseArray<Module> data, boolean serials) {
        this.data = data;
        this.serials = serials;
        notifyDataSetChanged();
    }

    public void setForceDelete(boolean forceDelete) {
        this.forceDelete = forceDelete;
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

    public void delete(int pos) {
        if (pos < 0 || pos > data.size()) return;
        if (forceDelete) data.removeAt(pos);
        notifyItemRemoved(pos);
    }

    public void update(int pos) {
        if (pos < 0 || pos >= data.size()) return;
        notifyItemChanged(pos);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = inflater.inflate(R.layout.modules_item, viewGroup, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bind(data.valueAt(i));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
