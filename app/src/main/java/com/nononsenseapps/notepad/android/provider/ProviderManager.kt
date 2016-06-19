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

package com.nononsenseapps.notepad.android.provider

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.net.Uri
import android.os.Bundle

import java.util.ArrayList

/**
 * This class handles things related to (possibly 3rd party) providers.
 */
class ProviderManager(context: Context) {
    val METADATA_PROTOCOL_VERSION = "protocolVersion"
    val METADATA_REQUIRES_CONFIG = "requiresConfig"
    val METADATA_SETTINGS_ACTIVITY = "settingsActivity"

    private val applicationContext = context.applicationContext;

    /**

     * @return a list of providers which are available for use/setup.
     */
    // Instead of wrapping code in multiple ifs
    val availableProviders: List<Provider>
        get() {
            val availableUris = ArrayList<Provider>()
            val pm = applicationContext.packageManager
            val resolveInfos = pm.queryIntentContentProviders(Intent(ProviderContract.ACTION_PROVIDER),
                    PackageManager.GET_META_DATA)
            for (resolveInfo in resolveInfos) {
                val metadata = resolveInfo.providerInfo.metaData
                if (providerHasValidMetadata(metadata)) {
                    availableUris.add(Provider(pm, resolveInfo.providerInfo))
                }
            }

            return availableUris
        }

    /**

     * @return a list of providers which are available for use. Note that a provider might appear
     * * more than once here, if it's been configured with different settings (different folders/user accounts, etc).
     */
    // First get all providers which do not require configuration
    // Instead of wrapping code in multiple ifs
    // TODO include providers which have been setup by user
    val configuredProviders: List<Provider>
        get() {
            val availableUris = ArrayList<Provider>()
            val pm = applicationContext.packageManager
            val resolveInfos = pm.queryIntentContentProviders(Intent(ProviderContract.ACTION_PROVIDER),
                    PackageManager.GET_META_DATA)
            for (resolveInfo in resolveInfos) {
                val metadata = resolveInfo.providerInfo.metaData
                if (providerHasValidMetadata(metadata) && !providerRequiresConfig(metadata)) {
                    availableUris.add(Provider(pm, resolveInfo.providerInfo))
                }
            }

            return availableUris
        }

    /**
     * Checks that a provider specifies correct metadata.
     * @param metadata for provider
     * *
     * @return true or false
     */
    fun providerHasValidMetadata(metadata: Bundle): Boolean {
        // Only one protocol level atm
        var result = 1 == metadata.getInt(METADATA_PROTOCOL_VERSION, -1)

        // If config is required, then a settingsactivity must be specified
        if (result && metadata.getBoolean(METADATA_REQUIRES_CONFIG, false)) {
            result = metadata.containsKey(METADATA_SETTINGS_ACTIVITY)
        }

        return result
    }

    /**

     * @param metadata for a given provider
     * *
     * @return true if provider is valid and specifies no required config
     */
    fun providerRequiresConfig(metadata: Bundle): Boolean {
        return metadata.getBoolean(METADATA_REQUIRES_CONFIG, false)
    }

    class Provider(pm: PackageManager, providerInfo: ProviderInfo) {
        val authority: String
        val uriBase: Uri
        val uriList: Uri
        val uriDetails: Uri
        val label: String
        val icon: Int

        init {
            label = providerInfo.loadLabel(pm).toString()
            authority = providerInfo.authority
            uriBase = Uri.parse("content://" + authority)
            uriList = Uri.withAppendedPath(uriBase, "/list")
            uriDetails = Uri.withAppendedPath(uriBase, "/details")
            icon = providerInfo.iconResource
            /*if (null != providerInfo.metaData) {
                // Optional stuff like settingsActivity and capabilities
                String settingsActivity = providerInfo.metaData.getString("settingsActivity");
            }*/
        }
    }
}
