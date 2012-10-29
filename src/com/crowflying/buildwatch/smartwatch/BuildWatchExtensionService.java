package com.crowflying.buildwatch.smartwatch;

import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.util.Log;

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
		int retval = super.onStartCommand(intent, flags, startId);
		if (getString(R.string.action_build_broke).equals(intent.getAction())) {
			notifyWatch(intent);
		}
		return retval;
	}

	private void notifyWatch(Intent intent) {
		String username = intent.getStringExtra(getString(R.string.extra_user));
		String project = intent
				.getStringExtra(getString(R.string.extra_project));
		ContentValues eventValues = new ContentValues();
		eventValues.put(Notification.EventColumns.EVENT_READ_STATUS, false);
		eventValues.put(Notification.EventColumns.DISPLAY_NAME, username);
		eventValues.put(Notification.EventColumns.MESSAGE, String.format(
				getString(R.string.fmt_build_fail_message), username, project));
		// TODO: if someone else broke the build, this should be different.
		int iBrokeTheBuild = 1;
		eventValues.put(Notification.EventColumns.PERSONAL, iBrokeTheBuild);
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
						BuildWatchPreferencesActivity.class.getName());

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
								R.drawable.ic_buildwatch));
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
