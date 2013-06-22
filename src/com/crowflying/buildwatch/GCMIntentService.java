/*
 * Copyright 2012 Valentin v. Seggern
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

package com.crowflying.buildwatch;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.crowflying.buildwatch.jenkins.RegisterGCMTokenCommand;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService {
	private static final String LOG_TAG = "GCMIntentService";
	private final static String GCM_KEY_COMITTERS = "i";
	private final static String GCM_KEY_MESSAGE = "m";

	private Handler handlerOnUIThread;
	private Tracker tracker;

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
	protected String[] getSenderIds(Context context) {
		return new String[] { PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString(
				ConfigurationActivity.PREFS_KEY_GCM_SENDER_ID, "") };
	}

	@Override
	public void onCreate() {
		super.onCreate();
		EasyTracker.getInstance().setContext(getApplicationContext());
		tracker = EasyTracker.getTracker();
		// The looper for this handler will be on the UI thread.
		handlerOnUIThread = new Handler();
	}

	@Override
	protected void onError(Context context, String errorId) {
		Log.d(LOG_TAG, String.format("Error %s", errorId));
		tracker.trackEvent("gcm", "error", "error", 0L);
		handlerOnUIThread.post(new DisplayToast(String.format(
				getString(R.string.fmt_gcm_error), errorId)));
	}

	@Override
	protected void onRegistered(Context context, String regId) {
		Log.d(LOG_TAG, String.format("Registered with token %s", regId));
		handlerOnUIThread.post(new DisplayToast(getString(R.string.new_token)));
		try {
			Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
					.edit();
			editor.putString("gcm_token", regId);
			editor.commit();
			GCMRegistrar.setRegisteredOnServer(context, true);
			boolean registrationSuccessful = new RegisterGCMTokenCommand(this,
					regId).execute();

			Log.i(LOG_TAG, String.format(
					"Registering token on server success: %s",
					registrationSuccessful));

			if (registrationSuccessful) {
				tracker.trackEvent("gcm", "registration_on_jenkins", "success",
						0L);
				handlerOnUIThread.post(new DisplayToast(String
						.format(getString(R.string.fmt_registering_success))));
				return;

			} else {
				tracker.trackEvent("gcm", "registration_on_jenkins", "failure",
						0L);

			}
		} catch (IOException e) {
			tracker.trackEvent("gcm", "registration_on_jenkins",
					"io_exception", 0L);
			handlerOnUIThread
					.post(new DisplayToast(String.format(
							getString(R.string.fmt_registering_failed),
							e.getMessage())));
		}

		// If we reach here, registration was not successful. If that is the
		// case, users probably want to see jenkins messages on the device,
		// because they don't have connectivity to their jenkins server from the
		// phone.
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
				.edit();
		editor.putBoolean(ConfigurationActivity.PREFS_KEY_OPEN_IN_BROWSER,
				false);
		editor.commit();
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		Log.d(LOG_TAG, String.format("Unregistered with token %s", regId));
		tracker.trackEvent("gcm", "unregistration", "unregistered", 0L);
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
				.edit();
		editor.remove("gcm_token");
		editor.commit();
		try {
			// Setting the API token on jenkins to the empty string.
			boolean resetSuccess = new RegisterGCMTokenCommand(this, "")
					.execute();
			Log.i(LOG_TAG, String.format(
					"Resetting token on server success: %s", resetSuccess));
		} catch (IOException e) {
			handlerOnUIThread.post(new DisplayToast(String.format(
					getString(R.string.fmt_unregistering_failed),
					e.getMessage())));
		}
		GCMRegistrar.setRegisteredOnServer(context, false);
	}

	@Override
	protected void onMessage(Context context, Intent gcm) {
		// Format the string.
		Intent jenkins = new Intent(context.getString(R.string.action_jenkins));
		String message = gcm.getStringExtra(GCM_KEY_MESSAGE);
		tracker.trackEvent("gcm", "message", "received", 0L);

		Log.d(LOG_TAG,
				String.format("GCM message received: %s. Message: %s",
						gcm.getAction(), message));
		jenkins.putExtra(getString(R.string.extra_message), message);
		jenkins.putExtra(getString(R.string.extra_ifuckedup), didIDoIt(gcm));
		jenkins.putExtra(getString(R.string.extra_build_url),
				parseBuildUrl(message));
		context.startService(jenkins);
	}

	/**
	 * Determine if the current user caused this event...
	 * 
	 * @param intent
	 * @return
	 */
	private boolean didIDoIt(Intent intent) {
		String username = PreferenceManager
				.getDefaultSharedPreferences(this)
				.getString(ConfigurationActivity.PREFS_KEY_JENKINS_USERNAME, "");
		String comitters = intent.getStringExtra(GCM_KEY_COMITTERS);
		if (TextUtils.isEmpty(comitters) || TextUtils.isEmpty(username)) {
			Log.d(LOG_TAG,
					"No comitters or no username configured... Wasn't me then I guess..");
			return false;
		}
		boolean res = comitters.contains(username);
		Log.d(LOG_TAG, String.format(
				"Checking if I (%s) am among the comitters (%s): %s", username,
				comitters, res));
		return res;
	}

	/**
	 * Try to parse the URL of the build from the message to be openend. If
	 * parsing doesn't succeed return the jenkins base URL.
	 * 
	 * @param message
	 *            The message from jenkins
	 * @return The build url or the jenkins base url from the config.
	 */
	private String parseBuildUrl(String message) {
		// This is not really a robust way to get the build URL from the message
		// but it is only until the jenkins GCM plugin can parse this stuff and
		// send it to us in the message...
		if (message != null) {
			Pattern pattern = Pattern.compile("(http(s*)://.*?)$");
			Matcher matcher = pattern.matcher(message);
			if (matcher.find()) {
				String match = matcher.group(0);
				Log.d(LOG_TAG, " matched: " + match);
				return match;
			}
		}
		String jenkinsBaseUrl = PreferenceManager.getDefaultSharedPreferences(
				this)
				.getString(ConfigurationActivity.PREFS_KEY_JENKINS_URL, "");
		return jenkinsBaseUrl;
	}
}
