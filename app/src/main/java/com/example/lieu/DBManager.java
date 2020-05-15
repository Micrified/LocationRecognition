package com.example.lieu;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DBManager {

    private DataBaseHelper dbHelper;

    private Context context;

    private SQLiteDatabase database;

    public DBManager (Context context) {
        this.context = context;
    }

    public DBManager open() throws SQLException {
        dbHelper = new DataBaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    // Closes the database
    public void close () {
        dbHelper.close();
    }

    // Inserts a new entry into the database
    public void insert (int cell_id, int iteration, String bssid, float rss) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DataBaseHelper.CELL, cell_id);
        contentValue.put(DataBaseHelper.ITERATION, iteration);
        contentValue.put(DataBaseHelper.BSSID, bssid);
        contentValue.put(DataBaseHelper.RSS, rss);
        database.replace(DataBaseHelper.TABLE_NAME, null, contentValue);
        //database.insert(DataBaseHelper.TABLE_NAME, null, contentValue);
    }

    // Extracts all entries from the database
    public Cursor fetchCell (int cell_id) {
        String[] columns = new String[] {
                DataBaseHelper.CELL,
                DataBaseHelper.ITERATION,
                DataBaseHelper.BSSID,
                DataBaseHelper.RSS
        };
        String condition = DataBaseHelper.CELL + "=?";
        String[] arguments = new String[]{cell_id + ""};

        Cursor c = database.query(DataBaseHelper.TABLE_NAME, columns, condition, arguments, null, null, null);
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    // Deletes the entire database
    public void deleteAll () {
        database.execSQL("DELETE FROM " + DataBaseHelper.TABLE_NAME);
    }
}
