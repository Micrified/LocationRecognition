package com.example.lieu;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;

public class Area {

    protected int area_id;

    protected int width;

    protected int height;

    protected int detect_radius = 30;

    protected Point origin;

    protected int border = 2;

    protected boolean useHeightMargin = false;

    protected boolean needsLayout;

    protected ShapeDrawable fillShape;

    protected ShapeDrawable borderShape;

    private ShapeDrawable radiusShape;

    public Area (int area_id, Point origin, int width, int height) {
        this.area_id = area_id;
        this.width = width;
        this.height = height;
        this.origin = origin;
        this.needsLayout = true;
        this.fillShape = new ShapeDrawable(new RectShape());
        this.borderShape = new ShapeDrawable(new RectShape());
        this.fillShape.getPaint().setColor(Color.DKGRAY);
        this.borderShape.getPaint().setColor(Color.WHITE);

        this.radiusShape = new ShapeDrawable(new OvalShape());
        int A = 128, R = 255, G = 200, B = 200;
        int color = (A & 0xff) << 24 | (R & 0xff) << 16 | (G & 0xff) << 8 | (B & 0xff);
        this.radiusShape.getPaint().setColor(color);

        layout();
    }

    // [DEBUG] Sets the origin again
    public void setOrigin (Point p) {
        this.origin = p;
        this.needsLayout = true;
    }

    // Returns the center point
    public Point getCenter () {
        return new Point(this.origin.x + (width / 2), this.origin.y + (height / 2));
    }

    // Returns the area
    public int getArea () {
        return this.width * this.height;
    }

    // Returns the area ID
    public int getID () {
        return this.area_id;
    }

    // Return width
    public int getWidth () {
        return this.width;
    }

    // Return height
    public int getHeight () {
        return this.height;
    }

    // Return X origin
    public int getX () {
        return this.origin.x;
    }

    // Return Y origin
    public int getY () {
        return this.origin.y;
    }

    // Draws the shape with a bit of a height margin
    public void drawWithHeightMargin (boolean useHeightMargin) {
        this.useHeightMargin = useHeightMargin;
        this.needsLayout = true;
    }

    // Returns true if the point is within the area
    public boolean isPointInside (Point p) {
        return (p.x >= borderShape.getBounds().left &&
                p.x <= borderShape.getBounds().right &&
                p.y >= borderShape.getBounds().top &&
                p.y <= borderShape.getBounds().bottom);
    }

    // Returns the capture radius
    private float getCaptureRadius () {
        return 1.6f * this.detect_radius;
    }

    // Returns true if a particle is inside a radius of size max(width, height) from the origin
    public boolean containsPointWithinRadius (Point p) {
        int mx = origin.x + (width / 2);
        int my = origin.y + (height / 2);
        double euclid = Math.sqrt((mx - p.x) * (mx - p.x) + (my - p.y) * (my - p.y));
        return ((int)euclid < getCaptureRadius());
    }

    // Returns the euclidean distance from a point to the area center
    public double distanceToCenter (Point p) {
        int mx = origin.x + (width / 2);
        int my = origin.y + (height / 2);
        double euclid = Math.sqrt((mx - p.x) * (mx - p.x) + (my - p.y) * (my - p.y));
        return euclid;
    }

    public void layout () {
        int l = origin.x;
        int r = origin.x + width;
        int t = origin.y + (useHeightMargin ? 5 : 0);
        int b = origin.y + height - (useHeightMargin ? 5 : 0);

        int b_l = l - border;
        int b_r = r + border;
        int b_t = t - border;
        int b_b = b + border;

        int rr = (int)getCaptureRadius();
        int mx = origin.x + (width / 2);
        int my = origin.y + (height / 2);
        int ml = mx - rr;
        int mr = mx + rr;
        int mt = my - rr;
        int mb = my + rr;

        borderShape.setBounds(b_l, b_t, b_r, b_b);
        fillShape.setBounds(l, t, r, b);
        radiusShape.setBounds(ml, mt, mr, mb);

    }

    // Draws the border, then the rectangle on the given canvas
    public void draw (Canvas canvas) {

        if (needsLayout) {
            layout();
            needsLayout = false;
        }

        borderShape.draw(canvas);
        fillShape.draw(canvas);
        //radiusShape.draw(canvas);
    }
}
