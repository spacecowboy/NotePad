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
package com.nononsenseapps.notepad.sync.orgsync;

/**
 * An interface which defines a "Monitor". A monitor is an object which
 * monitors a specific sync source for changes, such as a FileMonitor.
 */
public interface Monitor {

	/**
	 * Start monitoring. Call handler on changes.
	 */
	void startMonitor(final OrgSyncService.SyncHandler handler);

	/**
	 * Pausing, it might be restarted later.
	 */
	void pauseMonitor();

	/**
	 * Service is destroying itself. Remove any references.
	 */
	void terminate();
}
