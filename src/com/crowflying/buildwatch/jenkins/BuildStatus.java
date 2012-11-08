package com.crowflying.buildwatch.jenkins;

public class BuildStatus {

	private Result result;

	public enum Result {
		SUCCESS, FAILURE, UNSTABLE, IN_PROGRESS /* builing true, result: null */, PENDING /* ? */
	}

	public void setResult(Result result) {
	}

	public Result getResult() {
		return result;
	}
}
