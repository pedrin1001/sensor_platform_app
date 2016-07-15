package com.pedro.sensorplatform;

import android.content.ContentValues;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by pedro on 15.07.16.
 */
public class SensorData {
    private ContentValues mValues;
    SensorData(String[] sensorObjs) {
        mValues = new ContentValues();
        for (String obj : sensorObjs) {
            obj = obj.replaceAll("\\s+", "");
            String[] sensor = obj.split(":");
            int index = SensorDB.getColumns().indexOf(sensor[0]);
            if (index == -1) {
                Log.i("parsingERROR", String.valueOf(sensor[0].length()));
            } else {
                int value = Integer.parseInt(sensor[1]);
//                            Log.i("parsing", "idx: " + mSensorDB.getColumns().get(index) + " value: " + value);
                mValues.put(SensorDB.getColumns().get(index), value);
            }
        }
    }

    public ContentValues getValues() {
        return mValues;
    }

    public void addLocation(double lat, double lon) {
        mValues.put(SensorDB.LAT, lat);
        mValues.put(SensorDB.LON, lon);
    }
}
