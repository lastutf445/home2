package com.lastutf445.home2.fragments.menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

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
import com.lastutf445.home2.util.SimpleAnimator;

public class Modules extends NavigationFragment {

    private SparseArray<com.lastutf445.home2.containers.Module> modules;
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
        selectAllButton = view.findViewById(R.id.modulesSelectAll);

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

        if (!hasAddButton) view.findViewById(R.id.modulesDiscovery).setVisibility(View.GONE);
        view.findViewById(R.id.modulesDiscovery).setOnClickListener(e);
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
                        selectAllButton.setEnabled(true);
                        SimpleAnimator.fadeIn(selectAllButton, 200);

                        ((FloatingActionButton) view.findViewById(R.id.modulesDiscovery)).setImageDrawable(
                                getResources().getDrawable(R.drawable.delete)
                        );
                    }

                } else if (adapter.getSelected().size() == 0) {
                    adapter.setSelectMode(false);
                    ((FloatingActionButton) view.findViewById(R.id.modulesDiscovery)).setImageDrawable(
                            getResources().getDrawable(R.drawable.add)
                    );

                    selectAllButton.setEnabled(false);
                    SimpleAnimator.fadeOut(selectAllButton, 200, new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            //selectAllButton.setVisibility(View.GONE);
                            selectAllButton.setSystemUiVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
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
