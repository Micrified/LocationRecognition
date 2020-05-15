package com.example.lieu;

public class TrainingSample {

    // RSS level
    double rss;

    // BSSID
    String bssid;

    // Getter: RSS
    public double getRss () {
        return this.rss;
    }

    // Getter: BSSID
    public String getBssid () {
        return this.bssid;
    }

    // Constructor
    public TrainingSample (String bssid, int level) {
        this.rss = (double)level;
        this.bssid = bssid;
    }
}
