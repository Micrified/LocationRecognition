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

    // Averages: Lux per environment
    private Float average_stairs  = Float.NaN;
    private Float average_outside = Float.NaN;
    private Float average_inside  = Float.NaN;

    // Samples: Per environment
    private ArrayList<Float> samples_stairs;
    private ArrayList<Float> samples_outside;
    private ArrayList<Float> samples_inside;


    public AmbientLight () {

        // Initialize data arrays
        this.samples_stairs = new ArrayList<Float>();
        this.samples_outside = new ArrayList<Float>();
        this.samples_inside = new ArrayList<Float>();
    }

    // Return the average value for a given environment
    public float get_average (Environment environment)
    {
        // If the average is already computed then it doesn't need to be recomputed
        switch (environment) {
            case INSIDE: {
                return average_inside;
            }

            case STAIRS: {
                return average_stairs;
            }

            case OUTSIDE: {
                return average_outside;
            }
        }

        return Float.NaN;
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
        float new_average = 0.0f;
        switch (environment) {
            case INSIDE: {
                this.samples_inside.add(value);
                for (Float f : this.samples_inside) {
                    new_average += f.floatValue();
                }
                this.average_inside = (new_average / (float)this.samples_inside.size());
            }
            break;

            case STAIRS: {
                this.samples_stairs.add(value);
                for (Float f : this.samples_stairs) {
                    new_average += f.floatValue();
                }
                this.average_stairs = (new_average / (float)this.samples_stairs.size());
            }
            break;

            case OUTSIDE: {
                this.samples_outside.add(value);
                for (Float f : this.samples_outside) {
                    new_average += f.floatValue();
                }
                this.average_outside = (new_average / (float)this.samples_outside.size());
            }
            break;

            case NONE: {
                Log.e("AmbientLight", "Cannot register a value under: " + environment);
                return;
            }
        }
    }

    // Clear all samples for the given environment
    public void clear_samples (Environment environment)
    {
        switch (environment) {

            case INSIDE: {
                this.samples_inside.clear();
                this.average_inside = Float.NaN;
            }
            break;

            case STAIRS: {
                this.samples_stairs.clear();
                this.average_stairs = Float.NaN;
            }
            break;

            case OUTSIDE: {
                this.samples_outside.clear();
                this.average_outside = Float.NaN;
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

        // Return none if no data is available for any category
        if (samples_stairs.size() == 0 || samples_outside.size() == 0 || samples_inside.size() == 0) {
            return Environment.NONE;
        }

        // Compute difference between measured value and the different categories
        float diff_inside = (average_inside - sample) * (average_inside - sample);
        float diff_stairs = (average_stairs - sample) * (average_stairs - sample);
        float diff_outside = (average_outside - sample) * (average_outside - sample);

        // Return the best matching option
        if (diff_inside < diff_stairs) {
            return (diff_inside < diff_outside ? Environment.INSIDE : Environment.OUTSIDE);
        } else {
            return (diff_stairs < diff_outside ? Environment.STAIRS : Environment.OUTSIDE);
        }
    }

}
