/*
 * <Modbus JNI Accelerometer>
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of  MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.fireraven.libmodbus;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class main extends Activity implements SensorEventListener
{
    private  SensorManager mSensorManager;
    private  Sensor mAccelerometer;
    private  Button btnConnect;
    private  Button btnDisconnect;
    private  TextView tvX;
    private  TextView tvY;
    private  TextView tvZ;
    Handler handler;
    private float axisX;
    private float axisY;
    private float axisZ;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        tvX = (TextView) findViewById(R.id.tvX);
        tvY = (TextView) findViewById(R.id.tvY);
        tvZ = (TextView) findViewById(R.id.tvZ);
        btnDisconnect.setEnabled(false);
        handler = new Handler();
    }

    public void Connect(View view) {
        int rc;
        String ip = "192.168.0.6";
        int port = 1502;

        rc = modbusConnect(ip, port);

        if ( rc == -1 ) {
            //Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            error("Connection Error");
        } else {
            handler.post(sendData);
            btnDisconnect.setEnabled(true);
            btnConnect.setEnabled(false);
            btnConnect.setText("Connected");
        }
    }

    public void Disconnect(View view) {
        modbusDisconnect();
        btnConnect.setText("Connect");
        handler.removeCallbacks(sendData);
        btnDisconnect.setEnabled(false);
        btnConnect.setEnabled(true);
        btnConnect.setText("Connect");
        tvX.setText("0.0 m/s2");
        tvY.setText("0.0 m/s2");
        tvZ.setText("0.0 m/s2");
    }

    public native int       modbusConnect(String ip, int port);
    public native int       modbusSendSensor(float axisX, float axisY, float axisZ);
    public native void      modbusDisconnect();

    static {
        System.loadLibrary("modbus-jni");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        axisX = event.values[0];
        axisY = event.values[1];
        axisZ = event.values[2];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void error(String msg){
        btnConnect.setText("Connect");
        handler.removeCallbacks(sendData);
        btnDisconnect.setEnabled(false);
        btnConnect.setEnabled(true);
        btnConnect.setText("Connect");
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private final Runnable sendData = new Runnable() {
        public void run() {
            try {
                modbusSendSensor(axisX, axisY, axisZ);
                tvX.setText(Float.toString(axisX) + " m/s2");
                tvY.setText(Float.toString(axisY) + " m/s2");
                tvZ.setText(Float.toString(axisZ) + " m/s2");
                handler.postDelayed(this, 500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}
