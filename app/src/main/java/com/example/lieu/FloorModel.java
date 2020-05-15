package com.example.lieu;

import android.graphics.Point;

import java.util.ArrayList;

public class FloorModel {

    public static float getRoomWidth (float scale) {
        return scale * 6.1f;
    }

    public static float getRoomHeight (float scale) {
        return scale * 4.0f;
    }

    public static float getHallSegmentWidth (float scale) {
        return scale * 2.5f;
    }

    public static float getHallSegmentHeight (float scale) {
        return scale * 4.0f;
    }

    public static float getHallEndSegmentWidth (float scale) {
        return scale * 2.5f;
    }

    public static float getHallEndSegmentHeight (float scale) {
        return scale * 12.0f;
    }

    public static float getCoffeeRoomHallWidth (float scale) {
        return scale * 3.1f;
    }

    public static float getCoffeeRoomHallHeight (float scale) {
        return scale * 1.5f;
    }

    public static float getCoffeeRoomWidth (float scale) {
        return scale * 3.0f;
    }

    public static float getCoffeeRoomHeight (float scale) {
        return scale * 4.0f;
    }

    public static int getWidthForModel (float scale) {
        return (int)(2.0f * getRoomWidth(scale) + getHallSegmentWidth(scale));
    }

    public static int getHeightForModel (float scale) {
        return (int)(10.0f * getHallSegmentHeight(scale) + getHallEndSegmentHeight(scale));
    }

    public static ArrayList<Area> getRooms (float scale, int dx, int dy) {
        ArrayList<Area> areas = new ArrayList<Area>();

        // Left-side rooms
        Area L03 = new Area(3, new Point(dx, dy), (int)getRoomWidth(scale), (int)getRoomHeight(scale));
        Area L11 = new Area(11, new Point(dx, dy + (int)(6.9f * getRoomHeight(scale))), (int)(getRoomWidth(scale)), (int)(getRoomHeight(scale)));
        Area L13 = new Area(13, new Point(dx, dy + (int)(8.0f * getRoomHeight(scale))), (int)(getRoomWidth(scale)), (int)(getRoomHeight(scale)));
        L13.drawWithHeightMargin(true);
        Area L16 = new Area(16, new Point(dx, dy + (int)(9.1f * getRoomHeight(scale))), (int)(getRoomWidth(scale)), (int)(getRoomHeight(scale)));

        // Hallway segments
        int hx = (int)getRoomWidth(scale);
        Area H02 = new Area (2, new Point(dx + hx, dy), (int)getHallSegmentWidth(scale), (int)getHallSegmentHeight(scale));
        Area H04 = new Area (4, new Point(dx + hx, dy + (int)(1.0f * getHallSegmentHeight(scale))), (int)getHallSegmentWidth(scale), (int)getHallSegmentHeight(scale));
        Area H05 = new Area (5, new Point(dx + hx, dy + (int)(2.0f * getHallSegmentHeight(scale))), (int)getHallSegmentWidth(scale), (int)getHallSegmentHeight(scale));
        Area H06 = new Area (6, new Point(dx + hx, dy + (int)(3.0f * getHallSegmentHeight(scale))), (int)getHallSegmentWidth(scale), (int)getHallSegmentHeight(scale));
        Area H07 = new Area (7, new Point(dx + hx, dy + (int)(4.0f * getHallSegmentHeight(scale))), (int)getHallSegmentWidth(scale), (int)getHallSegmentHeight(scale));
        Area H08 = new Area (8, new Point(dx + hx, dy + (int)(5.0f * getHallSegmentHeight(scale))), (int)getHallSegmentWidth(scale), (int)getHallSegmentHeight(scale));
        Area H09 = new Area (9, new Point(dx + hx, dy + (int)(6.0f * getHallSegmentHeight(scale))), (int)getHallSegmentWidth(scale), (int)getHallSegmentHeight(scale));
        Area H10 = new Area (10, new Point(dx + hx, dy + (int)(7.0f * getHallSegmentHeight(scale))), (int)getHallSegmentWidth(scale), (int)getHallSegmentHeight(scale));
        Area H12 = new Area (12, new Point(dx + hx, dy + (int)(8.0f * getHallSegmentHeight(scale))), (int)getHallSegmentWidth(scale), (int)getHallSegmentHeight(scale));
        Area H15 = new Area (15, new Point(dx + hx, dy + (int)(9.0f * getHallSegmentHeight(scale))), (int)getHallSegmentWidth(scale), (int)getHallSegmentHeight(scale));

        // Hallway end segment
        Area H_END = new Area (-1, new Point(dx + hx, dy + (int)(10.0f * getHallSegmentHeight(scale))), (int)getHallEndSegmentWidth(scale), (int)getHallEndSegmentHeight(scale));

        // Right-side rooms
        hx = (int)(getRoomWidth(scale) + getHallSegmentWidth(scale));
        Area R01 = new Area (1, new Point(dx + hx, dy), (int)getRoomWidth(scale), (int)getRoomHeight(scale));

        // Coffee-room connection hall
        Area HCF = new Area (14, new Point(dx + hx, dy + (int)(9.0f * getHallSegmentHeight(scale))), (int)getCoffeeRoomHallWidth(scale), (int)getCoffeeRoomHallHeight(scale));

        // Coffee-room
        hx = (int)(getRoomWidth(scale) + getHallSegmentWidth(scale) + getCoffeeRoomHallWidth(scale));
        Area CF = new Area (14, new Point(dx + hx, dy + (int)(9.0f * getHallSegmentHeight(scale))), (int)getCoffeeRoomWidth(scale), (int)getCoffeeRoomHeight(scale));

        // Add all rooms and return
        areas.add(L03);
        areas.add(L11);
        areas.add(L13);
        areas.add(L16);
        areas.add(H02);
        areas.add(H04);
        areas.add(H05);
        areas.add(H06);
        areas.add(H07);
        areas.add(H08);
        areas.add(H09);
        areas.add(H10);
        areas.add(H12);
        areas.add(H15);
        areas.add(H_END);
        areas.add(R01);
        areas.add(HCF);
        areas.add(CF);

        return areas;
    }

}
