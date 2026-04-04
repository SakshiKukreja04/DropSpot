package com.example.dropspot;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ReminderDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "ReminderDBHelper";
    
    // Database Info
    private static final String DATABASE_NAME = "ReminderDB_29";
    private static final int DATABASE_VERSION = 1;

    // Table Name
    public static final String TABLE_REMINDERS = "reminder_29";

    // Column Names
    public static final String KEY_ID = "id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_TIME = "time";
    public static final String KEY_LOCATION = "location";
    public static final String KEY_STATUS = "status";

    public ReminderDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_REMINDERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TITLE + " TEXT,"
                + KEY_DESCRIPTION + " TEXT,"
                + KEY_TIME + " TEXT,"
                + KEY_LOCATION + " TEXT,"
                + KEY_STATUS + " TEXT"
                + ")";
        db.execSQL(CREATE_TABLE);
        Log.d(TAG, "Database table created: " + TABLE_REMINDERS);
        
        // Optional: Insert dummy data for initial testing
        db.execSQL("INSERT INTO " + TABLE_REMINDERS + " (title, description, status) VALUES ('Initial Task', 'Check Database Inspector', 'Pending')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMINDERS);
        onCreate(db);
    }
}
