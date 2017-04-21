package com.stradigi.stradigibot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.stradigi.stradigibot.sensors.hcsr04.HCSR04Driver;
import java.io.IOException;

import static com.stradigi.stradigibot.RobotShutdownReceiver.SHUTDOWN_ACTION;

public class MainActivity extends Activity implements SensorEventListener {

  private static final String TAG = MainActivity.class.getSimpleName();

  private SensorManager mSensorManager;

  private HCSR04Driver mHCSR04Driver1 = null;
  private HCSR04Driver mHCSR04Driver2 = null;
  private HCSR04Driver mHCSR04Driver3 = null;

  private static final int DISTANCE_VALUE = 0;
  private static final int MAX_DISTANCE_FROM_OBJ = 10;
  private SensorCallback sensorCallback;

  private Robot robot;

  private BroadcastReceiver shutdownReceiver;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

    sensorCallback = new SensorCallback();
    mSensorManager.registerDynamicSensorCallback(sensorCallback);

    //robot = new Robot();
    //robot.start();

    mHCSR04Driver1 = new HCSR04Driver(BoardDefaults.HCSR04_1_TRIGGER, BoardDefaults.HCSR04_1_ECHO, new HCSR04Driver.SimpleEchoFilter());

    shutdownReceiver = new BroadcastReceiver() {
      @Override public void onReceive(Context context, Intent intent) {
        if (SHUTDOWN_ACTION.equalsIgnoreCase(intent.getAction())) {
          MainActivity.this.finish();
        }
      }
    };
  }

  @Override protected void onResume() {
    super.onResume();
    try {
      mHCSR04Driver1.registerSensor();
    } catch (IOException e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  @Override protected void onPause() {
    super.onPause();
    try {
      mHCSR04Driver1.unregisterSensor();
    } catch (IOException e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  @Override protected void onStart() {
    super.onStart();
    LocalBroadcastManager.getInstance(this)
        .registerReceiver(shutdownReceiver, new IntentFilter(SHUTDOWN_ACTION));
  }

  @Override protected void onStop() {
    super.onStop();
   // robot.shutDown();
    Log.i(TAG, "********* ONSTOP ************");
    if (shutdownReceiver != null) {
      LocalBroadcastManager.getInstance(this).unregisterReceiver(shutdownReceiver);
      shutdownReceiver = null;
    }

    mSensorManager.unregisterDynamicSensorCallback(sensorCallback);
    mSensorManager.unregisterListener(this);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    Log.i(TAG, "********* ONDESTROY ************");
  }

  private class SensorCallback extends SensorManager.DynamicSensorCallback {
    @Override public void onDynamicSensorConnected(Sensor sensor) {
      Log.i(TAG, sensor.getName() + " has been connected");
      if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
        mSensorManager.registerListener(MainActivity.this, sensor, SensorManager.SENSOR_DELAY_GAME);
      }
    }

    @Override public void onDynamicSensorDisconnected(Sensor sensor) {
      Log.i(TAG, sensor.getName() + " has been disconnected");
      //mSensorManager.unregisterListener(MainActivity.this);
    }
  }

  @Override public void onSensorChanged(SensorEvent event) {
    if (event.values.length == 0) return;

    float maxRange = event.sensor.getMaximumRange();
    float currentDistanceToObj = event.values[DISTANCE_VALUE];

    Log.i("TEST", "Max Range: " + String.valueOf(maxRange) + " Current Distance: " + String.valueOf(currentDistanceToObj));

    /**
     * If the value is equal to the maximum range of the sensor, it's safe to assume that there's nothing nearby.
     * Conversely, if it is less than the maximum range, it means that there is something nearby
     */
    if (currentDistanceToObj >= maxRange) {
      //Robot go forward!
      //robot.forward();
    } else if (currentDistanceToObj <= MAX_DISTANCE_FROM_OBJ) {
      //robot move backward
      //Should have some logic to turn left or right..
      //robot.backward();
    }


  }

  @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
    Log.i("Accuracy", String.valueOf(accuracy));
  }
}
