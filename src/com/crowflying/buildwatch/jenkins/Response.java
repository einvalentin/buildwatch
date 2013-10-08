package com.crowflying.buildwatch.jenkins;

import java.io.InputStream;
import java.util.Scanner;

public class Response {
	public final int statusCode;
	private final String responseBody;

	/**
	 * Create a new response.
	 * 
	 * @param statusCode
	 *            The status code of the call.
	 * @param stream
	 *            The stream to read the data from. Currently just reads it into
	 *            a string and closes the stream. Note: This is probably going
	 *            to have to be reworked if we are downloading bigger stuff.
	 */
	public Response(int statusCode, InputStream stream) {
		this.statusCode = statusCode;
		this.responseBody = readStream(stream);
	}

	public Response(int statusCode) {
		this.statusCode = statusCode;
		this.responseBody = "";
	}

	private String readStream(InputStream stream) {
		Scanner s = new java.util.Scanner(stream, "utf-8").useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	public String getResponseBody() {
		return responseBody;
	}

	public boolean isFailure() {
		return statusCode >= 400;
	}

}
