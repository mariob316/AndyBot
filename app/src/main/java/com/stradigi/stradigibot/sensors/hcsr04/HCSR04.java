/*
 * Copyright 2017 Holger Schmidt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stradigi.stradigibot.sensors.hcsr04;

import android.util.Log;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class HCSR04 implements AutoCloseable {

  private static final String TAG = HCSR04.class.getSimpleName();

  private Gpio mTriggerPin;
  private Gpio mEchoPin;

  private Timer mTimer;

  private float mDistance;
  private long mTriggerTime;

  public HCSR04(String triggerPin, String echoPin) throws IOException {

    PeripheralManagerService pioService = new PeripheralManagerService();

    mTriggerPin = pioService.openGpio(triggerPin);
    mTriggerPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
    mTriggerPin.setActiveType(Gpio.ACTIVE_HIGH);

    mEchoPin = pioService.openGpio(echoPin);
    mEchoPin.setDirection(Gpio.DIRECTION_IN);
    mEchoPin.setActiveType(Gpio.ACTIVE_HIGH);

    mEchoPin.setEdgeTriggerType(Gpio.EDGE_BOTH);
    mEchoPin.registerGpioCallback(mGpioCallback);

    Resume();
  }

  @Override public void close() throws IOException {

    mTimer.cancel();

    mTriggerPin.close();
    mTriggerPin = null;

    mEchoPin.close();
    mEchoPin = null;
  }

  public void Pause() throws IOException {

    if (mTimer != null) {
      mTimer.cancel();
    }

    mTimer = null;
  }

  int keepBusy = 0;
  long time1;
  long time2;

  protected void readDistanceSync() throws IOException, InterruptedException {
    // Just to be sure, set the trigger first to false
    mTriggerPin.setValue(false);
    Thread.sleep(0, 2000);

    // Hold the trigger pin HIGH for at least 10 us
    mTriggerPin.setValue(true);
    Thread.sleep(0, 10000); //10 microsec

    // Reset the trigger pin
    mTriggerPin.setValue(false);

    // Wait for pulse on ECHO pin
    while (mEchoPin.getValue() == false) {
      //long t1 = System.nanoTime();
      //Log.d(TAG, "Echo has not arrived...");

      // keep the while loop busy
      keepBusy = 0;

      //long t2 = System.nanoTime();
      //Log.d(TAG, "diff 1: " + (t2-t1));
    }
    time1 = System.nanoTime();
    Log.i(TAG, "Echo ARRIVED!");

    // Wait for the end of the pulse on the ECHO pin
    while (mEchoPin.getValue() == true) {
      //long t1 = System.nanoTime();
      //Log.d(TAG, "Echo is still coming...");

      // keep the while loop busy
      keepBusy = 1;

      //long t2 = System.nanoTime();
      //Log.d(TAG, "diff 2: " + (t2-t1));
    }
    time2 = System.nanoTime();
    Log.i(TAG, "Echo ENDED!");

    // Measure how long the echo pin was held high (pulse width)
    long pulseWidth = time2 - time1;

    // Calculate distance in centimeters. The constants
    // are coming from the datasheet, and calculated from the assumed speed
    // of sound in air at sea level (~340 m/s).
    double distance = (pulseWidth / 1000.0) / 58.23; //cm

    // or we could calculate it withe the speed of the sound:
    //double distance = (pulseWidth / 1000000000.0) * 340.0 / 2.0 * 100.0;
    if (mOnDistanceReading != null) {
      mOnDistanceReading.OnDistance((float) distance);
    }
    Log.i(TAG, "distance: " + distance + " cm");
  }

  public void Resume() {

    if (mTimer != null) {
      mTimer.cancel();
    }

    mTriggerTime = System.nanoTime();

    mTimer = new Timer();
    mTimer.schedule(new TimerTask() {
      @Override public void run() {
        // trigger();
        try {
          readDistanceSync();
        } catch (Exception ex) {
          Log.d(TAG, "io exception:" + ex.getMessage());
        }
      }
    }, 0, 300);
  }

  public interface OnDistanceListener {
    void OnDistance(float distance);
  }

  private OnDistanceListener mOnDistanceReading;

  public void SetOnDistanceListener(OnDistanceListener onDistance) {
    mOnDistanceReading = onDistance;
  }

  public float GetDistance() {
    return mDistance;
  }

  private Runnable mTriggerRunnable = new Runnable() {
    @Override public void run() {
      try {
        mTriggerPin.setValue(true);
        Thread.sleep(0, 10 * 1000);  //10 µs pulse
        mTriggerPin.setValue(false);
      } catch (IOException e) {
        Log.e(TAG, e.getMessage(), e);
      } catch (InterruptedException e) {
        Log.e(TAG, e.getMessage(), e);
      } catch (NullPointerException e) {
        Log.e(TAG, e.getMessage(), e);
      }
    }
  };

  private void trigger() {
    mTriggerRunnable.run();
  }

  private GpioCallback mGpioCallback = new GpioCallback() {
    @Override public boolean onGpioEdge(Gpio gpio) {

      boolean rising = false;
      long nano = System.nanoTime();
      try {
        rising = gpio.getValue();
      } catch (IOException e) {
        Log.e(TAG, e.getMessage(), e);
      }

      if (rising) {
        mTriggerTime = nano;
      } else {
        echo(nano - mTriggerTime);
      }

      return super.onGpioEdge(gpio);
    }
  };

  //echoTime in [ns]
  private void echo(long peakTime) {

    float echoDelay = peakTime / 1000000000.0f; // [nsec] -> [sec]
    float soundVelocity = 343.2f; // [m/sec] TODO: adjust for temperature
    float meter = echoDelay * soundVelocity / 2; // [m], (roundtrip -> /2)

    //valid senor results
    if (meter >= 0.1f && meter <= 4.0f) {
      float centimeter = meter * 100.f; //[cm]
      mDistance = centimeter;

      if (mOnDistanceReading != null) {
        mOnDistanceReading.OnDistance(mDistance);
      }
    }
  }
}