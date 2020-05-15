package com.example.lieu;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.ArrayList;

public class DataFragment extends Fragment implements OnCellDataItemClickListener, Swap {

    // The recycler view
    private RecyclerView recyclerView;

    // The recycler view adapter
    private RecyclerView.Adapter recyclerViewAdaptor;

    // The recycler view layout manager
    private RecyclerView.LayoutManager recyclerViewLayoutManager;

    // The list of cell items to show
    private ArrayList<CellDataItem> cellDataItems;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Retreive the DataManager singleton
        DataManager dataManager = DataManager.getInstance();

        // Initialize the array of items
        this.cellDataItems = new ArrayList<CellDataItem>();

        // Initialize the cells
        for (int i = 0; i < dataManager.getCellCount(); ++i) {
            CellDataItem item = new CellDataItem(i + 1);
            this.cellDataItems.add(item);
        }


        return inflater.inflate(R.layout.fragment_data, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        // Initialize the recycler view
        this.recyclerView = getView().findViewById(R.id.recyclerView);

        // Set fixed size if we know our items won't be changing
        this.recyclerView.setHasFixedSize(true);

        // Initialize the layout manager
        this.recyclerViewLayoutManager = new LinearLayoutManager(getActivity());

        // Set the layout manager for the recycler view
        this.recyclerView.setLayoutManager(this.recyclerViewLayoutManager);

        // Initialize the adaptor to our custom one
        this.recyclerViewAdaptor = new DataFragmentRVAdaptor(this, this.cellDataItems);

        // Set the adaptor for the recycler view
        this.recyclerView.setAdapter(this.recyclerViewAdaptor);
    }

    @Override
    public void onCellDataItemClick (CellDataItem item) {
        System.out.println("You selected cell " + item.getCellText());

        // Create new fragment
        Fragment cellFragment = new CellFragment();

        // Pass argument through bundle
        Bundle args = new Bundle();
        args.putInt("cellID", item.getCellID());
        cellFragment.setArguments(args);

        // Create a new transaction
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.replace(R.id.fragment_container, cellFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    public void swappingOut () {

    }

}
