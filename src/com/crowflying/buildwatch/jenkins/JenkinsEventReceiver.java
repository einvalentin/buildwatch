package com.crowflying.buildwatch.jenkins;

import com.crowflying.buildwatch.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class JenkinsEventReceiver extends BroadcastReceiver {

	private static final String LOG_TAG = "JenkinsEventReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (context.getString(R.string.action_jenkins).equals(
				intent.getAction())) {
			handleJenkinsIntent(context, intent);
		}
	}

	private void handleJenkinsIntent(Context context, Intent intent) {
		Log.i(LOG_TAG,
				String.format("Received intent from jenkins: %s", intent));

		
		
	}

}
