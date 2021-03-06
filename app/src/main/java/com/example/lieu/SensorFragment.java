package com.example.lieu;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;

public class SensorFragment extends Fragment implements View.OnClickListener, SensorEventListener, Swap, AdapterView.OnItemSelectedListener {

    // Sensor manager
    private SensorManager sensorManager;

    // Sensor: Ambient light
    private Sensor ambience;

    // Sensor: Barometer
    private Sensor barometer;

    // Textview: Sample counter for light
    TextView textview_light_sample_counter;

    // Textview: Live samples from light sensor
    TextView textview_light_live_samples;

    // Textview: Displays detected zone
    TextView textview_light_test_result;

    // Textview: Displays average light value for inside
    TextView textview_average_inside;

    // Textview: Displays average for stairs
    TextView textview_average_stairs;

    // Textview: Displays average for outside
    TextView textview_average_outside;

    // Textview: Displays average pressure upstairs
    TextView textview_average_upstairs;

    // Textview: Displays avg. pressure downstairs
    TextView textview_average_downstairs;

    // Textview: Displays current pressure value
    TextView textview_current_barometer;

    // Spinner: Environment for light
    Spinner spinner_light_environment;

    // Sample: Light (lux)
    float global_light_sample;

    // Sample: Barometer (mbar)
    float barometer_sample;


    // Button: Sample for the light sensor
    Button button_sample_light_sensor;

    // Button: Light environment testing
    Button button_test_light_environment;

    // Button: Clear
    Button button_clear;

    // Buttons: training
    Button button_train_downstairs;
    Button button_train_upstairs;

    // Currently selected light environment
    AmbientLight.Environment global_light_environment = AmbientLight.Environment.INSIDE;

    // String displayed in light result textview
    AmbientLight.Environment global_test_result = AmbientLight.Environment.NONE;

