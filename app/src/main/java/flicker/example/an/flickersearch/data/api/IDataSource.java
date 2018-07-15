package flicker.example.an.flickersearch.data.api;

import flicker.example.an.flickersearch.data.model.RxResult;
import flicker.example.an.flickersearch.data.model.FlickerApiResponse;
import io.reactivex.Observable;

public interface IDataSource {
    Observable<RxResult<FlickerApiResponse>> search(String searchText);
    Observable<RxResult<FlickerApiResponse>> loadMore(String searchText, int lastIndex);
}
