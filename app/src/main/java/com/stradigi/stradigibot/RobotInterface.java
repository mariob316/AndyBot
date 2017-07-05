package com.stradigi.stradigibot;

import android.support.annotation.IntRange;

/**
 * Created by Mario on 2017-04-12.
 */

public interface RobotInterface{

  void forward(@IntRange(from = 0, to = 255) int speed);

  void backward(@IntRange(from = 0, to = 255) int speed);

  void left(@IntRange(from = 0, to = 255) int speed);

  void right(@IntRange(from = 0, to = 255) int speed);

  void stop();

  void reduceSpeed();

  void turnLeftByDegrees(int degrees);

}
