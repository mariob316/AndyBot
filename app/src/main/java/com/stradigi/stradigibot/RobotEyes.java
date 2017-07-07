package com.stradigi.stradigibot;

import android.hardware.SensorEvent;
import android.util.Log;
import com.stradigi.stradigibot.sensors.hcsr04.HCSR04Driver;
import java.io.IOException;

/**
 * Created by O1 on 2017-07-04.
 */

public class RobotEyes {

  public static final String TAG = RobotEyes.class.getSimpleName();

  private HCSR04Driver mHCSR04DriverFront = null;
  private HCSR04Driver mHCSR04DriverRight = null;
  private HCSR04Driver mHCSR04DriverLeft = null;

  private RobotInterface robotInterface;

  public RobotEyes(RobotInterface listener) {
    this.robotInterface = listener;
    open();
  }

  private void open() {
    mHCSR04DriverFront =
        new HCSR04Driver(BoardDefaults.HCSR04_FRONT_TRIGGER, BoardDefaults.HCSR04_FRONT_ECHO,
            "HCSR-FRONT", new HCSR04Driver.SimpleEchoFilter());

    mHCSR04DriverRight =
        new HCSR04Driver(BoardDefaults.HCSR04_RIGHT_TRIGGER, BoardDefaults.HCSR04_RIGHT_ECHO,
            "HCSR-RIGHT", null);

    mHCSR04DriverLeft =
        new HCSR04Driver(BoardDefaults.HCSR04_LEFT_TRIGGER, BoardDefaults.HCSR04_LEFT_ECHO,
            "HCSR-LEFT", null);

    try {
      mHCSR04DriverFront.registerSensor();
      mHCSR04DriverRight.registerSensor();
      mHCSR04DriverLeft.registerSensor();
    } catch (IOException e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  public void close() {
    try {
      mHCSR04DriverFront.unregisterSensor();
      mHCSR04DriverRight.unregisterSensor();
      mHCSR04DriverLeft.unregisterSensor();
    } catch (IOException e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  public void watch(SensorEvent event) {
    switch (event.sensor.getName()) {
      case BoardDefaults.HCSR04_FRONT_NAME:
        parseFrontSensorData(event);
        return;
      case BoardDefaults.HCSR04_LEFT_NAME:
        parseLeftSensorData(event);
        return;
      case BoardDefaults.HCSR04_RIGHT_NAME:
        parseRightSensorData(event);
    }
  }

  private void parseFrontSensorData(SensorEvent event) {
    float currentDistanceToObj = event.values[Robot.DISTANCE_VALUE];

    if (currentDistanceToObj >= Robot.SAFE_DISTANCE_TO_OBJ) {
      Log.i(event.sensor.getName(), " Current Distance: " + String.valueOf(currentDistanceToObj));
      //Robot go forward!
      robotInterface.forward(Robot.DEFAULT_SPEED);
    } else if (currentDistanceToObj <= Robot.SAFE_DISTANCE_TO_OBJ
        && currentDistanceToObj > Robot.MAX_DISTANCE_FROM_OBJ) {
      // Log.i(event.sensor.getName(), " Reducing Speed: " + String.valueOf(currentDistanceToObj));
      robotInterface.reduceSpeed(); //reduces the speed up to Zero
    } else {
      //Log.i(event.sensor.getName(), " Stopped: " + String.valueOf(currentDistanceToObj));
      robotInterface.turnLeftByDegrees(90);
    }
  }

  private void parseLeftSensorData(SensorEvent event) {
    float maxRange = event.sensor.getMaximumRange();
    float currentDistanceToObj = event.values[Robot.DISTANCE_VALUE];
    Log.i(event.sensor.getName(), " Current Distance: " + String.valueOf(currentDistanceToObj));
  }

  private void parseRightSensorData(SensorEvent event) {
    float maxRange = event.sensor.getMaximumRange();
    float currentDistanceToObj = event.values[Robot.DISTANCE_VALUE];
    Log.i(event.sensor.getName(), " Current Distance: " + String.valueOf(currentDistanceToObj));
  }
}
