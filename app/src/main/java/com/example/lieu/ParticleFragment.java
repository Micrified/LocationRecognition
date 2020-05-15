package com.example.lieu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import java.util.concurrent.locks.ReentrantLock;

public class ParticleFragment extends Fragment implements View.OnClickListener, SensorEventListener, Swap {

    // The reset button
    private Button resetButton;

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

    // The step-detector
    private Sensor stepDetector;

    // The angle offset (also in degrees)
    float global_angle_correction = -171.109421f;

    // The angle in degrees
    float global_angle_degrees = 0f;

    // Total steps so far
    int g_total_steps = 0;

    // The global screen canvas width and height
    int g_width, g_height;

    // The scaling factor
    float g_scale = 16.0f;

    // The user height in meters
    float g_user_height = 1.70f;

    // The timestamp of the last step
    long g_timestamp_last_step;

    // Constant representing how long a step typically takes in nanoseconds
    private final long g_step_time = (long)1E9;

    // The list of areas to draw
    private ArrayList<Area> g_areas;

    // The list of particles to draw
    private ArrayList<Particle> g_particles;

    // The number of particles used
    private int g_particle_count = 750;

    // Convergence-particle
    private Particle convergence_particle = null;

    // Boolean indicating if convergence has been achieved yet
    private Area convergedArea = null;

    // Constant describing what percentage of particles must be in a cell to converge
    private float min_convergence_ratio = 0.65f;

