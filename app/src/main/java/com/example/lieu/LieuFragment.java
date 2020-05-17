package com.example.lieu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;


public class LieuFragment extends Fragment implements View.OnClickListener, ScanUpdateInterface, Swap {

    // Button for triggering location detection
    Button locateButton;

    // TextView for displaying the chosen cell after convergence
    TextView chosenOneTextView;

    // TextView for displaying bayesian location output
    TextView feedbackTextView;

    // TextView for displaying the number of rounds thus completed in locating
    TextView roundCountTextView;

    // The global data lock
    private ReentrantLock lock;

    // The number of rounds to scan for before convergence
    int g_max_scan_rounds = 5;

    // The number of rounds completed in the ongoing scan
    int g_current_scan_rounds = 0;


    // Boolean if currently conducting a round
    boolean is_conducting_localization_round = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lieu, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        // Initialize the lock
        this.lock = new ReentrantLock();

        // Setup the button for locating user
        this.locateButton = getView().findViewById(R.id.locate_button);
        this.locateButton.setOnClickListener(this);

        // Bind the TextViews
        this.chosenOneTextView = getView().findViewById(R.id.location_text_view);
        this.feedbackTextView = getView().findViewById(R.id.feedback_text_view);
        this.roundCountTextView = getView().findViewById(R.id.scancount_text_view);

        // Make feedback scrollable
        feedbackTextView.setMovementMethod(new ScrollingMovementMethod());

        // Toggle the reset
        reset();

        // Initialize the scanner to watch for whether to conduct more WiFi scans
        this.initScannerTimer(1000);
    }

    // Initializes the automatic scanner
    public void initScannerTimer (long ms) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask(){
            public void run() {

                // Scan boolean
                boolean performScan;

                // Number of rounds copy
                int n_rounds_done;

                // Copy the current scan value
                LieuFragment.this.lock.lock();
                performScan = LieuFragment.this.is_conducting_localization_round;
                n_rounds_done = LieuFragment.this.g_current_scan_rounds;
                LieuFragment.this.lock.unlock();

                // If not doing a scan, then return.
                if (performScan == false) {
                    return;
                }

                //  Check if we have enough scans already
                if (g_current_scan_rounds >= g_max_scan_rounds) {
                    LieuFragment.this.lock.lock();
                    LieuFragment.this.is_conducting_localization_round = false;
                    getActivity().runOnUiThread(new Runnable(){
                        public void run() {
                            LieuFragment.this.locateButton.setEnabled(true);
                        }
                    });
                    LieuFragment.this.lock.unlock();
                }

                // Otherwise we need to do a scan
                LieuFragment.this.scanWiFi();

            }
        }, 0, ms);
    }



    // Clears and resets the interface
    private void reset () {

        // Reset the scan rounds
        g_current_scan_rounds = 0;

        // Setup the round-count text view text
        String roundCountText = String.format("Scan Count: %2d/%2d", g_current_scan_rounds, g_max_scan_rounds);
        this.roundCountTextView.setText(roundCountText);

        // Setup the chosen cell text
        String chosenOneText = "Ready";
        this.chosenOneTextView.setText(chosenOneText);

        // Clear the feedback text
        feedbackTextView.setText("");
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.locate_button:

                // Reset the interface and rounds
                reset();

                // Reset cell priors
                DataManager.getInstance().resetCells();

                // Disable the scan button
                this.locateButton.setEnabled(false);

                // Lock, set the scan boolean to true, and then unlock
                this.lock.lock();
                this.is_conducting_localization_round = true;
                this.lock.unlock();

                break;
        }
    }


    public void updateIOStatusWithString (final String s) {
        getActivity().runOnUiThread(new Runnable(){
            public void run() {
                String current = LieuFragment.this.feedbackTextView.getText().toString();
                String updated = current + s;
                LieuFragment.this.feedbackTextView.setText(updated);
                LieuFragment.this.feedbackTextView.invalidate();
            }
        });
    }

    // Allows the best cell to be displayed
    public void setBestCandidateCell (final int cell_id) {
        getActivity().runOnUiThread(new Runnable(){
            public void run() {
                String s = String.format("Cell %d", cell_id + 1);
                LieuFragment.this.chosenOneTextView.setText(s);
                LieuFragment.this.chosenOneTextView.invalidate();
            }
        });
    }

    // Allow the number of scans to be updated
    public void updateScanCountAndReset () {
        getActivity().runOnUiThread(new Runnable(){
            public void run() {
                String s = String.format("Scan Count: %2d/%2d", g_current_scan_rounds, g_max_scan_rounds);

                LieuFragment.this.lock.lock();
                g_current_scan_rounds += 1;
                LieuFragment.this.lock.unlock();

                LieuFragment.this.roundCountTextView.setText(s);
                LieuFragment.this.roundCountTextView.invalidate();
            }
        });
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
                    updateIOStatusWithString("Scan: Success\n");
                } else {
                    updateIOStatusWithString("Scan: Failure\n");
                    return;
                }

                //filter method
                ArrayList unfiltered = new ArrayList<ScanResult>();
                unfiltered.addAll(results);
                final ArrayList<ScanResult> filtered = APFilter.FilterScanResults(unfiltered);

                //ArrayList<ScanResult> sample = new ArrayList<ScanResult>(results.size());
                //sample.addAll(results);
                DataManager.getInstance().processWiFiSample(filtered, LieuFragment.this);
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
