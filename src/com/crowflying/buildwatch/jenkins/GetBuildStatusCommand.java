package com.crowflying.buildwatch.jenkins;

import android.content.Context;

public class GetBuildStatusCommand extends JenkinsRetrievalCommand<BuildStatus> {

	private final String buildUrl;

	public GetBuildStatusCommand(Context context, String buildUrl) {
		super(context);
		this.buildUrl = buildUrl;
	}

	@Override
	protected BuildStatus parseResponse(Response resp) throws ParserException {
		BuildStatus result = new BuildStatus();
		
		return result;

	}

	@Override
	protected String getUrl() {
		return buildUrl;
	}

}
