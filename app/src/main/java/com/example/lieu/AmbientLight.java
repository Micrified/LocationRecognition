package com.example.lieu;

public class AmbientLight {

    enum Environment {
        INSIDE,
        STAIRS,
        OUTSIDE
    }

    private float average_stairs;
    private float average_outside;
    private float average_inside;

    public AmbientLight (float average_stairs, float average_outside, float average_inside) {
        this.average_stairs = average_stairs;
        this.average_outside = average_outside;
        this.average_inside = average_inside;
    }

    public float get_average_stairs  () { return this.average_stairs;  }
    public float get_average_outside () { return this.average_outside; }
    public float get_average_inside  () { return this.average_inside;  }

    public void set_average_stairs (float value)  { this.average_stairs = value;  }
    public void set_average_outside (float value) { this.average_outside = value; }
    public void set_average_inside (float value)  { this.average_inside = value;  }

    // Returns the class most like the given sample
    public Environment getMatchingEnvironment (float sample)
    {
        float diff_inside = (average_inside - sample) * (average_inside - sample);
        float diff_stairs = (average_stairs - sample) * (average_stairs - sample);
        float diff_outside = (average_outside - sample) * (average_outside - sample);

        if (diff_inside < diff_stairs) {
            return (diff_inside < diff_outside ? Environment.INSIDE : Environment.OUTSIDE);
        } else {
            return (diff_stairs < diff_outside ? Environment.STAIRS : Environment.OUTSIDE);
        }
    }

}
