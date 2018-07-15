package flicker.example.an.flickersearch.view;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import flicker.example.an.flickersearch.BuildConfig;
import flicker.example.an.flickersearch.FlickrTestApplication;
import flicker.example.an.flickersearch.data.api.FlickrDataSource;
import flicker.example.an.flickersearch.data.model.FlickerApiResponse;
import flicker.example.an.flickersearch.data.model.RxResult;
import io.reactivex.Observable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest="AndroidManifest.xml", sdk = 21, application = FlickrTestApplication.class)
public class SearchPresenterTest {

    private SearchPresenter searchPresenter;

    @Mock
    public FlickrDataSource flickrDataSource;

    @Mock
    ISearchView<FlickerApiResponse> searchView;

    SearchResultHolder searchResultHolder;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        searchResultHolder = mock(SearchResultHolder.class);
        searchPresenter = new SearchPresenter( flickrDataSource, searchResultHolder);
        searchPresenter.onAttach(searchView);
    }

    @Test
    public void search() {
        String query = "Kitty";
        FlickerApiResponse apiResponse = FlickerApiResponse.getEmptyResponse();
        apiResponse.setQuery(query);
        doReturn(Observable.just(new RxResult<FlickerApiResponse>(apiResponse)))
                .when(flickrDataSource)
                .search(query);

        when(searchResultHolder.update(any(FlickerApiResponse.class))).thenReturn(apiResponse);

        searchPresenter.search(query);
        verify(searchView).onSearchStart(query);
        verify(searchView).onResultsFetched(apiResponse, false);
        verify(searchView).onSearchComplete(query, false);
    }

    @Test
    public void loadMore() {
        String query = "Kitty";
        FlickerApiResponse apiResponse = FlickerApiResponse.getEmptyResponse();
        apiResponse.setQuery(query);

        doReturn(Observable.just(new RxResult<FlickerApiResponse>(apiResponse)))
                .when(flickrDataSource)
                .loadMore(query, 2);

        when(searchResultHolder.update(any(FlickerApiResponse.class))).thenReturn(apiResponse);
        when(searchResultHolder.getNextPage()).thenReturn(2);

        searchPresenter.subscribeLoadForTest();
        searchPresenter.loadMore(query);
        verify(searchView).onLoadMoreStart(query);
        verify(searchView).onResultsFetched(apiResponse, true);
        verify(searchView).onLoadMoreComplete(query, true);
    }


    @After
    public void tearDown() throws Exception {
        searchPresenter.onDetach();
    }
}