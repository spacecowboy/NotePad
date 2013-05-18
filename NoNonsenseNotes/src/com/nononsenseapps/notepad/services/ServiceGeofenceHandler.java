package com.nononsenseapps.notepad.services;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.location.Geofence;
import com.googlecode.androidannotations.annotations.EService;
import com.nononsenseapps.util.GeofenceUtils.REMOVE_TYPE;
import com.nononsenseapps.util.GeofenceUtils.REQUEST_TYPE;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

@EService
public class ServiceGeofenceHandler extends Service {
	
	// Store the current request
    private REQUEST_TYPE mRequestType;

    // Store the current type of removal
    private REMOVE_TYPE mRemoveType;

    // Store a list of geofences to add
    List<Geofence> mCurrentGeofences = new ArrayList<Geofence>();

    // Add geofences handler
    //private GeofenceRequester mGeofenceRequester;
    // Remove geofences handler
   // private GeofenceRemover mGeofenceRemover;

    /*
     * An instance of an inner class that receives broadcasts from listeners and from the
     * IntentService that receives geofence transition events
     */
    //private GeofenceSampleReceiver mBroadcastReceiver;

    // An intent filter for the broadcast receiver
    //private IntentFilter mIntentFilter;

    // Store the list of geofences to remove
    //private List<String> mGeofenceIdsToRemove;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_NOT_STICKY;
	}
}
