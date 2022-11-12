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

package com.nononsenseapps.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Helper class which handles runtime permissions.
 */
public class PermissionsHelper {

	public static final String[] PERMISSIONS_GTASKS = new String[] { Manifest.permission.GET_ACCOUNTS,
			Manifest.permission.WRITE_SYNC_SETTINGS, Manifest.permission.READ_SYNC_SETTINGS,
			Manifest.permission.READ_SYNC_STATS, Manifest.permission.INTERNET };

	public static final String[] PERMISSIONS_SD =
			new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE };

	public static final int REQUEST_CODE_SD_PERMISSIONS = 1;
	public static final int REQUEST_CODE_GTASKS_PERMISSIONS = 2;

	public static boolean hasPermissions(@NonNull Context context, String... permissions) {
		for (String permission : permissions) {
			if (!hasPermission(context, permission)) {
				return false;
			}
		}
		return true;
	}

	private static boolean hasPermission(@NonNull Context context, @NonNull String permission) {
		return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, permission);
	}

	/**
	 * @param permissions  that were requested
	 * @param grantResults of the request
	 * @return true if all results were granted, false otherwise
	 */
	public static boolean permissionsGranted(@NonNull String[] permissions, @NonNull int[] grantResults) {
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
