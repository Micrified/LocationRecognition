package com.example.lieu;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FilterFragment extends Fragment implements OnCellFilterItemClickListener, Swap, View.OnClickListener {

    // The recycler view
    private RecyclerView wifi_filtered_recycler_view;
    private RecyclerView wifi_nonfiltered_recycler_view;

    // The recycler view adapter
    private RecyclerView.Adapter wifi_filtered_recycler_view_adapter;
    private RecyclerView.Adapter wifi_nonfiltered_recycler_view_adapter;

    // The recycler view layout manager
    private RecyclerView.LayoutManager wifi_filtered_recycler_view_layout_manager;
    private RecyclerView.LayoutManager wifi_nonfiltered_recycler_view_layout_manager;

    // The scan button
    private Button button_scan_wifi;

    // Loading dialog
    private LoadingDialog loadingDialog;

    // A hashmap of all filtered APs
    private ArrayList<FilterDataItem> data_filtered_items;
    private HashMap<String, FilterDataItem> wifi_filtered_data_items_hashmap;

    // A hashmap of all non-filtered APs
    private ArrayList<FilterDataItem> data_nonfiltered_items;
    private HashMap<String, FilterDataItem> wifi_nonfiltered_data_items_hashmap;


    /*
     *******************************************************************************
     *                              Required Methods                               *
     *******************************************************************************
    */


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {

        // Initialize the arrays
        this.data_filtered_items = new ArrayList<FilterDataItem>();
        this.data_nonfiltered_items = new ArrayList<FilterDataItem>();

        // Initialize the hashmaps
        this.wifi_filtered_data_items_hashmap = new HashMap<>();
        this.wifi_nonfiltered_data_items_hashmap = new HashMap<>();

        // TODO:
        // Load contents into the hashmap - the arrays will update in onViewCreated



        return inflater.inflate(R.layout.fragment_filter, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        // Initialize the recycler view
        this.wifi_filtered_recycler_view = getView().findViewById(R.id.wifi_filtered_recycler_view);
        this.wifi_nonfiltered_recycler_view = getView().findViewById(R.id.wifi_nonfiltered_recycler_view);

        // Set fixed size
        this.wifi_filtered_recycler_view.setHasFixedSize(true);
        this.wifi_nonfiltered_recycler_view.setHasFixedSize(true);

        // Initialize the layout manager
        this.wifi_filtered_recycler_view_layout_manager = new LinearLayoutManager(getActivity());
        this.wifi_nonfiltered_recycler_view_layout_manager = new LinearLayoutManager(getActivity());

        // Set the layout manager for the recycler view
        this.wifi_filtered_recycler_view.setLayoutManager(this.wifi_filtered_recycler_view_layout_manager);
        this.wifi_nonfiltered_recycler_view.setLayoutManager(this.wifi_nonfiltered_recycler_view_layout_manager);

        // Initialize the adapter with the custom one
        this.wifi_filtered_recycler_view_adapter = new FilterFragmentRVAdapter(this,
                this.data_filtered_items, getContext());
        this.wifi_nonfiltered_recycler_view_adapter = new FilterFragmentRVAdapter(this,
                this.data_nonfiltered_items, getContext());

        // Set the adaptor for the recycler view
        this.wifi_filtered_recycler_view.setAdapter(this.wifi_filtered_recycler_view_adapter);
        this.wifi_nonfiltered_recycler_view.setAdapter(this.wifi_nonfiltered_recycler_view_adapter);

        // Initialize the loading dialog popup class
        loadingDialog = new LoadingDialog(this.getActivity());

        // Hook up the button and connect it
        this.button_scan_wifi = getView().findViewById(R.id.button_scan_wifi);
        button_scan_wifi.setOnClickListener(this);

        // Update the data
        refreshDataSources();
    }

    @Override
    public void onFilterDataItemClick(View v, final FilterDataItem item) {
        System.out.println("THIS CAME FROM VIEW: " + v.getId());
        System.out.println("You selected AP " + item.getBSSID());
        String option_text = String.format("%s exclusion of %s (%s)?",
                (item.getFiltered() == true ? "Disable" : "Enable"),
                item.getSSID(), item.getBSSID());

        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setCancelable(false);
        dialog.setTitle("Filter Settings");
        dialog.setMessage(option_text);
        dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                FilterFragment.this.setFilterStateForItem(item, (item.getFiltered() == false));
            }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });

        final AlertDialog alert = dialog.create();
        alert.show();
    }

    @Override
    public void swappingOut() {

        // Update data-manager with the new filter
        System.out.println("Swapping out!");

        DataManager.getInstance().setAccessPointFilter(this.wifi_filtered_data_items_hashmap);
    }

    public void setFilterStateForItem (FilterDataItem item, boolean isFiltered)
    {
        HashMap<String, FilterDataItem> removeHashMap;
        HashMap<String, FilterDataItem> insertHashMap;


        if (isFiltered) {
            System.out.println("Will be filtering " + item.getSSID());

            // Add to filtered set
            insertHashMap = this.wifi_filtered_data_items_hashmap;

            // Remove from non-filtered set
            removeHashMap = this.wifi_nonfiltered_data_items_hashmap;
        } else {
            System.out.println("Will now include " + item.getSSID());

            // Remove from filtered set
            removeHashMap = this.wifi_filtered_data_items_hashmap;

            // Add to non-filtered set
            insertHashMap = this.wifi_nonfiltered_data_items_hashmap;
        }

        // Remove from removeSet (should only appear once)
        String key = item.getBSSID();
        removeHashMap.remove(key);

        // Insert into insertSet (only if not already there)
        if (insertHashMap.containsKey(key) == false) {

            // Create a new item with the opposite status
            FilterDataItem new_item =
                    new FilterDataItem(item.getBSSID(), item.getSSID(), !item.getFiltered());
            insertHashMap.put(key, new_item);
        }

        // Update both lists
        refreshDataSources();
    }


    // Update the filtered and nonfiltered data sources
    void refreshDataSources ()
    {
        this.data_filtered_items.clear();
        this.data_filtered_items.addAll(wifi_filtered_data_items_hashmap.values());

        this.data_nonfiltered_items.clear();
        this.data_nonfiltered_items.addAll(wifi_nonfiltered_data_items_hashmap.values());

        this.wifi_nonfiltered_recycler_view_adapter.notifyDataSetChanged();
        this.wifi_filtered_recycler_view_adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_scan_wifi: {
                this.scanWiFi();
                FilterFragment.this.loadingDialog.startLoadingDialog();
            }
            break;
            default:
                Log.e("FilterManager", "Unknown onClick parent (" + v.getId() + ")");
                break;
        }
    }

    public void scanWiFi () {

        // Get the WiFi manager
        final WifiManager wifiManager = DataManager.getInstance().getWiFiManager();

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                context.unregisterReceiver(this);

                // Get Scan results
                List<ScanResult> results = wifiManager.getScanResults();

                // Post an update
                if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
                    Log.e("WiFi Scan", "Success");
                } else {
                    Log.e("WiFi Scan", "Failure");
                }

                // Convert to an ArrayList
                final ArrayList<ScanResult> filtered = new ArrayList<ScanResult>();
                for (ScanResult result : results) {
                    filtered.add(result);
                }

                // Perform update to the UI and hashmap
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        // Only put in data that is not currently in the hashmap
                        for (ScanResult r : filtered) {
                            if (FilterFragment.this.wifi_filtered_data_items_hashmap.containsKey(
                                    r.BSSID) ||
                                    FilterFragment.this.wifi_nonfiltered_data_items_hashmap.containsKey(r.BSSID)) {
                                continue;
                            } else {

                                FilterFragment.this.wifi_nonfiltered_data_items_hashmap.put(r.BSSID,
                                        new FilterDataItem(r.BSSID, r.SSID, false));
                            }
                        }

                        // Refresh data sources
                        FilterFragment.this.refreshDataSources();

                        // Dismiss the loading dialog
                        FilterFragment.this.loadingDialog.dismissDialog();

                    }
                });

            }
        };

        // Register receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getContext().registerReceiver(receiver, intentFilter);

        // Start WiFi scan
        wifiManager.startScan();
    }
}
