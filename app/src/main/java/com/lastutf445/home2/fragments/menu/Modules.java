package com.lastutf445.home2.fragments.menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lastutf445.home2.R;
import com.lastutf445.home2.adapters.ModulesAdapter;
import com.lastutf445.home2.loaders.DataLoader;
import com.lastutf445.home2.loaders.FragmentsLoader;
import com.lastutf445.home2.loaders.ModulesLoader;
import com.lastutf445.home2.loaders.NotificationsLoader;
import com.lastutf445.home2.util.NavigationFragment;

public class Modules extends NavigationFragment {

    private SparseArray<com.lastutf445.home2.containers.Module> modules;
    private FloatingActionButton modulesDiscovery;
    private FloatingActionButton selectAllButton;
    private boolean returnSerial = false;
    private ModulesAdapter adapter;
    private RecyclerView content;
    private boolean hasAddButton;
    private boolean removable;

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
        modulesDiscovery = view.findViewById(R.id.modulesDiscovery);
        selectAllButton = view.findViewById(R.id.modulesSelectAll);
        selectAllButton.hide();

        View.OnClickListener c = new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View v) {
                int pos = content.getChildLayoutPosition(v);
                com.lastutf445.home2.containers.Module module = adapter.getModule(pos);
                Module mc = new Module();
                mc.setModule(module, pos);

                FragmentsLoader.addChild(mc, Modules.this);
            }
        };

        View.OnClickListener d = new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View v) {
                toParent.putInt("serial", adapter.getModule(content.getChildLayoutPosition(v)).getSerial());
                getActivity().onBackPressed();
            }
        };

        View.OnClickListener e = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.modulesDiscovery:
                        if (adapter.getSelected().size() == 0) {
                            FragmentsLoader.addChild(new ModulesDiscovery(), Modules.this);

                        } else {
                            //Log.d("LOGTAG", "are you sure?");
                            deleteAll();
                        }
                        break;
                    case R.id.modulesSelectAll:
                        if (modules.size() != adapter.getSelected().size()) {
                            adapter.selectAll();
                            //NotificationsLoader.makeToast("Selected all", true);

                        } else {
                            adapter.deselectAll();
                            //NotificationsLoader.makeToast("Deselected all", true);
                        }
                        break;
                }
            }
        };

        if (!hasAddButton) modulesDiscovery.hide();
        modulesDiscovery.setOnClickListener(e);
        selectAllButton.setOnClickListener(e);

        adapter = new ModulesAdapter(getLayoutInflater(), returnSerial ? d : c);
        content.setAdapter(adapter);
        adapter.setContent(content); // don't erase this line! uncaught NPE
        adapter.setRemovable(removable);

        adapter.setOnItemSelectedCallback(new ModulesAdapter.onItemSelectedCallback() {
            @Override
            public void onSelectionChanged(boolean selected) {
                if (selected) {
                    if (adapter.getSelected().size() == 1) {
                        adapter.setSelectMode(true);
                        selectAllButton.show();
                        //selectAllButton.setEnabled(true);

                        modulesDiscovery.setImageResource(
                                R.drawable.delete
                        );

                        modulesDiscovery.setBackgroundTintList(
                                ColorStateList.valueOf(
                                        Color.parseColor("#C2185B")
                                )
                        );
                    }

                } else if (adapter.getSelected().size() == 0) {
                    adapter.setSelectMode(false);

                    modulesDiscovery.setImageResource(
                            R.drawable.add
                    );

                    modulesDiscovery.setBackgroundTintList(
                            ColorStateList.valueOf(
                                    DataLoader.getAppResources().getColor(R.color.colorPrimary)
                            )
                    );

                    selectAllButton.hide();
                    //selectAllButton.setEnabled(false);
                }
            }
        });

        reload();
    }

    @Override
    protected void reload() {
        adapter.setData(modules);
        view.findViewById(R.id.modulesNoContent).setVisibility(adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
    }

    private void deleteAll() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity()
        );

        Resources res = DataLoader.getAppResources();
        builder.setTitle(res.getString(R.string.modulesDeleteAllTitle));
        builder.setMessage(res.getString(R.string.modulesDeleteAllMessage));

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int counter = 0;

                for (Integer serial: adapter.getSelected()) {
                    com.lastutf445.home2.containers.Module module = ModulesLoader.getModule(serial);
                    if (module != null) {
                        int pos = ModulesLoader.getModules().indexOfKey(serial);
                        ModulesLoader.removeModule(module);
                        delete(pos);
                        ++counter;
                    }
                }

                NotificationsLoader.makeToast("Deleted " + counter + " modules", true);
                adapter.deselectAll();
            }
        });

        builder.create().show();
    }

    private void delete(int pos) {
        adapter.delete(pos);
        view.findViewById(R.id.modulesNoContent).setVisibility(adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
    }

    private void update(int pos) {
        adapter.update(pos);
    }

    public void setModules(@NonNull SparseArray<com.lastutf445.home2.containers.Module> modules, boolean returnSerial) {
        this.returnSerial = returnSerial;
        this.modules = modules;
    }

    public void enableAddButton() {
        hasAddButton = true;
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    @Override
    public void onResult(@NonNull Bundle data) {
        if (data.containsKey("deleted")) {
            delete(data.getInt("deleted"));
        }
        if (data.containsKey("reload")) {
            reload();
        }
        if (data.containsKey("updated")) {
            update(data.getInt("updated"));
        }
    }
}
