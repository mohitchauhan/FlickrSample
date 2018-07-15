package flicker.example.an.flickersearch.view.adapter;

import android.content.Context;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import flicker.example.an.flickersearch.R;
import flicker.example.an.flickersearch.Utils;
import flicker.example.an.flickersearch.data.model.EmptyStateHeader;
import flicker.example.an.flickersearch.data.model.ListItem;
import flicker.example.an.flickersearch.data.model.Photo;
import flicker.example.an.flickersearch.data.model.ProgressItem;
import flicker.example.an.flickersearch.imageLoader.ImageDownloader;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


/**
 * Search results adapter.
 * Use {@link DiffUtil} for better search experience
 */

public class SearchAdapter extends RecyclerView.Adapter {

    private static String TAG = "SearchAdapter";
    private List<ListItem> dataSet;
    private int dataVersion;
    private CompositeDisposable compositeDisposable;
    private SparseArrayCompat<AdapterDelegate<? extends RecyclerView.ViewHolder, ListItem>> delegatorsList;
    private DefaultItemViewManager defaultAdapterDelegate;
    private ImageDownloader imageFetcher;
    private ProgressItem progressItem;
    private EmptyStateHeader emptyStateHeader;
    private Context context;


    public SearchAdapter(Context context, ImageDownloader imageFetcher) {
        this.context = context;
        dataSet = new ArrayList<>();
        progressItem = new ProgressItem();
        emptyStateHeader = new EmptyStateHeader();
        this.imageFetcher = imageFetcher;
        initialiseAdpaterDelegates();
    }

