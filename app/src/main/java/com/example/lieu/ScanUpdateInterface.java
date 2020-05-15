package com.example.lieu;

public interface ScanUpdateInterface {

    // Allows external object to post update message to Fragment
    public void updateIOStatusWithString (final String s);

    // Allows the best cell to be displayed
    public void setBestCandidateCell (final int cell_id);

    // Allow the number of scans to be updated and clears status so far
    public void updateScanCountAndReset ();

}
