package com.example.lieu;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class FilterFragmentRVAdapter extends RecyclerView.Adapter<FilterFragmentRVAdapter.FilterFragmentRVAdapterViewHolder>
{
    // Use context
    private Context context;

    // The click listener
    private OnCellFilterItemClickListener listener;

    // Contents
    private ArrayList<FilterDataItem> items;

    // Class describing cell contents
    public static class FilterFragmentRVAdapterViewHolder extends RecyclerView.ViewHolder {
        public TextView text_view_ssid, text_view_bssid, text_view_filtered;

        public FilterFragmentRVAdapterViewHolder (@NonNull View itemView) {
            super(itemView);
            text_view_ssid = itemView.findViewById(R.id.text_view_ssid);
            text_view_bssid = itemView.findViewById(R.id.text_view_bssid);
            text_view_filtered = itemView.findViewById(R.id.text_view_filtered);
        }

        public void bind (final FilterDataItem item, final OnCellFilterItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override public void onClick (View v) {
                    listener.onFilterDataItemClick(v, item);
                }
            });
        }
    }

    public FilterFragmentRVAdapter (OnCellFilterItemClickListener listener, ArrayList<FilterDataItem> items, Context context)
    {
        this.context = context;
        this.listener = listener;
        this.items = items;
    }

    @NonNull
    @Override
    public FilterFragmentRVAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cell_filter_item,
                viewGroup, false);
        FilterFragmentRVAdapter.FilterFragmentRVAdapterViewHolder ffvh = new FilterFragmentRVAdapterViewHolder(v);
        return ffvh;
    }

    @Override
    public void onBindViewHolder(@NonNull FilterFragmentRVAdapterViewHolder filterFragmentRVAdaptorViewHolder, int i) {

        // Extract item at location
        FilterDataItem item = items.get(i);

        // Is this item filtered?
        boolean filtered = item.getFiltered();

        // Set the color + text of the filtered textview
        int c = ContextCompat.getColor(context, R.color.colorLimeGreen);
        String filtered_text = "Included";
        if (filtered == true) {
            c = ContextCompat.getColor(context, R.color.colorAccent);
            filtered_text = "Excluded";
        }
        filterFragmentRVAdaptorViewHolder.text_view_filtered.setTextColor(c);
        filterFragmentRVAdaptorViewHolder.text_view_filtered.setText(filtered_text);

        // Set SSID
        filterFragmentRVAdaptorViewHolder.text_view_ssid.setText(item.getSSID());

        // Set the BSSID
        filterFragmentRVAdaptorViewHolder.text_view_bssid.setText(item.getBSSID());

        // Assign the listener
        filterFragmentRVAdaptorViewHolder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
