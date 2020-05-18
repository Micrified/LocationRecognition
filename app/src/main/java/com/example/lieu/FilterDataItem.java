package com.example.lieu;

public class FilterDataItem {
    private String bssid;
    private String ssid;
    private boolean filtered;

    public FilterDataItem (String bssid, String ssid, boolean filtered)
    {
        this.bssid = bssid;
        this.ssid  = ssid;
        this.filtered = filtered;
    }

    public String getBSSID ()
    {
        return this.bssid;
    }

    public String getSSID ()
    {
        return this.ssid;
    }

    public boolean getFiltered ()
    {
        return this.filtered;
    }

}
