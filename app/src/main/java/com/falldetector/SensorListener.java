package com.falldetector;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SensorListener implements SensorEventListener {
    private static final String TAG = "SensorListener";
    private SensorManager mSensorMgr;
    private Context mContext;
    double[] buffer = new double[256];

    public SensorListener(Context context){
        for(int i= 0;i<=255;i++){
            buffer[i] = 9.8;
        }
        mContext = context;
        resume();
    }

    public void resume(){
        mSensorMgr = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorMgr == null){
            throw new UnsupportedOperationException("Sensors not supported");
        }
        boolean supported = false;

        try {
            supported = mSensorMgr.registerListener(this, mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        }catch (Exception e){
            Toast.makeText(mContext, "Shaking not supported", Toast.LENGTH_LONG).show();
        }
        if ((!supported)&&(mSensorMgr != null)) mSensorMgr.unregisterListener(this);
    }

    public void pause() {
        if (mSensorMgr != null) {
            mSensorMgr.unregisterListener(this);
            mSensorMgr = null;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy){}

    public void onSensorChanged(SensorEvent event){
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;

        // Acquire sensor values:
        double x_val = (double)event.values[0];
        double y_val = (double)event.values[1];
        double z_val = (double)event.values[2];
        double acceleration = Math.sqrt(Math.pow(x_val,2) + Math.pow(y_val,2) + Math.pow(z_val,2));
        buffer = addNewData(round(acceleration,1));
    }

    public double[] getBuffer(){
        return buffer;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public double[] addNewData(double newData){
        for(int i = 0;i<=254;i++){
            buffer[i] = buffer[i+1];
        }
        buffer[255] = newData;
        return buffer;
    }
}