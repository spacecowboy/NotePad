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

package com.nononsenseapps.notepad.provider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * This singleton-class handles things related to (possibly 3rd party) providers.
 */
public class ProviderManager {

    private static final String ACTION_PROVIDER = "com.nononsenseapps.notepad.PROVIDER";
    private static ProviderManager sInstance;
    private final Context mApplicationContext;

    private ProviderManager(Context context) {
        mApplicationContext = context.getApplicationContext();
    }

    public static ProviderManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ProviderManager(context);
        }

        return sInstance;
    }

    /**
     *
     * @return a list of URIs, which installed providers provide.
     */
    public List<Provider> getAvailableProviders() {
        List<Provider> availableUris = new ArrayList<>();
        PackageManager pm = mApplicationContext.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentContentProviders(new Intent(ACTION_PROVIDER),
                PackageManager.GET_META_DATA);
        for (ResolveInfo resolveInfo: resolveInfos) {
            try {
                // URI is available in metadata
                availableUris.add(new Provider(pm, resolveInfo.providerInfo));
                // Service style
                //Bundle metaData = resolveInfo.serviceInfo.metaData;
                //availableUris.add(Uri.parse(metaData.getString("uri")));
            } catch (NullPointerException ignored) {
                // Instead of wrapping code in multiple ifs
            }
        }

        return availableUris;
    }

    public static class Provider {
        public final String authority;
        public final Uri uriBase;
        public final String label;

        public Provider(final PackageManager pm, final ProviderInfo providerInfo) {
            label = providerInfo.loadLabel(pm).toString();
            authority = providerInfo.authority;
            uriBase = Uri.parse("content://" + authority);
            /*if (null != providerInfo.metaData) {
                // Optional stuff like settingsActivity and capabilities
                String settingsActivity = providerInfo.metaData.getString("settingsActivity");
            }*/
        }
    }
}
