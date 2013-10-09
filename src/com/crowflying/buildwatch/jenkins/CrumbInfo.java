package com.crowflying.buildwatch.jenkins;

import android.util.Pair;

public class CrumbInfo {
	private String crumb;
	private String crumbRequestField;
	private boolean csrfEnabled = false;

	public void setCrumb(String crumb) {
		this.crumb = crumb;
	}

	public String getCrumb() {
		return this.crumb;
	}

	public void setCrumbRequestField(String crumbRequestField) {
		this.crumbRequestField = crumbRequestField;
	}

	public String getCrumbRequestField() {
		return this.crumbRequestField;
	}

	public void setCsrfEnabled(boolean csrfEnabled) {
		this.csrfEnabled = csrfEnabled;
	}

	public boolean isCsrfEnabled() {
		return csrfEnabled;
	}

	public Pair<String, String> getCrumbHeader() {
		return new Pair<String, String>(this.crumbRequestField, this.crumb);
	}
}
