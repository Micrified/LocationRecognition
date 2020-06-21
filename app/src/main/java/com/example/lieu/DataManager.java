package com.example.lieu;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static java.security.AccessController.getContext;

// This is a singleton class
public class DataManager implements Serializable {

    // The singleton class instance
    transient private static DataManager singleton = null;

    // The WiFi manager
    transient private WifiManager wifiManager;

    // The list of access-points to filter manually
    transient private HashMap<String, FilterDataItem> filter;

    // The number of cells that the application can identify
    private int cellCount = 8;

    // The cells
    private List<Cell> cells;

    // Ambient light value tracker
    private AmbientLight ambientLight;

    // Barometer data structure
    private Barometer barometer;

    // The adjustment component for particle filter
    private double particle_adjustment_component = -0.05f;

    // The user height
    private int user_height = 170;

    public int getCellCount () {
        return this.cellCount;
    }

    public List<Cell> getCells () {
        return this.cells;
    }



    // Private constructor for singleton
    private DataManager () {

        // Initialize the cell array
        this.cells = new ArrayList<Cell>();

        // Initialize the filtered access point array
        this.filter = new HashMap<String, FilterDataItem>();

        // Compute the prior
        double prior = 1.0 / (double)this.cellCount;

        // Populate the cell array
        for (int i = 0; i < this.cellCount; ++i) {
            Cell newCell = new Cell(i);
            newCell.setPrior(prior);
            this.cells.add(newCell);
        }
    }


    // Sets the filter
    public void setAccessPointFilter (HashMap<String, FilterDataItem> filter)
    {
        this.filter = filter;
    }

    // Getter for the filter
    public HashMap<String, FilterDataItem> getFilter ()
    {
        return this.filter;
    }

    // Returns true if the filter contains the given BSSID
    private boolean filterContains (String bssid)
    {
        // Null pointer check
        if (filter == null) {
            Log.e("FILTER", "No filter has been set!");
            return false;
        }

        // Check if the item is in the hashmap
        return this.filter.containsKey(bssid);
    }

    // Returns an array of scanresults that have been filtered by the filter
    public ArrayList<ScanResult> getFilteredScanResults (List<ScanResult> scanResults)
    {
        ArrayList<ScanResult> scan_results_array = new ArrayList();
        scan_results_array.addAll(scanResults);

        // Layer 1: Automatic detection and removal
        ArrayList<ScanResult> filtered_1 = APFilter.FilterScanResults(scan_results_array);

        // Layer 2: Manual removal
        ArrayList<ScanResult> filtered_2 = new ArrayList<ScanResult>();
        for (ScanResult r : filtered_1) {
            if (filterContains(r.BSSID) == false) {
                filtered_2.add(r);
            }
        }

        return filtered_2;
    }

    // Resets all cells
    public void resetCells () {

        double prior = 1.0 / (double)this.cellCount;

        for (Cell c : this.getCells()) {
            c.setPrior(prior);
        }

    }

    // Singleton initializer/retreiver
    public static DataManager getInstance() {
        if (singleton == null) {
            singleton = new DataManager();
        }
        return singleton;
    }

    // Shows all cells
    public void showCells () {
        for (int i = 0; i < cellCount; ++i) {
            System.out.println("Cell " + i + ":");
            cells.get(i).showSamples();
        }
    }

    // Getter for WiFiManager
    public WifiManager getWiFiManager () {
        return this.wifiManager;
    }

    // Setter for WiFiManager
    public void setWifiManager (WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }

    // Setter for cells
    public void setCells (List<Cell> cells) {
        this.cells = cells;
        this.cellCount = cells.size();
    }

    // Reads serialized data in from a file and reconstructs the cell objects
    public void scan (Context context, IOUpdateInterface delegate) {
        File file = null;
        InputStream inputStream = null;

        // Read in each cell
        for (int i = 0; i < cellCount; ++i) {
            String filename = "cell_" + (i+1) + ".txt";
            file = new File(context.getExternalFilesDir(null), filename);
            delegate.updateIOStatusWithString(String.format("• Reading file: %s\n", filename));
            try {
                inputStream = new FileInputStream(file);
                this.cells.get(i).scan(inputStream, delegate);
                inputStream.close();

            } catch (FileNotFoundException e) {
                String s = String.format("\tErr: File %s was not found!\n", filename);
                delegate.updateIOStatusWithString(s);
                e.printStackTrace();
            } catch (IOException e) {
                String s = String.format("\tErr: Something went wrong:\n%s", e.getMessage());
                delegate.updateIOStatusWithString(s);
                e.printStackTrace();
            }
        }
    }

