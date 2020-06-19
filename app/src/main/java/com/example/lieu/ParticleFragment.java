package com.example.lieu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.graphics.Canvas;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;
import static android.hardware.Sensor.TYPE_PRESSURE;

public class ParticleFragment extends Fragment implements View.OnClickListener, SensorEventListener, Swap, StepListener {

    // The reset button
    private Button resetButton;

    // The step button
    private Button stepButton;

    // The surface view holding the canvas
    private ImageView canvasView;

    // The status view holding the angle in text
    private TextView statusTextView;

    // Step text view
    private TextView stepTextView;

    // The convergence text view
    private TextView convergenceTextView;

    // The canvas itself
    private Canvas canvas;

    // The imageview holding the compass image
    private ImageView compassImageView;

    // Boolean indicating if the canvas is ready for drawing
    boolean is_canvas_initialized = false;

    // The global data lock
    private ReentrantLock lock;

    // The sensor manager
    private SensorManager sensorManager;

    // The accelerometer
    private Sensor accelerometer;

    // The gyroscope
    private Sensor gyroscope;

    // General orientation sensor
    private Sensor magnetometer;

    // Pressure sensor
    private Sensor barometer;

    // Ambient light sensor
    private Sensor ambience;

    // Sensor readings
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private final float[] rotationMatrix = new float[9];

    // Computed orientation
    private final float[] orientationAngles = new float[3];

    // The step-detector
    private SimpleStepDetector simpleStepDetector;

    // The angle offset (also in degrees)
    float global_angle_correction = 0f; //  -171.109421f;

    // The angle in degrees
    float global_angle_degrees = 0f;

    // The current ambient light value
    AmbientLight.Environment global_ambient_light_environment = AmbientLight.Environment.NONE;

    // The current reading on the barometer
    float global_barometer_value = 0f;

    // Total steps so far
    int g_total_steps = 0;

    // The global screen canvas width and height
    int g_width, g_height;

    // The list of zones to draw
    private ArrayList<Zone> g_zones, g_apartment_zones, g_exterior_zones;

    // The list of particles to draw
    private ArrayList<Particle> g_particles;

    // The number of particles used
    private int g_particle_count = 500;

    // Step distance in pixels
    int g_step_distance_pixels = 102;

    // Convergence-particle
    private Particle convergence_particle = null;

    // Boolean indicating if convergence has been achieved yet
    private Zone convergedZone = null;

    // Boolean indicating if light information is available
    private boolean ambient_light_ready = false;

    // Boolean indicating if light information is available
    private boolean barometer_ready = false;

    // The current ambient light value
    Barometer.Environment global_barometer_environment = Barometer.Environment.NONE;


    /*
     *******************************************************************************
     *                              Required Methods                               *
     *******************************************************************************
     */


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_particle, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        // Initialize the lock
        this.lock = new ReentrantLock();

        // Configure the reset button
        this.resetButton = getView().findViewById(R.id.reset_button);

        // Configure the reset button
        this.stepButton = getView().findViewById(R.id.step_button);

        // Set class as listener
        this.resetButton.setOnClickListener(this);

        // Set class as listener
        this.stepButton.setOnClickListener(this);

        // Configure the surface view for the canvas
        this.canvasView = getView().findViewById(R.id.canvas_image_view);

        // Register custom method to be run after the canvas is drawn
        this.canvasView.post(new Runnable() {
            @Override
            public void run() {
                ParticleFragment.this.lock.lock();
                ParticleFragment.this.g_height = ParticleFragment.this.canvasView.getHeight();
                ParticleFragment.this.g_width = ParticleFragment.this.canvasView.getWidth();
                initCanvas();
                ParticleFragment.this.lock.unlock();
            }
        });

        // Configure the compass imageview
        this.compassImageView = getView().findViewById(R.id.particle_compass_imageview);

        // Configure the status text view
        this.statusTextView = getView().findViewById(R.id.status_text_view);

        // The convergence text view
        this.convergenceTextView = getView().findViewById(R.id.convergence_text_view);
        this.convergenceTextView.setVisibility(View.INVISIBLE);

