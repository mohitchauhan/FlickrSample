package flicker.example.an.flickersearch.data.model;

/**
 * Class a wrapper for RxResponse. Useful in case of subject to prevent its termination in case of error
 * @param <T>
 */
public class RxResult<T> {
    public T data;
    public Throwable throwable;

    public RxResult(T data) {
        this.data = data;
    }

    public RxResult(Throwable throwable) {
        this.throwable = throwable;
    }

}
