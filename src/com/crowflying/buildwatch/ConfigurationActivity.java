package com.crowflying.buildwatch;

import android.app.Activity;
import android.os.Bundle;

public class ConfigurationActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new ConfigurationFragment())
				.commit();
	}

}
