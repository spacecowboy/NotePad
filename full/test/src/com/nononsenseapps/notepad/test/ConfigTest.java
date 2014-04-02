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
import android.test.suitebuilder.annotation.SmallTest;

import com.nononsenseapps.build.Config;

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

    @SmallTest
    public void testProps() {
        Properties props = Config.getProperties(context);

        assertNotNull(props);
        for (Object key: props.keySet()) {
            String b = key.toString();
        }
        assertEquals("Put your key here", props.getProperty(Config
                .KEY_GTASKS_API_KEY));
        assertEquals("Public key here", props.getProperty(Config
                .KEY_DROPBOX_SYNC_PUBLIC));
        assertEquals("Secret key here", props.getProperty(Config
                .KEY_DROPBOX_SYNC_SECRET));
    }
}
