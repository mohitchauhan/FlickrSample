package flicker.example.an.flickersearch.di;

import dagger.Module;
import dagger.Provides;
import flicker.example.an.flickersearch.data.api.FlickrDataSource;
import flicker.example.an.flickersearch.view.SearchPresenter;
import flicker.example.an.flickersearch.view.SearchResultHolder;

@Module
public class SearchViewModule {

    @Provides
    SearchResultHolder provideSearchResultHolder() {
        return new SearchResultHolder();
    }

    @Provides
    SearchPresenter provideSearchPresenter(FlickrDataSource remoteDataSource, SearchResultHolder searchResultHolder) {
        return new SearchPresenter(remoteDataSource, searchResultHolder);
    }
}
