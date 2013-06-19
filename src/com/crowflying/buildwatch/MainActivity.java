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

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.analytics.tracking.android.TrackedActivity;

public class MainActivity extends TrackedActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String message = getIntent().getStringExtra(
				getString(R.string.extra_message));

		if (!isConfigured() || TextUtils.isEmpty(message)) {
			startActivity(new Intent(this, ConfigurationActivity.class));
			finish();
			return;
		}
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancelAll();
		setContentView(R.layout.activity_main);
		((TextView) findViewById(R.id.message)).setText(message);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startActivity(new Intent(this, ConfigurationActivity.class));
			return true;
		default:
			return super.onMenuItemSelected(featureId, item);
		}

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
				ConfigurationActivity.PREFS_KEY_GCM_TOKEN, null));
	}

}
