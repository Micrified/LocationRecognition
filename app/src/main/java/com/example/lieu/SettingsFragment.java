package com.example.lieu;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;


public class SettingsFragment extends Fragment implements View.OnClickListener, IOUpdateInterface, Swap {

    // Database manager
    private DBManager dbManager;

    private Button adjustDownButton;

    private TextView adjustTextView;

    private Button adjustUpButton;

    private Button importButton;

    private Button exportButton;

    private TextView textView;

    private EditText heightText;


    // Database views

    private Button iterUpButton;
    private Button iterDownButton;
    private TextView iterTextView;
    private Button loadDBButton;
    private Button saveDBButton;
    private Button wipeDBButton;
    private Button heightButton;
    private Switch wipeDBSwitch;

    // Database global version
    private int g_training_version = 1;

    /*
     *******************************************************************************
     *                              Required Methods                               *
     *******************************************************************************
    */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        // Initialize the database
        dbManager = new DBManager(getActivity().getApplicationContext());

        // Setup database views
        this.iterUpButton = getView().findViewById(R.id.iter_adjust_up_button);
        this.iterUpButton.setOnClickListener(this);
        this.iterDownButton = getView().findViewById(R.id.iter_adjust_down_button);
        this.iterDownButton.setOnClickListener(this);
        this.iterTextView = getView().findViewById(R.id.iter_adjust_textview);
        this.heightText = getView().findViewById(R.id.height_number);
        this.heightButton = getView().findViewById(R.id.save_height);
        this.heightButton.setOnClickListener(this);

        this.loadDBButton = getView().findViewById(R.id.load_db_button);
        this.loadDBButton.setOnClickListener(this);
        this.saveDBButton = getView().findViewById(R.id.save_db_button);
        this.saveDBButton.setOnClickListener(this);
        this.wipeDBButton = getView().findViewById(R.id.wipe_db_button);
        this.wipeDBButton.setOnClickListener(this);
        this.wipeDBSwitch = getView().findViewById(R.id.wipe_db_switch);

        this.adjustDownButton = getView().findViewById(R.id.particle_adjust_down_button);
        this.adjustDownButton.setOnClickListener(this);

        this.adjustTextView = getView().findViewById(R.id.particle_adjust_textview);

        this.adjustUpButton = getView().findViewById(R.id.particle_adjust_up_button);
        this.adjustUpButton.setOnClickListener(this);

        this.importButton = getView().findViewById(R.id.import_button);
        importButton.setOnClickListener(this);

        this.exportButton = getView().findViewById(R.id.export_button);
        exportButton.setOnClickListener(this);

        this.textView = getView().findViewById(R.id.io_text_view);

        this.textView.setMovementMethod(new ScrollingMovementMethod());

        // Setup wipe switch and button
        this.wipeDBButton.setEnabled(false);
        this.wipeDBSwitch.setChecked(false);

