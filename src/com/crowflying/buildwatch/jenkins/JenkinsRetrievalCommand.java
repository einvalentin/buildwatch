package com.crowflying.buildwatch.jenkins;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.crowflying.buildwatch.ConfigurationActivity;

public abstract class JenkinsRetrievalCommand<T> {

	private static final String LOG_TAG = "JenkinsRetrivalTask";
	private CrumbInfo crumbInfo = null;

	protected final Context context;

	public JenkinsRetrievalCommand(Context context) {
		this.context = context;
	}

	/**
	 * Call this method to execute this command.
	 * 
	 * @return The result of the command.
	 * @throws IOException
	 *             if the command failed to execute.
	 */
	public T execute() throws IOException, ParserException {
		Response resp = connectToJenkins();
		return parseResponse(resp);
	}

	/**
	 * Get data from jenkins.
	 * 
	 * @return a response to be fed into a parser.
	 */
	protected Response connectToJenkins() throws IOException {
		Response resp;
		URL url = new URL(getUrl());
		String method = getMethod();
		Log.d(LOG_TAG, String.format("About to talk to %s", url));

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod(method);
		connection.setDoOutput("POST".equals(method));

		if ((crumbInfo == null) && "POST".equals(method)) {
			crumbInfo = new GetCrumbInfoCommand(context).execute();
		}

		connection.setConnectTimeout(30000);

		// Add headers.
		for (Pair<String, String> h : getExtraRequestHeaders()) {
			connection.addRequestProperty(h.first, h.second);
		}

		byte[] requestBody = getRequestBody();
		if (requestBody != null && requestBody.length > 0) {
			connection.setDoInput(true);
			DataOutputStream dos = new DataOutputStream(
					connection.getOutputStream());
			dos.write(getRequestBody());
			dos.close();
		}

		Log.d(LOG_TAG,
				String.format("Got back HTTP %s: %s",
						connection.getResponseCode(),
						connection.getResponseMessage()));
		try {
			resp = new Response(connection.getResponseCode(),
					connection.getInputStream());
		} catch (IOException ie) {
			resp = new Response(connection.getResponseCode());
		}
		return resp;
	}

	/**
	 * Set the headers to send with the request. By default only sets the
	 * Authentication Header. You probably want to call the superclass, if you
	 * use jenkins.
	 * 
	 * @return A list of Header, Value pairs.
	 */
	protected List<Pair<String, String>> getExtraRequestHeaders() {
		List<Pair<String, String>> headers = new LinkedList<Pair<String, String>>();
		try {
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(context);
			String auth = prefs.getString(
					ConfigurationActivity.PREFS_KEY_JENKINS_USERNAME, "")
					+ ":"
					+ prefs.getString(
							ConfigurationActivity.PREFS_KEY_JENKINS_TOKEN, "");
			String encoding = Base64.encodeToString(auth.getBytes("utf-8"),
					Base64.NO_WRAP);
			headers.add(new Pair<String, String>("Authorization", String
					.format("Basic %s", encoding)));
			Log.d(LOG_TAG, String.format(
					"Added Authorization Header with (%s) -> %s", auth,
					encoding));
			if ((this.crumbInfo != null) && this.crumbInfo.isCsrfEnabled()) {
				headers.add(this.crumbInfo.getCrumbHeader());
				Log.d(LOG_TAG, String.format(
						"Added crumb header with (%s) -> %s",
						this.crumbInfo.getCrumbRequestField(),
						this.crumbInfo.getCrumb()));
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, "Could not add authorization header", e);
		}
		return headers;
	}

	/**
	 * Get the HTTP Method for this command. Defaults to GET.
	 * 
	 * @return Can be POST, GET, ...
	 */
	protected String getMethod() {
		return "GET";
	}

	/**
	 * Set a request body. Default implementation has an empty request body.
	 * 
	 * @return the request body in UTF-8 encoded bytes.
	 */
	protected byte[] getRequestBody() {
		return new byte[] {};
	}

	/**
	 * Implementors parse the responses here.
	 * 
	 * @param resp
	 *            The response received from the server.
	 * @return
	 * @throws ParserException
	 *             If parsing didn't work
	 */
	protected abstract T parseResponse(Response resp) throws ParserException;

	/**
	 * Configure the URL for this command.
	 * 
	 * @return the url to connect to.
	 */
	protected abstract String getUrl();

	/**
	 * Thrown, if parsing the response did not work.
	 * 
	 * @author valentin
	 * 
	 */
	public static final class ParserException extends IOException {
		private static final long serialVersionUID = 252350300072318883L;
	}
}
