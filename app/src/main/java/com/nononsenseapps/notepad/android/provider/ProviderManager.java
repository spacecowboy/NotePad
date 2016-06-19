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

package com.nononsenseapps.notepad.android.provider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This singleton-class handles things related to (possibly 3rd party) providers.
 */
public class ProviderManager {

    public static final String METADATA_PROTOCOL_VERSION = "protocolVersion";
    public static final String METADATA_REQUIRES_CONFIG = "requiresConfig";
    public static final String METADATA_SETTINGS_ACTIVITY = "settingsActivity";

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
     * @return a list of providers which are available for use/setup.
     */
    public List<Provider> getAvailableProviders() {
        List<Provider> availableUris = new ArrayList<>();
        PackageManager pm = mApplicationContext.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentContentProviders(new Intent(ProviderContract.ACTION_PROVIDER),
                PackageManager.GET_META_DATA);
        for (ResolveInfo resolveInfo: resolveInfos) {
            try {
                Bundle metadata = resolveInfo.providerInfo.metaData;
                if (providerHasValidMetadata(metadata)) {
                    availableUris.add(new Provider(pm, resolveInfo.providerInfo));
                }
            } catch (NullPointerException ignored) {
                // Instead of wrapping code in multiple ifs
            }
        }

        return availableUris;
    }

    /**
     *
     * @return a list of providers which are available for use. Note that a provider might appear
     * more than once here, if it's been configured with different settings (different folders/user accounts, etc).
     */
    public List<Provider> getConfiguredProviders() {
        List<Provider> availableUris = new ArrayList<>();
        // First get all providers which do not require configuration
        PackageManager pm = mApplicationContext.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentContentProviders(new Intent(ProviderContract.ACTION_PROVIDER),
                PackageManager.GET_META_DATA);
        for (ResolveInfo resolveInfo: resolveInfos) {
            try {
                Bundle metadata = resolveInfo.providerInfo.metaData;
                if (providerHasValidMetadata(metadata) && !providerRequiresConfig(metadata)) {
                    availableUris.add(new Provider(pm, resolveInfo.providerInfo));
                }
            } catch (NullPointerException ignored) {
                // Instead of wrapping code in multiple ifs
            }
        }

        // TODO include providers which have been setup by user

        return availableUris;
    }

    /**
     * Checks that a provider specifies correct metadata.
     * @param metadata for provider
     * @return true or false
     */
    public boolean providerHasValidMetadata(@NonNull Bundle metadata) {
        // Only one protocol level atm
        boolean result = (1 == metadata.getInt(METADATA_PROTOCOL_VERSION, -1));

        // If config is required, then a settingsactivity must be specified
        if (result && metadata.getBoolean(METADATA_REQUIRES_CONFIG, false)) {
            result = metadata.containsKey(METADATA_SETTINGS_ACTIVITY);
        }

        return result;
    }

    /**
     *
     * @param metadata for a given provider
     * @return true if provider is valid and specifies no required config
     */
    public boolean providerRequiresConfig(@NonNull Bundle metadata) {
        return metadata.getBoolean(METADATA_REQUIRES_CONFIG, false);
    }

    public static class Provider {
        public final String authority;
        public final Uri uriBase;
        public final Uri uriList;
        public final Uri uriDetails;
        public final String label;
        public final int icon;

        public Provider(final PackageManager pm, final ProviderInfo providerInfo) {
            label = providerInfo.loadLabel(pm).toString();
            authority = providerInfo.authority;
            uriBase = Uri.parse("content://" + authority);
            uriList = Uri.withAppendedPath(uriBase, "/list");
            uriDetails = Uri.withAppendedPath(uriBase, "/details");
            icon = providerInfo.getIconResource();
            /*if (null != providerInfo.metaData) {
                // Optional stuff like settingsActivity and capabilities
                String settingsActivity = providerInfo.metaData.getString("settingsActivity");
            }*/
        }
    }
}
