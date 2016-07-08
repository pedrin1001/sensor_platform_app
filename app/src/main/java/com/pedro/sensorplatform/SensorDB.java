package com.pedro.sensorplatform;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by pedro on 08.07.16.
 */
public class SensorDB extends SQLiteOpenHelper {
    final static String TABLE_NAME = "sensor_data";
    final static String LAT = "lat";
    final static String LONG = "long";
    final static String MQ7 = "mq7";
    final static String MQ2 = "mq2";
    final static String MQ135 = "mq135";
    final static String TEMP = "temp";
    final static String HUM = "hum";
    final static String TH_IDX = "th_idx";
    final static String _ID = "_id";
    final static String CREATE_CMD =
            "CREATE TABLE if not exists " + TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            LAT + " FLOAT, " +
            LONG + " FLOAT, " +
            MQ7 + " INTEGER, " +
            MQ2 + " INTEGER, " +
            MQ135 + " INTEGER, " +
            TEMP + " INTEGER, " +
            HUM + " INTEGER, " +
            TH_IDX + " INTEGER)";


    final static String NAME = "SensorDB";
    final static Integer VERSION = 1;
    final private Context mContext;
    private SQLiteDatabase mDb;
    private SensorDB mDbHelper;

    public SensorDB(Context context) {
        super(context, NAME, null, VERSION);
        this.mContext = context;
    }

    public SensorDB open() throws SQLException {
        mDbHelper = new SensorDB(mContext);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CMD);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
