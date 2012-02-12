/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.nononsenseapps.notepad.sync;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker;
import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker.NotModifiedException;
import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker.PreconditionException;
import com.nononsenseapps.notepad.sync.googleapi.GoogleDBTalker;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTaskList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.text.format.Time;

import java.io.IOException;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider. This sample shows a basic 2-way sync
 * between the client and a sample server. It also contains an example of how to
 * update the contacts' status messages, which would be useful for a messaging
 * or social networking client.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "SyncAdapter";
	//public static final String AUTH_TOKEN_TYPE = "oauth2:https://www.googleapis.com/auth/tasks";
	public static final String AUTH_TOKEN_TYPE = "Manage your tasks"; // Alias for above
	public static final boolean NOTIFY_AUTH_FAILURE = false;

	private final AccountManager accountManager;

	private final Context mContext;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		// mAccountManager = AccountManager.get(context);
		accountManager = AccountManager.get(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		Log.d(TAG, "onPerformSync");
		// Initialize necessary stuff
		GoogleDBTalker dbTalker = new GoogleDBTalker(account.name, provider, syncResult);
		GoogleAPITalker apiTalker = GoogleAPITalker.getInstance();
		
		boolean connected = apiTalker.initialize(accountManager, account, AUTH_TOKEN_TYPE, NOTIFY_AUTH_FAILURE);
		
		if (connected) {
			Log.d(TAG, "We got an authToken atleast");
			
			try {
				// FIrst of all, we need the latest updated time later. So save that for now.
				String lastUpdated = dbTalker.getLastUpdated(account.name);
				
				// Upload locally modified objects
				for (GoogleTaskList list: dbTalker.getModifiedLists()) {
					GoogleTaskList result = apiTalker.uploadList(list);
					if (result != null) {
						dbTalker.uploaded(result);
					}
					else {
						handleConflict(dbTalker, apiTalker, list);
					}
					// List must exist on server before we can upload tasks inside
					list.uploadModifiedTasks(apiTalker, dbTalker);
					// Uploading the tasks will have changed the list's etag.
					// That means it will be included in the remotely modified download, so we'll get it there.
				}
				
				// Download remotely modified objects
				for (GoogleTaskList list: apiTalker.getModifiedLists(dbTalker.getAllLists())) {
					Log.d(TAG, "Saving modified: " + list.toJSON());
					dbTalker.SaveToDatabase(list);
					
					// Also save any modified tasks contained in that list
					// It is vital that this is done after we saved the list to database
					// or inserts here will fail for new lists.
					list.downloadModifiedTasks(apiTalker, dbTalker, lastUpdated);
				}
				
				// Erase deleted stuff
				//dbTalker.clearDeleted();
				
				Log.d(TAG, "Sync Complete!");
				
				
			} catch (ClientProtocolException e) {
				Log.d(TAG, "ClientProtocolException: " + e.getLocalizedMessage());
			} catch (JSONException e) {
				Log.d(TAG, "JSONException: " + e.getLocalizedMessage());
			} catch (PreconditionException e) {
				Log.d(TAG, "PreconditionException");
			} catch (NotModifiedException e) {
				Log.d(TAG, "NotModifiedException");
			} catch (IOException e) {
				Log.d(TAG, "IOException: " + e.getLocalizedMessage());
			} catch (RemoteException e) {
				Log.d(TAG, "RemoteException: " + e.getLocalizedMessage());
			}
			
			
			
			//-----------------------------------

//		} catch (final AuthenticatorException e) {
//			Log.e(TAG, "AuthenticatorException", e);
//			syncResult.stats.numParseExceptions++;
//		} catch (final OperationCanceledException e) {
//			Log.e(TAG, "OperationCanceledExcetpion", e);
//		} catch (final IOException e) {
//			Log.e(TAG, "IOException", e);
//			syncResult.stats.numIoExceptions++;
//		}
//		// catch (final AuthenticationException e) {
//		// Log.e(TAG, "AuthenticationException", e);
//		// syncResult.stats.numAuthExceptions++;
//		// }
//		catch (final ParseException e) {
//			Log.e(TAG, "ParseException", e);
//			syncResult.stats.numParseExceptions++;
//		}
//		// catch (final JSONException e) {
//		// Log.e(TAG, "JSONException", e);
//		// syncResult.stats.numParseExceptions++;
//		// }
		}
		else {
			// return real failure
			syncResult.stats.numAuthExceptions++;
		}
	}

	private void handleConflict(GoogleDBTalker dbTalker,
			GoogleAPITalker apiTalker, GoogleTaskList localList) throws ClientProtocolException, JSONException, PreconditionException, NotModifiedException, IOException, RemoteException {
		localList.etag = null; // Set this to null so we dont do any if-none-match gets
		GoogleTaskList remoteList = apiTalker.getList(localList);
		// Last updated one wins
		Time local = new Time();
		local.parse3339(localList.updated);
		Time remote = new Time();
		remote.parse3339(remoteList.updated);
		if (Time.compare(remote, local) >= 0) {
			Log.d(TAG, "Handling conflict: remote was newer");
			// remote is greater than local (or equal), save that to database
			remoteList.dbId = localList.dbId;
			dbTalker.SaveToDatabase(remoteList);
		} else {
			Log.d(TAG, "Handling conflict: local was newer");
			// Local is greater than remote, upload it.
			localList.etag = null;
			long dbId = localList.dbId;
			localList = apiTalker.uploadList(localList);
			localList.dbId = dbId;
			// Save new etag etc to db
			dbTalker.SaveToDatabase(localList);
		}
	}
}
