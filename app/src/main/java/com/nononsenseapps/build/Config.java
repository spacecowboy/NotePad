/*
 * Copyright (c) 2014 Jonas Kalderstam.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public static final String KEY_DROPBOX_API = "dropbox_api";
    public static final String KEY_DROPBOX_API_SECRET = "dropbox_api_secret";

    private static final String propFile = "secretkeys.properties";
    private static Properties props;

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

    public static String getKeyDropboxAPI(final Context context) {
        return getProperties(context).getProperty(KEY_DROPBOX_API);
    }

    public static String getKeyDropboxAPISecret(final Context context) {
        return getProperties(context).getProperty(KEY_DROPBOX_API_SECRET);
    }
}
