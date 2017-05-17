package com.stradigi.stradigibot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by Mario Bruno on 2017-04-19.
 */

public class RobotShutdownReceiver extends BroadcastReceiver {

  public static final String SHUTDOWN_ACTION = "com.stradigi.stradigibot.SHUTDOWN";


  @Override public void onReceive(Context context, Intent intent) {
    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(SHUTDOWN_ACTION));
  }
}
