package com.example.lieu;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;

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

    // Constructor with optional color
    public Particle (Point center, int radius, double weight) {
        this.center = center;
        this.radius = radius;
        this.weight = weight;
        this.fillShape = new ShapeDrawable(new OvalShape());
        this.fillShape.getPaint().setColor(Color.RED);
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
}
