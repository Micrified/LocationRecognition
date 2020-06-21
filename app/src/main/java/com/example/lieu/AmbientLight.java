package com.example.lieu;

import android.util.Log;

import java.util.ArrayList;

public class AmbientLight {

    public static final int required_sample_count = 10;

    // Enumeration: Possible environments
    enum Environment {
        NONE,
        INSIDE,
        STAIRS,
        OUTSIDE
    }

    // Class: Range
    public class Range {
        public float max = Float.NaN;
        public float min = Float.NaN;
        public Range (float max, float min) {
            this.max = max;
            this.min = min;
        }

        public boolean contains (float value) {
            return (value >= this.min && value <= this.max);
        }
    }

    // Averages: Lux per environment
    private Range inside_range;
    private Range stairs_range;
    private Range outside_range;

    // Samples: Per environment
    private ArrayList<Float> samples_stairs;
    private ArrayList<Float> samples_outside;
    private ArrayList<Float> samples_inside;


    public AmbientLight () {

        // Initialize data arrays
        this.samples_stairs = new ArrayList<Float>();
        this.samples_outside = new ArrayList<Float>();
        this.samples_inside = new ArrayList<Float>();

        // Initialize ranges
        this.inside_range = new Range((float)-1E5, (float)1E5);
        this.stairs_range = new Range((float)-1E5, (float)1E5);
        this.outside_range = new Range((float)-1E5, (float)1E5);
    }

    // Return the average value for a given environment
    public Range get_range (Environment environment)
    {
        // If the average is already computed then it doesn't need to be recomputed
        switch (environment) {
            case INSIDE: {
                return inside_range;
            }

            case STAIRS: {
                return stairs_range;
            }

            case OUTSIDE: {
                return outside_range;
            }
        }

        return null;
    }

    // Returns the sample count for a given environment
    public int get_sample_count (Environment environment) {
        ArrayList<Float> selector = null;
        switch (environment) {
            case INSIDE: {
                selector = samples_inside;
            }
            break;

            case STAIRS: {
                selector = samples_stairs;
            }
            break;

            case OUTSIDE: {
                selector = samples_outside;
            }
            break;
        }

        if (selector == null) {
            return -1;
        } else {
            return selector.size();
        }
    }

    // Add a sample for the given environment
    public void add_sample (float value, Environment environment)
    {
        Range r = null;
        switch (environment) {
            case INSIDE: {
                r = inside_range;
                this.samples_inside.add(value);
            }
            break;

            case STAIRS: {
                r = stairs_range;
                this.samples_stairs.add(value);
            }
            break;

            case OUTSIDE: {
                r = outside_range;
                this.samples_outside.add(value);
            }
            break;

            case NONE: {
                Log.e("AmbientLight", "Cannot register a value under: " + environment);
                return;
            }
        }

        r.max = Math.max(value, r.max);
        r.min = Math.min(value, r.min);
    }

    // Clear all samples for the given environment
    public void clear_samples (Environment environment)
    {
        switch (environment) {

            case INSIDE: {
                this.samples_inside.clear();
                this.inside_range = new Range((float)-1E5, (float)1E5);
            }
            break;

            case STAIRS: {
                this.samples_stairs.clear();
                this.stairs_range = new Range((float)-1E5, (float)1E5);
            }
            break;

            case OUTSIDE: {
                this.samples_outside.clear();
                this.outside_range = new Range((float)-1E5, (float)1E5);
            }
            break;
        }
    }

    // Returns true if all sample sets have at least the given number of samples
    public boolean hasAtLeastNSamples (int n)
    {
        return (samples_outside.size() >= n) &&
                (samples_inside.size() >= n) &&
                (samples_stairs.size() >= n);
    }


    // Returns the class most like the given sample
    public Environment getMatchingEnvironment (float sample)
    {
        int ranges_in = 0;
        Environment e = Environment.NONE;

        // Return none if no data is available for any category
        if (samples_stairs.size() == 0 || samples_outside.size() == 0 || samples_inside.size() == 0) {
            return Environment.NONE;
        }

        // Determine if in range
        if (inside_range.contains(sample))  { e = Environment.INSIDE; ranges_in++; }
        if (stairs_range.contains(sample))  { e = Environment.STAIRS; ranges_in++; }
        if (outside_range.contains(sample)) { e = Environment.OUTSIDE; ranges_in++; }

        // Return none if in range more than one zone or no zones
        if (ranges_in > 1 || ranges_in == 0) {
            return Environment.NONE;
        }

        // Return
        return e;
    }

}
