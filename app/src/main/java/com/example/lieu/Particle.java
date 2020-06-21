package com.example.lieu;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.util.Log;

import java.util.ArrayList;

public class Particle {

    // Drawable object
    private ShapeDrawable fillShape;

    // Center point
    private Point center;

    // Weight of particle
    private double weight;

    // Radius of particle
    private int radius;

    // Whether or not to show the particle
    private boolean visible = true;

    // ID of the zone the particle was last in
    private int last_zone_id = -1;

    // Constructor with optional color
    public Particle (int zone, Point center, int radius, double weight) {
        this.last_zone_id = zone;
        this.center = center;
        this.radius = radius;
        this.weight = weight;
        this.fillShape = new ShapeDrawable(new OvalShape());
        this.fillShape.getPaint().setColor(Color.MAGENTA);
    }

    // Constructor with optional color
    public Particle (int zone, Point center, int radius, double weight, int color) {
        this.last_zone_id = zone;
        this.center = center;
        this.radius = radius;
        this.weight = weight;
        this.fillShape = new ShapeDrawable(new OvalShape());
        this.fillShape.getPaint().setColor(color);
    }

    // Positions the particle
    private void setBounds () {
        this.fillShape.setBounds(center.x - radius,
                center.y - radius, center.x + radius,
                center.y + radius);
    }

    // Draws the particle
    public void draw (Canvas canvas) {
        if (this.visible) {
            this.setBounds();
            this.fillShape.draw(canvas);
        }
    }

    // Setter for origin
    public void setPosition (Point center) {
        this.center = center;
    }

    // Returns the origin
    public Point getPosition () {
        return this.center;
    }

    // Setter for visibility
    public void setVisible (boolean visible) { this.visible = visible; }

    // Getter for visibility
    public boolean getVisible () { return this.visible; }

    // Getter for the zone ID
    public int get_last_zone_id () { return this.last_zone_id; }

    // Setter for the zone ID
    public void set_last_zone_id (int zone) { this.last_zone_id = zone; }

    // Returns a random point near the particle
    public Point getRandomPointNearby(float radius) {
        float dx = (radius / 2) - (radius * (float)Math.random());
        float dy = (radius / 2) - (radius * (float)Math.random());
        int x = this.center.x + (int)dx;
        int y = this.center.y + (int)dy;
        return new Point(x,y);
    }

    // Setter for the weight
    public void setWeight (double weight) {
        this.weight = weight;
    }

    // Getter for the weight
    public double getWeight () {
        return this.weight;
    }

    // Assist function returning whether given zone belongs to given environment
    private static boolean zoneInEnvironment (Zone zone, AmbientLight.Environment environment)
    {
        switch (environment) {
            case INSIDE: {
                return (zone.getId() >= 0 && zone.getId() < 9);
            }

            case STAIRS: {
                return (zone.getId() == 9);
            }

            case OUTSIDE: {
                return (zone.getId() > 9 && zone.getId() <= 13);
            }
        }

        // Should never arrive here
        return false;
    }

    // Overload
    private static boolean zoneInEnvironment (Zone zone, Barometer.Environment environment)
    {
        switch (environment) {
            case INSIDE: {
                return (zone.getId() >= 0 && zone.getId() < 9);
            }

            case STAIRS: {
                return (zone.getId() == 9);
            }

            case OUTSIDE: {
                return (zone.getId() > 9 && zone.getId() <= 13);
            }
        }

        // Undecided
        return false;
    }

    // Update particle positions based on an observation
    public static ArrayList<Particle> resample (boolean ambient_light_ready, boolean barometer_ready, Barometer.Environment baro_env,
                                                AmbientLight.Environment light_env,
                                                double spawn_noise_radius,
                                                ArrayList<Particle> priors,
                                                ArrayList<Zone> zones)
    {
        int number_living_particles = 0;
        double normalization_constant = 0.0, cumulative_distribution_value = 0.0;
        double new_weight = (1.0 / priors.size());
        ArrayList<Double> distribution = new ArrayList<Double>(priors.size());
        ArrayList<Particle> resampled  = new ArrayList<Particle>(priors.size());

        // Update particle probabilities: (whether they are in the zone or not)
        for (Particle p : priors) {
            double obs = 0.0;
            for (Zone z : zones) {

                // Set probability to 1 if it exists
                if (z.containsPoint(p.getPosition())) {
                    obs = 1.0;
                    number_living_particles++;
                } else {
                    continue;
                }

                // If using ambient light
                if (ambient_light_ready == true) {

                    // If particle environment doesn't match then decrease by .25
                    if (Particle.zoneInEnvironment(z, light_env) == false) {
                        obs = Math.max(obs - 0.10, 0.0);
                    }
                }

                // If using ambient light
                if (barometer_ready == true) {

                    // If particle environment doesn't match then decrease by .25
                    if (Particle.zoneInEnvironment(z, baro_env) == false) {
                        obs = Math.max(obs - 0.10, 0.0);
                    }
                }
            }
            p.setWeight(p.getWeight() * obs);
            normalization_constant += p.getWeight();
        }

        // If no living particles, return NULL
        if (number_living_particles == 0) {
            return null;
        }

        // Re-normalize particle weights and built cumulative distribution
        for (int i = 0; i < priors.size(); ++i) {
            Particle p = priors.get(i);
            p.setWeight(p.getWeight() / normalization_constant);
            cumulative_distribution_value += p.getWeight();
            distribution.add(cumulative_distribution_value);
        }

        // Create a new distribution with just as many particles
        for (int i = 0; i < priors.size(); ++i) {
            double value = Math.random();
            int j;

            // Find slot it belongs to (cannot be zero)
            for (j = 1; j < (priors.size() - 1); ++j) {
                if (value >= distribution.get(j-1) && value < distribution.get(j)) {
                    break;
                }
            }

            // Extract parent particle
            Particle parent = priors.get(j);

            // Offset: generate some random offset
            double noise_x = (Math.random() * spawn_noise_radius) - (spawn_noise_radius / 2);
            double noise_y = (Math.random() * spawn_noise_radius) - (spawn_noise_radius / 2);

            // Compute the new point
            Point origin = new Point((int)(parent.getPosition().x + noise_x),
                    (int)(parent.getPosition().y + noise_y));

            // Respawn particle with reset weights, in approximate location of previous
            Particle next =
                    new Particle(parent.get_last_zone_id(), origin, parent.radius, new_weight, Color.MAGENTA);

            resampled.add(next);
        }

        return resampled;
    }
}