    // Serializes cell data and writes it to the output stream
    public void export (Context context, IOUpdateInterface delegate) {
        File file = null;
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStreamWriter = null;

        // Ensure that we can export
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) == false) {
            Log.e("OS", "Environment can't use external storage!");
            return;
        }

        // Have each cell write its own file with (ap-name mean var)
        for (int i = 0; i < cellCount; ++i) {
            String filename = "cell_" + (i+1) + ".txt";

            file = new File(context.getExternalFilesDir(null), filename);

            try {
                fileOutputStream = new FileOutputStream(file, false);
                outputStreamWriter = new OutputStreamWriter(fileOutputStream);

                String s = String.format("Exporting Cell %d: ", i+1);
                delegate.updateIOStatusWithString(s);
                this.cells.get(i).export(outputStreamWriter, delegate);

                outputStreamWriter.close();
                fileOutputStream.close();

                System.out.println("Just wrote cell " + (i+1) + " to file: " + filename);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Returns a normalization value for a given Access-Point with its RSS value
    public double getNormalizationForAccessPointWithRSS (String bssid, double rss) {
        double norm = 0.0;

        // The normalization is the sum of all probabilities of finding this value in
        // each cell
        for (Cell c : this.getCells()) {

            if (c.containsBSSID(bssid)) {
                double acc = c.getGuassianForAccessPointRSS(bssid, rss) * c.getPrior();
                norm += acc;
            }
        }

        return norm;
    }

    // Sorts WiFi samples
    private ArrayList<ScanResult> quickSort (ArrayList<ScanResult> list)
    {
        if (list.size() <= 1)
            return list; // Already sorted

        ArrayList<ScanResult> sorted = new ArrayList<ScanResult>();
        ArrayList<ScanResult> lesser = new ArrayList<ScanResult>();
        ArrayList<ScanResult> greater = new ArrayList<ScanResult>();
        ScanResult pivot = list.get(list.size() - 1); // Use last Vehicle as pivot
        for (int i = 0; i < list.size()-1; i++)
        {
            //int order = list.get(i).compareTo(pivot);
            if ((list.get(i).level - pivot.level) < 0)
                lesser.add(list.get(i));
            else
                greater.add(list.get(i));
        }

        lesser = quickSort(lesser);
        greater = quickSort(greater);

        lesser.add(pivot);
        lesser.addAll(greater);
        sorted = lesser;

        return sorted;
    }

    public void processWiFiSample (ArrayList<ScanResult> results, ScanUpdateInterface delegate)
    {
        List<Cell> cells = this.getCells();

        // For each result in the scan - update all the cell priors
        for (ScanResult r : results) {
            double prob_ap = 0.0; // Norm

            for (Cell c : cells) {
                double prob_cell_given_ap = 0.0;
                double prob_cell_prior    = c.getPrior();
                double prob_ap_given_cell = 0.0;

                // Derive probability of detecting AP in given cell
                if (c.containsBSSID(r.BSSID)) {
                    prob_ap_given_cell = c.getGuassianForAccessPointRSS(r.BSSID, r.level);
                } else {
                    prob_ap_given_cell = 1E-10;
                }

                // Compute the (non-normalized) posterior
                prob_cell_given_ap = prob_cell_prior * prob_ap_given_cell;

                // Update the cell with this new prior
                c.setPrior(prob_cell_given_ap);

                // Contribute to the normalization
                prob_ap += prob_cell_given_ap;
            }

            // Now renormalize
            for (Cell c : cells) {
                c.setPrior(c.getPrior() / prob_ap);
            }
        }

        // Find the cell with the highest probability
        int best_guess_cell_id = 0;
        double temporary_prior_sum_check = 0.0;
        String description = "{";
        for (Cell c : cells) {
            description += String.format("%.5f ", c.getPrior());
            temporary_prior_sum_check += c.getPrior();
            if (c.getPrior() > cells.get(best_guess_cell_id).getPrior()) {
                best_guess_cell_id = c.getID();
            }
        }
        description += "}";

        // Show most likely location
        delegate.setBestCandidateCell(best_guess_cell_id);

        // Add to output some scan result information
        String s = String.format("Sum %s = %.16f\n", description, temporary_prior_sum_check);
        delegate.updateIOStatusWithString(s);

        // Update the number of rounds completed so far
        delegate.updateScanCountAndReset();

    }

    // Returns the particle adjustment component
    public double getParticleAdjustmentComponent () {
        return this.particle_adjustment_component;
    }

    // Sets the particle adjustment component
    public void setParticleAdjustmentComponent (double component) {
        this.particle_adjustment_component = component;
    }

    // Gets the ambient light data structure. If not initialized - the value is initialized
    public AmbientLight getAmbientLight ()
    {
        if (this.ambientLight == null) {
            this.ambientLight = new AmbientLight();
        }

        return this.ambientLight;
    }

    // Gets the barometer data structure. If not initialized - the value is initialized
    public Barometer getBarometer ()
    {
        if (this.barometer == null) {
            this.barometer = new Barometer();
        }

        return this.barometer;
    }

    public int getUser_height() {
        return user_height;
    }

    public void setUser_height(int user_height) {
        this.user_height = user_height;
    }
}
