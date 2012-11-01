package com.crowflying.buildwatch;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;

public class MainActivity extends Activity {
	private static final String LOG_TAG = "MainActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// if (!isConfigured()) {
		startActivity(new Intent(this, ConfigurationActivity.class));
		finish();
		// }
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	/**
	 * Checks if the App is already configured
	 * 
	 * @return {@code true}, if the app is sufficiently configured,
	 *         {@code false} otherwise.
	 */
	private boolean isConfigured() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		// Currently we just check for the GCM token, because you need sender id
		// for that and we just hope that this is done via the QR code.
		return !TextUtils.isEmpty(prefs.getString(
				ConfigurationFragment.PREFS_KEY_GCM_TOKEN, null));
	}

}
