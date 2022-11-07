/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.build;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Contains build specific values used in code like API keys. It reads these
 * values for the secretkeys.properties file in the assets directory.
 */
public class Config {
	public final static boolean LOGGING = true;

	public static final String KEY_GTASKS_API_KEY = "gtasks_api_key";

	private static final String propFile = "secretkeys.properties";
	private static Properties props;

	// TODO remove this, remove the secretkeys file, and let the user copypaste the API key into an edittext in the settings
	public static Properties getProperties(final Context context) {
		if (props == null) {
			props = new Properties();

			try {
				final AssetManager assetManager = context.getAssets();
				final InputStream inputStream = assetManager.open(propFile);
				props.load(inputStream);
				inputStream.close();
			} catch (IOException e) {
				Log.e("NoNonsenseNotes Props:", e.getLocalizedMessage());
			}
		}
		return props;
	}

	public static String getGtasksApiKey(final Context context) {
		return getProperties(context).getProperty(KEY_GTASKS_API_KEY,
				"AIzaSyBtUvSWg41WVi9E3W1VaqDMlJ07a3B6JOs");
	}

}
