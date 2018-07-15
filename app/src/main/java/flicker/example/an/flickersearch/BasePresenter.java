package flicker.example.an.flickersearch;

public interface  BasePresenter<V> {
    void onAttach(V mvpView);
    void onDetach();

}
