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

    // Angle (degrees)
    private double angle;

    // Weight of particle
    private double weight;

    // Radius of particle
    private int radius;

    // Level particle was last in
    private int last_level = -1;

    // Constructor with optional color
    public Particle (Point center, int radius, double weight) {
        this.center = center;
        this.radius = radius;
        this.weight = weight;
        this.fillShape = new ShapeDrawable(new OvalShape());
        this.fillShape.getPaint().setColor(Color.MAGENTA);
    }

    // Constructor with optional color
    public Particle (Point center, int radius, double weight, double angle, int color) {
        this.center = center;
        this.angle = angle;
        this.radius = radius;
        this.weight = weight;
        this.fillShape = new ShapeDrawable(new OvalShape());
        this.fillShape.getPaint().setColor(color);
    }

    // Positions the particle
    private void setBounds () {
        this.fillShape.setBounds(center.x - radius, center.y - radius, center.x + radius, center.y + radius);
    }

    // Draws the particle
    public void draw (Canvas canvas) {
        this.setBounds();
        this.fillShape.draw(canvas);
    }


    public ShapeDrawable getDrawable () {
        return this.fillShape;
    }

    // Moves the particle along its internal angle
    public void move (double units, double xnoise, double ynoise) {
        double angle_radians = Math.toRadians(this.angle);
        double dx = Math.sin(angle_radians);
        double dy = Math.cos(angle_radians);
        if (dx == 0) {
            xnoise = 0;
        }
        if (dy == 0) {
            ynoise = 0;
        }
        this.center.x += (int)(dx * units + xnoise);
        this.center.y -= (int)(dy * units + ynoise);
    }


    // Sets the angle
    public void setAngle (double angle) {
        this.angle = angle;
    }

    // Gets the angle
    public double getAngle () { return this.angle; }

    // Setter for origin
    public void setPosition (Point center) {
        this.center = center;
    }

    // Returns the origin
    public Point getPosition () {
        return this.center;
    }

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


    // Update particle positions based on an observation
    public static ArrayList<Particle> resample (double spawn_noise_radius,
                                                double spawn_angle_fanout,
                                                ArrayList<Particle> priors,
                                                ArrayList<Zone> zones)
    {
        int number_living_particles = 0;
        double normalization_constant = 0.0, cumulative_distribution_value = 0.0;
        double new_weight = (1.0 / priors.size());
        ArrayList<Double> distribution = new ArrayList<Double>(priors.size());
        ArrayList<Particle> resampled = new ArrayList<Particle>(priors.size());

        // Update particle probabilities: (whether they are in the zone or not)
        for (Particle p : priors) {
            double obs = 0.0;
            for (Zone z : zones) {
                if (z.containsPoint(p.getPosition())) {
                    obs = 1.0;
                    number_living_particles++;
                    break;
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

            // Compute adjustment to fanout angle
            double noise_angle = (Math.random() * spawn_angle_fanout) - (spawn_angle_fanout / 2);

            // Respawn particle with reset weights, in approximate location of previous
            Particle next =
                    new Particle(origin, parent.radius, new_weight,
                            parent.getAngle(), Color.MAGENTA);

            resampled.add(next);
        }

        return resampled;
    }
}