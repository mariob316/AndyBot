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

import android.hardware.Sensor;
import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.UserSensor;
import com.google.android.things.userdriver.UserSensorDriver;
import com.google.android.things.userdriver.UserSensorReading;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class HCSR04Driver implements AutoCloseable {

  private static final String TAG = HCSR04Driver.class.getSimpleName();

  private static final String SENSOR_NAME = "HC-SR04";
  private static final String SENSOR_VENDOR = "Sparkfun";
  private static final int SENSOR_VERSION = 1;
  private static final int SENSOR_MIN_DELAY = 100000; //µsec
  private static final float  SENSOR_MAX_RANGE = 400f; //cm
  private static final float SENSOR_RESOLUTION = 0.5f; //cm

  private DistanceUserDriver mDistanceUserDriver = null;

  private String mTrigger = null;
  private String mEcho = null;
  private String mName = SENSOR_NAME;
  private DistanceFilter mFilter;

  public HCSR04Driver(String gpioTrigger, String gpioEcho, String name, DistanceFilter filter) {
    mTrigger = gpioTrigger;
    mEcho = gpioEcho;
    mFilter = filter;
    mName = name;
  }

  @Override
  public void close() throws IOException {
    unregisterSensor();
  }

  public void registerSensor() throws IOException{
    if (mDistanceUserDriver == null) {
      mDistanceUserDriver = new DistanceUserDriver(mTrigger, mEcho, mFilter);
      UserDriverManager.getManager().registerSensor(mDistanceUserDriver.getUserSensor());
    }
  }

  public void unregisterSensor() throws IOException {
    if (mDistanceUserDriver != null) {
      UserDriverManager.getManager().unregisterSensor(mDistanceUserDriver.getUserSensor());
      mDistanceUserDriver.close();
      mDistanceUserDriver = null;
    }
  }

  private class DistanceUserDriver extends UserSensorDriver {

    private UserSensor mUserSensor;
    private HCSR04 mHCSR04 = null;
    private float mDistance = 0.0f;
    private DistanceFilter mFilter = null;

    public DistanceUserDriver(String gpioTrigger, String gpioEcho, DistanceFilter filter) throws  IOException {
      mFilter = filter;
      mHCSR04 = new HCSR04(gpioTrigger, gpioEcho);
      mHCSR04.SetOnDistanceListener(new HCSR04.OnDistanceListener() {
        @Override
        public void OnDistance(float distance) {
          if(mFilter != null) {
            mDistance = mFilter.filter(distance);
          } else {
            mDistance = distance;
          }
        }
      });
    }

    public void close() throws IOException {
      mHCSR04.close();
    }

    private UserSensor getUserSensor() {
      if (mUserSensor == null) {
        mUserSensor = UserSensor.builder()
            .setType(Sensor.TYPE_PROXIMITY)
            .setName(mName)
            //.setVendor(SENSOR_VENDOR)
            //.setVersion(SENSOR_VERSION)
            .setMaxRange(SENSOR_MAX_RANGE)
            //.setResolution(SENSOR_RESOLUTION)
            //.setMinDelay(SENSOR_MIN_DELAY)
            .setUuid(UUID.randomUUID())
            .setDriver(this)
            .build();
      }
      return mUserSensor;
    }

    @Override
    public void setEnabled(boolean enabled) throws IOException {
      if(enabled){
        mHCSR04.Resume();
      } else {
        mHCSR04.Pause();
      }
    }

    @Override
    public UserSensorReading read() throws IOException {
      return new UserSensorReading(new float[]{ mDistance });
    }
  }

  public interface DistanceFilter {
    float filter(float distance);
  }

  //mixture of 'simple moving median' and 'moving average'
  //by using a moving window and then averaging the values in the middle of the window
  static public class SimpleEchoFilter implements DistanceFilter {

    private static final int FILER_WINDOW_= 3;
    private static final int FILER_UPPER_CUTOFF = 2;
    private static final int FILER_LOWER_CUTOFF = 0;

    private List<Float> distanceEchos = new LinkedList<>();

    public float filter(float value) {
      float filtered = 0.0f;

      if(distanceEchos.size() >= FILER_WINDOW_){
        distanceEchos.remove(0);
      }
      distanceEchos.add(value);

      List<Float> sortedEchos = new ArrayList<>(distanceEchos);
      Collections.sort(sortedEchos);

      if(sortedEchos.size() > (FILER_LOWER_CUTOFF + FILER_UPPER_CUTOFF)) {
        filtered = average(sortedEchos,
            FILER_LOWER_CUTOFF, sortedEchos.size() - FILER_UPPER_CUTOFF);
      } else {
        filtered = average(sortedEchos, 0, sortedEchos.size());
      }

      return filtered;
    }

    private float average(List<Float> echos, int begin, int end) {
      float avg = 0;
      for(int i = begin; i < end; ++i){
        avg += echos.get(i);
      }
      if(end-begin != 0) {
        return avg / (end - begin);
      } else {
        return avg;
      }
    }
  }
}