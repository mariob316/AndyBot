package com.stradigi.stradigibot.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by O1 on 2017-04-27.
 */

public class BluetoothHelper {
  private static final String TAG = "A2DPSinkHelper";


  public static final String ACTION_CONNECTION_STATE_CHANGED =
      "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED";


  public static int getPreviousAdapterState(Intent intent) {
    return intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
  }

  public static int getCurrentAdapterState(Intent intent) {
    return intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
  }

  public static int getPreviousProfileState(Intent intent) {
    return intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1);
  }

  public static int getCurrentProfileState(Intent intent) {
    return intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
  }

  public static BluetoothDevice getDevice(Intent intent) {
    return intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
  }

  /**
   * Provides a way to call the disconnect method in the BluetoothA2dpSink class that is
   * currently hidden from the public API. Avoid relying on this for production level code, since
   * hidden code in the API is subject to change.
   *
   * @param profile
   * @param device
   * @return
   */
  public static boolean disconnect(BluetoothProfile profile, BluetoothDevice device) {
    try {
      Method m = profile.getClass().getMethod("disconnect", BluetoothDevice.class);
      m.invoke(profile, device);
      return true;
    } catch (NoSuchMethodException e) {
      Log.w(TAG, "No disconnect method in the " + profile.getClass().getName() +
          " class, ignoring request.");
      return false;
    } catch (InvocationTargetException | IllegalAccessException e) {
      Log.w(TAG, "Could not execute method 'disconnect' in profile " +
          profile.getClass().getName() + ", ignoring request.", e);
      return false;
    }
  }

}
