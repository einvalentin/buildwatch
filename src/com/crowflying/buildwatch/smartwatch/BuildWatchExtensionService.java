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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.util.Log;

import com.crowflying.buildwatch.ConfigurationActivity;
import com.crowflying.buildwatch.utils.IntentUtils;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
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
	private Tracker tracker;

	public BuildWatchExtensionService() {
		super(EXTENSION_KEY);
		Log.d(LOG_TAG, "Creation");
	}

	public void onCreate() {
		super.onCreate();
		EasyTracker.getInstance().setContext(getApplicationContext());
		tracker = EasyTracker.getTracker();
	};

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
			sendMessage(intent);
		}
		stopSelfCheck();
		return retval;
	}

	private void sendMessage(Intent intent) {
		String message = intent
				.getStringExtra(getString(R.string.extra_message));
		boolean iBrokeTheBuild = intent.getBooleanExtra(
				getString(R.string.extra_ifuckedup), false);
		String fullName = intent
				.getStringExtra(getString(R.string.extra_fullname));
		String url = intent.getStringExtra(getString(R.string.extra_build_url));

		boolean messageSentToWatch = false;
		boolean accessoriesConnected = areAnyAccessoriesConnected();
		Log.i(LOG_TAG, String.format(
				"Notifying buildwatch users. Accessories connected: %s",
				accessoriesConnected));

		if (accessoriesConnected) {
			messageSentToWatch = notifyWach(message, fullName, url,
					iBrokeTheBuild);
		}
		if (!messageSentToWatch) {
			// Now, we know not many people have a Sony SmartWatch, so we are
			// trying to bring at least some value to others trying to use this
			// App and show a system notification.
			notifyDevice(message, fullName, url, iBrokeTheBuild);
		}

		// Meaning of val: 5: an accessory was connected and the message was
		// sent to it. 11: no accessory connected, 10: accessory conncted but
		// message could not be delivered to it.
		long val = accessoriesConnected ? 0L : 1L;
		val += messageSentToWatch ? 5 : 10;
		tracker.trackEvent("utilities", "message", "delivered", val);
	}

	/**
	 * Sends the notification to the watch.
	 * 
	 * @param message
	 * @param fullname
	 * @param url
	 * @param iBrokeTheBuild
	 * @return an indication whether the message was inserted successfully -
	 *         this might not be complete.
	 */
	private boolean notifyWach(String message, String fullname, String url,
			boolean iBrokeTheBuild) {
		ContentValues eventValues = new ContentValues();
		eventValues.put(Notification.EventColumns.EVENT_READ_STATUS, false);
		// TODO: Make the image depend on the result of the build.
		eventValues.put(Notification.EventColumns.PROFILE_IMAGE_URI,
				ExtensionUtils.getUriString(getApplicationContext(),
						R.drawable.bg_build_success));
		eventValues.put(Notification.EventColumns.DISPLAY_NAME, fullname);
		eventValues.put(Notification.EventColumns.MESSAGE, message);
		eventValues.put(Notification.EventColumns.PERSONAL, iBrokeTheBuild ? 1
				: 0);
		// my good friend, the url...
		eventValues.put(Notification.EventColumns.FRIEND_KEY, url);
		eventValues.put(Notification.EventColumns.PUBLISHED_TIME,
				System.currentTimeMillis());
		eventValues.put(Notification.EventColumns.SOURCE_ID, NotificationUtil
				.getSourceId(getApplicationContext(),
						getString(R.string.jenkins)));

		try {
			getContentResolver().insert(Notification.Event.URI, eventValues);
			return true;
		} catch (IllegalArgumentException e) {
			Log.e(LOG_TAG, "Failed to insert event", e);
		} catch (SecurityException e) {
			Log.e(LOG_TAG,
					"Failed to insert event, is Live Ware Manager installed?",
					e);
		} catch (SQLException e) {
			Log.e(LOG_TAG, "Failed to insert event", e);
		}
		return false;
	}

	/**
	 * Shows a build notification as a native android notifcation on the device
	 * for people without or with an unconnected SmartWatch..
	 * 
	 * @param message
	 * @param fullname
	 * @param url
	 * @param iBrokeTheBuild
	 */
	private void notifyDevice(String message, String fullname, String url,
			boolean iBrokeTheBuild) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// TODO: This should contain status and project name.
		android.app.Notification notification = new android.app.Notification(
				R.drawable.ic_buildwatch,
				getString(R.string.new_message_from_jenkins),
				System.currentTimeMillis());
		Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
		if (url != null) {
			notificationIntent.setData(Uri.parse(url));
		}
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this, String.format(
				getString(R.string.new_message_from_jenkins), fullname),
				message, contentIntent);
		notification.flags |= android.app.Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(4711, notification);

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
