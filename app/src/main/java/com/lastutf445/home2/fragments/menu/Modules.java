package com.lastutf445.home2.fragments.menu;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.ModulesAdapter;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.util.NavigationFragment;

public class Modules extends NavigationFragment {

    private SparseArray<com.lastutf445.home2.containers.Module> modules;
    private boolean returnSerial = false;
    private ModulesAdapter adapter;
    private RecyclerView content;
    private boolean forceDelete;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.menu_modules, container, false);
        init();
        return view;
    }

    @Override
    protected void init() {
        if (modules == null) return;

        content = view.findViewById(R.id.modulesContent);
        content.setLayoutManager(new LinearLayoutManager(DataLoader.getAppContext()));

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = content.getChildLayoutPosition(v);
                com.lastutf445.home2.containers.Module module = adapter.getModule(pos);
                Module mc = new Module();
                mc.setModule(module, pos);

                FragmentsLoader.addChild(mc, Modules.this);
            }
        };

        View.OnClickListener d = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toParent.putInt("updated", adapter.getModule(content.getChildLayoutPosition(v)).getSerial());
                getActivity().onBackPressed();
            }
        };

        adapter = new ModulesAdapter(getLayoutInflater(), returnSerial ? d : c);
        adapter.setForceDelete(forceDelete);
        content.setAdapter(adapter);

        reload();
    }

    @Override
    protected void reload() {
        adapter.setData(modules, false);
        view.findViewById(R.id.modulesContent).setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
    }

    public void setForceDelete(boolean forceDelete) {
        this.forceDelete = forceDelete;
    }

    private void delete(int pos) {
        adapter.delete(pos);
        view.findViewById(R.id.modulesContent).setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
    }

    private void update(int pos) {
        adapter.update(pos);
    }

    public void setModules(@NonNull SparseArray<com.lastutf445.home2.containers.Module> modules, boolean returnSerial) {
        this.returnSerial = returnSerial;
        this.modules = modules;
    }

    @Override
    public void onResult(Bundle data) {
        if (data.containsKey("deleted")) {
            delete(data.getInt("deleted"));
            toParent.putInt("deleted", data.getInt("deleted"));
        }
        if (data.containsKey("updated")) {
            update(data.getInt("updated"));
        }
        if (data.containsKey("syncStateChanged")) {
            toParent.putBoolean("syncStateChanged", true);
        }
    }
}
