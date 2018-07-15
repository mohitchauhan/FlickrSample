package flicker.example.an.flickersearch.view.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import flicker.example.an.flickersearch.R;
import flicker.example.an.flickersearch.data.model.EmptyStateHeader;
import flicker.example.an.flickersearch.data.model.ListItem;

public class EmptyStateItemManager implements AdapterDelegate<EmptyStateItemManager.EmptyViewHolder, ListItem> {

    @Override
    public boolean isItemForViewType(ListItem feed) {
        return feed instanceof EmptyStateHeader ? true: false;
    }

    @Override
    public EmptyViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_empty_state, parent, false);
        return new EmptyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ListItem feed, EmptyViewHolder holder) {
        holder.textMessage.setText(R.string.empty_message);
    }


    public static class EmptyViewHolder extends RecyclerView.ViewHolder {

        TextView textMessage;

        public EmptyViewHolder(View itemView) {
            super(itemView);
            this.textMessage = itemView.findViewById(R.id.emptyStateMessage);
        }
    }


}