    public void initialiseAdpaterDelegates() {
        this.delegatorsList = new SparseArrayCompat<>();

        int iterator = 0;

        EmptyStateItemManager emptyStateItemManager = new EmptyStateItemManager();
        this.delegatorsList.put(iterator++, emptyStateItemManager);

        ProgressItemManager progressItemManager = new ProgressItemManager();
        this.delegatorsList.put(iterator++, progressItemManager);

        defaultAdapterDelegate = new DefaultItemViewManager(imageFetcher);

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AdapterDelegate<? extends RecyclerView.ViewHolder, ListItem> delegate = getDelegateForViewType(viewType);
        return delegate.onCreateViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem info = dataSet.get(position);
        AdapterDelegate delegate = getDelegateForViewType(holder.getItemViewType());
        delegate.onBindViewHolder(info, holder);
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    @Override
    public int getItemViewType(int position) {
        return getItemViewType(dataSet.get(position));
    }

    private int getItemViewType(ListItem feed) {
        int delegatesCount = delegatorsList.size();
        if (feed != null) {
            for (int index = 0; index < delegatesCount; index++) {
                AdapterDelegate<? extends RecyclerView.ViewHolder, ListItem> delegate = delegatorsList.valueAt(index);
                if (delegate.isItemForViewType(feed)) {
                    return delegatorsList.keyAt(index);
                }
            }
        }
        return delegatesCount;
    }

    public ListItem getItem(int position) {
        if (position < 0 || dataSet == null || dataSet.size() <= position) {
            return null;
        }
        return dataSet.get(position);
    }

    public AdapterDelegate<? extends RecyclerView.ViewHolder, ListItem> getDelegateForViewType(int viewType) {
        if (viewType == delegatorsList.size()) {
            return defaultAdapterDelegate;
        }

        AdapterDelegate<? extends RecyclerView.ViewHolder, ListItem> delegate = delegatorsList.get(viewType);
        if (delegate == null) {
            return defaultAdapterDelegate;
        }
        return delegate;
    }





    @MainThread
    public void replace(final List<ListItem> update) {
        imageFetcher.setPauseWork(true);
        dataVersion ++;
        if (dataSet == null) {
            if (update == null) {
                return;
            }
            dataSet = update;
            notifyDataSetChanged();
        } else if (update == null) {
            int oldSize = dataSet.size();
            dataSet = null;
            notifyItemRangeRemoved(0, oldSize);
        } else {
            final int startVersion = dataVersion;
            final List<ListItem> oldItems = dataSet;
            if (compositeDisposable == null) compositeDisposable = new CompositeDisposable();
            compositeDisposable.add(Observable.just(update).map(new Function<List<ListItem>, DiffUtil.DiffResult>() {
                @Override
                public DiffUtil.DiffResult apply(final List<ListItem> newList) throws Exception {
                    return DiffUtil.calculateDiff(new DiffUtil.Callback() {
                        @Override
                        public int getOldListSize() {
                            return oldItems.size();
                        }

                        @Override
                        public int getNewListSize() {
                            return newList.size();
                        }

                        @Override
                        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                            return SearchAdapter.this.areItemsTheSame(oldItems.get(oldItemPosition), newList.get(newItemPosition));
                        }

                        @Override
                        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                            return SearchAdapter.this.areContentsTheSame(oldItems.get(oldItemPosition), newList.get(newItemPosition));
                        }
                    }, false);

                }
            }).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableObserver<DiffUtil.DiffResult>() {
                        @Override
                        public void onNext(DiffUtil.DiffResult diffResult) {
                            if (startVersion != dataVersion) {
                                // ignore update
                                return;
                            }
                            dataSet = update;
                            imageFetcher.setPauseWork(false);
                            diffResult.dispatchUpdatesTo(SearchAdapter.this);

                        }

                        @Override
                        public void onError(Throwable e) {
                        }

                        @Override
                        public void onComplete() {
                        }
                    }));

        }
    }

    private  boolean areItemsTheSame(ListItem oldItem, ListItem newItem){
        if (oldItem instanceof Photo && newItem instanceof Photo){
            return Utils.equals(((Photo)oldItem).getId(), ((Photo)newItem).getId());
        }else  if (oldItem instanceof ProgressItem && newItem instanceof ProgressItem){
            return true;
        } if (oldItem instanceof EmptyStateHeader && newItem instanceof EmptyStateHeader){
            return true;
        } else {
            return false;
        }
    }

    private  boolean areContentsTheSame(ListItem oldItem, ListItem newItem){
        if (oldItem instanceof Photo && newItem instanceof Photo){
            return Utils.equals(((Photo)oldItem).getTitle(), ((Photo)newItem).getTitle());
        }else  if (oldItem instanceof ProgressItem && newItem instanceof ProgressItem){
            return true;
        } if (oldItem instanceof EmptyStateHeader && newItem instanceof EmptyStateHeader){
            return true;
        }else {
            return false;
        }
    }

    public void release(){
        if (compositeDisposable != null && !compositeDisposable.isDisposed()){
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
    }

    public void showProgressBar(boolean isLoadMore){
        if (!(dataSet.contains(progressItem))) {
            progressItem.setLoadingText(isLoadMore ? context.getString(R.string.txt_loading)
                    : context.getString(R.string.txt_searching));
            boolean isAdded = dataSet.add(progressItem);
            if (isAdded){
                notifyItemInserted(dataSet.size());
            }
        }
    }


    public void hideProgressBar(){
        boolean removed = dataSet.remove(progressItem);
        if (removed){
            notifyItemRemoved(dataSet.size());
        }
    }


    public void showEmptyState(){
        if (!(dataSet.contains(emptyStateHeader))) {
            boolean isAdded = dataSet.add(emptyStateHeader);
            if (isAdded){
                notifyItemInserted(dataSet.size());
            }
        }
    }


    public void hideEmptyState(){
        boolean removed = dataSet.remove(emptyStateHeader);
        if (removed){
            notifyItemRemoved(dataSet.size());
        }
    }


    public List<ListItem> getItems() {
        return dataSet;
    }

    public ListItem getProgressBarItem() {
        return progressItem;
    }
}
