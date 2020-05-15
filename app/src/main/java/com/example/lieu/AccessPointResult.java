package com.example.lieu;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;

public class AccessPointResult implements Serializable {

    // The non-unique human readable name given to the access point
    private String ssid;

    // The unique identifier of the access point
    private String bssid;

    // Array of RSS values captured when sampling this access-point at a specific location
    private ArrayList<Double> samples;

    // The mean (cached if already computed)
    transient private double mean = Double.NaN;

    // The variance (cached if already computed)
    transient private double variance = Double.NaN;

    // Boolean indicating if any recomputation is needed
    boolean needsRecompute;


    // Constructor
    public AccessPointResult(String ssid, String bssid, ArrayList<Double> samples) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.samples = samples;
        needsRecompute = true;
    }

    // Resets the AccessPointResult after being deserialized
    public void reset() {
        this.mean = Double.NaN;
        this.variance = Double.NaN;
        this.needsRecompute = true;
    }

    // Gets the sample mean
    public double getSampleMean () {
        if (this.needsRecompute) {
            computeSampleVariance();
            needsRecompute = false;
        }
        return this.mean;
    }

    // Gets the sample variance
    public double getSampleVariance () {
        if (this.needsRecompute) {
            computeSampleVariance();
            needsRecompute = false;
        }
        return this.variance;
    }


    // Computes and returns the mean RSS value across the stored sample of RSS values
    public double computeSampleMean () {

        double acc, n = (double)this.samples.size();

        if (this.samples.size() == 0) {
            System.out.println("There are no samples to compute the mean for!\n");
            return Double.NaN;
        } else {
            acc = this.samples.get(0);
        }

        for (int i = 1; i < this.samples.size(); ++i) {
            acc += this.samples.get(i);
        }

        this.mean = (acc / n);

        return this.mean;
    }


    // Computes and returns the variance of RSS values stored in the sample
    public double computeSampleVariance () {
        double acc, mean = this.computeSampleMean(), n = (double)this.samples.size();

        if (this.samples.size() == 1) {
            return 0.0;
        }

        if (this.samples.size() == 0) {
            System.out.println("There are no samples to compute the variance for!\n");
            return Double.NaN;
        } else {
            acc = (this.samples.get(0) - mean) * (this.samples.get(0) - mean);
        }

        for (int i = 1; i < this.samples.size(); ++i) {
            acc += (this.samples.get(i) - mean) * (this.samples.get(i) - mean);
        }

        this.variance = (acc / (n - 1.0));

        return this.variance;
    }


    // Getter
    public String getSsid () {
        return this.ssid;
    }

    // Getter
    public String getBssid () {
        return this.bssid;
    }

    // Getter
    public ArrayList<Double> getSamples () {
        return this.samples;
    }

    // Writes the AccessPointResult to an output stream writer
    public void export (OutputStreamWriter writer) {
        String format = this.getBssid() + " " + this.getSsid();
        for (Double d : this.samples) {
            String segment = String.format(" %f", d);
            format = format + segment;
        }
        format = format + "\n";
        System.out.println(format);
        try {
            writer.write(format);
        } catch (IOException e) {
            Log.e("IO", "Couldn't write access-point: " + this.bssid + " results because of error: " + e.toString());
        }
    }

}