    // Used for the orientation of the device
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];


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

        // Set class as listener
        this.resetButton.setOnClickListener(this);

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

        // Configure the accelerometer sensor
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (this.accelerometer == null) {
            Log.e("Sensors", "No Accelerometer!");
        } else {
            Log.e("Sensors", "Accelerometer Exists!");
            sensorManager.registerListener(this, this.accelerometer, sensorManager.SENSOR_DELAY_NORMAL);
        }

        // Configure the gyroscope sensor
        this.gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (this.gyroscope == null) {
            Log.e("Sensors", "No Gyroscope!");
        } else {
            Log.e("Sensors", "Gyroscope Exists!");
            sensorManager.registerListener(this, this.gyroscope, sensorManager.SENSOR_DELAY_FASTEST);
        }

        // [BACKUP] Configure the step-detector
        this.stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (this.stepDetector == null) {
            Log.e("Sensors", "No Step-Detector!");
        } else {
            Log.e("Sensors", "Step-Detector Exists!");
            sensorManager.registerListener(this, this.stepDetector, sensorManager.SENSOR_DELAY_NORMAL);
        }

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
        Bitmap bitmap = Bitmap.createBitmap(g_width, g_height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);
        canvasView.setImageBitmap(bitmap);

        // Initialize the areas
        this.initAreas();

        // Initialize the particles
        this.g_particles = this.initParticles(this.g_areas);

        // Mark true
        is_canvas_initialized = true;
    }


    // Creates all areas and adds it to the area list
    public void initAreas () {
        int dx = (g_width / 2) - (FloorModel.getWidthForModel(g_scale) / 2);
        int dy = (g_height / 2) - (FloorModel.getHeightForModel(g_scale) / 2);
        this.g_areas = FloorModel.getRooms(g_scale, dx, dy);
    }

    // Creates all particles and adds them to the particle list
    public ArrayList<Particle> initParticles (ArrayList<Area> areas) {
        ArrayList<Particle> ps = new ArrayList<Particle>();

        // Create array of normalized areas
        float[] norm_areas = new float[areas.size()];
        float norm = 0;
        for (int i = 0; i < areas.size(); ++i) {
            float area = areas.get(i).getArea();
            norm_areas[i] = area;
            norm += area;
        }
        for (int i = 0; i < areas.size(); ++i) {
            norm_areas[i] /= norm;
        }

        // Allocate particles for each area.
        int total_allocated = 0;
        for (int i = 0; i < areas.size(); ++i) {
            int w = areas.get(i).getWidth();
            int h = areas.get(i).getHeight();
            int x = areas.get(i).getX();
            int y = areas.get(i).getY();

            int particle_count = (int)(g_particle_count * norm_areas[i]);
            total_allocated += particle_count;

            for (int j = 0; j < particle_count; ++j) {
                int px = x + (int)((float)w * Math.random());
                int py = y + (int)((float)h * Math.random());
                Particle p = new Particle(new Point(px, py), 2, 0.0f);
                ps.add(p);
            }
        }

        // Update actual count
        g_particle_count = total_allocated;

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
        for (Area a : this.g_areas) {
            a.draw(canvas);
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
        this.convergedArea = null;
        this.convergence_particle = null;
        this.convergenceTextView.setVisibility(View.INVISIBLE);

        // Check for convergence
        Point p = this.getCluster(this.g_particles);

        if (p == null) {
            return;
        }

        this.convergence_particle = new Particle(p, 15, 0.0f, 0.0f, Color.GREEN);

        for (Area a : this.g_areas) {
            if (a.containsPointWithinRadius(p)) {
                this.convergedArea = a;
                if (a.getID() > 0) {
                    this.convergenceTextView.setText("Cell " + a.getID());
                } else {
                    this.convergenceTextView.setText("Unlabeled Zone");
                }
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

        if (average_separation < 50.0f) {
            return new Point(mean_x, mean_y);
        } else {
            return null;
        }
    }

    // Particle explosion system
    private ArrayList<Particle> explode (ArrayList<Particle> killed) {
//        Particle heaviest = killed.get(0);
//
//        // Locate the particle with the highest weight
//        for (int i = 1; i < killed.size(); ++i) {
//            if (killed.get(i).getWeight() > heaviest.getWeight()) {
//                heaviest = killed.get(i);
//            }
//        }
//
//        // Determine explode position
//        Point p = heaviest.getPosition();
//
//        // Find all rooms within a certain radius
//        ArrayList<Area> nearby = new ArrayList<Area>();
//        float search_radius = 300;
//        for (Area a : this.g_areas) {
//            if (a.distanceToCenter(p) < search_radius) {
//                nearby.add(a);
//            }
//        }

        return initParticles(this.g_areas);
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

        // Compute the distance to step in pixels
        float adjust = (float)DataManager.getInstance().getParticleAdjustmentComponent();
        float step_distance_meters = g_user_height * 0.44f + adjust;
        int step_distance_pixels = (int)(step_distance_meters * g_scale);

        // If no particles - do nothing
        if (is_canvas_initialized == false || this.g_particles == null) {
            return;
        }

        // Find out how many particles to add back in and spawn them on survivors
        int toRespawn = g_particle_count - g_particles.size();

        // If there are none left - explosion (or return for now)
        if (toRespawn == g_particle_count) {
            return;
        }

        // For each survivor, spawn particles depending on their weight
        if (toRespawn > 0) {
            ArrayList<Particle> respawns = new ArrayList<Particle>();
            for (Particle survivor : g_particles) {
                int toSpawn = (int)(survivor.getWeight() * g_particle_count);
                for (int i = 0; i < toSpawn; ++i) {
                    Point spawn_position = survivor.getPosition();
                    if (this.convergedArea != null) {
                        int noise = (int)((float)step_distance_pixels * (Math.random() - 1.0f));
                        spawn_position.x += noise;
                        spawn_position.y += noise;
                    }
                    Particle spawn = new Particle(survivor.getPosition(), 2, 0);
                    respawns.add(spawn);
                }
            }

            // If any particles forgot to get spawned. Scatter them again
            for (int i = 0; i < (toRespawn - respawns.size()); ++i) {
                int randomIndex = (int)(Math.random() * g_particles.size());
                Particle survivor = g_particles.get(randomIndex);
                Particle spawn = new Particle(survivor.getPosition(), 2, 0);
            }

            // Add all respawns
            g_particles.addAll(respawns);
        }

        // Compute the noise to add to the step
        int step_noise = (int)((float)step_distance_pixels * (Math.random() - 1.0f));

        // If converged, add no noise
        if (this.convergedArea != null) {
            step_noise = 0;
        }

        // Compute the direction to step (Up/Down/Left/Right)
        double angle_corrected = global_angle_degrees - global_angle_correction;
        int compass_angle = ((int)angle_corrected + 360) % 360;
        double angle = toNearestNinety360(compass_angle);

        // Move all particles by this distance
        for (Particle p : g_particles) {
            double angle_radians = Math.toRadians(angle);
            int dx = (int)(Math.sin(angle_radians));
            int dy = (int)(-Math.cos(angle_radians));
            int move_x = dx * step_distance_pixels + (dx * step_noise);
            int move_y = dy * step_distance_pixels + (dy * step_noise);
            Point location = new Point(p.getPosition().x + move_x, p.getPosition().y + move_y);
            p.setPosition(location);
        }

        // Sort particles by killed and survivors
        ArrayList<Particle> survivors = new ArrayList<Particle>();
        ArrayList<Particle> killed = new ArrayList<Particle>();
        for (Particle p : g_particles) {
            if (isPointOutOfBounds(p.getPosition())) {
                killed.add(p);
            } else {
                survivors.add(p);
            }
        }

        // Redistribute weight across survivors
        for (Particle p : killed) {
            double piece = p.getWeight() / survivors.size();
            for (Particle s : survivors) {
                s.setWeight(s.getWeight() + piece);
            }
        }

        // If no particles remain, explode them at one of the killed
        if (survivors.size() == 0) {
            System.out.println("EXPLODE!");
            survivors = explode(killed);
        }

        // Set particles as survivors
        this.g_particles = survivors;
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

    // Returns true if a point is out of bounds
    private boolean isPointOutOfBounds (Point p) {
        for (Area a : this.g_areas) {
            if (a.isPointInside(p)) {
                return false;
            }
        }
        return true;
    }

    // Sets the step counter
    private void setGlobalStepCount (int steps) {
        g_total_steps = steps;
        this.stepTextView.setText(String.format("Steps: %d", g_total_steps));
    }


    // [SYNC] Resets all particles, and saves next rotation offset as error
    private void reset () {
        lock.lock();
        this.convergedArea = null;
        this.global_angle_correction = this.global_angle_degrees;
        this.g_particles = this.initParticles(this.g_areas);
        lock.unlock();
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.reset_button) {
            setGlobalStepCount(0);
            reset();
        }
    }


    /*
     *******************************************************************************
     *                          Sensor Interface Methods                           *
     *******************************************************************************
    */


    // Window length
    public static int window_length = 50;

    // Window index (not used for correlator)
    public int window_index;

    // The data window
    float[] window = new float[window_length];

    // [SYNC] Handler for change in sensors
    @Override
    public void onSensorChanged (SensorEvent sensorEvent) {

        switch(sensorEvent.sensor.getType()) {

            // Accelerometer update handler
            case Sensor.TYPE_ACCELEROMETER:

                // Slide the window
//                window[0] = window[1];
//                window[1] = window[2];
//                window[2] = window[3];
//                window[3] = window[4];
//                window[4] = window[5];
//                window[5] = window[6];
//                window[6] = window[7];
//                window[7] = window[8];
//                window[8] = window[9];
//                window[9] = window[10];
//                window[10] = window[11];
//                window[11] = window[12];
//                window[12] = window[13];
//                window[13] = window[14];
//                window[14] = window[15];
//                window[15] = window[16];
//                window[16] = window[17];
//                window[17] = window[18];
//                window[18] = window[19];
//                window[19] = window[20];
//                window[20] = window[21];
//                window[21] = window[22];
//                window[22] = window[23];
//                window[23] = sensorEvent.values[2];

                // Check if we have a step
//                if (Correlator.isStep(window)) {
//                    this.lock.lock();
//                    setGlobalStepCount(g_total_steps + 1);
//                    this.updateModel();
//                    this.checkConvergence();
//                    this.lock.unlock();
//                }

                // Enqueue the next sample.
//                window[window_index] = sensorEvent.values[2];
//
//                // Update the index
//                window_index = (window_index + 1) % window_length;
//
//                // Check if the variance is above limit. Then register step
//                float variance = Correlator.variance(window);
//                System.out.println(variance);
//                if (variance >= 4.0) {
//                    this.lock.lock();
//                    setGlobalStepCount(g_total_steps + 1);
//                    this.updateModel();
//                    this.checkConvergence();
//                    this.lock.unlock();
//                }

                break;

            case Sensor.TYPE_STEP_DETECTOR:
                this.lock.lock();
                setGlobalStepCount(g_total_steps + 1);
                this.updateModel();
                this.checkConvergence();
                this.lock.unlock();
                break;

            // Gyroscope-field update handler
            case Sensor.TYPE_GYROSCOPE:
                //System.out.println(sensorEvent.timestamp - last_timestamp);

                float rate_yaw = sensorEvent.values[2];
                float period = (1.0f/200.0f);
                float dyaw = rate_yaw * period;

                this.lock.lock();
                this.global_angle_degrees += Math.toDegrees(-dyaw);
                updateCompass();
                this.lock.unlock();
                break;
        }
    }

    // Handler for change in sensor accuracy
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // Handler for resuming application context
    @Override
    public void onResume() {
        super.onResume();
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

    }

    // Handler for losing application context
    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    public void swappingOut () {

    }


    /*
     *******************************************************************************
     *                                  Recording                                  *
     *******************************************************************************
    */


    // Exports recorded data
    public void exportAccelerometer (ArrayList<Float> amplitude, ArrayList<Long> timestamps) {
        String filename = "accelerometer_data.txt";
        File file = null;
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStreamWriter = null;

        // Ensure that we can export
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) == false) {
            Log.e("OS", "Environment can't use external storage!");
            return;
        }

        // Open the file
        file = new File(getContext().getExternalFilesDir(null), filename);

        try {
            fileOutputStream = new FileOutputStream(file, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream);

            for (int i = 0; i < amplitude.size(); ++i) {
                String out = String.format("%d %f\n", timestamps.get(i), amplitude.get(i));
                outputStreamWriter.write(out);
            }

            outputStreamWriter.close();
            fileOutputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
