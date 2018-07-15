package flicker.example.an.flickersearch.data.model;

import android.util.Log;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class FlickerApiResponse {

    @SerializedName("photos")
    @Expose
    private Photos photos;
    @SerializedName("stat")
    @Expose
    private String stat;

    private String query = "";
    private boolean empty;

    public Photos getPhotos() {
        return photos;
    }

    public void setPhotos(Photos photos) {
        this.photos = photos;
    }

    public String getStat() {
        return stat;
    }

    public void setStat(String stat) {
        this.stat = stat;
    }

    public static FlickerApiResponse getEmptyResponse(){
        FlickerApiResponse apiResponse = new FlickerApiResponse();
        Photos photos = new Photos();
        photos.setPhoto(new ArrayList<Photo>());
        apiResponse.photos = photos;
        return apiResponse;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void update(FlickerApiResponse newResult) {
        if (newResult.photos.getTotal() == 0) return;
        Log.d("FlickerApiResponse", newResult.toString());
        photos.getPhoto().addAll(newResult.photos.getPhoto());
        photos.setTotal(newResult.photos.getTotal());
        photos.setPages(newResult.photos.getPages());
        photos.setPage(newResult.photos.getPage());
    }

    @Override
    public String toString() {
        return "FlickerApiResponse{" +
                "photos=" + photos +
                ", stat='" + stat + '\'' +
                ", query='" + query + '\'' +
                '}';
    }

    public boolean isEmpty() {
        return photos.getTotal() == 0;
    }
}



