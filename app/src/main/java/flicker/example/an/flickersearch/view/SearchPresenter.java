package flicker.example.an.flickersearch.view;

import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import flicker.example.an.flickersearch.BasePresenter;
import flicker.example.an.flickersearch.data.api.FlickrDataSource;
import flicker.example.an.flickersearch.data.api.IDataSource;
import flicker.example.an.flickersearch.data.model.FlickerApiResponse;
import flicker.example.an.flickersearch.data.model.RxResult;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class SearchPresenter implements BasePresenter<ISearchView<FlickerApiResponse>>, ISearchPresenter {

    private static final String TAG = "SearchPresenter";

    private PublishSubject<String> mLoadMoreSubject;
    private PublishSubject<String> mSubject;
    private ISearchView<FlickerApiResponse> searchView;
    private CompositeDisposable compositeDisposable;
    private Disposable loadMoreDisposable;
    private Disposable loadDefaultsDisposable;
    private Disposable timeOutDisposable;
    private IDataSource dataSource;
    private SearchResultHolder searchResultHolder;

    @Inject
    public SearchPresenter(FlickrDataSource dataSource, SearchResultHolder searchResultHolder) {
        this.dataSource = dataSource;
        this.searchResultHolder = searchResultHolder;
    }

    @Override
    public void loadDefaults() {
        handleSearchStart("");
        loadDefaultsDisposable = dataSource.search("").subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<RxResult<FlickerApiResponse>>() {
                    @Override
                    public void onNext(RxResult<FlickerApiResponse> value) {
                        if (value.throwable == null){
                            handleSearchResult(value.data, false);
                        }else{
                            handleSearchError("", value.throwable);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        handleSearchError("", e);
                    }

                    @Override
                    public void onComplete() {
                    }
                });
        compositeDisposable.add(loadDefaultsDisposable);
    }

    @Override
    public void search(String query) {
        if (loadDefaultsDisposable != null && !loadDefaultsDisposable.isDisposed()){
            loadDefaultsDisposable.dispose();
        }
        mSubject.onNext(query);
    }

    @Override
    public void loadMore(String query) {
        mLoadMoreSubject.onNext(query);
    }

    @Override
    public void onAttach(ISearchView<FlickerApiResponse> searchView) {
        this.searchView = searchView;
        mSubject = PublishSubject.create();
        mLoadMoreSubject = PublishSubject.create();
        compositeDisposable = new CompositeDisposable();
        subscribeSearch();
    }


    /**
     * Method subscribe to Search subject
     */
    private void subscribeSearch() {
        compositeDisposable.add(mSubject.debounce(300, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(String query) throws Exception {
                        handleSearchStart(query);
                    }
                })
                .switchMap(new Function<String, ObservableSource<Void>>() {
                    @Override
                    public ObservableSource<Void> apply(final String query) throws Exception {
                        if (TextUtils.isEmpty(query)) {
                            return getEmptyResultsObservable(query);
                        } else {
                            disposeOldSearchResetDisposable();
                            timeOutDisposable = getSearchRequestTimeoutObservable(query)
                                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Consumer<FlickerApiResponse>() {
                                        @Override
                                        public void accept(FlickerApiResponse searchResults) throws Exception {
                                            handleSearchResult(searchResults, false);
                                        }
                                    });
                            compositeDisposable.add(timeOutDisposable);

                            return getSearchRequestObservable(query).observeOn(AndroidSchedulers.mainThread()).doOnComplete(new Action() {
                                        @Override
                                        public void run() throws Exception {
                                            handleSearchComplete(query);
                                        }
                                    });
                        }

                    }
                })
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Void>() {
                    @Override
                    public void onNext(Void value) {
                        Log.d(TAG, "PublishSubject onResult");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "PublishSubject onError should not get called ", e);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "PublishSubject final onComplete");
                    }
                }));

    }

    /**
     * Method Returns Search Observable
     * @param query
     * @return
     */
    private Observable<Void> getSearchRequestObservable(final String query) {
        return dataSource.search(query)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<RxResult<FlickerApiResponse>, ObservableSource<Void>>() {
                    @Override
                    public ObservableSource<Void> apply(RxResult<FlickerApiResponse> result) throws Exception {
                        if (result.throwable != null) {
                            handleSearchError(query, result.throwable);
                        } else {
                            handleSearchResult(result.data, true);
                        }
                        return Observable.empty();
                    }
                })
                .onErrorResumeNext(new Function<Throwable, ObservableSource<Void>>() {
                    @Override
                    public ObservableSource<Void> apply(Throwable throwable) throws Exception {
                        handleSearchError(query, throwable);
                        return Observable.empty();
                    }
                }).delay(200, TimeUnit.MILLISECONDS);
    }

    /**
     * Method returns timeout observable.
     * To make experience smooth, we don't clear old query results until api call success or request timeout trigger for new query
     * @param query
     * @return Observable
     */
    private Observable<FlickerApiResponse> getSearchRequestTimeoutObservable(final String query) {
        return Observable.just(FlickerApiResponse.getEmptyResponse()).map(new Function<FlickerApiResponse, FlickerApiResponse>() {
            @Override
            public FlickerApiResponse apply(FlickerApiResponse searchResults) throws Exception {
                searchResults.setQuery(query);
                return searchResults;
            }
        }).delay(800, TimeUnit.MILLISECONDS);
    }

    /**
     * Method returns empty results for blank query trigger by clear search
     * @param query
     * @return Observable
     */
    private Observable<Void> getEmptyResultsObservable(final String query) {
        return Observable.just(FlickerApiResponse.getEmptyResponse())
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<FlickerApiResponse, ObservableSource<Void>>() {
                    @Override
                    public ObservableSource<Void> apply(FlickerApiResponse searchResults) throws Exception {
                        handleSearchResult(searchResults, false);
                        return Observable.empty();
                    }
                }).delay(200, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        handleSearchComplete(query);
                    }
                });
    }


    private void disposeOldSearchResetDisposable() {
        if (timeOutDisposable != null && !timeOutDisposable.isDisposed()) {
            timeOutDisposable.dispose();
        }
    }

    private void handleSearchStart(String query) {
        searchResultHolder.reset();
        disposeFetchMoreObservable();
        searchView.onSearchStart(query);
    }

    private void handleSearchComplete(String query) {
        subscribeLoadMore();
        searchView.onSearchComplete(query, searchResultHolder.hasMore());
    }

    private void handleSearchError(String query, Throwable throwable) {
        searchView.onSearchError(query, throwable);
    }

    private void handleSearchResult(FlickerApiResponse searchResults, boolean isLoadMore) {
        searchView.onResultsFetched(searchResultHolder.update(searchResults), isLoadMore);
    }



    private void handleLoadMoreComplete(String query) {
        searchView.onLoadMoreComplete(query, searchResultHolder.getNextPage() != SearchResultHolder.NO_MORE_RESULT);
    }

    private void handleLoadMoreError(String query, Throwable throwable) {
        searchView.onLoadMoreError(query, throwable);
    }

    private void handleLoadMoreStart(String query) {
        searchView.onLoadMoreStart(query);
    }

    @VisibleForTesting
    public void subscribeLoadForTest(){
        subscribeLoadMore();
    }

    /**
     * Method subscribe to load more when a search is finished
     *
     */
    private void subscribeLoadMore() {
        loadMoreDisposable = mLoadMoreSubject
                .flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(final String query) throws Exception {
                        return Observable.just(query);
                    }
                })
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(String query) throws Exception {
                        handleLoadMoreStart(query);
                    }
                })
                .flatMap(new Function<String, ObservableSource<Void>>() {
                    @Override
                    public ObservableSource<Void> apply(final String query) throws Exception {
                        return getLoadMoreObservable(query);
                    }
                })
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Void>() {
                    @Override
                    public void onNext(Void value) {
                        Log.d(TAG, "FetchMorePublishSubject onResult");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "FetchMorePublishSubject onError should not get called ", e);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "FetchMorePublishSubject final onComplete");
                    }
                });
        compositeDisposable.add(loadMoreDisposable);
    }

    /**
     * Return load more observable
     * @param query
     * @return Observable
     */
    private Observable<Void> getLoadMoreObservable(String query) {
        return dataSource.loadMore(query, searchResultHolder.getNextPage())
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<RxResult<FlickerApiResponse>, ObservableSource<Void>>() {
                    @Override
                    public ObservableSource<Void> apply(RxResult<FlickerApiResponse> result) throws Exception {
                        if (result.throwable != null) {
                            handleLoadMoreError(query, result.throwable);
                        } else {
                            handleSearchResult(result.data, true);
                        }
                        return Observable.empty();
                    }
                })
                .onErrorResumeNext(new Function<Throwable, ObservableSource<Void>>() {
                    @Override
                    public ObservableSource<Void> apply(Throwable throwable) throws Exception {
                        handleLoadMoreError(query, throwable);
                        return Observable.empty();
                    }
                })
                .delay(200, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        handleLoadMoreComplete(query);
                    }
                });
    }


    /**
     * Dispose load more observable when search query change and until search not finished for it
     */
    private void disposeFetchMoreObservable() {
        if(loadMoreDisposable != null && !loadMoreDisposable.isDisposed()) {
            loadMoreDisposable.dispose();
        }
        loadMoreDisposable = null;
    }



    @Override
    public void onDetach() {
        if (compositeDisposable != null && compositeDisposable.isDisposed()){
            compositeDisposable.dispose();
        }
        searchView = null;
    }
}
