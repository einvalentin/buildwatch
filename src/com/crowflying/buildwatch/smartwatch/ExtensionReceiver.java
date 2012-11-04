/*
 * Copyright 2012 Valentin v. Seggern
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.crowflying.buildwatch.smartwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ExtensionReceiver extends BroadcastReceiver {

	private static final String LOG_TAG = "ExtensionReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LOG_TAG, String.format("onReceive %s", intent.getAction()));
		intent.setClass(context, BuildWatchExtensionService.class);
		context.startService(intent);
	}

}
