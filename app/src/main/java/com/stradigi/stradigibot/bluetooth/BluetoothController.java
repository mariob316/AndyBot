package com.stradigi.stradigibot.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import java.util.Objects;

/**
 * Created by O1 on 2017-04-27.
 */

public class BluetoothController {

  private Context context;
  private BluetoothAdapter bluetoothAdapter;
  private BluetoothProfile bluetoothProfile;


  public BluetoothController(Context context) {
    this.context = context;
    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
  }

  public void startScan() {

    context.registerReceiver(mAdapterStateChangeReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    context.registerReceiver(mSinkProfileStateChangeReceiver, new IntentFilter(BluetoothHelper.ACTION_CONNECTION_STATE_CHANGED));

    if (bluetoothAdapter != null) {
      if (bluetoothAdapter.isEnabled()) {
        discover();
      } else {
        Log.d("BLE", "**** Not Enabled ***");
        bluetoothAdapter.enable();
      }
    }
  }

  private void enableDiscoverable() {
    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 250);
    context.startActivity(discoverableIntent);
  }

  private void discover() {
    bluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
      @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
        bluetoothProfile = proxy;
        enableDiscoverable();
      }

      @Override public void onServiceDisconnected(int profile) {
      }
    }, 4);
  }

  public void close() {
    bluetoothAdapter.closeProfileProxy(4, bluetoothProfile);

    //bluetoothAdapter.cancelDiscovery();
    context.unregisterReceiver(mSinkProfileStateChangeReceiver);
    context.unregisterReceiver(mAdapterStateChangeReceiver);
  }

  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
        Log.d("BLE", "Starting Scan!!");
      } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
        Log.d("BLE", "Disconnected");
      } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
          Log.d("BLE", "Connected to: " + device.getName());
        }
      }
    }
  };

  private final BroadcastReceiver mAdapterStateChangeReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      int oldState = BluetoothHelper.getPreviousAdapterState(intent);
      int newState = BluetoothHelper.getCurrentAdapterState(intent);
      Log.d("BluetoohController",
          "Bluetooth Adapter changing state from " + oldState + " to " + newState);
      if (newState == BluetoothAdapter.STATE_ON) {
        Log.i("BluetoohController", "Bluetooth Adapter is ready");
        discover();
      }
    }
  };

  private final BroadcastReceiver mSinkProfileStateChangeReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(BluetoothHelper.ACTION_CONNECTION_STATE_CHANGED)) {
        int oldState = BluetoothHelper.getPreviousProfileState(intent);
        int newState = BluetoothHelper.getCurrentProfileState(intent);
        BluetoothDevice device = BluetoothHelper.getDevice(intent);
        Log.d("Bluetooth Controller", "Bluetooth A2DP sink changing connection state from " + oldState +
            " to " + newState + " device " + device);
        if (device != null) {
          String deviceName = Objects.toString(device.getName(), "a device");
          if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d("Bluetooth Controller", "Connected to " + deviceName);
          } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d("Bluetooth Controller", "Disconnected from " + deviceName);
          }
        }
      }
    }
  };
}
