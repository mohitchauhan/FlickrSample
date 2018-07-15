package flicker.example.an.flickersearch.view;

public interface ISearchView<T> {
    void onSearchStart(String query);
    void onResultsFetched(T result, boolean isLoadMore);
    void onSearchError(String query, Throwable error);
    void onSearchComplete(String query, boolean hasMore);

    void onLoadMoreStart(String query);
    void onLoadMoreError(String query, Throwable error);
    void onLoadMoreComplete(String query, boolean hasMore);
}
