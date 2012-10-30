package com.crowflying.buildwatch;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.android.gcm.GCMRegistrar;
import com.google.zxing.integration.android.IntentIntegratorV30;

public class ConfigurationFragment extends PreferenceFragment implements
		OnPreferenceClickListener, OnSharedPreferenceChangeListener {
	public static final String PREFS_AUTOSETUP = "scan_qr_code";
	public static final String PREFS_KEY_GCM_TOKEN = "gcm_token";
	public static final String PREFS_KEY_GCM_SENDER_ID = "gcm_sender_id";
	public static final String PREFS_KEY_JENKINS_URL = "jenkins_base_url";
	public static final String PREFS_KEY_JENKINS_USERNAME = "jenkins_username";
	public static final String PREFS_KEY_JENKINS_TOKEN = "jenkins_token";
	public static final String PREFS_KEY_JENKINS_PROJECTS = "jenkins_projects";

	private static final String LOG_TAG = "BuildWatchPreferencesActivity";

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d(LOG_TAG, String.format("Pref %s just changed..", key));
		// This is obviously suboptimal from a performance point of view, but I
		// was too lazy to type the extra checks to make sure only the right
		// summary is updated.
		refreshSummaries();
		// If you change the GCM sender id, we want to fetch a new Auth token
		// for you...
		if (PREFS_KEY_GCM_SENDER_ID.equals(key)) {
			new GetCloudDeviceMessagingToken().execute(sharedPreferences
					.getString(key, null));
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		// Call the code for autosetup...
		if (PREFS_AUTOSETUP.equals(preference.getKey())) {
			Log.d(LOG_TAG, "Calling XZING.");
			IntentIntegratorV30 integrator = new IntentIntegratorV30(this);
			integrator.setMessage(getString(R.string.scan_qr_code));
			integrator.initiateScan();
			return true;
		}
		return false;
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(LOG_TAG, "onCreate()");
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public void onResume() {
		super.onResume();

		refreshSummaries();

		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		getPreferenceScreen().findPreference(PREFS_AUTOSETUP)
				.setOnPreferenceClickListener(this);
	}

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

		List<Pair<String, Integer>> setSummaryListPrefs = new LinkedList<Pair<String, Integer>>();
		setSummaryListPrefs.add(new Pair<String, Integer>(
				PREFS_KEY_JENKINS_PROJECTS,
				R.string.config_jenkins_projects_summary));
		SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

		for (Pair<String, Integer> p : setSummaryTextPrefs) {
			findPreference(p.first).setSummary(
					String.format(getString(p.second),
							prefs.getString(p.first, "")));
		}

		for (Pair<String, Integer> p : setSummaryListPrefs) {
			findPreference(p.first).setSummary(
					String.format(getString(p.second),
							prefs.getStringSet(p.first, new TreeSet<String>())
									.size()));
		}

		Log.i(LOG_TAG, String.format(
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
	 * Parse JSON config in the background.
	 * 
	 */

	private class ConfigureAppFromQRCode extends
			AsyncTask<Intent, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Intent... data) {
			try {
				return autoconfigureFromCode(data[0]);
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
					getActivity()).edit();

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

		@Override
		protected String doInBackground(String... params) {
			String senderId = params[0];

			// Remove old GCM token and unregister from GCM.
			Log.i(LOG_TAG,
					"sender_id update detected... Unregistering and removing old token");
			GCMRegistrar.unregister(getActivity());

			Editor editor = PreferenceManager.getDefaultSharedPreferences(
					getActivity()).edit();
			editor.remove(PREFS_KEY_GCM_TOKEN);
			editor.commit();

			Log.i(LOG_TAG, String.format(
					"Trying to request a new gcm token for sender_id: %s",
					senderId));
			GCMRegistrar.checkDevice(getActivity());
			// TODO: Remove - this is only necessary in development.
			GCMRegistrar.checkManifest(getActivity());
			final String regId = GCMRegistrar.getRegistrationId(getActivity());

			if (TextUtils.isEmpty(regId)) {
				GCMRegistrar.register(getActivity(), senderId);
			} else {
				Log.d(LOG_TAG,
						String.format("Already registered. Token %s", regId));
			}
			return regId;
		}
	}

}
