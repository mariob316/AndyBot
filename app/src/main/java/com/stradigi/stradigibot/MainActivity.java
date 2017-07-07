package com.stradigi.stradigibot;

import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.LifecycleRegistryOwner;
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

public class MainActivity extends AppCompatActivity
    implements SensorEventListener, LifecycleRegistryOwner {

  //adb shell am broadcast -a com.stradigi.stradigibot.SHUTDOWN

  private static final String TAG = MainActivity.class.getSimpleName();

  private SensorManager mSensorManager;
  private SensorManager.DynamicSensorCallback dynamicSensorCallback;

  private Robot robot;
  private BroadcastReceiver shutdownReceiver;
  private LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

  private BluetoothController bluetoothController;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);


    registerSensorListeners();
    robot = new Robot(getLifecycle());

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
  }

  @Override protected void onStart() {
    super.onStart();
    LocalBroadcastManager.getInstance(this)
        .registerReceiver(shutdownReceiver, new IntentFilter(SHUTDOWN_ACTION));
  }

  @Override protected void onStop() {
    super.onStop();
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
    robot.handleProximitySensorData(event);
  }

  @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
    Log.i("Accuracy", String.valueOf(accuracy));
  }

  @Override public LifecycleRegistry getLifecycle() {
    return lifecycleRegistry;
  }
}
