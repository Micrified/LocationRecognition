package com.example.lieu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TrainFragment extends Fragment implements View.OnClickListener, Swap {

    // The selected cell text-view
    private TextView selectionTextView;

    // The Next button
    private Button nextButton;

    // The Back button
    private Button backButton;

    // The currently selected cell
    private int cell_index = 0;

    // The time text view
    private TextView timeTextView;

    // The progress text view
    private TextView progressTextView;

    // The start training button
    private Button startButton;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_train, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        // Bind selection button
        this.selectionTextView = getView().findViewById(R.id.selection_text_view);

        // Bind next button
        this.nextButton = getView().findViewById(R.id.next_button);

        // Bind back button
        this.backButton = getView().findViewById(R.id.back_button);

        // Bind the text-view showing the time countdown
        this.timeTextView = getView().findViewById(R.id.time_text_view);

        // Bind the text-view showing the number of rounds completed
        this.progressTextView = getView().findViewById(R.id.progress_text_view);

        // Bind the start button
        this.startButton = getView().findViewById(R.id.start_button);

        // Set this class as a listener for the buttons
        this.nextButton.setOnClickListener(this);
        this.backButton.setOnClickListener(this);
        this.startButton.setOnClickListener(this);

        // Setup the initial cell.
        scrollCell(0);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.back_button:
                scrollCell(-1);
                break;
            case R.id.next_button:
                scrollCell(1);
                break;
            case R.id.start_button:
                this.startTraining();
                break;
        }
    }

    // Handles changes to the directional scrolling arrows
    private void scrollCell (int by) {
        int index = cell_index + by;

        if (index <= 0) {
            index = 0;
            backButton.setEnabled(false);
        } else {
            backButton.setEnabled(true);
        }

        if (index >= DataManager.getInstance().getCellCount()) {
            index = DataManager.getInstance().getCellCount() - 1;
            nextButton.setEnabled(false);
        } else {
            nextButton.setEnabled(true);
        }

        // Update index
        this.cell_index = index;

        // Update displayed text
        String selectionText = String.format("Cell %d", index + 1);
        this.selectionTextView.setText(selectionText);

        Cell cell = DataManager.getInstance().getCells().get(cell_index);
        if (cell.getSamples() != null && cell.getSamples().size() > 0) {
            this.progressTextView.setText("Done (" + cell.getSamples().size() + " scans)");
        } else {
            this.progressTextView.setText("Progress (0/30)");
        }

    }

    // Locks or unlocks the UI
    private void lockScrolling (boolean lock) {

        // Lock the start button (straightforward)
        this.startButton.setEnabled(!lock);

        // Locking is easy - lock them all
        if (lock == true) {
            this.nextButton.setEnabled(false);
            this.backButton.setEnabled(false);
            return;
        }

        // When unlocking, check index first
        if (cell_index > 0) {
            backButton.setEnabled(true);
        }

        if (cell_index < DataManager.getInstance().getCellCount()) {
            nextButton.setEnabled(true);
        }
    }

    // Dispatches a thread which performs a countdown (UI locked in the meantime)
    private void startTraining () {

        // Lock scrolling
        this.lockScrolling(true);


        new Thread(new Runnable() {
            public void run() {

                // The background thread runs 12 times a scan
                for (int i = 0; i < 30; ++i) {

                    // Initiate the scan
                    System.out.println("Scanning ...\n");
                    scanWiFiForCell(cell_index);

                    // Wait two seconds
                    for (int j = 0; j < 2; ++j) {

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        final int f_j = j;
                        timeTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                String text = String.format("%02ds", 2 - f_j);
                                timeTextView.setText(text);
                            }
                        });
                    }

                    final int f_i = i;
                    progressTextView.post(new Runnable() {
                        @Override
                        public void run() {
                            String text = String.format("Progress: %d/30", f_i+1);
                            progressTextView.setText(text);
                        }
                    });
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        timeTextView.setText("00s");
                        progressTextView.setText("Done (30 scans)");
                        lockScrolling(false);
                    }
                });
            }
        }).start();
    }


    // Public (main thread?) method meant for adding data
    public static void addDataToCellIndex (int cell_index, ArrayList<ScanResult> data) {
        System.out.println("Adding " + data.size() + " results to Cell " + (cell_index + 1));
        DataManager.getInstance().getCells().get(cell_index).addScanResults(data);
        System.out.println("Samples in this cell are: ");
        DataManager.getInstance().getCells().get(cell_index).showSamples();
    }


    // Private method for performing a WiFi scan. Should be called by the runnable background thread
    private void scanWiFiForCell (final int cell_index) {

        // Get the WiFi manager
        final WifiManager wifiManager = DataManager.getInstance().getWiFiManager();

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                context.unregisterReceiver(this);
                List<ScanResult> results = wifiManager.getScanResults();
                if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
                    System.out.println("Scan: Success!");
                } else {
                    System.out.println("Scan: Failed!");
                    return;
                }

                // Get a reference to a cell
                final Cell cell = DataManager.getInstance().getCells().get(cell_index);
  
                // Apply manual filter
                final ArrayList<ScanResult> filtered =
                        DataManager.getInstance().getFilteredScanResults(results);

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        TrainFragment.addDataToCellIndex(cell_index, filtered);
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

    public void swappingOut () {

    }
}
