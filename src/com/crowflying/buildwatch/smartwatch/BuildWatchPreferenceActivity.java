package com.crowflying.buildwatch.smartwatch;

import android.app.Activity;
import android.os.Bundle;

import com.crowflying.buildwatch.ConfigurationFragment;

public class BuildWatchPreferenceActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new ConfigurationFragment())
				.commit();
	}
}
