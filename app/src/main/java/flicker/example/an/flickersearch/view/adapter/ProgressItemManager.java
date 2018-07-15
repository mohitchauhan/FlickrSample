package flicker.example.an.flickersearch.view.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import flicker.example.an.flickersearch.R;
import flicker.example.an.flickersearch.data.model.ListItem;
import flicker.example.an.flickersearch.data.model.ProgressItem;

/**
 */

public class ProgressItemManager implements AdapterDelegate<ProgressItemManager.SearchProgressViewHolder, ListItem> {

    public ProgressItemManager() {
    }


    @Override
    public boolean isItemForViewType(ListItem feed) {
        return feed instanceof ProgressItem ? true : false;
    }

    @Override
    public SearchProgressViewHolder onCreateViewHolder(ViewGroup parent) {
        LayoutInflater mInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView = mInflater.inflate(R.layout.layout_search_progress, parent, false);
        return new SearchProgressViewHolder(convertView);
    }

    @Override
    public void onBindViewHolder(ListItem feed, SearchProgressViewHolder holder) {
        holder.txtMessage.setText(((ProgressItem)feed).getLoadingText());
    }


    public static class SearchProgressViewHolder extends RecyclerView.ViewHolder {
        private TextView txtMessage;

        public SearchProgressViewHolder(View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.searching);
        }
    }

}
