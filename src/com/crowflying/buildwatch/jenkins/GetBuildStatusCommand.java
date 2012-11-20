package com.crowflying.buildwatch.jenkins;

import android.content.Context;

public class GetBuildStatusCommand extends JenkinsRetrievalCommand<BuildStatus> {

	private final String apiEndpoint;

	public GetBuildStatusCommand(Context context, String buildUrl) {
		super(context);
		this.apiEndpoint = String.format("%s/api/json", buildUrl);
	}

	@Override
	protected BuildStatus parseResponse(Response resp) throws ParserException {
		BuildStatus result = new BuildStatus();
		// TODO: Parse response.
		return result;
	}

	@Override
	protected String getMethod() {
		return "GET";
	}

	@Override
	protected String getUrl() {
		return apiEndpoint;
	}

}
