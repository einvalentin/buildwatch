package com.crowflying.buildwatch.jenkins;

import android.util.Pair;

public class CrumbInfo {
	private String crumb;
	private String crumbRequestField;
	private boolean crumbRequired;

	public CrumbInfo() {
		this.crumbRequired = false;
	}

	public boolean getCrumbRequired() {
		return this.crumbRequired;
	}

	public void setCrumb(String crumb) {
		this.crumbRequired = true;
		this.crumb = crumb;
	}

	public String getCrumb() {
		return this.crumb;
	}

	public void setCrumbRequestField(String crumbRequestField) {
		this.crumbRequired = true;
		this.crumbRequestField = crumbRequestField;
	}

	public String getCrumbRequestField() {
		return this.crumbRequestField;
	}

	public Pair<String, String> getCrumbHeader() {
		return new Pair<String, String>(this.crumbRequestField, this.crumb);
	}
}
