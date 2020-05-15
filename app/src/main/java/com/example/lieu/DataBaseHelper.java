package com.example.lieu;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBaseHelper extends SQLiteOpenHelper {

    // Table name
    public static final String TABLE_NAME = "RSSDATA";

    // Table columns
    public static final String CELL = "cell";
    public static final String ITERATION = "iteration";
    public static final String BSSID = "bssid";
    public static final String RSS = "rss";

    // Database information
    static final String DB_NAME = "LIEU_TRAINING_RSS.DB";

    // Database version
    static final int DB_VERSION = 1;

    // Table creation query
    private static final String Q_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + "("
            + CELL      + " INTEGER, "
            + ITERATION + " INTEGER, "
            + BSSID     + " TEXT, "
            + RSS       + " REAL, "
            + "PRIMARY KEY (" + CELL + ", " + ITERATION + ")" + ");";




    public DataBaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    // Override to create our table
    @Override
    public void onCreate (SQLiteDatabase db) {
        db.execSQL(Q_CREATE_TABLE);
    }


    // Override for handling upgrade
    @Override
    public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

}
