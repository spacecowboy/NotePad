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

package com.nononsenseapps.notepad.test;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.Suppress;

import com.nononsenseapps.build.Config;
import com.nononsenseapps.notepad.*;
import com.nononsenseapps.notepad.BuildConfig;

import java.util.Properties;

/**
 * Make sure the properties file with api keys can be read correctly
 */
public class ConfigTest  extends AndroidTestCase {

    private Context context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = getContext();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @MediumTest
    @Suppress
    public void testProps() {
        Properties props = Config.getProperties(context);

        assertNotNull(props);

        assertNotNull(Config.getGtasksApiKey(context));
        assertFalse(Config.getGtasksApiKey(context).isEmpty());

        assertNotNull(Config.getKeyDropboxAPI(context));
        assertFalse(Config.getKeyDropboxAPI(context).isEmpty());

        assertNotNull(Config.getKeyDropboxAPISecret(context));
        assertFalse(Config.getKeyDropboxAPISecret(context).isEmpty());
    }
}
