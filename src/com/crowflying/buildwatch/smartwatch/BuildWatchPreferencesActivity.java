package com.crowflying.buildwatch.smartwatch;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.util.Log;

import com.crowflying.buildwatch.R;
import com.google.android.gcm.GCMRegistrar;
import com.google.zxing.integration.android.IntentIntegrator;

public class BuildWatchPreferencesActivity extends PreferenceActivity {

	private static final String LOG_TAG = "BuildWatchPreferencesActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(LOG_TAG, "onCreate()");
		addPreferencesFromResource(R.xml.preferences);

		// Hook up the qr code preference to actually initiate XZING.
		findPreference("scan_qr_code").setOnPreferenceClickListener(
				new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						Log.d(LOG_TAG, "Initiating XZING.");
						IntentIntegrator integrator = new IntentIntegrator(
								BuildWatchPreferencesActivity.this);
						integrator.initiateScan();
						return true;
					}
				});

		// Changing the gcm sender id causes a new token request.
		findPreference("gcm_sender_id").setOnPreferenceChangeListener(
				new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						String senderId = (String) newValue;
						Log.i(LOG_TAG, "sender_id update detected...");
						if (TextUtils.isEmpty(senderId)) {
							Log.i(LOG_TAG,
									"No GCM sender id configured yet... Not requesting token.");
							return false;
						} else {
							new GetCloudDeviceMessagingToken()
									.execute(senderId);
						}
						return true;
					}
				});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(LOG_TAG, String.format("Got result: %s, %s, %s", requestCode,
				resultCode, data));
		// TODO: Parse configuration data and store in config - maybe showing a
		// blocking spinner in the meantime..
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
			Log.i(LOG_TAG, String.format(
					"Trying to request a new gcm token for sender_id: %s",
					senderId));
			GCMRegistrar.checkDevice(BuildWatchPreferencesActivity.this);
			// TODO: Remove - this is only necessary in development.
			GCMRegistrar.checkManifest(BuildWatchPreferencesActivity.this);
			final String regId = GCMRegistrar
					.getRegistrationId(BuildWatchPreferencesActivity.this);

			if (TextUtils.isEmpty(regId)) {
				GCMRegistrar.register(BuildWatchPreferencesActivity.this,
						senderId);
			} else {
				Log.d(LOG_TAG,
						String.format("Already registered. Token %s", regId));
			}
			return regId;
		}
	}
}
