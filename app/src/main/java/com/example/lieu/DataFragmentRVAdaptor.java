package com.example.lieu;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class DataFragmentRVAdaptor extends RecyclerView.Adapter<DataFragmentRVAdaptor.DataFragmentRVAdaptorViewHolder> {

    // The click listener
    private OnCellDataItemClickListener listener;

    // The contents
    private ArrayList<CellDataItem> items;

    public static class DataFragmentRVAdaptorViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public DataFragmentRVAdaptorViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
        }

        public void bind (final CellDataItem item, final OnCellDataItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onCellDataItemClick(item);
                }
            });
        }

    }

    // Constructor
    public DataFragmentRVAdaptor (OnCellDataItemClickListener listener, ArrayList<CellDataItem> items) {
        this.listener = listener;
        this.items = items;
    }

    @NonNull
    @Override
    public DataFragmentRVAdaptorViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cell_data_item,
                viewGroup, false);
        DataFragmentRVAdaptorViewHolder dfvh = new DataFragmentRVAdaptorViewHolder(v);
        return dfvh;
    }

    @Override
    public void onBindViewHolder(@NonNull DataFragmentRVAdaptorViewHolder dataFragmentRVAdaptorViewHolder, int i) {

        // Extract item at location
        CellDataItem item = items.get(i);

        // Set text
        dataFragmentRVAdaptorViewHolder.textView.setText(item.getCellText());

        // Assign the listener
        dataFragmentRVAdaptorViewHolder.bind(items.get(i), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