    // String displayed in barometer textview
    Barometer.Environment global_test_result_barometer = Barometer.Environment.NONE;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sensors, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        // Configure the sensor manager
        this.sensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);

        // Configure the textviews
        this.textview_light_sample_counter = getView().findViewById(R.id.textview_light_sample_counter);
        this.textview_light_live_samples = getView().findViewById(R.id.textview_light_live_samples);
        this.textview_light_test_result = getView().findViewById(R.id.textview_light_test_result);

        this.textview_average_inside = getView().findViewById(R.id.textview_average_inside);
        this.textview_average_stairs = getView().findViewById(R.id.textview_average_stairs);
        this.textview_average_outside = getView().findViewById(R.id.textview_average_outside);


        this.textview_average_upstairs = getView().findViewById(R.id.textview_average_upstairs);
        this.textview_average_downstairs = getView().findViewById(R.id.textview_average_downstairs);
        this.textview_current_barometer = getView().findViewById(R.id.textview_current_barometer);


        // Configure the spinner
        this.spinner_light_environment = getView().findViewById(R.id.spinner_light_environment);
        ArrayAdapter<CharSequence> spinner_light_adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.environments, android.R.layout.simple_spinner_item);
        spinner_light_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinner_light_environment.setAdapter(spinner_light_adapter);
        this.spinner_light_environment.setSelection(0);
        this.spinner_light_environment.setOnItemSelectedListener(this);


        // Configure the buttons
        this.button_sample_light_sensor = getView().findViewById(R.id.button_sample_light_sensor);
        this.button_sample_light_sensor.setOnClickListener(this);

        this.button_test_light_environment = getView().findViewById(R.id.button_test_light_environment);
        this.button_test_light_environment.setOnClickListener(this);

        this.button_clear = getView().findViewById(R.id.button_clear);
        this.button_clear.setOnClickListener(this);

        this.button_train_downstairs = getView().findViewById(R.id.button_train_downstairs);
        this.button_train_downstairs.setOnClickListener(this);

        this.button_train_upstairs = getView().findViewById(R.id.button_train_upstairs);
        this.button_train_upstairs.setOnClickListener(this);

        // Only enable the test button if enough samples have been collected
        updateUIState();
    }

    @Override
    public void onClick(View v) {
        final AmbientLight ambientLight = DataManager.getInstance().getAmbientLight();
        final Barometer barometerValues = DataManager.getInstance().getBarometer();

        // Add a new sample
        switch (v.getId()) {

            // Button pressed to collect samples on lower elevation
            case R.id.button_train_downstairs: {

                // Reset result information
                global_test_result_barometer = Barometer.Environment.OUTSIDE;
                barometerValues.add_sample(barometer_sample, global_test_result_barometer);
            }
            break;

            // Button pressed to collect samples on higher elevation
            case R.id.button_train_upstairs: {

                // Set result information
                global_test_result_barometer = Barometer.Environment.INSIDE;
                barometerValues.add_sample(barometer_sample, global_test_result_barometer);
            }
            break;

            // Button pressed to collect ambient light sample
            case R.id.button_sample_light_sensor: {
                ambientLight.add_sample(global_light_sample, global_light_environment);
            }
            break;

            // Button pressed to test detected environment
            case R.id.button_test_light_environment: {

                // Get classification of current sample
                AmbientLight.Environment environment = ambientLight.getMatchingEnvironment(global_light_sample);

                // Set the global value
                global_test_result = environment;
            }
            break;

            // Button pressed to clear all samples
            case R.id.button_clear: {

                // Clear the ambient light information
                ambientLight.clear_samples(global_light_environment);


                // Clear the barometer information
                barometerValues.clear_samples(Barometer.Environment.OUTSIDE);
                barometerValues.clear_samples(Barometer.Environment.INSIDE);

                // Reset result information
                global_test_result = AmbientLight.Environment.NONE;
            }
            break;


        }

        // Update the interface
        updateUIState();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Acquire the barometer
        this.barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (this.barometer == null) {
            Log.e("Sensors", "Unable to acquire barometer!");
        } else {
            Log.i("Sensors", "Barometer acquired!");
            sensorManager.registerListener(this, this.barometer, sensorManager.SENSOR_DELAY_FASTEST);
        }

        // Acquire the ambient light sensor
        this.ambience = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (this.ambience == null) {
            Log.e("Sensors", "Unable to acquire ambient light sensor!");
        } else {
            Log.i("Sensors", "Ambient light sensor acquired!");
            sensorManager.registerListener(this, this.ambience, sensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Disable sensor updates
        sensorManager.unregisterListener(this);
    }


    public void swappingOut () {
        System.out.println("LEAVING");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {

            case Sensor.TYPE_LIGHT: {

                // Update global value
                this.global_light_sample = event.values[0];
            }
            break;

            case Sensor.TYPE_PRESSURE: {
                this.barometer_sample = event.values[0];
            }
            break;
        }

        getActivity().runOnUiThread(new Runnable(){
            public void run() {

                // Update UI elements
                SensorFragment.this.textview_light_live_samples.setText(
                        String.format("%.1flx", global_light_sample)
                );

                SensorFragment.this.textview_current_barometer.setText(String.format("%.1f", barometer_sample));
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.w("Sensors", "Accuracy changed warning!");
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.spinner_light_environment: {
                System.out.println("You selected " + parent.getItemAtPosition(position));
                AmbientLight.Environment environment_to_set = AmbientLight.Environment.NONE;
                if (position == 0) {
                    environment_to_set = AmbientLight.Environment.INSIDE;
                }
                if (position == 1) {
                    environment_to_set = AmbientLight.Environment.STAIRS;
                }
                if (position == 2) {
                    environment_to_set = AmbientLight.Environment.OUTSIDE;
                }
                System.out.println("Environment now set to " + environment_to_set);

                // Set environment
                this.global_light_environment = environment_to_set;
            }
            break;

            // TODO: Barometer
        }

        // Update the interface
        updateUIState();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    public void updateUIState ()
    {
        final AmbientLight ambientLight = DataManager.getInstance().getAmbientLight();
        final Barometer barometerPressure = DataManager.getInstance().getBarometer();

        // Disable the button if sufficient samples for current environment
        if (ambientLight.get_sample_count(global_light_environment) >= AmbientLight.required_sample_count) {
            button_sample_light_sensor.setEnabled(false);
        } else {
            button_sample_light_sensor.setEnabled(true);
        }

        // Enable the test button if sufficient samples collected
        button_test_light_environment.setEnabled(ambientLight.hasAtLeastNSamples(AmbientLight.required_sample_count));

        // Update the result text
        textview_light_test_result.setText("" + global_test_result);

        // Update the ranges
        AmbientLight.Range inside_range = ambientLight.get_range(AmbientLight.Environment.INSIDE);
        AmbientLight.Range stairs_range = ambientLight.get_range(AmbientLight.Environment.STAIRS);
        AmbientLight.Range outside_range = ambientLight.get_range(AmbientLight.Environment.OUTSIDE);

        textview_average_inside.setText(String.format("%.1f-%.1f", inside_range.min, inside_range.max));
        textview_average_stairs.setText(String.format("%.1f-%.1f", stairs_range.min, stairs_range.max));
        textview_average_outside.setText(String.format("%.1f-%.1f", outside_range.min, outside_range.max));

        //averages barometer
        textview_average_upstairs.setText(String.format("%.2f", barometerPressure.get_average(Barometer.Environment.INSIDE)));
        textview_average_downstairs.setText(String.format("%.2f", barometerPressure.get_average(Barometer.Environment.OUTSIDE)));

        textview_current_barometer.setText(String.format("%.2f", barometer_sample));

        // Update sample counters
        getActivity().runOnUiThread(new Runnable(){
            public void run() {

                // Update UI elements
                SensorFragment.this.textview_light_sample_counter.setText(
                        String.format("%d/%d", ambientLight.get_sample_count(global_light_environment),
                                AmbientLight.required_sample_count)
                );

                if(barometerPressure.get_sample_count(Barometer.Environment.INSIDE) < barometerPressure.required_sample_count)
                {
                    textview_average_upstairs.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
                else
                {
                    textview_average_upstairs.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }

                if(barometerPressure.get_sample_count(Barometer.Environment.OUTSIDE) < barometerPressure.required_sample_count)
                {
                    textview_average_downstairs.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
                else
                {
                    textview_average_downstairs.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            }
        });
    }
}
