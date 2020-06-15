package com.example.lieu;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;

import java.util.ArrayList;

public class Zone {

    // Integer id of the zone
    protected int id;

    // Name of the zone
    protected String name;

    // Dimensions of the zone
    protected Rect rect;

    // Fill
    protected ShapeDrawable fill;

    // Border
    protected  ShapeDrawable border;

    // Boolean: enabled
    protected boolean enabled;

    // Boolean: requires a redraw
    protected boolean needs_layout = true;

    // Integer: level
    protected int level;

    // Constructor
    public Zone (int id, String name, int level, Rect rect, boolean enabled)
    {
        // Assign fields
        this.id = id;
        this.name = name;
        this.level = level;
        this.rect = rect;
        this.fill = new ShapeDrawable(new RectShape());
        this.border = new ShapeDrawable(new RectShape());
        this.enabled = enabled;

        // Configure appearance
        configure_appearance();
    }

    // Getters

    public int getId () { return this.id; }
    public String getName () { return this.name; }
    public Rect getRect () { return this.rect; }
    public ShapeDrawable getFill () { return this.fill; }
    public ShapeDrawable getBorder () { return this.border; }

    // Setters

    public void setRect (Rect rect)
    {
        this.rect = rect;
        this.needs_layout = true;
    }

    // Internal: Fill rect
    private Rect rectWithMargin (Rect rect, int margin)
    {
        return new Rect(rect.left + margin, rect.top + margin, rect.right - margin,
                rect.bottom - margin);
    }

    // Internal: Create RGBA integer descriptor
    private int argb (int A, int R, int G, int B)
    {
        return (A & 0xff) << 24 | (R & 0xff) << 16 | (G & 0xff) << 8 | (B & 0xff);
    }

    // Internal: Create a rect given starting points and the width and height
    private static Rect rectFrom (int x, int y, int width, int height)
    {
        return new Rect(x, y, x + width, y + height);
    }

    // Internal: Configures the appearance of zones
    private void configure_appearance ()
    {
        int fill_opacity = 255, border_opacity = 255;

        if (enabled == false) {
            fill_opacity = 128;
            border_opacity = 0;
        }

        int fill_color = argb(fill_opacity, 255, 255, 255);
        int border_color = argb(border_opacity, 220, 220, 220);
        this.fill.getPaint().setColor(fill_color);
        this.border.getPaint().setColor(border_color);
    }

    // External: Area of the zone
    public int getArea ()
    {
        return this.rect.width() * this.rect.height();
    }

    // External: Whether or not area contains point
    public boolean containsPoint (Point p)
    {
        return (p.x >= border.getBounds().left  &&
                p.x <= border.getBounds().right &&
                p.y >= border.getBounds().top   &&
                p.y <= border.getBounds().bottom);
    }

    // External: Draw to given canvas
    public void draw (Canvas canvas)
    {

        // Layout if needed
        if (this.needs_layout == true) {

            border.setBounds(this.rect);
            fill.setBounds(rectWithMargin(this.rect, 2));

            this.needs_layout = false;
        }

        // Draw
        border.draw(canvas);
        fill.draw(canvas);
    }

    // Returns true if transition between zones is permitted
    public static boolean adjacentZones (int from, int to)
    {
        int[][] matrix = new int[][]{
                // Row index is the room, columns are whether adjacent
                //  0  1  2  3  4  5  6  7  8  9 10 11 12 13
                { 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
                { 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0 },
                { 0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 },
                { 0, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0 },
                { 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0 },
                { 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0 },
                { 0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 },
                { 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0 },
                { 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0 },
                { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0 },
                { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0 },
                { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 },
                { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1 }
        };

        // Return false if any argument is -1
        if (from == -1 || to == -1) {
            return false;
        }

        return (matrix[from][to] == 1);
    }

    // Get layout for apartment
    public static ArrayList<Zone> getApartmentLayout ()
    {
        ArrayList<Zone> zs = new ArrayList<Zone>();

        // Rects: Rooms
        Rect rz1     = Zone.rectFrom(46, 175, 284, 445);
        Rect rz2     = Zone.rectFrom(358, 472, 310, 313);
        Rect rz2_2   = Zone.rectFrom(533, 774, 135, 98);
        Rect rz3     = Zone.rectFrom(697, 724, 602, 713);
        Rect rz4     = Zone.rectFrom(368, 1144, 300, 293);

        // Rects: Doors
        Rect r_z1_z2 = Zone.rectFrom(321, 473, 47, 147);
        Rect r_z0_z2 = Zone.rectFrom(321, 638, 47, 147);
        Rect r_z2_z3 = Zone.rectFrom(660, 724, 47, 147);
        Rect r_z3_z4 = Zone.rectFrom(660, 1145, 47, 147);

        // Zones:
        zs.add(new Zone(0, "Cell 1", 2, rz1, true));
        zs.add(new Zone(1, "Cell 2", 2, rz2, true));
        zs.add(new Zone(2, "Cell 2", 2, rz2_2, true));
        zs.add(new Zone(3, "Cell 3", 2, rz3, true));
        zs.add(new Zone(4, "Cell 4", 2, rz4, true));
        zs.add(new Zone(5, "Door (1/2)", 2, r_z1_z2, true));
        zs.add(new Zone(6, "Door (0/2)", 2, r_z0_z2, true));
        zs.add(new Zone(7, "Door (2/3)", 2, r_z2_z3, true));
        zs.add(new Zone(8, "Door (3/4)", 2, r_z3_z4, true));

        return zs;
    }

    // Get layout for exterior
    public static ArrayList<Zone> getExteriorLayout ()
    {
        ArrayList<Zone> zs = new ArrayList<Zone>();

        // Rects: Rooms
        Rect rz5 = Zone.rectFrom(46, 1477, 339, 284);
        Rect rz6 = Zone.rectFrom(382, 1477, 339, 284);
        Rect rz7 = Zone.rectFrom(718, 1477, 339, 284);
        Rect rz8 = Zone.rectFrom(1055, 1477, 339, 284);

        // Rect: Stairs
        Rect rz0     = Zone.rectFrom(46, 638, 284, 848);

        // Zones [Stairs]
        zs.add(new Zone(9, "Stairs", 1, rz0, true));

        // Zones [Rooms]
        zs.add(new Zone(10, "Cell 5", 0, rz5, true));
        zs.add(new Zone(11, "Cell 6", 0, rz6, true));
        zs.add(new Zone(12, "Cell 7", 0, rz7, true));
        zs.add(new Zone(13, "Cell 8", 0, rz8, true));

        return zs;
    }

}