        // Configure the sensor manager
        this.sensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);

        // Configure the textview
        this.stepTextView = getView().findViewById(R.id.step_text_view);

        // Setup the step detector
        this.simpleStepDetector = new SimpleStepDetector();
        simpleStepDetector.registerListener(this);

        // Run the updater
        this.initRefreshTimer(50);
    }

    /*
     *******************************************************************************
     *                              UI Update Methods                              *
     *******************************************************************************
     */


    // Initializes the UI refresh timer
    public void initRefreshTimer (long ms) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask(){
            public void run() {

                while (isAdded() == false);

                getActivity().runOnUiThread(new Runnable(){
                    public void run() {
                        ParticleFragment.this.lock.lock();
                        if (ParticleFragment.this.is_canvas_initialized) {

                            // Update rotation matrix, which is needed to update orientation angles.
                            SensorManager.getRotationMatrix(rotationMatrix, null,
                                    accelerometerReading, magnetometerReading);

                            // Obtain the orientation angles
                            SensorManager.getOrientation(rotationMatrix, orientationAngles);

                            // Update the orientation
                            float rate_yaw = orientationAngles[0];
                            ParticleFragment.this.lock.lock();
                            ParticleFragment.this.global_angle_degrees = (float)Math.toDegrees(rate_yaw);
                            updateCompass();
                            ParticleFragment.this.lock.unlock();

                            // Update the zone
                            if (ambient_light_ready) {
                                String fmt = String.format("Steps: %3d Env: %7s",
                                        g_total_steps, "" + global_ambient_light_environment);
                                ParticleFragment.this.stepTextView.setText(fmt);
                            }

                            // Repaint the canvas
                            ParticleFragment.this.paintCanvas();

                            // Then request the canvas be updated
                            if (getView() != null && getView().findViewById(R.id.canvas_image_view) != null) {
                                getView().findViewById(R.id.canvas_image_view).invalidate();
                            }

                        }
                        ParticleFragment.this.lock.unlock();
                    }
                });

            }
        }, 0, ms);
    }


    // Basically draws the canvas with the correct width and height after initialization
    public void initCanvas () {

        // Configure the canvas
        Log.e("Dimensions", "Width = " + g_width + " Height = " + g_height);
        Bitmap bitmap = Bitmap.createBitmap(g_width, g_height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);
        canvasView.setImageBitmap(bitmap);

        // Initialize the areas
        this.initAreas();

        // Initialize the particles
        this.g_particles = this.initParticles(this.g_zones);

        // Mark true
        is_canvas_initialized = true;
    }

    // Creates all areas and adds it to the area list
    public void initAreas () {

        // Get and set apartment zones
        g_apartment_zones = Zone.getApartmentLayout();

        // Get and set exterior zones
        g_exterior_zones = Zone.getExteriorLayout();

        // Mesh all zones: Place apartment last so rendered on top
        ArrayList<Zone> all_zones = new ArrayList<Zone>();
        all_zones.addAll(g_exterior_zones);
        all_zones.addAll(g_apartment_zones);
        this.g_zones = all_zones;
    }

    // Creates all particles and adds them to the particle list
    public ArrayList<Particle> initParticles (ArrayList<Zone> zones) {
        ArrayList<Particle> ps = new ArrayList<Particle>();

        // Create array of normalized areas
        float[] norm_areas = new float[zones.size()];
        float norm = 0;
        for (int i = 0; i < zones.size(); ++i) {
            float area = zones.get(i).getArea();
            norm_areas[i] = area;
            norm += area;
        }
        for (int i = 0; i < zones.size(); ++i) {
            norm_areas[i] /= norm;
        }

        // Allocate particles for each area.
        int total_allocated = 0;
        for (int i = 0; i < zones.size(); ++i) {
            int zone_id = zones.get(i).getId();
            int w = zones.get(i).getRect().width();
            int h = zones.get(i).getRect().height();
            int x = zones.get(i).getRect().left;
            int y = zones.get(i).getRect().top;

            // Particles to allocate
            int particle_count = (int)Math.ceil((double)g_particle_count * (double)norm_areas[i]);

            // Trim if will go over limit
            if (total_allocated + particle_count > g_particle_count) {
                particle_count = (g_particle_count - total_allocated);
            }

            // Update total allocated
            total_allocated += particle_count;

            for (int j = 0; j < particle_count; ++j) {
                int px = x + (int)((float)w * Math.random());
                int py = y + (int)((float)h * Math.random());
                Particle p = new Particle(zone_id, new Point(px, py), 6, 0.0f);
                ps.add(p);
            }
        }

        // Update actual count
        g_particle_count = total_allocated;

        // Log cout
        Log.e("PARTICLE", "Count = " + total_allocated);

        // Update particle weights
        for (Particle p : ps) {
            p.setWeight(1.0f / (double)g_particle_count);
        }

        // Update particles
        return ps;
    }


    // Draws all areas and particles on the user interface
    public void paintCanvas () {

        // Draw background color
        canvas.drawColor(Color.BLACK);

        // First draw all areas
        for (Zone z : this.g_zones) {
            z.draw(canvas);
        }

        // Then draw all particles
        for (Particle p : this.g_particles) {
            p.draw(canvas);
        }

        // Optionally draw convergence particle
        if (this.convergence_particle != null) {
            this.convergence_particle.draw(canvas);
        }

    }


    /*
     *******************************************************************************
     *                                 Convergence                                 *
     *******************************************************************************
     */


    // Checks if convergence has been achieved yet by particles in rooms
    public void checkConvergence () {

        // Reset the converged cell
        this.convergedZone = null;
        this.convergence_particle = null;
        this.convergenceTextView.setVisibility(View.INVISIBLE);

        // Check for convergence
        Point p = this.getCluster(this.g_particles);

        if (p == null) {
            return;
        }

        this.convergence_particle = new Particle(-1, p, 45, 0.0f, Color.GREEN);

        for (Zone z : this.g_zones) {
            if (z.containsPoint(p)) {
                this.convergedZone = z;
                this.convergenceTextView.setText(z.getName());
                this.convergenceTextView.setVisibility(View.VISIBLE);
                return;
            }
        }
    }


    // Returns null if no cluster exists yet. Otherwise returns cluster center point
    public Point getCluster (ArrayList<Particle> data) {

        // If there is no data, or no particles, return NULL (no cluster)
        if (data == null || data.size() == 0) {
            return null;
        }

        // If there is only one point. Return just that
        if (data.size() == 1) {
            return data.get(0).getPosition();
        }

        // Otherwise find the mean (x,y) point for all elements
        int mean_x = data.get(0).getPosition().x;
        int mean_y = data.get(0).getPosition().y;

        for (int i = 1; i < data.size(); ++i) {
            mean_x = (data.get(i).getPosition().x + mean_x) / 2;
            mean_y = (data.get(i).getPosition().y + mean_y) / 2;
        }
        double average_separation = 0;
        int n = 0;
        for (int i = 0; i < data.size() - 1; ++i) {
            Point a = data.get(i).getPosition();
            for (int j = i + 1; j < data.size(); ++j, n++) {
                Point b = data.get(j).getPosition();
                average_separation += Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y));
            }
        }
        average_separation /= n;

        System.out.println("Average separation = " + average_separation);

        if (average_separation < (2 * g_step_distance_pixels)) {
            return new Point(mean_x, mean_y);
        } else {
            return null;
        }
    }

    // Particle explosion system
    private ArrayList<Particle> explode () {
        return initParticles(this.g_zones);
    }

    /*
     *******************************************************************************
     *                                Model Update                                 *
     *******************************************************************************
     */

    // Updates the compass and bearing
    public void updateCompass () {
        double angle = global_angle_degrees - global_angle_correction;
        int compass_angle = ((int)angle + 360) % 360;
        this.compassImageView.setRotation(compass_angle);
        this.statusTextView.setText(String.format("%3.0f°", angle));
    }

    // Updates the model. Called when a step has occurred.
    public void updateModel () {

        // If no particles - do nothing
        if (is_canvas_initialized == false || this.g_particles == null) {
            return;
        }

        // Move all particles
        double angle_corrected = global_angle_degrees - global_angle_correction;
        double compass_angle = (double)(((int)angle_corrected + 360) % 360);
        double movement_noise_max = 0.5 * (double)g_step_distance_pixels;
        double movement_noise = (movement_noise_max * Math.random()) - (movement_noise_max / 2);

        for (Particle p : g_particles) {
            double angle_radians = Math.toRadians(compass_angle);
            double dx = Math.sin(angle_radians), dy = -Math.cos(angle_radians);

            int move_x = (int)(dx * ((double)g_step_distance_pixels + movement_noise));
            int move_y = (int)(dy * ((double)g_step_distance_pixels + movement_noise));
            Point destination = new Point(p.getPosition().x + move_x,
                                             p.getPosition().y + move_y);
            p.setPosition(destination);
        }

        // Resample particles
        double spawn_noise_radius = (double)((g_step_distance_pixels * 2) / 3);
        g_particles = Particle.resample(ambient_light_ready, barometer_ready, global_barometer_environment, global_ambient_light_environment,
                spawn_noise_radius, g_particles, g_zones);

        // Check if we ran out of particles
        if (g_particles == null) {
            Log.e("Particle Filters", "Exploding ...");
            g_particles = explode();
        }

        // Clean up particles out of bounds or those that made illegal jumps
        for (Particle p : g_particles) {
            p.setVisible(false);
            int current_zone = -1;
            for (Zone z : g_zones) {
                if (z.containsPoint(p.getPosition())) {
                    p.setVisible(true);
                    current_zone = z.getId();
                    break;
                }
            }

            // Extract old zone, set new zone
            int old_zone = p.get_last_zone_id();
            int new_zone = current_zone;

            // If the new zone is out of bounds, then finished as it is not visible
            if (new_zone == -1) {
                break;
            }

            // If it is in bounds, but not a legal move. Then move out of bounds and mark
            if (Zone.adjacentZones(old_zone, new_zone) == false) {
                p.setVisible(false);
                p.setPosition(new Point(0, 0));
            }

            // Otherwise update the zone
            p.set_last_zone_id(new_zone);
        }

        // Auto check convergence
        this.checkConvergence();
    }


    /*
     *******************************************************************************
     *                               Actions Methods                               *
     *******************************************************************************
     */

    // Returns the 90 degree angle division
    private float toNearestNinety360 (float angle) {
        if ((angle >= 315 && angle < 360) || (angle >= 0 && angle < 45)) {
            return 0;
        }
        if (angle >= 45 && angle < 135) {
            return 90;
        }
        if (angle >= 135 && angle < 225) {
            return 180;
        }
        return 270;
    }


    // Sets the step counter
    private void setGlobalStepCount (int steps) {
        g_total_steps = steps;
        String fmt = String.format("Steps: %3d Env: %7s",
                g_total_steps, "" + global_ambient_light_environment);
        this.stepTextView.setText(fmt);
    }

    // [SYNC] Resets all particles, and saves next rotation offset as error
    private void reset () {
        lock.lock();
        this.convergedZone = null;
        this.global_angle_correction = this.global_angle_degrees;
        this.g_particles = this.initParticles(this.g_zones);
        lock.unlock();
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.reset_button) {
            setGlobalStepCount(0);
            reset();
        }

        if (v.getId() == R.id.step_button) {
            updateModel();
        }
    }


    /*
     *******************************************************************************
     *                          Sensor Interface Methods                           *
     *******************************************************************************
     */


    // [SYNC] Handler for change in sensors
    @Override
    public void onSensorChanged (SensorEvent sensorEvent) {

        // Averaging buffer for the ambient light
        float[] ambient_light_ring_buffer = new float[8];

        switch(sensorEvent.sensor.getType()) {

            // Magnetometer
            case TYPE_MAGNETIC_FIELD: {

                // Copy for orientation
                System.arraycopy(sensorEvent.values, 0, magnetometerReading,
                        0, magnetometerReading.length);
            }
            break;

            // Accelerometer
            case Sensor.TYPE_ACCELEROMETER: {

                // Copy to step detector
                simpleStepDetector.updateAccel(sensorEvent.timestamp,
                        sensorEvent.values[0], sensorEvent.values[1],
                        sensorEvent.values[2]);

                // Copy for orientation
                System.arraycopy(sensorEvent.values, 0, accelerometerReading,
                        0, accelerometerReading.length);
            }
            break;

            // Gyroscope-field update handler
            case Sensor.TYPE_GYROSCOPE:
                //System.out.println(sensorEvent.timestamp - last_timestamp);
//                float rate_yaw = sensorEvent.values[2];
//                float period = (1.0f/200.0f);
//                float dyaw = rate_yaw * period;
//
//                this.lock.lock();
//                this.global_angle_degrees += Math.toDegrees(-dyaw);
//                updateCompass();
//                this.lock.unlock();
                break;


            // Barometer
            case Sensor.TYPE_PRESSURE: {
                this.lock.lock();
                global_barometer_value = sensorEvent.values[0];

                global_barometer_environment = DataManager.getInstance().
                        getBarometer().getMatchingEnvironment(sensorEvent.values[0]);
                this.lock.unlock();
            }
            break;

            // Ambient light
            case Sensor.TYPE_LIGHT: {

                // Update the ring buffer
                ambient_light_ring_buffer[0] = ambient_light_ring_buffer[1];
                ambient_light_ring_buffer[1] = ambient_light_ring_buffer[2];
                ambient_light_ring_buffer[2] = ambient_light_ring_buffer[3];
                ambient_light_ring_buffer[3] = ambient_light_ring_buffer[4];
                ambient_light_ring_buffer[4] = ambient_light_ring_buffer[5];
                ambient_light_ring_buffer[5] = ambient_light_ring_buffer[6];
                ambient_light_ring_buffer[6] = sensorEvent.values[0];

                // Compute the new average
                float average_ambient_light_buffer = 0.0f;
                for (int i = 0; i < ambient_light_ring_buffer.length; ++i) {
                    average_ambient_light_buffer += ambient_light_ring_buffer[i];
                }
                average_ambient_light_buffer /= 8;


                this.lock.lock();
                global_ambient_light_environment = DataManager.getInstance().
                        getAmbientLight().getMatchingEnvironment(average_ambient_light_buffer);

                // Adjust step distance for stairs if ambient light enabled
                if (ambient_light_ready) {
                    if (global_ambient_light_environment == AmbientLight.Environment.STAIRS) {
                        // 25.0 cm roughly for 9inches * 1.4 pixels per cm
                        g_step_distance_pixels = (int)(25.0f * 1.4f);
                    } else {
                        g_step_distance_pixels = 102;
                    }
                }
                this.lock.unlock();
            }
            break;
        }
    }

    // Handler for change in sensor accuracy
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private int heightInCMToStepDistanceInPixels (int height_cm)
    {
        return (int) Math.ceil(height_cm * 0.415f * 1.4f);
    }

    // Handler for resuming application context
    @Override
    public void onResume() {
        super.onResume();

        // Extract user height (may have been updated)
        int user_height = DataManager.getInstance().getUser_height();

        // Update the step distance knowing that: 1.4px per cm
        g_step_distance_pixels = heightInCMToStepDistanceInPixels(user_height);

        // Update whether light information is available
        this.ambient_light_ready =
                DataManager.getInstance().getAmbientLight().hasAtLeastNSamples(AmbientLight.required_sample_count);


        // Update whether light information is available
        this.barometer_ready =
                DataManager.getInstance().getBarometer().hasAtLeastNSamples(Barometer.required_sample_count);

        Log.e("Update", "Ambient light enabled: " + ambient_light_ready);


        Log.e("Update", "Barometer enabled: " + barometer_ready);

        // Acquire the accelerometer sensor
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (this.accelerometer == null) {
            Log.e("Sensors", "Unable to acquire accelerometer!");
        } else {
            Log.i("Sensors", "Accelerometer acquired!");
            sensorManager.registerListener(this, this.accelerometer, sensorManager.SENSOR_DELAY_FASTEST);
        }

        // Acquire the gyroscope
        this.gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (this.gyroscope == null) {
            Log.e("Sensors", "Unable to acquire gyroscope!");
        } else {
            Log.i("Sensors", "Gyroscope acquired!");
            sensorManager.registerListener(this, this.gyroscope, sensorManager.SENSOR_DELAY_FASTEST);
        }

        // Acquire the magnetometer
        this.magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (this.magnetometer == null) {
            Log.e("Sensors", "Unable to acquire magnetometer!");
        } else {
            Log.i("Sensors", "Magnetometer acquired!");
            sensorManager.registerListener(this, this.magnetometer, sensorManager.SENSOR_DELAY_FASTEST);
        }

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

    // Handler for losing application context
    @Override
    public void onPause() {
        super.onPause();

        // Disable sensor updates
        sensorManager.unregisterListener(this);
    }

    public void swappingOut () {

    }


    /*
     *******************************************************************************
     *                               Step Detection                                *
     *******************************************************************************
     */

    @Override
    public void step(long timeNs) {
        this.lock.lock();
        setGlobalStepCount(g_total_steps + 1);
        this.updateModel();
        this.lock.unlock();
    }
}