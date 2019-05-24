package com.lastutf445.home2.adapters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Node;

public class NodesAdapter extends RecyclerView.Adapter<NodesAdapter.ViewHolder> {

    private SparseArray<Node> data = new SparseArray<>();
    private View.OnClickListener listener;
    private LayoutInflater inflater;
    private boolean forceDelete;

    public NodesAdapter(LayoutInflater inflater, View.OnClickListener listener) {
        this.inflater = inflater;
        this.listener = listener;
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private TextView count;

        public ViewHolder(@NonNull View view) {
            super(view);

            title = view.findViewById(R.id.nodesItemTitle);
            count = view.findViewById(R.id.nodesItemModulesCount);
            view.setOnClickListener(listener);
        }

        public void bind(Node op) {
            //Log.d("LOGTAG", "nodeoption added - " + op.getSerial());
            title.setText(op.getTitle());
            count.setText(String.valueOf(op.getModulesCount()));
        }
    }

    public void setData(@NonNull SparseArray<Node> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public void setForceDelete(boolean forceDelete) {
        this.forceDelete = forceDelete;
    }

    public void pushData(@NonNull Node node) {
        if (data.get(node.getSerial()) != null) return;
        data.put(node.getSerial(), node);
        notifyItemInserted(data.indexOfKey(node.getSerial()));
    }

    public void delete(int pos) {
        if (pos < 0 || pos >= data.size()) return;
        if (forceDelete) data.removeAt(pos);
        notifyItemRemoved(pos);
    }

    public void deleteAll() {
        int oldSize = data.size();
        data = new SparseArray<>();
        notifyItemRangeRemoved(0, oldSize);
    }

    public void update(int pos) {
        if (pos < 0 || pos >= data.size()) return;
        notifyItemChanged(pos);
    }

    @Nullable
    public Node getNode(int pos) {
        return pos >= 0 && pos < data.size() ? data.valueAt(pos) : null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(inflater.inflate(R.layout.nodes_item, viewGroup, false));
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
