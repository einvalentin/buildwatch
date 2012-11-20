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
package com.crowflying.buildwatch.smartwatch;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.util.Log;

import com.crowflying.buildwatch.ConfigurationActivity;
import com.crowflying.buildwatch.utils.IntentUtils;
import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;
import com.sonyericsson.extras.liveware.sdk.R;

public class BuildWatchExtensionService extends ExtensionService {

	private static final String LOG_TAG = "BuildWatchExtensionService";

	static final String EXTENSION_KEY = "com.crowflying.buildwatch";

	public BuildWatchExtensionService() {
		super(EXTENSION_KEY);
		Log.d(LOG_TAG, "Creation");
	}

	@Override
	public void onRegisterResult(boolean success) {
		Log.d(LOG_TAG, String.format("onRegisterResult. Success: %s", success));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(LOG_TAG, String.format("Got a jenkins watch command: %s", intent));
		int retval = super.onStartCommand(intent, flags, startId);
		if (intent != null
				&& getString(R.string.action_jenkins)
						.equals(intent.getAction())) {
			notifyWatch(intent);
		}
		stopSelfCheck();
		return retval;
	}

	private void notifyWatch(Intent intent) {
		String message = intent
				.getStringExtra(getString(R.string.extra_message));
		boolean iBrokeTheBuild = intent.getBooleanExtra(
				getString(R.string.extra_ifuckedup), false);
		String fullName = intent
				.getStringExtra(getString(R.string.extra_fullname));

		ContentValues eventValues = new ContentValues();
		eventValues.put(Notification.EventColumns.EVENT_READ_STATUS, false);
		// TODO: Make the image depend on the result of the build.
		eventValues.put(Notification.EventColumns.PROFILE_IMAGE_URI,
				ExtensionUtils.getUriString(getApplicationContext(),
						R.drawable.bg_build_success));
		eventValues.put(Notification.EventColumns.DISPLAY_NAME, fullName);
		eventValues.put(Notification.EventColumns.MESSAGE, message);
		eventValues.put(Notification.EventColumns.PERSONAL, iBrokeTheBuild ? 1
				: 0);
		// my good friend, the url...
		eventValues.put(Notification.EventColumns.FRIEND_KEY,
				intent.getStringExtra(getString(R.string.extra_build_url)));
		eventValues.put(Notification.EventColumns.PUBLISHED_TIME,
				System.currentTimeMillis());
		eventValues.put(Notification.EventColumns.SOURCE_ID, NotificationUtil
				.getSourceId(getApplicationContext(),
						getString(R.string.jenkins)));

		try {
			getContentResolver().insert(Notification.Event.URI, eventValues);
		} catch (IllegalArgumentException e) {
			Log.e(LOG_TAG, "Failed to insert event", e);
		} catch (SecurityException e) {
			Log.e(LOG_TAG,
					"Failed to insert event, is Live Ware Manager installed?",
					e);
		} catch (SQLException e) {
			Log.e(LOG_TAG, "Failed to insert event", e);
		}
	}

	@Override
	protected void onViewEvent(Intent intent) {
		IntentUtils.printIntentExtras(intent);
		int eventId = intent.getIntExtra(Notification.Intents.EXTRA_EVENT_ID,
				-1);
		Cursor cursor = getContentResolver().query(Notification.Event.URI,
				null, Notification.EventColumns._ID + " = ?",
				new String[] { "" + eventId }, null);
		if (cursor != null && cursor.moveToFirst()) {
			String url = cursor.getString(cursor
					.getColumnIndex(Notification.EventColumns.FRIEND_KEY));
			Intent browse = new Intent(Intent.ACTION_VIEW);
			browse.setData(Uri.parse(url));
			browse.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(browse);
		}
	}

	@Override
	protected RegistrationInformation getRegistrationInformation() {
		Log.d(LOG_TAG, "getRegistrationInformation()");
		return new RegistrationInformation() {

			@Override
			public int getRequiredWidgetApiVersion() {
				// 0: Does not use API, 1: uses API.
				return 0;
			}

			@Override
			public int getRequiredSensorApiVersion() {
				// 0: Does not use API, 1: uses API.
				return 0;
			}

			@Override
			public int getRequiredNotificationApiVersion() {
				// 0: Does not use API, 1: uses API.
				return 1;
			}

			@Override
			public int getRequiredControlApiVersion() {
				// 0: Does not use API, 1: uses API.
				return 0;
			}

			@Override
			public ContentValues getExtensionRegistrationConfiguration() {
				ContentValues values = new ContentValues();
				values.put(
						Registration.ExtensionColumns.CONFIGURATION_ACTIVITY,
						ConfigurationActivity.class.getName());

				values.put(Registration.ExtensionColumns.CONFIGURATION_TEXT,
						getString(R.string.configuration_text));
				values.put(Registration.ExtensionColumns.EXTENSION_ICON_URI,
						ExtensionUtils.getUriString(getApplicationContext(),
								R.drawable.ic_buildwatch));
				values.put(Registration.ExtensionColumns.EXTENSION_KEY,
						EXTENSION_KEY);
				values.put(Registration.ExtensionColumns.HOST_APP_ICON_URI,
						ExtensionUtils.getUriString(getApplicationContext(),
								R.drawable.ic_buildwatch));
				values.put(Registration.ExtensionColumns.NAME,
						getString(R.string.app_name));
				values.put(
						Registration.ExtensionColumns.NOTIFICATION_API_VERSION,
						getRequiredNotificationApiVersion());
				values.put(Registration.ExtensionColumns.PACKAGE_NAME,
						getApplication().getPackageName());

				return values;
			}

			@Override
			public ContentValues[] getSourceRegistrationConfigurations() {
				ContentValues values = new ContentValues();
				values.put(Notification.SourceColumns.ENABLED, true);
				values.put(Notification.SourceColumns.ICON_URI_1,
						ExtensionUtils.getUriString(getApplicationContext(),
								R.drawable.jenkins_30x30));
				values.put(Notification.SourceColumns.ICON_URI_2,
						ExtensionUtils.getUriString(getApplicationContext(),
								R.drawable.jenkins_18x18));
				values.put(Notification.SourceColumns.ACTION_1,
						getString(R.string.show_in_browser));
				values.put(Notification.SourceColumns.NAME,
						getString(R.string.jenkins));
				values.put(Notification.SourceColumns.EXTENSION_SPECIFIC_ID,
						getString(R.string.jenkins));
				return new ContentValues[] { values };
			}
		};
	}

	@Override
	protected boolean keepRunningWhenConnected() {
		return false;
	}

}
