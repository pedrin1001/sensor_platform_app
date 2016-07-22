package com.pedro.sensorplatform;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by pedro on 21.07.16.
 */
public class ExportToCSV extends Thread {
    private Cursor c;
    private String fileName;
    private Context context;
    private Boolean sdCard;

    public ExportToCSV(Cursor c, String fileName, Context context, Boolean sdCard) {
        this.c = c;
        this.fileName = fileName;
        this.context = context;
        this.sdCard = sdCard;
    }

    public void run() {
        Log.i("exporting", "trying to save");
        int rowCount;
        int colCount;
        FileWriter fw;
        BufferedWriter bfw;
        File saveDir;
        if (sdCard) {
            saveDir = Environment.getExternalStorageDirectory();
        } else {
            saveDir = context.getFilesDir();
        }
        File saveFile = new File(saveDir, fileName);
        try {
            c.moveToFirst();
            rowCount = c.getCount();
            colCount = c.getColumnCount();
            fw = new FileWriter(saveFile);
            bfw = new BufferedWriter(fw);
            if (rowCount > 0) {
                c.moveToFirst();
                for (int i = 0; i < colCount; i++) {
                    if (i != colCount - 1) {
                        try {
                            bfw.write(c.getColumnName(i) + ',');
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            bfw.write(c.getColumnName(i));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                bfw.newLine();
                for (int i = 0; i < rowCount; i++) {
                    c.moveToPosition(i);
                    for (int j = 0; j < colCount; j++) {
                        if (j != colCount - 1) {
                            try {
                                bfw.write(c.getString(j) + ',');
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                bfw.write(c.getString(j));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    bfw.newLine();
                }
            }
            bfw.flush();
            bfw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Log.i("exporting", "saved");
            c.close();
        }
    }
}