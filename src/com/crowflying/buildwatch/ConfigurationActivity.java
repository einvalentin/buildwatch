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

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.TrackedPreferenceActivity;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gcm.GCMRegistrar;
import com.google.zxing.integration.android.IntentIntegrator;

public class ConfigurationActivity extends TrackedPreferenceActivity implements
		OnSharedPreferenceChangeListener, OnPreferenceClickListener {

	public static final String PREFS_AUTOSETUP = "scan_qr_code";
	public static final String PREFS_FORGET_SETTINGS = "forget_settings";
	public static final String PREFS_KEY_GCM_TOKEN = "gcm_token";
	public static final String PREFS_KEY_GCM_SENDER_ID = "gcm_sender_id";
	public static final String PREFS_KEY_JENKINS_URL = "jenkins_base_url";
	public static final String PREFS_KEY_JENKINS_USERNAME = "jenkins_username";
	public static final String PREFS_KEY_JENKINS_TOKEN = "jenkins_token";
	public static final String PREFS_KEY_JENKINS_PROJECTS = "jenkins_projects";
	public static final String PREFS_KEY_ANALYTICS_OPTOUT = "analytics_opt_out";
	public static final String PREFS_KEY_LAUNCH_WEBSITE = "launch_website";
	public static final String PREFS_KEY_OPEN_IN_BROWSER  ="open_in_browser";

	private static final String LOG_TAG = "ConfigurationActivity";

	private Tracker tracker;

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d(LOG_TAG, String.format("Pref %s just changed..", key));
		// This is obviously suboptimal from a performance point of view.
		refreshSummaries();
		// If you change the GCM sender id, we want to fetch a new Auth token
		// for you...
		if (PREFS_KEY_GCM_SENDER_ID.equals(key)) {
			new GetCloudDeviceMessagingToken().execute(sharedPreferences
					.getString(key, null));
		}

		if (PREFS_KEY_ANALYTICS_OPTOUT.equals(key)) {
			boolean optout = !sharedPreferences.getBoolean(key, true);
			tracker.trackEvent("configuration", "analytics", "output",
					optout ? 0L : 1L);
			if (optout) {
				Log.d(LOG_TAG, "Opting the user out of analytics");
			} else {
				Log.d(LOG_TAG, "Opting the user in to analytics");
			}
			GoogleAnalytics.getInstance(getApplicationContext()).setAppOptOut(
					optout);
		}

	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		Log.d(LOG_TAG,
				String.format("Preference %s was clicked...",
						preference.getKey()));
		// Call the code for autosetup...
		if (PREFS_AUTOSETUP.equals(preference.getKey())) {
			Log.i(LOG_TAG, "Calling XZING.");
			tracker.trackEvent("configuration", "subscreen", "autosetup", 0L);

			IntentIntegrator integrator = new IntentIntegrator(this);
			integrator
					.setMessageByID(R.string.barcode_scanner_not_installed_message);
			integrator.initiateScan();
			return true;
		}
		if (PREFS_FORGET_SETTINGS.equals(preference.getKey())) {
			Log.i(LOG_TAG, "Forgetting all settings");
			tracker.trackEvent("configuration", "action", "settings_cleared",
					0L);
			PreferenceManager.getDefaultSharedPreferences(this).edit().clear()
					.commit();
			return true;
		}
		if (PREFS_KEY_LAUNCH_WEBSITE.equals(preference.getKey())) {
			Log.i(LOG_TAG, "Launching website");
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(getString(R.string.config_launch_website_url)));
			startActivity(i);
			return true;
		}
		if (PREFS_KEY_GCM_TOKEN.equals(preference.getKey())) {
			String token = getPreferenceManager().getSharedPreferences()
					.getString(PREFS_KEY_GCM_TOKEN, "");
			if (!TextUtils.isEmpty(token)) {
				Log.i(LOG_TAG,
						"Token clicked. Echoing it to the logfile for convencience: "
								+ token);
				Intent sharingIntent = new Intent(
						android.content.Intent.ACTION_SEND);
				sharingIntent.setType("text/plain");
				sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
						getString(R.string.share_gcm_token_subject));
				sharingIntent
						.putExtra(android.content.Intent.EXTRA_TEXT, token);
				startActivity(Intent.createChooser(sharingIntent,
						getString(R.string.share_gcm_token_using)));
			}
			return true;
		}
		return false;
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EasyTracker.getInstance().setContext(getApplicationContext());
		tracker = EasyTracker.getTracker();
		Uri data = getIntent().getData();
		if (data != null && data.toString().contains("server")) {
			addPreferencesFromResource(R.xml.server_preferences);
		} else {
			addPreferencesFromResource(R.xml.preferences);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(LOG_TAG, "onCreateOptionsMenu...");
		super.onCreateOptionsMenu(menu);
		menu.add(getString(R.string.menu_request_new_token));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (getString(R.string.menu_request_new_token).equals(item.getTitle())) {
			tracker.trackEvent("configuration", "menu", "req_new_gcm", 0L);

			new GetCloudDeviceMessagingToken().execute(getPreferenceScreen()
					.getSharedPreferences().getString(PREFS_KEY_GCM_SENDER_ID,
							""));
			return true;
		}
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();

		refreshSummaries();

		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		setPreferenceClickListener(PREFS_AUTOSETUP);
		setPreferenceClickListener(PREFS_FORGET_SETTINGS);
		setPreferenceClickListener(PREFS_KEY_LAUNCH_WEBSITE);
		setPreferenceClickListener(PREFS_KEY_GCM_TOKEN);
	}

	private void setPreferenceClickListener(String key) {
		Preference pref = findPreference(key);
		if (pref != null) {
			pref.setOnPreferenceClickListener(this);
		} else {
			Log.d(LOG_TAG, String.format("Preference %s was null...", key));
		}
	}

	/**
	 * Refresh the summaries of the prefs. This is done in one single hunk for
	 * all prefs which is suboptimal from a performance point of view, but is
	 * less code to write. To optimize, only refresh summaries on the settings
	 * that actually changed.
	 */
	private void refreshSummaries() {
		long t1 = System.currentTimeMillis();
		List<Pair<String, Integer>> setSummaryTextPrefs = new LinkedList<Pair<String, Integer>>();
		setSummaryTextPrefs
				.add(new Pair<String, Integer>(PREFS_KEY_GCM_SENDER_ID,
						R.string.config_gcm_sender_id_summary));
		setSummaryTextPrefs.add(new Pair<String, Integer>(PREFS_KEY_GCM_TOKEN,
				R.string.config_gcm_token_summary));
		setSummaryTextPrefs
				.add(new Pair<String, Integer>(PREFS_KEY_JENKINS_TOKEN,
						R.string.config_jenkins_token_summary));
		setSummaryTextPrefs
				.add(new Pair<String, Integer>(PREFS_KEY_JENKINS_URL,
						R.string.config_jenkins_base_url_summary));
		setSummaryTextPrefs.add(new Pair<String, Integer>(
				PREFS_KEY_JENKINS_USERNAME,
				R.string.config_jenkins_username_summary));
		SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

		for (Pair<String, Integer> p : setSummaryTextPrefs) {
			Preference pref = findPreference(p.first);
			if (pref != null) {
				pref.setSummary(String.format(getString(p.second),
						prefs.getString(p.first, "")));

			}
		}

		// Add 'press to share' to GCM token pref, if there is a token
		// configured.
		if (!TextUtils.isEmpty(prefs.getString(PREFS_KEY_GCM_TOKEN, ""))) {
			Preference pref = findPreference(PREFS_KEY_GCM_TOKEN);
			if (pref != null) {
				CharSequence summary = pref.getSummary();
				summary = summary + " "
						+ getString(R.string.config_gcm_token_summary_share);
				pref.setSummary(summary);
			}
		}

		Log.d(LOG_TAG, String.format(
				"Updating summaries took %s ms. This can easily be optimized.",
				(System.currentTimeMillis() - t1)));

	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(LOG_TAG, String.format("Got result: %s, %s, %s", requestCode,
				resultCode, data));
		// We won't check for XZing originating intent here - lets just hope
		// this will not bite later.
		if (resultCode == Activity.RESULT_OK) {
			new ConfigureAppFromQRCode().execute(data);
		}
	}

	/**
	 * Parse JSON config in the background and also insert them into the
	 * SharedPrefs.
	 * 
	 */

	private class ConfigureAppFromQRCode extends
			AsyncTask<Intent, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Intent... data) {
			try {
				boolean configWorked = autoconfigureFromCode(data[0]);
				tracker.trackEvent("configuration", "autosetup",
						"parsed_configuration", configWorked ? 0L : 1L);
				if (configWorked) {
					new GetCloudDeviceMessagingToken()
							.execute(getPreferenceScreen()
									.getSharedPreferences().getString(
											PREFS_KEY_GCM_SENDER_ID, ""));
				}
				return configWorked;
			} catch (JSONException e) {
				Log.e(LOG_TAG, String.format(
						"Result from QR code was not parsable: %s", e));
			}
			return false;
		}

		/**
		 * Parse the configuration data from the (hopefully) jenkins provided QR
		 * code and store it in settings.
		 * 
		 * @param data
		 * @return {@code true} if the configuration was changed.
		 */
		private boolean autoconfigureFromCode(Intent data) throws JSONException {
			long t1 = System.currentTimeMillis();
			JSONObject config = new JSONObject(
					data.getStringExtra("SCAN_RESULT"));
			Editor editor = PreferenceManager.getDefaultSharedPreferences(
					ConfigurationActivity.this).edit();

			int counter = 0;
			if (extractAndStore(config, "senderId", editor,
					PREFS_KEY_GCM_SENDER_ID)) {
				counter += 1;
			}

			if (extractAndStore(config, "url", editor, PREFS_KEY_JENKINS_URL)) {
				counter += 1;
			}

			if (extractAndStore(config, "username", editor,
					PREFS_KEY_JENKINS_USERNAME)) {
				counter += 1;
			}

			if (extractAndStore(config, "token", editor,
					PREFS_KEY_JENKINS_TOKEN)) {
				counter += 1;
			}
			editor.commit();
			Log.i(LOG_TAG, String.format(
					"Stored %s configuration values from the QR code in %s ms",
					counter, (System.currentTimeMillis() - t1)));

			// return true, if we changed at least one Setting.
			return counter > 0;
		}

		/**
		 * Extract the configuration value from the JSON and store it in the
		 * editor (without commiting it).
		 * 
		 * @param json
		 *            The JSON from the QR code.
		 * @param jsonKey
		 *            The key of where to find the value in the JSON.
		 * @param editor
		 *            The Editor to store the configuration to.
		 * @param prefKey
		 *            The key to store the config value.
		 * @return {@code true}, if the value was stored in the prefs.
		 */
		private boolean extractAndStore(JSONObject json, String jsonKey,
				Editor editor, String prefKey) {
			try {
				String value = json.getString(jsonKey);
				editor.putString(prefKey, value);
				return true;
			} catch (JSONException e) {
				Log.e(LOG_TAG,
						String.format("Key %s not found in %s", jsonKey, json));
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean somethingChanged) {
			if (somethingChanged) {

			}
		}
	}

	/**
	 * Request a new GCM token in the background.
	 * 
	 * @author valentin
	 * 
	 */
	private class GetCloudDeviceMessagingToken extends
			AsyncTask<String, Void, String> {

		boolean deviceCheckFailed = false;

		@Override
		protected String doInBackground(String... params) {
			String senderId = params[0];

			// Remove old GCM token and unregister from GCM.
			Log.i(LOG_TAG,
					"sender_id update detected... Unregistering and removing old token");
			GCMRegistrar.unregister(ConfigurationActivity.this);

			Editor editor = PreferenceManager.getDefaultSharedPreferences(
					ConfigurationActivity.this).edit();
			editor.remove(PREFS_KEY_GCM_TOKEN);
			editor.commit();

			Log.i(LOG_TAG, String.format(
					"Trying to request a new gcm token for sender_id: %s",
					senderId));
			try {
				GCMRegistrar.checkDevice(ConfigurationActivity.this);
			} catch (Exception e) {
				tracker.trackEvent("configuration", "gcm", "device_no_gcm", 0L);
				Log.e(LOG_TAG, "Device can't use C2DM", e);
				deviceCheckFailed = true;
				return null;
			}

			GCMRegistrar.register(ConfigurationActivity.this, senderId);
			return GCMRegistrar.getRegistrationId(ConfigurationActivity.this);
		}

		@Override
		protected void onPostExecute(String result) {
			if (deviceCheckFailed) {
				Toast.makeText(ConfigurationActivity.this,
						getString(R.string.unsupported_device_c2dm),
						Toast.LENGTH_LONG).show();
			}
		}
	}

}
