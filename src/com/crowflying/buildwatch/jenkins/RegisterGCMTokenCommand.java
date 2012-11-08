package com.crowflying.buildwatch.jenkins;

import java.io.IOException;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crowflying.buildwatch.ConfigurationActivity;

public class RegisterGCMTokenCommand extends JenkinsRetrievalCommand<Boolean> {
	private static final String LOG_TAG = "RegisterGcmTokenCommand";
	private String regId;

	public RegisterGCMTokenCommand(Context context, String regId) {
		super(context);
		this.regId = regId;
	}

	@Override
	protected String getUrl() {
		String jenkins = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(ConfigurationActivity.PREFS_KEY_JENKINS_URL, "");
		String uri = jenkins + "/gcm/register";
		return uri;
	}

	@Override
	protected String getMethod() {
		return "POST";
	}

	@Override
	protected Boolean parseResponse(Response resp) throws ParserException {
		return !resp.isFailure();
	}

	@Override
	protected byte[] getRequestBody() {
		try {
			return String.format("token=%s", regId).getBytes("utf-8");
		} catch (IOException e) {
			Log.e(LOG_TAG,
					String.format("Could not convert token %s to utf-8", regId),
					e);
		}
		return super.getRequestBody();
	}
}
