package com.lastutf445.home2.adapters;

import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lastutf445.home2.R;
import com.lastutf445.home2.containers.Scenario;
import com.lastutf445.home2.util.SimpleAnimator;

import java.util.ArrayList;

public class ScenariosAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int TYPE_ITEM = 0, TYPE_LOADER = 1;
    private int VISIBLE_THRESHOLD = 3;

    private final SparseArray<Scenario> data = new SparseArray<>();
    private Scenario dummy;

    private OnClickListener listener;
    private LayoutInflater inflater;
    private RecyclerView content;
    private Loader loader;

    private boolean isLoading = false, end = false, loaderAllowed = false;
    private int lastVisibleItem = 0;
    private int lastItemId = -1;

    private View.OnClickListener listenerWrapper = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onClick(
                        data.valueAt(content.getChildAdapterPosition(v))
                );
            }
        }
    };

    public class ScenarioViewHolder extends RecyclerView.ViewHolder {
        private TextView title, count;

        public ScenarioViewHolder(@NonNull View view) {
            super(view);

            title = view.findViewById(R.id.scenarioTitle);
            count = view.findViewById(R.id.scenarioModulesCount);
            view.setOnClickListener(listenerWrapper);
        }

        public void bind(@NonNull Scenario scenario) {
            title.setText(scenario.getTitle());

            count.setText(
                    String.valueOf(
                            scenario.getDataSize()
                    )
            );
        }
    }

    public class ScenarioLoaderViewHolder extends RecyclerView.ViewHolder {
        private boolean stopped = false;
        private ConstraintLayout loader;
        private AppCompatButton retry;

        public ScenarioLoaderViewHolder(@NonNull View view) {
            super(view);

            loader = view.findViewById(R.id.scenarioLoaderFace);
            retry = view.findViewById(R.id.scenarioLoaderRetry);

            retry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    retry();
                }
            });

            retry.setClickable(false);
        }

        public void retry() {
            if (stopped) {
                switchBar();
                isLoading = false;
                callLoader();
            }
        }

        public void stopped() {
            if (!stopped) {
                switchBar();
            }
        }

        private void switchBar() {
            if (stopped) {
                retry.setClickable(false);
                SimpleAnimator.alpha2(retry, 150, 0.0f);
                SimpleAnimator.alpha2(loader, 250, 1.0f);

            } else {
                retry.setClickable(true);
                SimpleAnimator.alpha2(retry, 250, 1.0f);
                SimpleAnimator.alpha2(loader, 350, 0.0f);
            }

            stopped = !stopped;
        }
    }

    public ScenariosAdapter(RecyclerView content, LayoutInflater inflater) {
        final LinearLayoutManager manager = (LinearLayoutManager) content.getLayoutManager();
        this.inflater = inflater;
        this.content = content;

        dummy = new Scenario(
                -1,
                "",
                new ArrayList<Scenario.Item>()
        );

        content.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                lastVisibleItem = manager.findLastVisibleItemPosition();
                if (loaderAllowed && !isLoading && !end && lastVisibleItem + VISIBLE_THRESHOLD >= manager.getItemCount()) {
                    callLoader();
                }
            }
        });
    }

    public void callLoader() {
        if (!isLoading && loader != null) {
            startLoading();
            loader.onThresholdReached();
        }
    }

    private void startLoading() {
        isLoading = true;
    }

    public void setLoaded(boolean end) {
        Log.d("LOGTAG", "loaded!");
        this.end = end;

        if (end) {
            loaderAllowed = false;
            data.remove(Integer.MAX_VALUE);
            notifyItemRemoved(data.size());
        }

        isLoading = false;
    }

    public void setData(@NonNull ArrayList<Scenario> data) {
        synchronized (this.data) {
            this.data.clear();

            for (Scenario i : data) {
                if (i == null) {
                    throw new IllegalArgumentException(
                            "Scenario object cannot be null"
                    );
                }

                this.data.put(i.getId(), i);
                lastItemId = Math.max(lastItemId, i.getId());
            }

            notifyDataSetChanged();
        }
    }

    public void clearData() {
        loaderAllowed = false;
        data.clear();
        notifyDataSetChanged();
    }

    public void allowLoaderItem() {
        if (data.get(Integer.MAX_VALUE, dummy) == dummy) {
            data.put(Integer.MAX_VALUE, null);
            notifyItemInserted(data.size() - 1);
        }

        loaderAllowed = true;
    }

    @Nullable
    public ScenarioLoaderViewHolder getLoaderItem() {
        if (data.size() == 0) return null;

        RecyclerView.ViewHolder i = content.getChildViewHolder(
                content.getChildAt(data.size() - 1)
        );

        return !(i instanceof ScenarioLoaderViewHolder) ? null :
                (ScenarioLoaderViewHolder) i;
    }

    public void stopLoader() {
        loaderAllowed = false;
    }

    public int getLastItemId() {
        return lastItemId;
    }

    @NonNull
    public SparseArray<Scenario> getData() {
        return data;
    }

    public void pushData(@NonNull Scenario scenario) {
        synchronized (data) {
            boolean override = (data.get(scenario.getId()) != null);
            data.put(scenario.getId(), scenario);

            if (!override) {
                notifyItemInserted(data.indexOfKey(scenario.getId()));

            } else {
                notifyItemChanged(data.indexOfKey(scenario.getId()));
            }

            lastItemId = Math.max(lastItemId, scenario.getId());
        }
    }

    public void deleteData(int scenarioId) {
        synchronized (data) {
            int pos = data.indexOfKey(scenarioId);

            if (pos != -1) {
                data.remove(scenarioId);
                notifyItemRemoved(pos);
            }
        }
    }

    public void setListener(@NonNull OnClickListener listener) {
        this.listener = listener;
    }

    public void setLoader(@NonNull Loader loader) {
        this.loader = loader;
    }

    @Override
    public int getItemViewType(int position) {
        return data.valueAt(position) == null ? TYPE_LOADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            return new ScenarioViewHolder(
                    inflater.inflate(R.layout.scenarios_item, parent, false)
            );

        } else {
            return new ScenarioLoaderViewHolder(
                    inflater.inflate(R.layout.scenarios_item_loading, parent, false)
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ScenarioViewHolder) {
            ScenarioViewHolder viewHolder = (ScenarioViewHolder) holder;
            viewHolder.bind(data.valueAt(position));
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public interface OnClickListener {
        void onClick(Scenario scenario);
    }

    public interface Loader {
        void onThresholdReached();
    }
}