        // Make sure wipe button is only enabled if switch also toggled
        wipeDBSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SettingsFragment.this.wipeDBButton.setEnabled(isChecked);
            }
        });

        // Set the particle adjustment to zero
        this.adjust_particle(0.0f);

        // Set the version adjustment
        this.adjust_training_version(0);

    }

    // Adjusts the global particle adjustment factor for stepping
    private void adjust_particle (double inc) {
        double component = DataManager.getInstance().getParticleAdjustmentComponent();
        component += inc;
        DataManager.getInstance().setParticleAdjustmentComponent(component);
        this.adjustTextView.setText(String.format("%.2f", component));
    }

    // Adjusts the global training version number for saving to a database
    private void adjust_training_version (int inc) {
        g_training_version += inc;

        // Don't allow negative numbers
        if (g_training_version <= 1) {
            g_training_version = 1;
            this.iterDownButton.setEnabled(false);
        } else {
            this.iterDownButton.setEnabled(true);
        }

        this.iterTextView.setText(String.format("%d", g_training_version));
    }

    private void saveHeight()
    {
        MainActivity.height = Integer.parseInt(heightText.getText().toString());
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.particle_adjust_down_button:
                System.out.println("Button: Adjust Down");
                adjust_particle(-0.005f);
                break;

            case R.id.particle_adjust_up_button:
                System.out.println("Button: Adjust Up");
                adjust_particle(0.005f);
                break;

            case R.id.iter_adjust_down_button:
                System.out.println("Button: Iter Down");
                adjust_training_version(-1);
                break;

            case R.id.iter_adjust_up_button:
                System.out.println("Button: Iter Up");
                adjust_training_version(1);
                break;

            case R.id.load_db_button:
                System.out.println("Button: Load DB");
                this.loadDBButton.setEnabled(false);
                this.saveDBButton.setEnabled(false);
                showLoadDBWarningPopup();
                this.loadDBButton.setEnabled(true);
                this.saveDBButton.setEnabled(true);
                break;

            case R.id.save_db_button:
                System.out.println("Button: Save DB");
                this.loadDBButton.setEnabled(false);
                this.saveDBButton.setEnabled(false);
                this.showSaveDBWarningPopup();
                this.loadDBButton.setEnabled(true);
                this.saveDBButton.setEnabled(true);
                break;

            case R.id.wipe_db_button:
                System.out.println("Button: Wipe DB");
                dbManager.open();
                this.dbManager.deleteAll();
                dbManager.close();
                this.showSuccessPopup("Database erased!");
                this.wipeDBSwitch.setChecked(false);
                break;

            case R.id.export_button:
                System.out.println("Button: Export");
                this.exportCellData();
                break;

            case R.id.import_button:
                System.out.println("Button: Import");
                this.importCellData();
                break;

            case R.id.save_height:
                System.out.println("Button: Height");
                this.saveHeight();
                this.showSuccessPopup("Height Saved!");
                break;
        }
    }


    /*
     *******************************************************************************
     *                              I/O Methods                               *
     *******************************************************************************
    */


    public void showSaveDBWarningPopup () {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        SettingsFragment.this.archiveSamplesToDatabase();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        String msg = String.format("Save all training data as version %d (will replace if existing)?", g_training_version);
        builder.setMessage(msg).setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    public void showLoadDBWarningPopup () {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        SettingsFragment.this.extractDatabaseContents();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        String msg = String.format("Replace memory with database content?", g_training_version);
        builder.setMessage(msg).setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    public void showSuccessPopup (String msg) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        String fmt = String.format("Success: %s", msg);
        builder.setMessage(fmt).setPositiveButton("Close", dialogClickListener).show();
    }

    public void updateIOStatusWithString (final String s) {
        getActivity().runOnUiThread(new Runnable(){
            public void run() {
                String current = SettingsFragment.this.textView.getText().toString();
                String next = String.format("%s%s", current, s);
                SettingsFragment.this.textView.setText(next);
                SettingsFragment.this.textView.postInvalidate();
            }
        });
    }

    // Launches a thread that scans in all cell data
    private void importCellData () {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                DataManager.getInstance().scan(SettingsFragment.this.getContext(), SettingsFragment.this);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                SettingsFragment.this.importButton.setEnabled(true);
                SettingsFragment.this.exportButton.setEnabled(true);
                super.onPostExecute(aVoid);
            }
        }.execute();
    }

    // Launches a thread that exports  all cell data
    private void exportCellData () {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                DataManager.getInstance().export(SettingsFragment.this.getContext(), SettingsFragment.this);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                SettingsFragment.this.importButton.setEnabled(true);
                SettingsFragment.this.exportButton.setEnabled(true);
                super.onPostExecute(aVoid);
            }
        }.execute();
    }


    /*
     *******************************************************************************
     *                              Database Methods                               *
     *******************************************************************************
    */


    // Reads all the database content back
    public void extractDatabaseContents () {

        // Open the database
        dbManager.open();

        // Tracks number of unique versions
        Set<Integer> versions = new HashSet<Integer>();

        // Track total sample count
        int total_sample_count = 0;

        // Create a new array of cells
        ArrayList<Cell> cells = new ArrayList<Cell>();

        for (int i = 0; i < DataManager.getInstance().getCellCount(); ++i) {

            // Create a new cell.
            Cell cell = new Cell(i);

            // Extract cell data
            Cursor cursor = dbManager.fetchCell(i);

            // Create a hashmap mapping BSSIDS to arraylist of doubles
            HashMap<String,ArrayList<Double>> map = new HashMap<String, ArrayList<Double>>();

            // For all records for the cell
            if (cursor.moveToFirst()) {
                do {
                    int version = cursor.getInt(cursor.getColumnIndex(DataBaseHelper.ITERATION));
                    String bssid = cursor.getString(cursor.getColumnIndex(DataBaseHelper.BSSID));
                    float rss = cursor.getFloat(cursor.getColumnIndex(DataBaseHelper.RSS));

                    // Add to total sample count
                    total_sample_count++;

                    // Add to the versions list the version captured
                    versions.add(version);

                    // If the BSSID exists in the hashmap, add the rss value to it.
                    if (map.containsKey(bssid)) {
                        ArrayList<Double> value = map.get(bssid);
                        value.add((double)rss);
                        map.put(bssid, value);
                    } else {

                        // Otherwise create a new entry in the hashmap
                        ArrayList<Double> value = new ArrayList<Double>();
                        value.add((double)rss);
                        map.put(bssid, value);
                    }

                } while (cursor.moveToNext());
            }

            // For all bssids and their values in the hashmap, we create a list of access points
            ArrayList<AccessPointResult> aps = new ArrayList<AccessPointResult>(map.size());
            for (Map.Entry e : map.entrySet()) {
                String bssid = (String)e.getKey();
                ArrayList<Double> samples = (ArrayList<Double>)e.getValue();

                AccessPointResult ap = new AccessPointResult("", bssid, samples);
                aps.add(ap);
            }

            // Add all access-point results to the cell
            cell.setAccessPointResults(aps);

            // Reset the cell
            cell.reset(1.0f / (double)DataManager.getInstance().getCellCount());

            System.out.println("Setting cell with " + cell.getAccessPointResults().size() + " results!");

            // Insert the cell
            cells.add(cell);

            // Close the cursor
            cursor.close();
        }

        // Update DataManager
        DataManager.getInstance().setCells(cells);

        // Close the database
        dbManager.close();

        String load_msg = String.format("Loaded %d samples over %d training sets!", total_sample_count, versions.size());

        showSuccessPopup(load_msg);

    }

    // Saves all content to the database
    public void archiveSamplesToDatabase () {

        // Open the database
        dbManager.open();

        // List the sample count
        int total_sample_count = 0;

        // Get all cells
        DataManager d = DataManager.getInstance();
        List<Cell> cells = d.getCells();

        // Archive all contents to the database
        for (Cell c : cells) {
            ArrayList<AccessPointResult> aps = c.getAccessPointResults();

            for (AccessPointResult ap : aps) {
                ArrayList<Double> samples = ap.getSamples();

                for (Double rss : samples) {
                    total_sample_count++;
                    dbManager.insert(c.getID(), g_training_version, ap.getBssid(), rss.floatValue());
                }
            }
        }

        // Close the database
        dbManager.close();

        String save_msg = String.format("Saved %d samples under training set %d", total_sample_count, g_training_version);

        showSuccessPopup(save_msg);
    }

    /*
     *******************************************************************************
     *                                    Misc                                     *
     *******************************************************************************
    */

    // Handler for resuming application context
    @Override
    public void onResume() {
        super.onResume();
        System.out.println("RESUMING");
    }

    public void swappingOut () {
        System.out.println("LEAVING");
    }

}
