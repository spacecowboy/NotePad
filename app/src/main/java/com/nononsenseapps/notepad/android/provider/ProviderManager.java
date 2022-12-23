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
import android.net.Uri;
import android.os.Bundle;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles things related to (possibly 3rd party) providers.
 */
public final class ProviderManager {

	public final String METADATA_PROTOCOL_VERSION = "protocolVersion";
	public final String METADATA_REQUIRES_CONFIG = "requiresConfig";
	public final String METADATA_SETTINGS_ACTIVITY = "settingsActivity";

	private final Context applicationContext;

	public ProviderManager(@NotNull Context context) {
		this.applicationContext = context.getApplicationContext();
	}

	/**
	 * @return a list of providers which are available for use/setup.
	 */
	@NotNull
	public final List<Provider> getAvailableProviders() {
		ArrayList<Provider> availableUris = new ArrayList<>();
		PackageManager pm = this.applicationContext.getPackageManager();
		var var10000 = pm
				.queryIntentContentProviders(
						new Intent(com.nononsenseapps.notepad.providercontract.ProviderContract.ACTION_PROVIDER),
						PackageManager.GET_META_DATA);

		for (var resolveInfo : var10000) {
			var metadata = resolveInfo.providerInfo.metaData;
			if (providerHasValidMetadata(metadata)) {
				availableUris.add(new Provider(pm, resolveInfo.providerInfo));
			}
		}

		return availableUris;
	}

	/**
	 * @return a list of providers which are available for use. Note that a provider might appear
	 * * more than once here, if it's been configured with different settings (different folders/user accounts, etc).
	 */
	// First get all providers which do not require configuration
	// Instead of wrapping code in multiple ifs
	// TODO include providers which have been setup by user
	@NotNull
	public final ArrayList<Provider> getConfiguredProviders() {
		var availableUris = new ArrayList<Provider>();
		var pm = applicationContext.getPackageManager();
		var resolveInfos = pm.queryIntentContentProviders(
				new Intent(com.nononsenseapps.notepad.providercontract.ProviderContract.ACTION_PROVIDER),
				PackageManager.GET_META_DATA);
		for (var resolveInfo : resolveInfos) {
			var metadata = resolveInfo.providerInfo.metaData;
			if (providerHasValidMetadata(metadata) && !providerRequiresConfig(metadata)) {
				availableUris.add(new Provider(pm, resolveInfo.providerInfo));
			}
		}

		return availableUris;
	}

	/**
	 * Checks that a provider specifies correct metadata.
	 *
	 * @param metadata for provider
	 *                 *
	 * @return true or false
	 */
	public final boolean providerHasValidMetadata(@NotNull Bundle metadata) {
		// Only one protocol level atm
		var result = 1 == metadata.getInt(METADATA_PROTOCOL_VERSION, -1);

		// If config is required, then a settingsactivity must be specified
		if (result && metadata.getBoolean(METADATA_REQUIRES_CONFIG, false)) {
			result = metadata.containsKey(METADATA_SETTINGS_ACTIVITY);
		}

		return result;
	}

	/**
	 * @param metadata for a given provider
	 *                 *
	 * @return true if provider is valid and specifies no required config
	 */
	public final boolean providerRequiresConfig(@NotNull Bundle metadata) {
		return metadata.getBoolean(METADATA_REQUIRES_CONFIG, false);
	}


	public static final class Provider {
		@NotNull
		private final String authority;
		@NotNull
		private final Uri uriBase;
		@NotNull
		private final Uri uriList;
		@NotNull
		private final Uri uriDetails;
		@NotNull
		private final String label;
		private final int icon;

		public Provider(@NotNull PackageManager pm, @NotNull ProviderInfo providerInfo) {

			this.label = providerInfo.loadLabel(pm).toString();
			this.authority = providerInfo.authority;
			this.uriBase = Uri.parse("content://" + this.authority);
			this.uriList = Uri.withAppendedPath(this.uriBase, "/list");
			this.uriDetails = Uri.withAppendedPath(this.uriBase, "/details");
			this.icon = providerInfo.getIconResource();
			if (null != providerInfo.metaData) {
                // Optional stuff like settingsActivity and capabilities
                // String settingsActivity = providerInfo.metaData.getString("settingsActivity");
            }
		}

		@NotNull
		public final String getAuthority() {
			return this.authority;
		}

		@NotNull
		public final Uri getUriBase() {
			return this.uriBase;
		}

		@NotNull
		public final Uri getUriList() {
			return this.uriList;
		}

		@NotNull
		public final Uri getUriDetails() {
			return this.uriDetails;
		}

		@NotNull
		public final String getLabel() {
			return this.label;
		}

		public final int getIcon() {
			return this.icon;
		}
	}
}
