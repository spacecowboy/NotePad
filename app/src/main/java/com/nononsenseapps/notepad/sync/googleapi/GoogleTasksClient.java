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

package com.nononsenseapps.notepad.sync.googleapi;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.nononsenseapps.helpers.NnnLogger;



import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;

/**
 * Communication client with Google Tasks API.
 */
public class GoogleTasksClient {

	// TODO useless, remove

	static final String BASE_URL = "https://www.googleapis.com/tasks/v1";
	// https://www.googleapis.com/auth/tasks.readonly
	private static final String OAUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/tasks";
	final GoogleTasksAPI api;
	final String accountName;
	private final String key;

	public GoogleTasksClient(final String token, final String key, final String accountName) {
		this.api = GetGoogleTasksAPI(token);
		this.key = key;
		this.accountName = accountName;
	}

	public static String getAuthToken(AccountManager accountManager, @NonNull Account account,
									  boolean notifyAuthFailure)
			throws AuthenticatorException, OperationCanceledException, IOException {
		NnnLogger.debug(GoogleTasksClient.class, "getAuthToken");
		String authToken;
		// Might be invalid in the cache
		authToken = accountManager.blockingGetAuthToken(account, OAUTH_SCOPE, notifyAuthFailure);

		NnnLogger.debug(GoogleTasksClient.class, "invalidate auth token: " + authToken);
		accountManager.invalidateAuthToken("com.google", authToken);

		authToken = accountManager.blockingGetAuthToken(account, OAUTH_SCOPE, notifyAuthFailure);
		NnnLogger.debug(GoogleTasksClient.class, "fresh auth token: " + authToken);

		return authToken;
	}

	/**
	 * Get an AuthToken asynchronously. Use this in a foreground activity which will ask the user
	 * for permission.
	 */
	public static void getAuthTokenAsync(FragmentActivity activity, @NonNull Account account,
										 AccountManagerCallback<Bundle> callback) {
		NnnLogger.debug(GoogleTasksClient.class, "getAuthTokenAsync");

		AccountManager.get(activity)
				.getAuthToken(account, OAUTH_SCOPE, Bundle.EMPTY, activity, callback, null);
	}

	static GoogleTasksAPI GetGoogleTasksAPI(final String token) throws IllegalArgumentException {
		if (token == null || token.isEmpty()) {
			throw new IllegalArgumentException("Auth token can't be empty!");
		}
		NnnLogger.debug(GoogleTasksClient.class, "Using token: " + token);

		// TODO untested! try to get it working
		OkHttpClient httpClient = new OkHttpClient.Builder()
				.addInterceptor(chain -> {
					Request original = chain.request();

					// Customize the request
					Request newRequest = original.newBuilder()
							.header("Authorization", "Bearer " + token)
							.method(original.method(), original.body())
							.build();

					// TODO here you may want to add some logging code
					okhttp3.Response response = chain.proceed(newRequest);
					return response;
				})
				.build();

		// Create a very simple REST adapter, with oauth header
		Retrofit restAdapter = new Retrofit.Builder()
				.baseUrl(BASE_URL)
				.client(httpClient)
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
		ArrayList<GoogleTask> remoteTasks = new ArrayList<>();
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
