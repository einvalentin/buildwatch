package com.crowflying.buildwatch.smartwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ExtensionReceiver extends BroadcastReceiver {

	private static final String LOG_TAG = "ExtensionReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LOG_TAG, String.format("onReceive %s", intent.getAction()));
		intent.setClass(context, BuildWatchExtensionService.class);
		context.startService(intent);
	}

}
