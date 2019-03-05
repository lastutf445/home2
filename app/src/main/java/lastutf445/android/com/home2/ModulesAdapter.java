package lastutf445.android.com.home2;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;

class ModulesAdapter extends RecyclerView.Adapter<ModulesAdapter.ViewHolder> {

    abstract static class Listener implements View.OnClickListener {}

    class ViewHolder extends RecyclerView.ViewHolder {
        private View layout;
        private ImageView icon;
        private TextView title;
        private TextView serial;

        public ViewHolder(View view, ModulesAdapter.Listener listener) {
            super(view);

            layout = view;
            icon = view.findViewById(R.id.modulesItemIcon);
            title = view.findViewById(R.id.modulesItemTitle);
            serial = view.findViewById(R.id.modulesItemSerial);
            view.setOnClickListener(listener);
        }

        public void bind(ModuleOption op) {
            icon.setImageResource(Dashboard.getIconByType(op.getType()));
            title.setText(op.getTitle());
            serial.setText(String.valueOf(op.getSerial()));
        }
    }

    private ArrayList<ModuleOption> modules = new ArrayList<>();
    private Listener listener;

    public ModulesAdapter(ModulesAdapter.Listener listener) {
        this.listener = listener;
    }

    public void setItems(Collection<ModuleOption> modules) {
        this.modules.addAll(modules);
        notifyDataSetChanged();
    }

    public void clearItems() {
        modules.clear();
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_modules_item, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bind(modules.get(i));
    }

    @Override
    public int getItemCount() {
        return modules.size();
    }


}
