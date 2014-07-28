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

package com.nononsenseapps.notepad.sync.orgsync;

/**
 * An interface which defines a "Monitor". A monitor is an object which
 * monitors a specific sync source for changes, such as a FileMonitor or
 * Dropbox PathListener.
 */
public interface Monitor {
    /**
     * Start monitoring. Call handler on changes.
     * @param handler
     */
    public void startMonitor(final OrgSyncService.SyncHandler handler);

    /**
     * Pausing, it might be restarted later.
     */
    public void pauseMonitor();

    /**
     * Service is destroying itself. Remove any references.
     */
    public void terminate();
}
