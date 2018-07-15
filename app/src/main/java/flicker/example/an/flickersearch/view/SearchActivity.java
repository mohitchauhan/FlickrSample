package flicker.example.an.flickersearch.view;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import flicker.example.an.flickersearch.widgets.EndlessRecyclerViewScrollListener;
import flicker.example.an.flickersearch.R;
import flicker.example.an.flickersearch.Utils;
import flicker.example.an.flickersearch.data.api.ApiException;
import flicker.example.an.flickersearch.data.model.FlickerApiResponse;
import flicker.example.an.flickersearch.data.model.ListItem;
import flicker.example.an.flickersearch.imageLoader.ImageDownloader;
import flicker.example.an.flickersearch.view.adapter.AdapterDelegate;
import flicker.example.an.flickersearch.view.adapter.DefaultItemViewManager;
import flicker.example.an.flickersearch.view.adapter.SearchAdapter;
import flicker.example.an.flickersearch.widgets.SpacesItemDecoration;

public class SearchActivity extends AppCompatActivity implements ISearchView<FlickerApiResponse>{

    private static final String TAG = "SearchActivity";
    private static final int SPAN_COUNT = 3;
    @Inject
    public SearchPresenter searchPresenter;
    private SearchAdapter searchAdapter;
    private RecyclerView recyclerView;

    private String lastQuery;
    private EndlessRecyclerViewScrollListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        searchPresenter.onAttach(this);
        recyclerView = findViewById(R.id.recycler_view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, SPAN_COUNT);
        recyclerView.setLayoutManager(gridLayoutManager);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.spacing);
        recyclerView.addItemDecoration(new SpacesItemDecoration(spacingInPixels));

        int width  = Utils.getScreenWidth(this);
        width = width == -1 ? Utils.dpToPx(100): width/3;
        ImageDownloader imageFetcher = new ImageDownloader(this, width);
        imageFetcher.addImageCache(this,"imageCache");
        searchAdapter = new SearchAdapter(this, imageFetcher);
        recyclerView.setAdapter(searchAdapter);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                AdapterDelegate<? extends RecyclerView.ViewHolder, ListItem> delegate = searchAdapter.getDelegateForViewType(searchAdapter.getItemViewType(position));
                if (!(delegate instanceof DefaultItemViewManager)){
                    return SPAN_COUNT;
                }else {
                    return 1;
                }
            }
        });
        initialiseLoadMore(gridLayoutManager);
        searchPresenter.loadDefaults();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        MenuItem searchItem = menu.findItem(R.id.app_bar_search);
        initSearchView((SearchView) searchItem.getActionView());
        return super.onCreateOptionsMenu(menu);
    }


    private void initSearchView(SearchView searchView) {
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String inputQuery) {
                searchPresenter.search(inputQuery);
                return true;
            }
        });

    }

    public void initialiseLoadMore(GridLayoutManager layoutManager) {
        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                searchPresenter.loadMore(lastQuery);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        };
        recyclerView.addOnScrollListener(scrollListener);
    }


    @Override
    public void onSearchStart(String query) {
        Log.d(TAG, "onSearchStart");
        hideEmptyState();
        scrollListener.disableLoadMore();
        searchAdapter.showProgressBar(false);
    }


    @Override
    public void onResultsFetched(FlickerApiResponse result, boolean isLoadMore) {
        lastQuery = result.getQuery();
        List<ListItem> newData = new ArrayList<ListItem>(result.getPhotos().getPhoto());
        Log.d(TAG, "onResultsFetched "+ lastQuery + " "+newData.size());
        if (!TextUtils.isEmpty(lastQuery)){
            if (!isLoadMore && !newData.contains(searchAdapter.getProgressBarItem())){
                newData.add(0, searchAdapter.getProgressBarItem());
            }
        }
        /*int progressBarItemIndex = searchAdapter.getProgressBarItemIndex();
        if (newData.size() <= 0 && progressBarItemIndex == 0
                && !newData.contains(searchAdapter.getProgressBarItem()) && searchAdapter.getItemCount()<6) {
            newData.add(progressBarItemIndex, searchAdapter.getProgressBarItem());
        }else if (newData.size() == 0){
            searchAdapter.replace(newData);
            newData.add(searchAdapter.getProgressBarItem());
        }*/
        searchAdapter.replace(newData);

    }

    private void hideEmptyState() {
        searchAdapter.hideEmptyState();
    }


    private void showEmptyState() {
        scrollListener.disableLoadMore();
        searchAdapter.showEmptyState();
        searchAdapter.hideProgressBar();
    }



    @Override
    public void onSearchError(String query, Throwable error) {
        error.printStackTrace();
        handleError(error);
        scrollListener.disableLoadMore();
    }

    @Override
    public void onSearchComplete(String query, boolean hasMore) {
        Log.d(TAG, "onSearchComplete "+ query +" hasMore "+hasMore);
        recyclerView.scrollToPosition(0);
        searchAdapter.hideProgressBar();
        if (searchAdapter.getItems().size() == 0 && !TextUtils.isEmpty(query)){
            showEmptyState();
        }
        if (hasMore){
            scrollListener.enableLoadMore();
        }else {
            scrollListener.disableLoadMore();
        }

    }


    @Override
    public void onLoadMoreStart(String query) {
        Log.d(TAG, "onLoadMoreStart "+ query);
        searchAdapter.showProgressBar(true);
    }



    @Override
    public void onLoadMoreError(String query, Throwable error) {
        error.printStackTrace();
        handleError(error);
    }

    private void handleError(Throwable error){
        String message;
        if (error instanceof ApiException && error.getCause() != null &&
                error.getCause() instanceof IOException){
            message = getString(R.string.msg_no_internet);
        }else {
            message = getString(R.string.msg_unknown_error);
        }
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onLoadMoreComplete(String query, boolean hasMore) {
        Log.d(TAG, "onLoadMoreComplete "+ query);
        searchAdapter.hideProgressBar();
        if (!hasMore){
            scrollListener.disableLoadMore();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchPresenter.onDetach();
        searchAdapter.release();
    }
}
