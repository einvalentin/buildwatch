package com.crowflying.buildwatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService {
	private static final String LOG_TAG = "GCMIntentService";

	private Handler handlerOnUIThread;

	private class DisplayToast implements Runnable {
		String mText;

		public DisplayToast(String text) {
			mText = text;
		}

		public void run() {
			Toast.makeText(GCMIntentService.this, mText, Toast.LENGTH_SHORT)
					.show();
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// The looper for this handler will be on the UI thread.
		handlerOnUIThread = new Handler();
	}

	@Override
	protected void onError(Context context, String errorId) {
		Log.d(LOG_TAG, String.format("Error %s", errorId));
		handlerOnUIThread.post(new DisplayToast(String.format(getString(R.string.fmt_gcm_error), errorId)));
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
		handlerOnUIThread.post(new DisplayToast(getString(R.string.new_token)));
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
				.edit();
		editor.putString("gcm_token", regId);
		editor.commit();
		// TODO: Send registration to the server.
		GCMRegistrar.setRegisteredOnServer(context, true);
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		Log.d(LOG_TAG, String.format("Unregistered with token %s", regId));
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
				.edit();
		editor.remove("gcm_token");
		editor.commit();
		// TODO: Send deregistration to the server.
		GCMRegistrar.setRegisteredOnServer(context, false);
	}

}
