package com.crowflying.buildwatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {
	private static final String LOG_TAG = "GCMIntentService";

	@Override
	protected void onError(Context context, String errorId) {
		Log.d(LOG_TAG, String.format("Error %s", errorId));
		Toast.makeText(getApplicationContext(),
				String.format(getString(R.string.fmt_gcm_error), errorId),
				Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.d(LOG_TAG,
				String.format("GCM message received: %s", intent.getAction()));
		// TODO: Handle message, broadcast jenkins originating ones in the app.
	}

	@Override
	protected void onRegistered(Context context, String regId) {
		Log.d(LOG_TAG, String.format("Registered with token %s", regId));
		Toast.makeText(getApplicationContext(), getString(R.string.new_token),
				Toast.LENGTH_SHORT).show();
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
				.edit();
		editor.putString("gcm_token", regId);
		editor.commit();
		// TODO: Send registration to the server.
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		Log.d(LOG_TAG, String.format("Unregistered with token %s", regId));
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
				.edit();
		editor.remove("gcm_token");
		editor.commit();
		// TODO: Send deregistration to the server.
	}

}
