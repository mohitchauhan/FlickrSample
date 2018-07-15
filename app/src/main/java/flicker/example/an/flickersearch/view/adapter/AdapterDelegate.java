package flicker.example.an.flickersearch.view.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

/**
 * AdapterDelegate for delegate pattern
 */


public interface AdapterDelegate<T extends RecyclerView.ViewHolder, D> {

    boolean isItemForViewType(D item);

    T onCreateViewHolder(ViewGroup parent);

    void onBindViewHolder(D item, T holder);

}