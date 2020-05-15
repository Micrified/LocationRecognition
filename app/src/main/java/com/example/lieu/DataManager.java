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
import java.util.List;

import static java.security.AccessController.getContext;

// This is a singleton class
public class DataManager implements Serializable {

    // The singleton class instance
    transient private static DataManager singleton = null;

    // The WiFi manager
    transient private WifiManager wifiManager;

    // The number of cells that the application can identify
    private int cellCount = 16;

    // The cells
    private List<Cell> cells;


    // The adjustment component for particle filter
    private double particle_adjustment_component = -0.05f;

    public int getCellCount () {
        return this.cellCount;
    }

    public List<Cell> getCells () {
        return this.cells;
    }



    // Private constructor for singleton
    private DataManager () {

        // Initialize the cell array
        this.cells = new ArrayList<Cell>(); //Collections.synchronizedList(new ArrayList<Cell>());

        // Compute the prior
        double prior = 1.0 / (double)this.cellCount;

        // Populate the cell array
        for (int i = 0; i < this.cellCount; ++i) {
            Cell newCell = new Cell(i);
            newCell.setPrior(prior);
            this.cells.add(newCell);
        }
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

    // Reads the data for each cell
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

    // Writes the data for each cell
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

        for (Cell c : this.getCells()) {

            if (c.containsBSSID(bssid)) {
                double acc = c.getGuassianForAccessPointRSS(bssid, rss) * c.getPrior();
                norm += acc;
            }
        }

        return norm;
    }


    public void testTraining () {

        // For all cells
        for (Cell c : this.getCells()) {

            // Reset all priors
            for (Cell w : this.getCells()) {
                w.setPrior(1.0/16.0);
            }

            // Run four tests
            for (int k = 0; k < 12; ++k) {

                // Make a new test-set
                ArrayList<TrainingSample> testSet = new ArrayList<>();

                // For each access-point, add all samples into the test-set
                for (AccessPointResult ap : c.getAccessPointResults()) {
                    int fetch_index = k % ap.getSamples().size();
                    double sample = ap.getSamples().get(fetch_index);
                    testSet.add(new TrainingSample(ap.getBssid(), ap.getSamples().get(0).intValue()));
                }

                if (k == 11) {
                    processTestSet(testSet, true);
                } else {
                    processTestSet(testSet, false);
                }

            }

        }
    }

    // Processes a training sample
    public void processTestSet (ArrayList <TrainingSample> trainingSamples, boolean shouldPrint) {
        ArrayList<Cell> candidates = new ArrayList<Cell>();

        // Sort results
        Collections.sort(trainingSamples, new Comparator() {
            public int compare(Object a, Object b) {
                if (((TrainingSample) a).getRss() > ((TrainingSample) b).getRss()) {
                    return -1;
                }
                return 1;
            }
        });

        // Determine how many results to use
        int n = trainingSamples.size();
        if (n > 10) {
           n = 10;
        }

        // For all cells ...
        for (Cell c : this.getCells()) {
            boolean atLeastOne = false;
            double cell_posterior = 0.0;
            int count = 0;

            // For each access-point in the results
            for (int i = 0; i < n; ++i) {
                TrainingSample t = trainingSamples.get(i);

                // Skip if doesn't contain access-point
                if (c.containsBSSID(t.getBssid()) == false) {
                    continue;
                }

                // Otherwise, there exists at least one
                atLeastOne = true;
                count++;

                // Update posterior
                double acc = c.posterior(t.getBssid(), t.getRss());
                cell_posterior += acc;
            }

            // Only save + update posterior if it had at least one access-point
            if (atLeastOne) {
                c.setPrior(cell_posterior); // not / count
                candidates.add(c);
            }
        }

        // Normalize the cell priors
        double norm = 0.0;
        for (Cell c : this.getCells()) {
            if (c.getPrior() < 1E-6) {
                c.setPrior(1E-6);
            }
            norm += c.getPrior();
        }

        double sum = 0.0;

        String output = "\n";
        for (Cell c : this.getCells()) {
            c.setPrior(c.getPrior() / norm);
            sum += c.getPrior();
            output += String.format("Cell %s | Prior = %.16f\n", c.getID() + 1, c.getPrior());
        }

        if (shouldPrint) {
            System.out.print(output);
            System.out.printf("Sum of all priors = %f\n", sum);
        }

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

    // Update all priors
    public void processWiFiSample (ArrayList<ScanResult> results, ScanUpdateInterface delegate) {
        ArrayList<Cell> candidates = new ArrayList<Cell>();

        // Sort results
        results = quickSort(results);

        // Determine how many results to use
        int n = results.size();
        if (n > 5) {
            n = 5;
        }


        // For all cells
        for (Cell c : this.getCells()) {
            boolean atLeastOne = false;
            double cell_posterior = 0.0;
            int count = 0;

            // For each access point in the results
            for (int i = 0; i < n; ++i) {
                ScanResult result = results.get(i);

                // If cell doesn't have access - point, skip it
                if (c.containsBSSID(result.BSSID) == false) {
                    continue;
                }

                // Otherwise, there is at least one
                atLeastOne = true;
                count++;

                // Update the posterior
                double acc = c.posterior(result.BSSID, result.level);
                cell_posterior += acc;
            }

            // Only save + update posterior if it had at least one access-point
            if (atLeastOne) {
                c.setPrior(cell_posterior / (double)count);
                candidates.add(c);
            }
        }

        // Normalize the cell priors
        double norm = 0.0;
        for (Cell c : this.getCells()) {
            norm += c.getPrior();
        }
        double sum = 0.0;

        // Find the cell with the highest prior
        int highest_cell_id = 0;

        for (Cell c : this.getCells()) {
            c.setPrior(c.getPrior() / norm);
            if (c.getPrior() > this.getCells().get(highest_cell_id).getPrior()) {
                highest_cell_id = c.getID();
            }
            sum += c.getPrior();
            //System.out.printf("Cell %s | Prior = %.16f\n", c.getID() + 1, c.getPrior());
            String s = String.format("Cell %d | Prior = %.16f\n", c.getID() + 1, c.getPrior());
            delegate.updateIOStatusWithString(s);
        }

        // Update the current best cell
        delegate.setBestCandidateCell(highest_cell_id);

        // Add to output some scan result information
        String s = String.format("Sum of all priors = %.16f\n", sum);
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
}
