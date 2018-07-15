package flicker.example.an.flickersearch.data.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.inject.Inject;

import flicker.example.an.flickersearch.data.model.FlickerApiResponse;
import flicker.example.an.flickersearch.data.model.RxResult;
import io.reactivex.Observable;
import io.reactivex.Observer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FlickrDataSource implements IDataSource {

    private static final int PAGE_SIZE = 15;

    private final OkHttpClient client;
    private final UrlProvider urlProvider;
    private final Gson gson;

    @Inject
    public FlickrDataSource(OkHttpClient client, UrlProvider urlProvider, Gson gson) {
        this.client = client;
        this.urlProvider = urlProvider;
        this.gson = gson;
    }

    /**
     * Method fetch search results from server
     * @param searchText Search Query
     * @return Observable
     */
    @Override
    public Observable<RxResult<FlickerApiResponse>> search(final String searchText) {
        return new Observable<RxResult<FlickerApiResponse>>() {
            @Override
            protected void subscribeActual(Observer<? super RxResult<FlickerApiResponse>> observer) {
                    Request request = new Request.Builder()
                            .addHeader("Accept", "application/json; q=0.5")
                            .url(urlProvider.provideSearchUrl(searchText, PAGE_SIZE, 1))
                            .build();
                try {
                    Response response = client.newCall(request).execute();
                    FlickerApiResponse apiResponse = handleResponse(response, new TypeToken<FlickerApiResponse>(){}.getType());
                    observer.onNext(new RxResult<FlickerApiResponse>(apiResponse));
                    observer.onComplete();
                } catch (ApiException apiException) {
                    observer.onNext(new RxResult<FlickerApiResponse>(apiException));
                    observer.onComplete();
                }catch (Exception e){
                    ApiException apiException = new ApiException(e);
                    observer.onNext(new RxResult<FlickerApiResponse>(apiException));
                    observer.onComplete();
                }
            }
        };
    }

    /**
     * Method lode more results
     * @param searchText Search query
     * @param lastIndex last page index
     * @return Observable
     */
    @Override
    public Observable<RxResult<FlickerApiResponse>> loadMore(final String searchText, final int lastIndex) {
        return new Observable<RxResult<FlickerApiResponse>>() {
            @Override
            protected void subscribeActual(Observer<? super RxResult<FlickerApiResponse>> observer) {

                Request request = new Request.Builder()
                        .addHeader("Accept", "application/json; q=0.5")
                        .url(urlProvider.provideSearchUrl(searchText, PAGE_SIZE, lastIndex))
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    FlickerApiResponse apiResponse = handleResponse(response, new TypeToken<FlickerApiResponse>(){}.getType());
                    observer.onNext(new RxResult<FlickerApiResponse>(apiResponse));
                    observer.onComplete();
                } catch (ApiException apiException) {
                    observer.onNext(new RxResult<FlickerApiResponse>(apiException));
                    observer.onComplete();
                }catch (Exception e){
                    ApiException apiException = new ApiException(e);
                    observer.onNext(new RxResult<FlickerApiResponse>(apiException));
                    observer.onComplete();
                }
            }
        };
    }

    /**
     * Handle the given response, return the deserialized object when the response is successful.
     *
     * @param <T> Type
     * @param response Response
     * @param returnType Return type
     * @throws ApiException If the response has a unsuccessful status code or
     *   fail to deserialize the response body
     * @return Type
     */
    public <T> T handleResponse(Response response, Type returnType) throws ApiException {
        if (response.isSuccessful()) {
            if (returnType == null || response.code() == 204) {
                // returning null if the returnType is not defined,
                // or the status code is 204 (No Content)
                return null;
            } else {
                return deserialize(response, returnType);
            }
        } else {
            String respBody = null;
            if (response.body() != null) {
                try {
                    respBody = response.body().source().readUtf8();
                } catch (IOException e) {
                    throw new ApiException(response.message(), e, response.code(), response.headers().toMultimap());
                }
            }
            throw new ApiException(response.message(), response.code(), response.headers().toMultimap(), respBody);
        }
    }

     /**
     * Deserialize response body to Java object, according to the return type and
     * the Content-Type response header.
     *
     * @param <T> Type
     * @param response HTTP response
     * @param returnType The type of the Java object
     * @return The deserialized Java object
     * @throws ApiException If fail to deserialize response body, i.e. cannot read response body
     *   or the Content-Type of the response is not supported.
     */
    @SuppressWarnings("unchecked")
    public <T> T deserialize(Response response, Type returnType) throws ApiException {
        if (response == null || returnType == null) {
            return null;
        }

        String respBody;
        try {
            if (response.body() != null)
                respBody = response.body().string();
            else
                respBody = null;
        } catch (IOException e) {
            throw new ApiException(e);
        }

        if (respBody == null || "".equals(respBody)) {
            return null;
        }

        String contentType = response.headers().get("Content-Type");
        if (contentType == null) {
            // ensuring a default content type
            contentType = "application/json";
        }
        if (isJsonMime(contentType)) {
            return deserialize(respBody, returnType);
        } else if (returnType.equals(String.class)) {
            // Expecting string, return the raw response body.
            return (T) respBody;
        } else {
            throw new ApiException(
                    "Content type \"" + contentType + "\" is not supported for type: " + returnType,
                    response.code(),
                    response.headers().toMultimap(),
                    respBody);
        }
    }

    /**
     * Deserialize response body to Java object, according to the return type and
     * the Content-Type response header.
     *
     * @param <T> Type
     * @param body HTTP response body
     * @param returnType The type of the Java object
     * @return The deserialized Java object
     * @throws ApiException If fail to deserialize response body, i.e. cannot read response body
     *   or the Content-Type of the response is not supported.
     */
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String body, Type returnType) {
        return gson.fromJson(body, returnType);
    }


    public boolean isJsonMime(String mime) {
        return mime != null && mime.matches("(?i)application\\/json(;.*)?");
    }


}
