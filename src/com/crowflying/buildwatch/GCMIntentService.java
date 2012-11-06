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

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService {
	private static final String LOG_TAG = "GCMIntentService";

	private static final int BUILD_STATE_SUCCESS = 0;
	private static final int BUILD_STATE_FAILURE = 1;
	private static final int BUILD_STATE_UNSTABLE = 2;
	private static final int BUILD_STATE_SCHEDULED = 3;
	private static final int BUILD_STATE_STARTED = 4;

	private final static String GCM_KEY_STATE = "s";
	private final static String GCM_KEY_JENKINS_URL = "u";
	private final static String GCM_KEY_PROJECT_NAME = "p";
	private final static String GCM_KEY_BUILD_NUMBER = "b";
	private final static String GCM_KEY_REASONS = "r";
	private final static String GCM_KEY_COMITTERS = "i";
	private final static String GCM_KEY_FULLNAMES = "n";
	private final static String GCM_KEY_COMMITCOMMENTS = "c";
	private final static String GCM_KEY_MESSAGE = "m";

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
		handlerOnUIThread.post(new DisplayToast(String.format(
				getString(R.string.fmt_gcm_error), errorId)));
	}

	@Override
	protected void onRegistered(Context context, String regId) {
		Log.d(LOG_TAG, String.format("Registered with token %s", regId));
		handlerOnUIThread.post(new DisplayToast(getString(R.string.new_token)));
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
				.edit();
		editor.putString("gcm_token", regId);
		editor.commit();
		try {
			postTokenToJenkins(regId);
		} catch (IOException e) {
			handlerOnUIThread
					.post(new DisplayToast(String.format(
							getString(R.string.fmt_registering_failed),
							e.getMessage())));
		}
		GCMRegistrar.setRegisteredOnServer(context, true);
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		Log.d(LOG_TAG, String.format("Unregistered with token %s", regId));
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
				.edit();
		editor.remove("gcm_token");
		editor.commit();
		// Setting the API token on jenkins to the empty string.
		try {
			postTokenToJenkins("");
		} catch (IOException e) {
			handlerOnUIThread.post(new DisplayToast(String.format(
					getString(R.string.fmt_unregistering_failed),
					e.getMessage())));
		}
		GCMRegistrar.setRegisteredOnServer(context, false);
	}

	private boolean postTokenToJenkins(String regId) throws IOException {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		String jenkins = prefs.getString(
				ConfigurationActivity.PREFS_KEY_JENKINS_URL, "");
		String uri = jenkins + "/gcm/register";
		Log.d(LOG_TAG, String.format("About to talk to %s", uri));
		URL url = new URL(uri);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setConnectTimeout(30000);
		String auth = prefs.getString(
				ConfigurationActivity.PREFS_KEY_JENKINS_USERNAME, "")
				+ ":"
				+ prefs.getString(
						ConfigurationActivity.PREFS_KEY_JENKINS_TOKEN, "");
		String encoding = Base64.encodeToString(auth.getBytes("utf-8"),
				Base64.NO_WRAP);
		Log.d(LOG_TAG, String.format(
				"POSTing %s to %s - authenticating with (%s) -> %s", regId,
				uri, auth, encoding));
		connection.setRequestProperty("Authorization", "Basic " + encoding.trim());
		DataOutputStream dos = new DataOutputStream(
				connection.getOutputStream());
		dos.write((String.format("token=%s", regId).getBytes("utf-8")));
		dos.close();
		if (connection.getResponseCode() == 200) {
			Log.d(LOG_TAG, "Hey got back HTTP 200. Beautifuuuuuuul");
			return true;
		}
		Log.w(LOG_TAG, String.format(
				"Crap. The server didn't like this. Status %s, Message: %s",
				connection.getResponseCode(), connection.getResponseMessage()));
		return false;
	}

	@Override
	protected void onMessage(Context context, Intent gcm) {
		Log.d(LOG_TAG,
				String.format("GCM message received: %s", gcm.getAction()));
		// Format the string.
		Intent jenkins = new Intent(context.getString(R.string.action_jenkins));
		String message = gcm.getStringExtra(GCM_KEY_MESSAGE);
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
		Pattern pattern = Pattern.compile("(http(s*)://.*?)\\s");
		Matcher matcher = pattern.matcher(message);
		if (matcher.find()) {
			String match = matcher.group(0);
			Log.d(LOG_TAG, " matched: " + match);
			return match;
		}

		String jenkinsBaseUrl = PreferenceManager.getDefaultSharedPreferences(
				this)
				.getString(ConfigurationActivity.PREFS_KEY_JENKINS_URL, "");
		return jenkinsBaseUrl;
	}
}
