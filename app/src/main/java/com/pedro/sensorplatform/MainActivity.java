package com.pedro.sensorplatform;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final String HC_06 = "HC-06";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MESSAGE_READ = 2;
    private TextView temp, infla, connection, showLoc;
    private Button exportButton;
    public BluetoothAdapter mBluetoothAdapter;
    public Handler mHandler;
    private BluetoothDevice mDevice;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private static int counter = 0;
    private SensorDB mSensorDB;
    private SensorData currentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeWidgets();
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getApplicationContext();
                Cursor c = mSensorDB.getAllEntries();
                Log.i("db", String.valueOf(c.getColumnCount()));
                Log.i("db", String.valueOf(c.getCount()));
                Log.i("exporting", "clicked");
                String fileName = SensorDB.TABLE_NAME + ".csv";
                ExportToCSV exportThread = new ExportToCSV(c, fileName, context, true);
                exportThread.start();
                Toast.makeText(context, "exported!", Toast.LENGTH_SHORT);
            }
        });
        startLocation();
        initDatabase();
        checkBluetooth();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_READ) {
                    temp.setText("message read " + counter++);
                    byte[] writeBuf = (byte[]) msg.obj;
                    int begin = msg.arg1;
                    int end = msg.arg2;
                    String writeMessage = new String(writeBuf);
                    writeMessage = writeMessage.substring(begin, end);
                    String[] sensorObjs = writeMessage.split(",");
                    infla.setText(writeMessage.replaceAll("\\s+", ""));
                    currentData = new SensorData((sensorObjs));
                }
            }
        };
    }

    private void initDatabase() {
        mSensorDB = new SensorDB(this);
        mSensorDB.open();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "bluetooth enabled!", Toast.LENGTH_SHORT).show();
                pairWithHC();
            } else {
                Toast.makeText(this, "please turn on bluetooth!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initializeWidgets() {
        temp = (TextView) findViewById(R.id.temperature);
        infla = (TextView) findViewById(R.id.humidity);
        connection = (TextView) findViewById(R.id.CO);
        showLoc = (TextView) findViewById(R.id.location);
        exportButton = (Button) findViewById(R.id.send);
        infla.setBackgroundColor(Color.CYAN);
    }

    private void checkBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            temp.setText("bluetooth not supported");
            finish();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            pairWithHC();
        }
    }

    private void pairWithHC() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(HC_06)) {
                    mDevice = device;
                }
            }
            connection.setText("paired to: " + mDevice.getName()
                    + " [" + mDevice.getAddress() + "]");
            mConnectThread = new ConnectThread(mDevice);
            mConnectThread.start();
        }
    }

    private void makeUseOfNewLocation(Location location) {
        if (currentData != null) {
            showLoc.setText("lat: " + location.getLatitude() + " long:" + location.getLongitude());
            try {
                currentData.addLocation(location.getLatitude(), location.getLongitude());
                mSensorDB.addData(currentData.getValues());
            } catch (SQLException e) {
                Log.getStackTraceString(e);
            }
        }
    }

    private void startLocation() {
        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                makeUseOfNewLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "please activate GPS", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(this, "GPS activated", Toast.LENGTH_SHORT).show();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, locationListener);
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Looper.prepare();
                Log.i("connect thread", "rfcomm exception");
            }
            mmSocket = tmp;
        }
        public void run() {
            Log.i("connect thread", "run");
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            }
            catch (IOException connectException) {
                Log.i("connect thread", "connect exception");
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            Looper.prepare();
            Log.i("connected thread", "run");
            this.write("*".getBytes());
            byte[] buffer = new byte[100];
            int begin = 0;
            int bytes = 0;
            while (true) {
                try {
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    for(int i = begin; i < bytes; i++) {
                        if(buffer[i] == ";".getBytes()[0]) {
                            mHandler.obtainMessage(MESSAGE_READ, begin, i, buffer).sendToTarget();
                            bytes = 0;
                            begin = 0;
                            break;
                        }
                    }
                } catch (IOException e) {
                    Log.i("IOE", e.toString());
                    break;
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch
                    (IOException e) { }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}


