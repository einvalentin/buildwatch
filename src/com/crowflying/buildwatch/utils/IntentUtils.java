package com.crowflying.buildwatch.utils;

import android.content.Intent;
import android.util.Log;

public final class IntentUtils {
	private final static String LOG_TAG = "IntentUtils";

	private IntentUtils() {
		// Don't do it..
	}

	public static void printIntentExtras(Intent i) {
		for (String k : i.getExtras().keySet()) {
			Log.d(LOG_TAG, String.format("%s -> %s", k, i.getExtras().get(k)));
		}
	}

}
