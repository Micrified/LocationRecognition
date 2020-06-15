package com.example.lieu;

import android.net.wifi.ScanResult;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class Cell implements Serializable {

    // Cell identifier
    private int id;

    // Prior probability
    transient private double prior;

    // Cached access-point-results
    private ArrayList<AccessPointResult> accessPointResults;

    // Array of WiFi samples (over time)
    transient private ArrayList<ArrayList<ScanResult>> samples;

    // Transient marker set when you add more samples to an already processed cell
    transient private boolean didAddMoreSamples = false;

    // Getter: Returns the list of raw internal samples
    public ArrayList<ArrayList<ScanResult>> getSamples() {
        return this.samples;
    }

    // Constructor
    public Cell (int id) {
        this.id = id;
        this.samples = new ArrayList<ArrayList<ScanResult>>();
    }

    // Resets the cell after being deserialized
    public void reset (double prior) {
        this.prior = prior;
        this.samples = null;
        this.didAddMoreSamples = false;
        for (AccessPointResult r : this.accessPointResults) {
            r.reset();
        }
    }

    // Return cell number
    public int getID () {
        return this.id;
    }

    // Return prior
    public double getPrior () {
        return this.prior;
    }

    // Allow the prior to be set
    public void setPrior (double prior) {
        this.prior = prior;
    }

    // TODO: Method to get all unique access-points (BSSIDs) in a given sample set
    private ArrayList<String> getUniqueAccessPoints () {
        ArrayList<String> unique_bssids = new ArrayList<String>();
        HashMap<String, ScanResult> set = new HashMap<String, ScanResult>();


        for (ArrayList<ScanResult> scan : this.samples) {
            for (ScanResult result : scan) {

                // Drop the last three bytes of the BSSID in order to weed out duplicates
                String hash_bssid = result.BSSID.substring(0, result.BSSID.length() - 3);

                // Only put the first occurrence
                if (set.containsKey(hash_bssid) == false) {
                    set.put(hash_bssid, result);
                }
            }
        }

        // Collect all unique BSSIDs by getting all values and then the full BSSIDs
        for (ScanResult r : set.values()) {
            unique_bssids.add(r.BSSID);
        }

        return unique_bssids;
    }

    // TODO: Method to sort samples by access-point, and return a list of access-point samples
    public ArrayList<AccessPointResult> getAccessPointResults () {
        ArrayList<AccessPointResult> access_point_results = new ArrayList<AccessPointResult>();

        // If there is no sample data, and no loaded data - return an empty arraylist
        if (this.samples == null && this.accessPointResults == null) {
            return access_point_results;
        }


        // If the access-point results are not NULL and no more samples have been added, return
        if (this.accessPointResults != null && this.didAddMoreSamples == false) {
            return this.accessPointResults;
        }

        // Otherwise either access-point results are null, or more samples were added.

        // If more samples were added, we invalidate the old access-point results (replacing them)
        if (didAddMoreSamples == true) {
            System.out.println("Replacing data ...");
            didAddMoreSamples = false;
            this.accessPointResults = null;
        }

        // Get a list of all unique access points
        ArrayList<String> unique_bssids = this.getUniqueAccessPoints();

        // For each BSSID ...
        for (String bssid : unique_bssids) {
            ArrayList<Double> samples = new ArrayList<Double>();
            String ssid = "Unknown";

            // For each scan
            for (ArrayList<ScanResult> scan : this.samples) {

                // Assume nothing found
                boolean didFindResult = false;
                double rss = Double.NaN;

                // Look for presence of bssid in the scan
                for (ScanResult result : scan) {
                    if (result.BSSID.equals(bssid)) {
                        ssid = result.SSID;
                        rss = (double)result.level;
                        didFindResult = true;
                        break;
                    }
                }

                // Add result to list - but only if something was detected
                if (didFindResult) {
                    samples.add(rss);
                }
            }

            // Add an entry for that BSSID in the access-point array
            access_point_results.add(new AccessPointResult(ssid, bssid, samples));
        }

        // Set the access-point results
        this.accessPointResults = access_point_results;

        // Return the filtered/computed data for the cell.
        return access_point_results;
    }

    private AccessPointResult containsAccessPointResult (String bssid) {
        AccessPointResult result = null;
        for (AccessPointResult r : this.accessPointResults) {
            if (r.getBssid().equals(bssid)) {
                return r;
            }
        }
        return result;
    }

    public void setSamples(ArrayList<ArrayList<ScanResult>> samples) {

        // Don't allow null to be set
        if (samples == null) {
            return;
        }

        // Set the samples
        this.samples = samples;

        // Clear the cached access-point results
        this.accessPointResults = null;
    }

    // Prints how many samples it has
    public void showSamples () {
        if (samples.size() == 0) {
            System.out.println("<None>");
            return;
        }
        for (int i = 0; i < samples.size(); ++i) {
            System.out.println("Sample " + i + " has " + samples.size() + " points!");
        }
    }

    // Adds a list of results to the cell sample data
    public void addScanResults(ArrayList<ScanResult> scanResults) {

        // If null, reset
        if (this.samples == null) {
            this.samples = new ArrayList<ArrayList<ScanResult>>();
        }

        // Add the sample
        samples.add(scanResults);

        // Mark that more has been added
        this.didAddMoreSamples = true;
    }

    // Writes the cell data to a file
    public void export (OutputStreamWriter writer, IOUpdateInterface delegate) {

        // If we have nothing to write, return
        if (this.samples == null && this.accessPointResults == null) {
            String s = String.format("Skipped\n");
            delegate.updateIOStatusWithString(s);
            return;
        }

        // If we have only samples but no access point results, then compute them
        if (this.samples != null && this.accessPointResults == null) {
            this.accessPointResults = getAccessPointResults(); // redundant assignment it auto caches but don't want it optimized out
        }

        // Write the access point results
        if (this.accessPointResults != null) {
            for (AccessPointResult result : this.accessPointResults) {
                result.export(writer);
            }
            String s = String.format("Done\n");
            delegate.updateIOStatusWithString(s);
        }
    }

    // Reads the cell data from a file
    public void scan (InputStream inputStream, IOUpdateInterface delegate) {
        Scanner scanner = new Scanner(inputStream);
        Scanner lineScanner = null;
        String bssid = null;
        String ssid = null;
        ArrayList<Double> samples;
        int samples_per_cell = 0;

        // The Access-Point array must first be reset
        this.accessPointResults = new ArrayList<AccessPointResult>();

        // Scan all lines of the input
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            lineScanner = new Scanner(line);
            samples = new ArrayList<Double>();

            // Scan in the BSSID
            if (lineScanner.hasNext()) {
                bssid = lineScanner.next();
            }

            // Scan in the SSID
            if (lineScanner.hasNext()) {
                ssid = lineScanner.next();
            }

            // Scan in all samples
            while (lineScanner.hasNextDouble()) {
                samples_per_cell++;
                samples.add(lineScanner.nextDouble());
            }

            // Ignore entries with zero samples since they cause problems
            if (samples.size() > 0) {
                AccessPointResult ap = new AccessPointResult(ssid, bssid, samples);
                this.accessPointResults.add(ap);
            }


            // Close the line scanner
            lineScanner.close();
        }

        String s = String.format("\tSamples: %d\n", samples_per_cell);
        delegate.updateIOStatusWithString(s);
        scanner.close();
    }

    // Returns the Access-Point with the given BSSID - if any
    private AccessPointResult getAccessPointWithBSSID (String bssid) {
        for (AccessPointResult ap : this.getAccessPointResults()) {
            if (ap.getBssid().equals(bssid)) {
                return ap;
            }
        }
        return null;
    }

    // Sets the list of access point results
    public void setAccessPointResults (ArrayList<AccessPointResult> results) {
        this.accessPointResults = results;
    }


    public boolean containsBSSID (String bssid) {
        for (AccessPointResult ap : this.getAccessPointResults()) {
            if (ap.getBssid().equals(bssid)) {
                return  true;
            }
        }

        return  false;
    }

    // Guassian function
    private double guassian (double x, double mean, double variance) {

        // Always have a nonzero minimum variance
        if (variance == 0.0) {
            variance = 1E-10;
        }

        double denominator = Math.sqrt(2.0 * Math.PI * variance);
        System.out.println("denominator = " + denominator);
        double power = -(Math.pow((x - mean), 2.0) / (2.0 * variance));
        System.out.println("power = " + power);
        double numerator = Math.exp(power);
        System.out.println("numerator = " + numerator);

        double result = (numerator / denominator);

        if (Double.isNaN(result) || result < 1E-10) {
            return 1E-10;
        } else {
            return result;
        }
    }

    // External guassian
    public double getGuassianForAccessPointRSS (String bssid, double x) {
        AccessPointResult ap = this.getAccessPointWithBSSID(bssid);
        if (ap == null) {
            return Double.NaN;
        }
        return this.guassian(x, ap.getSampleMean(), ap.getSampleVariance());
    }

    // Returns the posterior probability for a given Access Point and RSS value
    public double posterior (String bssid, double rss) {
        double prior = this.prior;
        double norm = DataManager.getInstance().getNormalizationForAccessPointWithRSS(bssid, rss);

        // Find the access-point for the given bssid
        AccessPointResult ap = this.getAccessPointWithBSSID(bssid);


        if (ap == null) {
            return Double.NaN;
        }

        // Compute the likelihood
        double value = guassian(rss, ap.getSampleMean(), ap.getSampleVariance());

        return (prior * value) / norm;
    }
}
