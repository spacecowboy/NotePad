/*
 * Copyright (c) 2015. Jonas Kalderstam
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Helper class which handles runtime permissions.
 */
public final class PermissionsHelper {

	public static final String[] FOR_GOOGLETASKS = new String[] {
			Manifest.permission.GET_ACCOUNTS, Manifest.permission.WRITE_SYNC_SETTINGS,
			Manifest.permission.READ_SYNC_SETTINGS, Manifest.permission.READ_SYNC_STATS,
			Manifest.permission.INTERNET };

	/**
	 * Permissions to write to the internal storage
	 */
	public static final String[] FOR_SDCARD =
			new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE };

	/**
	 * Permissions to show notifications
	 */
	public static final String[] FOR_NOTIFICATIONS =
			new String[] { "android.permission.POST_NOTIFICATIONS" };

	public static final int REQCODE_WRITE_SD = 1;
	public static final int REQCODE_GOOGLETASKS = 2;
	public static final int REQCODE_NOTIFICATIONS = 3;

	/**
	 * @return TRUE if all the specified permissions are granted, FALSE otherwise
	 */
	public static boolean hasPermissions(@NonNull Context context, String... permissions) {
		for (String permission : permissions) {
			boolean hasPermission = PackageManager.PERMISSION_GRANTED
					== ContextCompat.checkSelfPermission(context, permission);
			if (!hasPermission) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param permissions  that were requested
	 * @param grantResults of the request
	 * @return true if all results were granted, false otherwise
	 */
	public static boolean permissionsGranted(@NonNull String[] permissions,
											 @NonNull int[] grantResults) {
		return permissions.length > 0 && allGranted(grantResults);
	}

	private static boolean allGranted(int[] items) {
		for (int item : items) {
			if (PackageManager.PERMISSION_GRANTED != item) {
				return false;
			}
		}
		return true;
	}
}
