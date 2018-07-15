package flicker.example.an.flickersearch.view;

public interface ISearchPresenter {
    public void loadDefaults();
    public void search(String query);
    public void loadMore(String query);
}
