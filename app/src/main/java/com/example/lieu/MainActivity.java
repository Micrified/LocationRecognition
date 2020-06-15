package com.example.lieu;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, StepListener, SensorEventListener {

    // The navigation drawer
    private DrawerLayout drawer;

    // Current fragment shown
    private Swap currentFragment;

    // ID of current fragment shown
    private int currentFragmentID;

    private SimpleStepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    public static int numSteps;
    public static int height = 175;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new SimpleStepDetector();
        simpleStepDetector.registerListener(this);

        // Initialize the DataManager Singleton
        DataManager dataManager = DataManager.getInstance();

        // Create the WiFiManager and assign it to the dataManager instance
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Enable Wifi
        wifiManager.setWifiEnabled(true);

        // Assign it
        dataManager.setWifiManager(wifiManager);

        // Register the custom toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set the navigation drawer
        this.drawer = findViewById(R.id.drawer_layout);

        // Retreive the navigation view
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Create an action bar toggle action
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        // Register the drawer action with the drawer
        this.drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Use default orientation when starting for the first time (no orientation change)
        if (savedInstanceState == null) {

            // Create the new fragment (save as last fragment since it is the first)
            currentFragment = new LieuFragment();

            // Create last fragment shown ID
            currentFragmentID = R.id.nav_lieu;

            // Show the first screen
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    (Fragment)currentFragment).commit();

            // Make sure it is checked in the navigation view
            navigationView.setCheckedItem(currentFragmentID);

        }
    }

    // Performs a fragment change to the fragment of the given ID.
    private void changeToFragment (int fragment_id) {

        // Don't change fragments if not necessary
        if (fragment_id == currentFragmentID) {
            return;
        }

        // Otherwise notify current fragment it is being hidden, and replace with new one.
        currentFragment.swappingOut();

        // Replace the current fragment id
        currentFragmentID = fragment_id;

        // Otherwise change the fragment
        switch (fragment_id) {
            case R.id.nav_particle:
                currentFragment = new FilterFragment();
                break;

            case R.id.nav_lieu:
                currentFragment = new LieuFragment();
                break;

            case R.id.nav_train:
                currentFragment = new TrainFragment();
                break;

            case R.id.nav_data:
                currentFragment = new DataFragment();
                break;

            case R.id.nav_settings:
                currentFragment = new SettingsFragment();
                break;
        }

        // Apply the change
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                (Fragment)currentFragment).commit();

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        // Apply fragment change
        changeToFragment(menuItem.getItemId());

        // Close the drawer
        drawer.closeDrawer(GravityCompat.START);

        // Return true (the item is selectable)
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // If the navigation drawer is open, just close it here instead of quitting
        if (this.drawer.isDrawerOpen(GravityCompat.START)) {
            this.drawer.closeDrawer(GravityCompat.START);
        } else {
            System.out.println("Exiting ...");
            super.onBackPressed();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onResume() {
        super.onResume();
        numSteps = 0;
        //textView.setText(TEXT_NUM_STEPS + numSteps);
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        //textView.setText(TEXT_NUM_STEPS + numSteps);
        LieuFragment.UpdateSteps(numSteps);
    }

}
