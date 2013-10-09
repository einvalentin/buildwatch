package com.crowflying.buildwatch.jenkins;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.preference.PreferenceManager;

import com.crowflying.buildwatch.ConfigurationActivity;

public class GetCrumbInfoCommand extends JenkinsRetrievalCommand<CrumbInfo> {

	public GetCrumbInfoCommand(Context context) {
		super(context);
	}

	@Override
	protected CrumbInfo parseResponse(Response resp) throws ParserException {
		CrumbInfo result = new CrumbInfo();
		try {
			JSONObject jsonobj = new JSONObject(resp.getResponseBody());
			String crumb = jsonobj.getString("crumb");
			String crumbRequestField = jsonobj.getString("crumbRequestField");
			result.setCrumb(crumb);
			result.setCrumbRequestField(crumbRequestField);
			result.setCsrfEnabled(true);
		} catch (JSONException je) {
			/* Failure simply implies that CSRF protection isn't enabled.*/
			result.setCsrfEnabled(false);
		}
		return result;
	}

	@Override
	protected String getMethod() {
		return "GET";
	}

	@Override
	protected String getUrl() {
		String jenkins = PreferenceManager.getDefaultSharedPreferences(context)
				.getString(ConfigurationActivity.PREFS_KEY_JENKINS_URL, "");
		String uri = jenkins + "/crumbIssuer/api/json";
		return uri;
	}
}
