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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.Bundle;

import com.nononsenseapps.notepad.data.model.gtasks.GoogleTask;
import com.nononsenseapps.notepad.data.model.gtasks.GoogleTaskList;
import com.nononsenseapps.notepad.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

/**
 * Communication client with Google Tasks API.
 */
public class GoogleTasksClient {
    static final String BASE_URL = "https://www.googleapis.com/tasks/v1";
    // https://www.googleapis.com/auth/tasks.readonly
    private static final String OAUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/tasks";
    private static final String TAG = "GoogleTasksClient";
    final GoogleTasksAPI api;
    final String accountName;
    private final String key;

    public GoogleTasksClient(final String token, final String key, final String accountName) {
        this.api = GetGoogleTasksAPI(token);
        this.key = key;
        this.accountName = accountName;
    }

    public static String getAuthToken(AccountManager accountManager, Account account, boolean notifyAuthFailure) throws AuthenticatorException, OperationCanceledException, IOException {
        Log.d(TAG, "getAuthToken");
        String authToken = null;
        // Might be invalid in the cache
        authToken = accountManager.blockingGetAuthToken(account, OAUTH_SCOPE, notifyAuthFailure);

        Log.d(TAG, "invalidate auth token: " + authToken);
        accountManager.invalidateAuthToken("com.google", authToken);

        authToken = accountManager.blockingGetAuthToken(account, OAUTH_SCOPE, notifyAuthFailure);
        Log.d(TAG, "fresh auth token: " + authToken);

        return authToken;
    }

    /**
     * Get an AuthToken asynchronously. Use this in a foreground activity which will ask the user
     * for permission.
     */
    public static void getAuthTokenAsync(Activity activity, Account account,
            AccountManagerCallback<Bundle> callback) {
        Log.d(TAG, "getAuthTokenAsync");
        AccountManager.get(activity).getAuthToken(account, OAUTH_SCOPE, Bundle.EMPTY, activity,
                callback, null);
    }

    static GoogleTasksAPI GetGoogleTasksAPI(final String token) throws IllegalArgumentException {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Auth token can't be empty!");
        }
        Log.d(TAG, "Using token: " + token);
        // Create a very simple REST adapter, with oauth header
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(BASE_URL)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("Authorization", "Bearer " + token);
                    }
                })
                .build();
        // Create an instance of the interface
        return restAdapter.create(GoogleTasksAPI.class);
    }


    public void listLists(final ArrayList<GoogleTaskList> remoteLists) {
        GoogleTasksAPI.ListListsResponse response;
        String pageToken = null;
        do {
            if (pageToken == null) {
                response = api.listLists(key);
            } else {
                response = api.listLists(key, pageToken);
            }
            pageToken = response.nextPageToken;

            if (response.items == null) {
                // No items
                break;
            } else {
                for (GoogleTasksAPI.TaskListResource taskListResource : response.items) {
                    remoteLists.add(new GoogleTaskList(taskListResource, accountName));
                }
            }
        } while (pageToken != null);
    }

    public void insertList(GoogleTaskList list) {
        GoogleTasksAPI.TaskListResource result = api.insertList(list.toTaskListResource(), key);
        list.updateFromTaskListResource(result);
    }

    public void deleteList(GoogleTaskList list) {
        api.deleteList(list.remoteId, key);
    }

    public void patchList(GoogleTaskList list) {
        GoogleTasksAPI.TaskListResource result = api.patchList(list.remoteId, list
                .toTaskListResource(), key);

        list.updateFromTaskListResource(result);
    }

    public void insertTask(GoogleTask task, GoogleTaskList taskList) {
        GoogleTasksAPI.TaskResource result = api.insertTask(taskList.remoteId, task
                .toTaskResource(), key);
        task.updateFromTaskResource(result);
    }

    public void deleteTask(GoogleTask task, GoogleTaskList taskList) {
        api.deleteTask(taskList.remoteId, task.remoteId, key);
    }

    public void patchTask(GoogleTask task, GoogleTaskList taskList) {
        GoogleTasksAPI.TaskResource result = api.patchTask(taskList.remoteId, task.remoteId, task
                .toTaskResource(), key);
        task.updateFromTaskResource(result);
    }

    public List<GoogleTask> listTasks(GoogleTaskList taskList) {
        ArrayList<GoogleTask> remoteTasks = new ArrayList<GoogleTask>();
        GoogleTasksAPI.ListTasksResponse response;
        String pageToken = null;
        do {
            if (pageToken == null) {
                response = api.listTasks(taskList.remoteId, key, true);
            } else {
                response = api.listTasks(taskList.remoteId, key, true, pageToken);
            }
            pageToken = response.nextPageToken;

            if (response.items == null) {
                // No items
                break;
            } else {
                for (GoogleTasksAPI.TaskResource taskResource : response.items) {
                    GoogleTask task = new GoogleTask(taskResource, accountName);
                    task.listdbid = taskList.dbid;
                    remoteTasks.add(task);
                }
            }
        } while (pageToken != null);
        return remoteTasks;
    }
}
