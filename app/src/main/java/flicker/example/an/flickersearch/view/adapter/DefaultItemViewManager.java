package flicker.example.an.flickersearch.view.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import flicker.example.an.flickersearch.R;
import flicker.example.an.flickersearch.data.model.ListItem;
import flicker.example.an.flickersearch.data.model.Photo;
import flicker.example.an.flickersearch.imageLoader.ImageDownloader;

public class DefaultItemViewManager implements AdapterDelegate<DefaultItemViewManager.DefaultItemViewHolder, ListItem> {

    private ImageDownloader imageFetcher;

    public DefaultItemViewManager(ImageDownloader imageFetcher) {
        this.imageFetcher = imageFetcher;
    }

    @Override
    public boolean isItemForViewType(ListItem item) {
        return item instanceof Photo;
    }

    @Override
    public DefaultItemViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search, parent, false);
        return new DefaultItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ListItem item, DefaultItemViewHolder holder) {
        Photo photo = (Photo) item;
        String url = String.format("http://farm%s.static.flickr.com/%s/%s_%s.jpg", photo.getFarm(), photo.getServer(), photo.getId(), photo.getSecret());
        imageFetcher.setLoadingImage(R.drawable.empty_photo);
       imageFetcher.loadImage(url, holder.photoImage);
    }


    public static class DefaultItemViewHolder extends RecyclerView.ViewHolder {

        ImageView photoImage;

        public DefaultItemViewHolder(View itemView) {
            super(itemView);
            this.photoImage = itemView.findViewById(R.id.photoView);
        }
    }


}
