package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.NodesAdapter;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.NodesLoader;
import com.lastutf445.home2.util.NavigationFragment;

public class Nodes extends NavigationFragment {

    private NodesAdapter adapter;
    private RecyclerView content;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.menu_nodes, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        content = view.findViewById(R.id.nodesContent);
        content.setLayoutManager(new LinearLayoutManager(DataLoader.getAppContext()));

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentsLoader.addChild(new NodesDiscovery(), Nodes.this);
            }
        };

        View.OnClickListener d = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = content.getChildLayoutPosition(v);

                Node node = new Node();
                node.setNode(adapter.getNode(pos), pos);
                FragmentsLoader.addChild(node, Nodes.this);
            }
        };

        view.findViewById(R.id.nodesDiscovery).setOnClickListener(c);
        adapter = new NodesAdapter(getLayoutInflater(), d);
        content.setAdapter(adapter);
        reload();
    }

    @Override
    protected void reload() {
        Log.d("LOGTAG", "reloaded nodes screen");
        adapter.setData(NodesLoader.getNodes());
        view.findViewById(R.id.nodesNoContent).setVisibility(adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
    }

    private void update(int pos) {
        adapter.update(pos);
    }

    private void delete(int pos) {
        adapter.delete(pos);
        view.findViewById(R.id.nodesNoContent).setVisibility(adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResult(Bundle data) {
        if (data.containsKey("deleted")) {
            delete(data.getInt("deleted"));
        }
        if (data.containsKey("updated")) {
            update(data.getInt("updated"));
            //update(data.getInt("updated"));
        }
        if (data.containsKey("reload")) {
            reload();
        }
    }
}
