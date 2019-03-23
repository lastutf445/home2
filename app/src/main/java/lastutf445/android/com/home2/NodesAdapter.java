package lastutf445.android.com.home2;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

class NodesAdapter extends RecyclerView.Adapter<NodesAdapter.ViewHolder> {

    abstract static class Listener implements View.OnClickListener {}

    class ViewHolder extends RecyclerView.ViewHolder {
        private View layout;
        private TextView title;
        private TextView count;
        private TextView serial;

        public ViewHolder(View view, NodesAdapter.Listener listener) {
            super(view);

            layout = view;
            title = view.findViewById(R.id.nodesItemTitle);
            count = view.findViewById(R.id.nodesItemModulesCount);
            serial = view.findViewById(R.id.nodesItemSerial);
            view.setOnClickListener(listener);
        }

        public void bind(NodeOption op) {
            Log.d("LOGTAG", "nodeoption added - " + op.getSerial());
            title.setText(op.getTitle());
            count.setText(String.valueOf(op.getModulesCount()));
            serial.setText(String.valueOf(op.getSerial()));
        }
    }

    private HashMap<Integer, NodeOption> nodes = new HashMap<>();
    private Listener listener;

    public NodesAdapter(NodesAdapter.Listener listener) {
        this.listener = listener;
    }

    public void addItems(Collection<NodeOption> nodes) {
        for (NodeOption i: nodes) {
            this.nodes.put(i.getSerial(), i);
        }

        notifyDataSetChanged();
    }

    public void clearItems() {
        nodes.clear();
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_nodes_item, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bind(nodes.get(i));
    }

    @Override
    public int getItemCount() {
        return nodes.size();
    }


}