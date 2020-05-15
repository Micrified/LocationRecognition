package com.example.lieu;

public class Correlator {

    // The correlation limit
    public static float correlation_limit = 148;

    // The kernel for correlation
    public static float[] kernel = new float[] {
        6.5032200000f,
        6.7761540000f,
        7.3962550000f,
        8.1384580000f,
        8.4568940000f,
        8.5191350000f,
        8.6005400000f,
        8.7346190000f,
        9.2517700000f,
        10.652374000f,
        12.287613000f,
        13.087280000f,
        12.153534000f,
        10.551819000f,
        10.254929000f,
        11.653152000f,
        13.137558000f,
        13.115997000f,
        11.844681000f,
        10.884613000f,
        10.671524000f,
        10.556610000f,
        9.4456940000f,
        7.9469300000f,
        7.0419160000f
    };

    public static boolean isStep (float[] window) {
        float sum = 0;
        for (int i = 0; i < kernel.length; ++i) {
            sum = sum + (kernel[i] - window[i]) * (kernel[i] - window[i]);
        }
        //System.out.println(sum);
        return (sum <= correlation_limit);
    }

    public static float variance (float[] window) {
        float mean = 0, variance = 0;

        // Compute the mean (N)
        for (int i = 0; i < window.length; ++i) {
            mean = mean + window[i];
        }
        mean /= window.length;

        // Compute the variance (N)
        for (int i = 0; i < window.length; ++i) {
            variance = variance + (window[i] - mean) * (window[i] - mean);
        }
        return variance / window.length;
    }
}
