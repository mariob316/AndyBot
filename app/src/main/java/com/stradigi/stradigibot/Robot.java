package com.stradigi.stradigibot;

import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.util.Log;
import com.zugaldia.adafruit.motorhat.library.AdafruitDCMotor;
import com.zugaldia.adafruit.motorhat.library.AdafruitMotorHat;
import java.lang.annotation.Retention;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Created by Mario on 2017-04-12.
 */

public class Robot implements RobotInterface {

  private final static String TAG = "ROBOT";

  private static final int DEFAULT_SPEED = 200;

  @Retention(SOURCE) @IntDef({ FORWARD, BACKWARD, LEFT, RIGHT, STOP }) private @interface ICommand {
  }

  private static final int STOP = 0;
  private static final int BACKWARD = 1;
  private static final int FORWARD = 2;
  private static final int LEFT = 3;
  private static final int RIGHT = 4;

  private AdafruitMotorHat mh;

  @ICommand private int command = 0;
  private int speed;
  private boolean run = true;
  private static final int PERIOD = 255;

  public Robot() {
    this.mh = new AdafruitMotorHat();
    robotThread.start();
  }

  private int validateSpeed(int speed) {
    return Math.max(0, Math.min(255, speed));
  }

  @Override public synchronized void forward(@IntRange(from = 0, to = 255) int speed) {
    command = FORWARD;
    this.speed = speed;
    setMotorSpeed(speed);
  }

  @Override public synchronized void backward(@IntRange(from = 0, to = 255) int speed) {
    command = BACKWARD;
    this.speed = speed;
    setMotorSpeed(speed);
  }

  @Override public synchronized void left(@IntRange(from = 0, to = 255) int speed) {
    command = LEFT;
    this.speed = speed;
    setMotorSpeed(speed);
  }

  @Override public synchronized void right(@IntRange(from = 0, to = 255) int speed) {
    command = RIGHT;
    this.speed = speed;
    setMotorSpeed(speed);
  }

  @Override public synchronized void stop() {
    command = STOP;
    this.speed = 0;
    setMotorSpeed(0);
  }

  public void shutDown() {
    Log.i(TAG, "!!!!Shutdown!!");
    run = false;
    robotThread.interrupt();
    stop();
    mh.close();
  }

  private AdafruitDCMotor getMotor(@IntRange(from = 1, to = 4) int position) {
    return mh.getMotor(position);
  }

  private void stopAll() {
    for (AdafruitDCMotor motor : mh.getMotors()) {
      motor.run(AdafruitMotorHat.RELEASE);
    }
  }

  private void goForward() {
    for (AdafruitDCMotor motor : mh.getMotors()) {
      motor.run(AdafruitMotorHat.FORWARD);
    }
  }

  private void goBackwards() {
    for (AdafruitDCMotor motor : mh.getMotors()) {
      motor.run(AdafruitMotorHat.BACKWARD);
    }
  }

  private void goLeft() {
    int count = 0;
    for (AdafruitDCMotor motor : mh.getMotors()) {
      motor.run((count % 2 == 0) ? AdafruitMotorHat.BACKWARD : AdafruitMotorHat.FORWARD);
      ++count;
    }
  }

  private void goRight() {
    int count = 0;
    for (AdafruitDCMotor motor : mh.getMotors()) {
      motor.run((count % 2 == 0) ? AdafruitMotorHat.FORWARD : AdafruitMotorHat.BACKWARD);
      ++count;
    }
  }

  private void setMotorSpeed(@IntRange(from = 0, to = 255) int speed) {
    for (AdafruitDCMotor motor : mh.getMotors()) {
      motor.setSpeed(validateSpeed(speed));
    }
  }

  private Thread robotThread = new Thread(new Runnable() {

    @Override public void run() {
      int curSpeed;
      @ICommand int curCmd;
      while (run) {
        curSpeed = speed;
        curCmd = command;
        try {
          if (curSpeed == 0 || curCmd == STOP) {
            stopAll();
            TimeUnit.MICROSECONDS.sleep(PERIOD);
          } else {
            switch (curCmd) {
              case FORWARD:
                goForward();
                break;
              case BACKWARD:
                goBackwards();
                break;
              case LEFT:
                goLeft();
                break;
              case RIGHT:
                goRight();
                break;
              default:
                break;
            }
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  });
}
