package flicker.example.an.flickersearch.data.api;

import android.net.Uri;

import javax.inject.Inject;

public class UrlProvider {

    private final String scheme;
    private final String host;

    @Inject
    public UrlProvider(String scheme, String host) {
        this.scheme = scheme;
        this.host = host;
    }


    /**
     * Method result search ui
     * @param query
     * @param pageSize
     * @param index
     * @return
     */
    public String provideSearchUrl(String query, int pageSize, int index){

        Uri.Builder builder = new Uri.Builder();
        return builder.scheme(scheme)
                .authority(host)
                .appendPath("services")
                .appendPath("rest")
                .appendQueryParameter("method", "flickr.photos.getRecent")
                .appendQueryParameter("api_key", "3e7cc266ae2b0e0d78e279ce8e361736")
                .appendQueryParameter("format", "json")
                .appendQueryParameter("safe_search", "1")
                .appendQueryParameter("text", query)
                .appendQueryParameter("per_page", String.valueOf(pageSize))
                .appendQueryParameter("page", String.valueOf(index))
                .appendQueryParameter("nojsoncallback", "1").build().toString();
    }

}
