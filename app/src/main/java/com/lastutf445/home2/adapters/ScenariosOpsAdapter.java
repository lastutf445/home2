package com.lastutf445.home2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Module;
import com.lastutf445.home2.fragments.scenarios.ScenarioOpsEditor;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ScenariosOpsAdapter extends RecyclerView.Adapter<ScenariosOpsAdapter.ViewHolder> {

    private int FLAG = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;

    private ArrayList<String> data = new ArrayList<>();
    private HashMap<String, Object> verifier = new HashMap<>();
    private ScenarioOpsEditor.Connector connector;
    private ItemTouchHelper itemTouchHelper;
    private LayoutInflater inflater;
    private RecyclerView content;

    public ScenariosOpsAdapter(LayoutInflater inflater, RecyclerView content) {
        this.inflater = inflater;
        this.content = content;
    }

    public void setConnector(@NonNull ScenarioOpsEditor.Connector connector) {
        this.connector = connector;
    }

    public void initCallback() {
        itemTouchHelper = new ItemTouchHelper(new Callback());
        itemTouchHelper.attachToRecyclerView(content);
    }

    public class Callback extends ItemTouchHelper.Callback {
        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(0, FLAG);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
            return true;
        }

        @Override
        public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, final int i) {
            int pos = viewHolder.getAdapterPosition();
            notifyItemChanged(pos);

            if (connector == null) {
                return;

            } else if (i == ItemTouchHelper.LEFT) {
                connector.requestEdit(pos);

            } else if (i == ItemTouchHelper.RIGHT) {
                connector.requestDelete(pos);
            }
        }

        @Override
        public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
            return 0.3f;
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
        private TextView title;

        public ViewHolder(@NonNull View view) {
            super(view);

            title = view.findViewById(R.id.opsItemTitle);
        }

        public void bind(@NonNull String title) {
            this.title.setText(title);
        }
    }

    public void setData(@NonNull Module module) {
        JSONObject ops = module.getOps();
        Iterator<String> it = ops.keys();

        data.clear();
        verifier.clear();

        while (it.hasNext()) {
            String key = it.next();

            try {
                data.add(key);
                verifier.put(key, ops.get(key));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        notifyDataSetChanged();
    }

    public HashMap<String, Object> getVerifier() {
        return verifier;
    }

    @NonNull
    public String getKeyByPos(int pos) {
        return data.get(pos);
    }

    @Nullable
    public Object getValueByKey(@NonNull String key) {
        return verifier.get(key);
    }

    public void pushData(@NonNull String key, @NonNull Object value) {
        if (!verifier.containsKey(key)) {
            verifier.put(key, value);
            data.add(key);
            notifyItemInserted(data.size() - 1);

        } else {
            verifier.put(key, value);
            notifyItemChanged(data.indexOf(key));
        }
    }

    public void delete(@NonNull String key) {
        if (verifier.containsKey(key)) {
            int pos = data.indexOf(key);
            verifier.remove(key);
            data.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    public void setDeletable(boolean deletable) {
        if (deletable) {
            FLAG = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;

        } else {
            FLAG = ItemTouchHelper.LEFT;
        }
    }

    @NonNull
    @Override
    public ScenariosOpsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = inflater.inflate(R.layout.scenario_ops_item, viewGroup, false);
        return new ScenariosOpsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScenariosOpsAdapter.ViewHolder viewHolder, int i) {
        viewHolder.bind(data.get(i));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
