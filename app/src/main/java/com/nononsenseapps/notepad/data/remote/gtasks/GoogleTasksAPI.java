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

package com.nononsenseapps.notepad.data.remote.gtasks;

import java.util.List;

import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.PATCH;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Google Tasks REST API.
 */
public interface GoogleTasksAPI {

    @GET("/users/@me/lists")
    ListListsResponse listLists(@Query("key") String key);

    @GET("/users/@me/lists")
    ListListsResponse listLists(@Query("key") String key, @Query("pageToken") String pageToken);

    // @Query("updatedMin") String updatedMin
    @GET("/lists/{tasklist}/tasks")
    ListTasksResponse listTasks(@Path("tasklist") String tasklist, @Query("key") String key,
            @Query("showDeleted") boolean showDeleted);

    // @Query("updatedMin") String updatedMin
    @GET("/lists/{tasklist}/tasks")
    ListTasksResponse listTasks(@Path("tasklist") String tasklist, @Query("key") String key,
            @Query("showDeleted") boolean showDeleted, @Query("pageToken") String pageToken);

    // Lists
    @POST("/users/@me/lists")
    TaskListResource insertList(@Body TaskListResource taskListResource, @Query("key") String key);

    @PATCH("/users/@me/lists/{tasklist}")
    TaskListResource patchList(@Path("tasklist") String tasklist, @Body TaskListResource
            taskListResource, @Query("key") String key);

    @DELETE("/users/@me/lists/{tasklist}")
    VoidResponse deleteList(@Path("tasklist") String tasklist, @Query("key") String key);

    // Tasks
    @POST("/lists/{tasklist}/tasks")
    TaskResource insertTask(@Path("tasklist") String tasklist, @Body TaskResource taskResource,
            @Query("key") String key);

    @PATCH("/lists/{tasklist}/tasks/{task}")
    TaskResource patchTask(@Path("tasklist") String tasklist, @Path("task") String task, @Body
    TaskResource taskResource, @Query("key") String key);

    @DELETE("/lists/{tasklist}/tasks/{task}")
    VoidResponse deleteTask(@Path("tasklist") String tasklist, @Path("task") String task, @Query("key")
    String key);

    class ListListsResponse {
        String etag;
        String nextPageToken;
        List<TaskListResource> items;
    }

    class ListTasksResponse {
        String etag;
        String nextPageToken;
        List<TaskResource> items;
    }

    class TaskResource {
        // Task identifier.
        public String id;
        // ETag of the resource.
        public String etag;
        // Title of the task.
        public String title;
        // Last modification time of the task (as a RFC 3339 timestamp).
        public String updated;
        // URL pointing to this task.
        public String selfLink;
        // Parent task identifier. This field is omitted if it is a top-level task. This field is
        // read-only. Use the "move" method to move the task under a different parent or to the
        // top level.
        public String parent;
        // indicating the position of the task among its sibling tasks under the same parent task
        // or at the top level. If this string is greater than another task's corresponding
        // position string according to lexicographical ordering, the task is positioned after
        // the other task under the same parent task (or at the top level). This field is
        // read-only. Use the "move" method to move the task to another position.
        public String position;
        // Notes describing the task. Optional.
        public String notes;
        // Status of the task. This is either "needsAction" or "completed".
        public String status;
        // Due date of the task (as a RFC 3339 timestamp). Optional.
        public String due;
        // Flag indicating whether the task has been deleted. The default if False.
        public Boolean deleted;
        // Flag indicating whether the task is hidden. This is the case if the task had been
        // marked completed when the task list was last cleared. The default is False. This field
        // is read-only.
        public Boolean hidden;
    }

    class TaskListResource {
        public String id; // Task list identifier.
        public String etag; // ETag of the resource.
        public String title; // Title of the task list.
        public String selfLink; // URL pointing to this task list.
        public String updated; // Last modification time of the task list (as a RFC 3339 timestamp).
    }

    class VoidResponse {
    }
}
