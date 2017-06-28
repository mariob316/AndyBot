package com.stradigi.stradigibot;

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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.stradigi.stradigibot.bluetooth.BluetoothController;
import com.stradigi.stradigibot.sensors.hcsr04.HCSR04Driver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.stradigi.stradigibot.RobotShutdownReceiver.SHUTDOWN_ACTION;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

  //adb shell am broadcast -a com.stradigi.stradigibot.SHUTDOWN

  private static final String TAG = MainActivity.class.getSimpleName();

  private SensorManager mSensorManager;
  private SensorManager.DynamicSensorCallback dynamicSensorCallback;

  private HCSR04Driver mHCSR04DriverFront = null;
  private HCSR04Driver mHCSR04DriverRight = null;
  private HCSR04Driver mHCSR04DriverLeft = null;

  private static final int DISTANCE_VALUE = 0;
  private static final int MAX_DISTANCE_FROM_OBJ = 20;
  private static final int SAFE_DISTANCE_TO_OBJ = 60;

  private Robot robot;

  private BroadcastReceiver shutdownReceiver;

  private float frontSensorDistance;
  private float leftSensorDistance;
  private float rightSensorDistance;

  private List<Float> lastFrontDistances = new LinkedList<>();
  private List<Float> lastLeftDistances = new LinkedList<>();
  private List<Float> lastRightDistances = new LinkedList<>();

  private BluetoothController bluetoothController;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    robot = new Robot();
    robot.start();

    mHCSR04DriverFront =
        new HCSR04Driver(BoardDefaults.HCSR04_FRONT_TRIGGER, BoardDefaults.HCSR04_FRONT_ECHO,
            "HCSR-FRONT", new HCSR04Driver.SimpleEchoFilter());

    //mHCSR04DriverRight =
    //    new HCSR04Driver(BoardDefaults.HCSR04_RIGHT_TRIGGER, BoardDefaults.HCSR04_RIGHT_ECHO,
    //        "HCSR-RIGHT", null);
    //
    //mHCSR04DriverLeft =
    //    new HCSR04Driver(BoardDefaults.HCSR04_LEFT_TRIGGER, BoardDefaults.HCSR04_LEFT_ECHO,
    //        "HCSR-LEFT", null);

    shutdownReceiver = new BroadcastReceiver() {
      @Override public void onReceive(Context context, Intent intent) {
        if (SHUTDOWN_ACTION.equalsIgnoreCase(intent.getAction())) {
          MainActivity.this.finish();
        }
      }
    };

    registerSensorListeners();

    //bluetoothController = new BluetoothController(this);
    //bluetoothController.startScan();
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override protected void onResume() {
    super.onResume();
    robot.forward();
    registerSensors();
  }

  @Override protected void onPause() {
    unregister();
    super.onPause();
  }

  @Override protected void onStart() {
    super.onStart();
    LocalBroadcastManager.getInstance(this)
        .registerReceiver(shutdownReceiver, new IntentFilter(SHUTDOWN_ACTION));
  }

  @Override protected void onStop() {
    super.onStop();
    robot.shutDown();
    Log.i(TAG, "********* ONSTOP ************");
    if (shutdownReceiver != null) {
      LocalBroadcastManager.getInstance(this).unregisterReceiver(shutdownReceiver);
      shutdownReceiver = null;
    }

    mSensorManager.unregisterDynamicSensorCallback(dynamicSensorCallback);
    //bluetoothController.close();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    Log.i(TAG, "********* ONDESTROY ************");
  }

  private void registerSensors() {
    try {
      mHCSR04DriverFront.registerSensor();
      //mHCSR04DriverRight.registerSensor();
      //mHCSR04DriverLeft.registerSensor();
    } catch (IOException e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  public void unregister() {
    try {
      mHCSR04DriverFront.unregisterSensor();
      //mHCSR04DriverRight.unregisterSensor();
      //mHCSR04DriverLeft.unregisterSensor();
    } catch (IOException e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  private void registerSensorListeners() {
    mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    dynamicSensorCallback = createDynamicSensorCallback(mSensorManager);
    mSensorManager.registerDynamicSensorCallback(dynamicSensorCallback);
  }

  private SensorManager.DynamicSensorCallback createDynamicSensorCallback(
      final SensorManager sensorManager) {
    return new SensorManager.DynamicSensorCallback() {
      @Override public void onDynamicSensorConnected(Sensor sensor) {
        super.onDynamicSensorConnected(sensor);
        if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
          sensorManager.registerListener(MainActivity.this, sensor,
              SensorManager.SENSOR_DELAY_GAME);
        }
      }

      @Override public void onDynamicSensorDisconnected(Sensor sensor) {
        if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
          sensorManager.unregisterListener(MainActivity.this);
        }
        super.onDynamicSensorDisconnected(sensor);
      }
    };
  }

  @Override public void onSensorChanged(SensorEvent event) {
    if (event.values.length == 0) return;

    switch (event.sensor.getName()) {
      case "HCSR-FRONT":
        parseFrontSensorData(event);
        return;
      case "HCSR-LEFT":
        //parseLeftSensorData(event);
        return;
      case "HCSR-RIGHT":
        //parseRightSensorData(event);
    }
  }

  //// Calculate standard deviation and use as threshold to accept values
  private float getLastValid(List<Float> list) {
    float sum = 0;
    for (float item : list) sum += item;
    float avg = sum / list.size();
    sum = 0;
    for (float item : list) {
      sum += (item - avg) * (item * avg);
    }


    int listSize = list.size();
    float threshold = (float) Math.sqrt((double) (sum / listSize));

    for (int i = (listSize - 1); i >= 0; i--) {
      Float f = list.get(i);
      if (f >= (avg - threshold) && f <= (avg + threshold)) return f;
    }

    return 0;
  }

  private void parseFrontSensorData(SensorEvent event) {
    float currentDistanceToObj = event.values[DISTANCE_VALUE];

    //lastFrontDistances.add(currentDistanceToObj);
    //if (lastFrontDistances.size() > 10) lastFrontDistances.remove(0);
    //this.frontSensorDistance = getLastValid(lastFrontDistances);//  currentDistanceToObj;

    //Log.i(event.sensor.getName(),
    //    "Max Range: " + String.valueOf(maxRange) + " Current Distance: " + String.valueOf(
    //        currentDistanceToObj));

    /**
     * If the value is equal to the maximum range of the sensor, it's safe to assume that there's nothing nearby.
     * Conversely, if it is less than the maximum range, it means that there is something nearby
     */

    if (currentDistanceToObj >= SAFE_DISTANCE_TO_OBJ) {
      //Log.i(event.sensor.getName(), " Current Distance: " + String.valueOf(currentDistanceToObj));
      //Robot go forward!
      robot.forward();

    } else if (currentDistanceToObj <= SAFE_DISTANCE_TO_OBJ
        && currentDistanceToObj > MAX_DISTANCE_FROM_OBJ) {
     // Log.i(event.sensor.getName(), " Reducing Speed: " + String.valueOf(currentDistanceToObj));
      robot.reduceSpeed(); //reduces the speed up to Zero
    } else {
      //Log.i(event.sensor.getName(), " Stopped: " + String.valueOf(currentDistanceToObj));
      //robot move backward
      //Should have some logic to turn left or right..
     // robot.turnLeft(180);
      robot.turnLeft(90);
    }

    //if (currentDistanceToObj <= maxRange) {
    //  //Robot go forward!
    //  robot.forward();
    //}
    //
    //if (currentDistanceToObj <= MAX_DISTANCE_FROM_OBJ) {
    //  //robot move backward
    //  //Should have some logic to turn left or right..
    //  robot.stop();
    //}
  }

  private void parseLeftSensorData(SensorEvent event) {
    float maxRange = event.sensor.getMaximumRange();
    float currentDistanceToObj = event.values[DISTANCE_VALUE];

    lastLeftDistances.add(currentDistanceToObj);
    if (lastLeftDistances.size() > 10) lastLeftDistances.remove(0);
    this.leftSensorDistance = getLastValid(lastLeftDistances);//  currentDistanceToObj;

    //Log.i(event.sensor.getName(),
    //    "Max Range: " + String.valueOf(maxRange) + " Current Distance: " + String.valueOf(
    //        currentDistanceToObj));
    //
    ///**
    // * If the value is equal to the maximum range of the sensor, it's safe to assume that there's nothing nearby.
    // * Conversely, if it is less than the maximum range, it means that there is something nearby
    // */
    //if (currentDistanceToObj >= maxRange) {
    //  //Robot go forward!
    //  //robot.forward();
    //} else if (currentDistanceToObj <= MAX_DISTANCE_FROM_OBJ) {
    //  //robot move backward
    //  //Should have some logic to turn left or right..
    //  //robot.backward();
    //}
  }

  private void parseRightSensorData(SensorEvent event) {
    float maxRange = event.sensor.getMaximumRange();
    float currentDistanceToObj = event.values[DISTANCE_VALUE];

    lastRightDistances.add(currentDistanceToObj);
    if (lastRightDistances.size() > 10) lastRightDistances.remove(0);
    this.rightSensorDistance = getLastValid(lastRightDistances);

    //Log.i(event.sensor.getName(),
    //    "Max Range: " + String.valueOf(maxRange) + " Current Distance: " + String.valueOf(
    //        currentDistanceToObj));
    //
    ///**
    // * If the value is equal to the maximum range of the sensor, it's safe to assume that there's nothing nearby.
    // * Conversely, if it is less than the maximum range, it means that there is something nearby
    // */
    //if (currentDistanceToObj >= maxRange) {
    //  //Robot go forward!
    //  //robot.forward();
    //} else if (currentDistanceToObj <= MAX_DISTANCE_FROM_OBJ) {
    //  //robot move backward
    //  //Should have some logic to turn left or right..
    //  //robot.backward();
    //}
  }

  @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
    Log.i("Accuracy", String.valueOf(accuracy));
  }
}
