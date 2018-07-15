package flicker.example.an.flickersearch.view;

import flicker.example.an.flickersearch.data.model.FlickerApiResponse;

/**
 * Search results processor for holding and updating results and pagination
 */
public class SearchResultHolder {

    public static final int NO_MORE_RESULT = -100;

    private FlickerApiResponse lastResult;
    private final Object lock = new Object();

    public SearchResultHolder() {
    }

    public FlickerApiResponse update(FlickerApiResponse newResult) {
        synchronized (lock) {
            if (lastResult == null || newResult.isEmpty()) {
                lastResult = newResult;
            } else {
                lastResult.update(newResult);
            }
            return lastResult;
        }
    }

    public void reset() {
        synchronized (lock) {
            lastResult = null;
        }
    }

    public int getNextPage() {
        synchronized (lock) {
            if (lastResult.getPhotos().getPages() <= lastResult.getPhotos().getPage()) {
                return NO_MORE_RESULT;
            }
            return lastResult.getPhotos().getPage() + 1;
        }
    }

    public boolean hasMore(){
        return getNextPage() != SearchResultHolder.NO_MORE_RESULT;
    }

}
