package com.nononsenseapps.notepad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.FragmentById;
import com.googlecode.androidannotations.annotations.SeekBarProgressChange;
import com.googlecode.androidannotations.annotations.SeekBarTouchStart;
import com.googlecode.androidannotations.annotations.SeekBarTouchStop;
import com.googlecode.androidannotations.annotations.SystemService;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SearchView.OnQueryTextListener;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBoundsCreator;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nononsenseapps.helpers.ActivityHelper;
import com.nononsenseapps.notepad.ActivityLocation.GeofenceSampleReceiver;
import com.nononsenseapps.notepad.database.Notification;
import com.nononsenseapps.util.GeofenceRemover;
import com.nononsenseapps.util.GeofenceRequester;
import com.nononsenseapps.util.GeofenceUtils;
import com.nononsenseapps.util.GeofenceUtils.REMOVE_TYPE;
import com.nononsenseapps.util.GeofenceUtils.REQUEST_TYPE;
import com.nononsenseapps.util.LocationSuggestionsProvider;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

@EActivity(R.layout.activity_map)
public class ActivityLocation extends Activity {

	public static final String EXTRA_ID = "extra_id";
	public static final String EXTRA_LATITUDE = "latitude";
	public static final String EXTRA_LONGITUDE = "longitude";
	public static final String EXTRA_RADIUS = "radius";
	public static final String EXTRA_LOCATION_NAME = "location_name";

	final static int fillColor = 0xaa33b5e5;
	final static int lineColor = 0xff33b5e5;

	double startLatitude = -999;
	double startLongitude = -999;
	double startRadius = -999;

	@SystemService
	LocationManager locationManager;

	@SystemService
	SearchManager searchManager;
	
	@SystemService
	InputMethodManager inputManager;

	@FragmentById
	MapFragment mapFragment;
	GoogleMap mMap;

	@ViewById
	SeekBar radiusSeekBar;

	@ViewById
	SearchView searchView;

	// Plus minimum
	final int radiusMin = 20;
	double radius = 20 + radiusMin;
	String locationName = "";

	final int zoomLevel = 12;

	Circle circle = null;
	Marker marker = null;

	private long mId = -1L;

	private REQUEST_TYPE mRequestType;
	private REMOVE_TYPE mRemoveType;

	private GeofenceRequester mGeofenceRequester;

	private GeofenceRemover mGeofenceRemover;

	private List<Geofence> mCurrentGeofences;
	private List<String> mGeofenceIdsToRemove;

	private GeofenceSampleReceiver mBroadcastReceiver;

	private IntentFilter mIntentFilter;

	private SearchRecentSuggestions suggestions;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		suggestions = new SearchRecentSuggestions(this,
				LocationSuggestionsProvider.AUTHORITY,
				LocationSuggestionsProvider.MODE);

		Intent i = getIntent();
		if (i == null || i.getExtras() == null
				|| !i.getExtras().containsKey(EXTRA_ID)) {
			finish();
		}
		else {
			mId = i.getExtras().getLong(EXTRA_ID);
			if (i.getExtras().containsKey(EXTRA_LATITUDE)) {
				startLatitude = i.getExtras().getDouble(EXTRA_LATITUDE);
				startLongitude = i.getExtras().getDouble(EXTRA_LONGITUDE);
				startRadius = i.getExtras().getDouble(EXTRA_RADIUS);
				
				Log.d("nononsenseapps", "JONAS 1: " + startLatitude + " " + startLongitude + " " + startRadius);
			}
			else {
				Toast.makeText(this, R.string.need_location_help,
						Toast.LENGTH_SHORT).show();
			}
		}

		mCurrentGeofences = new ArrayList<Geofence>();
		mGeofenceIdsToRemove = new ArrayList<String>();

		// Instantiate a Geofence requester
		mGeofenceRequester = new GeofenceRequester(this);

		// Instantiate a Geofence remover
		mGeofenceRemover = new GeofenceRemover(this);

		// Need to know when we have success
		// Create a new broadcast receiver to receive updates from the listeners
		// and service
		mBroadcastReceiver = new GeofenceSampleReceiver();

		// Create an intent filter for the broadcast receiver
		mIntentFilter = new IntentFilter();

		// Action for broadcast Intents that report successful addition of
		// geofences
		mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);

		// Action for broadcast Intents that report successful removal of
		// geofences
		mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);

		// Action for broadcast Intents containing various types of geofencing
		// errors
		mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);

		// All Location Services sample apps use this category
		mIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (!servicesConnected()) {
			finish();
		}

		// Register the broadcast receiver to receive status updates
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mBroadcastReceiver, mIntentFilter);
	}

	void addFence(final LatLng point) {
		// Remove previous markers
		mMap.clear();
		locationName = "";

		CircleOptions co = new CircleOptions();
		co.center(point).radius(radius).fillColor(fillColor)
				.strokeColor(lineColor).strokeWidth(2);
		// Add radius
		circle = mMap.addCircle(co);

		// Set location name to coordinates while we fetch the real name
		locationName = "" + point.latitude + ", " + point.longitude;

		// Add marker
		marker = mMap.addMarker(new MarkerOptions().position(point)
				.draggable(true).title(locationName));

		// Get actual location name
		getLocationName(point);
	}

	@Background
	void getLocationName(final LatLng point) {
		locationName = "" + point.latitude + ", " + point.longitude;

		final Geocoder geocoder = new Geocoder(getApplicationContext(),
				ActivityHelper.getUserLocale(this));
		try {
			List<Address> listAddresses = geocoder.getFromLocation(
					point.latitude, point.longitude, 1);
			if (null != listAddresses && listAddresses.size() > 0) {
				String _Location = "";
				// _Location += listAddresses.get(0).getAdminArea();
				// _Location += " " + listAddresses.get(0).getFeatureName();
				// _Location += " " + listAddresses.get(0).getThoroughfare();
				for (int i = 0; i < listAddresses.get(0)
						.getMaxAddressLineIndex(); i++) {
					if (_Location.length() > 0) _Location += " ";
					_Location += listAddresses.get(0).getAddressLine(i);
				}

				if (_Location.length() > 0) {
					locationName = _Location;
				}

				reportLocationName(locationName);

			}
		}
		catch (IOException e) {
		}
	}

	@UiThread
	void reportLocationName(final String text) {
		if (marker != null) {
			marker.setTitle(locationName);
		}
		// TODO remove?
		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT)
				.show();
	}

	@AfterViews
	protected void setupSearch() {
		// Assumes current activity is the searchable activity
		searchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));
		searchView.setQueryRefinementEnabled(true);
		searchView.setSubmitButtonEnabled(false);
		searchView.setIconifiedByDefault(false);

		searchView.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(final String query) {
				// Run in background
				// JONAS
				
				inputManager.hideSoftInputFromWindow(searchView
	                    .getWindowToken(), 0);

				final Geocoder geocoder = new Geocoder(getApplicationContext(),
						ActivityHelper.getUserLocale(ActivityLocation.this));

				try {
					List<Address> places = geocoder.getFromLocationName(query,
							10);

					if (null == places || places.isEmpty()) {

					}
					else {
						Address place = places.get(0);
						final LatLng point = new LatLng(place.getLatitude(),
								place.getLongitude());
						// mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point,
						// zoomLevel));

						refocusMap(point);
						addFence(point);

						String name = "";
						// _Location += listAddresses.get(0).getAdminArea();
						// _Location += " " +
						// listAddresses.get(0).getFeatureName();
						// _Location += " " +
						// listAddresses.get(0).getThoroughfare();
						for (int i = 0; i < place.getMaxAddressLineIndex(); i++) {
							if (name.length() > 0) name += " ";
							name += place.getAddressLine(i);
						}

						suggestions.saveRecentQuery(query, name);
					}
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				return true;
			}

			@Override
			public boolean onQueryTextChange(final String query) {
				return false;
			}
		});
		
		// In case we have a search intent
		onNewIntent(getIntent());
	}

	protected void onNewIntent(final Intent intent) {
		if (intent == null) {
			return;
		}
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			searchView.setQuery(intent.getStringExtra(SearchManager.QUERY),
					true);
		}
	}

	void refocusMap() {
		// if (marker == null) {
		refocusMap(mMap.getCameraPosition().target);
		// } else {
		// refocusMap(marker.getPosition());
		// }
	}
	
	LatLngBounds getBounds(final LatLng point) {
		// This is a hack to calculate an inaccurate location roughly that
				// distant
				// Inaccurate but very quick to calculate
				LatLng sw = new LatLng(point.latitude - 2 * radius / (1000.0 * 160),
						point.longitude - 2 * radius / (1000.0 * 160));
				LatLng ne = new LatLng(point.latitude + 2 * radius / (1000.0 * 160),
						point.longitude + 2 * radius / (1000.0 * 160));
				LatLngBounds bounds = new LatLngBounds(sw, ne);
				
				return bounds;
	}

	void refocusMap(final LatLng point) {
		mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getBounds(point), 0));
	}
	
	void moveMap(final LatLng point) {
		//mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(getBounds(point), 0, 0, 0));
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, zoomLevel));
	}

	@AfterViews
	protected void setupMap() {
		mMap = mapFragment.getMap();
		mMap.setMyLocationEnabled(false);
		Log.d("nononsenseapps", "JONAS startRadius: " + startRadius);
		if (startRadius > 0) {
			radiusSeekBar.setProgress(calcRadiusProgress(startRadius));
			// Zoom 14 is good
			LatLng point = new LatLng(startLatitude, startLongitude);
			moveMap(point);
			addFence(point);
		}
		else {
			// Set default view to current location
			final Location startLocation = locationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (startLocation != null) {
				moveMap(new LatLng(startLocation.getLatitude(), startLocation
								.getLongitude()));
			}
		}

		// Handle clicks
		mMap.setOnMapClickListener(new OnMapClickListener() {

			@Override
			public void onMapClick(LatLng point) {
				addFence(point);
			}
		});
		mMap.setOnMapLongClickListener(new OnMapLongClickListener() {

			@Override
			public void onMapLongClick(LatLng point) {
				addFence(point);
			}
		});

		// Handle moving of point and circle
		mMap.setOnMarkerDragListener(new OnMarkerDragListener() {
			@Override
			public void onMarkerDragStart(Marker marker) {
				if (circle != null) {
					circle.setVisible(false);
					// circle.setCenter(marker.getPosition());
				}
			}

			@Override
			public void onMarkerDragEnd(Marker marker) {
				if (circle != null) {
					circle.setVisible(true);
					circle.setCenter(marker.getPosition());
					getLocationName(marker.getPosition());
				}
			}

			@Override
			public void onMarkerDrag(Marker marker) {
				if (circle != null) {
					circle.setVisible(false);
					// circle.setCenter(marker.getPosition());
				}
			}
		});
	}

	// Adds minimum
	double calcRadius(final int progress) {
		final double x = radiusMin + progress;
		return x * x;
	}

	int calcRadiusProgress(final double radius) {
		return (int) (Math.sqrt(radius) - radiusMin);
	}

	@SeekBarTouchStart(R.id.radiusSeekBar)
	void onRadiusChangeStart(final SeekBar seekBar) {
		if (circle != null) {
			circle.setFillColor(Color.TRANSPARENT);
		}
	}

	@SeekBarProgressChange(R.id.radiusSeekBar)
	void onRadiusChanging(final SeekBar seekBar, final int progress) {
		radius = calcRadius(progress);
		if (circle != null) {
			circle.setRadius(radius);
		}
	}

	@SeekBarTouchStop(R.id.radiusSeekBar)
	void onRadiusChanged(final SeekBar seekBar) {
		radius = calcRadius(seekBar.getProgress());
		if (circle != null) {
			circle.setFillColor(fillColor);
			circle.setRadius(radius);
		}
		refocusMap();
	}

	@AfterViews
	void setupActionBar() {
		// Default result is fail
		setResult(Activity.RESULT_CANCELED);

		LayoutInflater inflater = (LayoutInflater) getActionBar()
				.getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		final View customActionBarView = inflater.inflate(
				R.layout.actionbar_custom_view_done_discard, null);
		customActionBarView.findViewById(R.id.actionbar_done)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// "Done"
						if (marker == null) {
							Toast.makeText(ActivityLocation.this,
									R.string.need_location_help,
									Toast.LENGTH_SHORT).show();
						}
						else {
							addGeoFence();
						}
					}
				});
		customActionBarView.findViewById(R.id.actionbar_discard)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						finish();

					}
				});

		// Show the custom action bar view and hide the normal Home icon and
		// title.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
				ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
						| ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setCustomView(customActionBarView,
				new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT));
	}

	/**
	 * Always finishes. Returns data if marker is not null
	 */
	private void returnAndFinish() {
		if (marker != null) {
			final Intent data = new Intent();
			data.putExtra(EXTRA_LATITUDE, marker.getPosition().latitude)
					.putExtra(EXTRA_LONGITUDE, marker.getPosition().longitude)
					.putExtra(EXTRA_RADIUS, radius)
					.putExtra(EXTRA_LOCATION_NAME, locationName);

			setResult(Activity.RESULT_OK, data);
		}
		finish();
	}

	/**
	 * Geofence code below
	 */

	/**
	 * Start adding the geofence. If successful, will finish and return result.
	 */
	private void addGeoFence() {
		// Save notification first to avoid odd behaviour when user is at the
		// location when creating it
		ContentValues values = new ContentValues();
		values.put(Notification.Columns.LATITUDE, marker.getPosition().latitude);
		values.put(Notification.Columns.LONGITUDE,
				marker.getPosition().longitude);
		values.put(Notification.Columns.RADIUS, radius);
		values.put(Notification.Columns.LOCATIONNAME, locationName);
		
		if (locationName != null && !locationName.isEmpty()) {
			suggestions.saveRecentQuery(locationName, null);
		}
		
		getContentResolver().update(Notification.getUri(mId), values, null,
				null);

		/*
		 * Record the request as an ADD. If a connection error occurs, the app
		 * can automatically restart the add request if Google Play services can
		 * fix the error
		 */
		mRequestType = GeofenceUtils.REQUEST_TYPE.ADD;

		/*
		 * Check for Google Play services. Do this after setting the request
		 * type. If connecting to Google Play services fails, onActivityResult
		 * is eventually called, and it needs to know what type of request was
		 * in progress.
		 */
		if (!servicesConnected()) {
			return;
		}

		final Geofence gf = new Geofence.Builder()
				.setCircularRegion(marker.getPosition().latitude,
						marker.getPosition().longitude, (float) radius)
				.setRequestId(Long.toString(mId))
				// Detect both entry and exit transitions
				.setTransitionTypes(
						Geofence.GEOFENCE_TRANSITION_ENTER
								| Geofence.GEOFENCE_TRANSITION_EXIT)
				.setExpirationDuration(Geofence.NEVER_EXPIRE).build();

		mCurrentGeofences.add(gf);
		// Start the request. Fail if there's already a request in progress
		try {
			mGeofenceRequester.addGeofences(mCurrentGeofences);
		}
		catch (UnsupportedOperationException e) {
			// Notify user that previous request hasn't finished.
			Toast.makeText(this,
					R.string.add_geofences_already_requested_error,
					Toast.LENGTH_LONG).show();
		}
	}

	/*
	 * Handle results returned to this Activity by other Activities started with
	 * startActivityForResult(). In particular, the method onConnectionFailed()
	 * in GeofenceRemover and GeofenceRequester may call
	 * startResolutionForResult() to start an Activity that handles Google Play
	 * services problems. The result of this call returns here, to
	 * onActivityResult. calls
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		// Choose what to do based on the request code
		switch (requestCode) {

		// If the request code matches the code sent in onConnectionFailed
		case GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST:

			switch (resultCode) {
			// If Google Play services resolved the problem
			case Activity.RESULT_OK:

				// If the request was to add geofences
				if (GeofenceUtils.REQUEST_TYPE.ADD == mRequestType) {

					// Toggle the request flag and send a new request
					mGeofenceRequester.setInProgressFlag(false);

					// Restart the process of adding the current geofences
					mGeofenceRequester.addGeofences(mCurrentGeofences);

					// If the request was to remove geofences
				}
				else if (GeofenceUtils.REQUEST_TYPE.REMOVE == mRequestType) {

					// Toggle the removal flag and send a new removal request
					mGeofenceRemover.setInProgressFlag(false);

					// If the removal was by Intent
					if (GeofenceUtils.REMOVE_TYPE.INTENT == mRemoveType) {

						// Restart the removal of all geofences for the
						// PendingIntent
						mGeofenceRemover
								.removeGeofencesByIntent(mGeofenceRequester
										.getRequestPendingIntent());

						// If the removal was by a List of geofence IDs
					}
					else {

						// Restart the removal of the geofence list
						mGeofenceRemover
								.removeGeofencesById(mGeofenceIdsToRemove);
					}
				}
				break;

			// If any other result was returned by Google Play services
			default:

				// Report that Google Play services was unable to resolve the
				// problem.
				Log.d(GeofenceUtils.APPTAG, getString(R.string.no_resolution));
			}

			// If any other request code was received
		default:
			// Report that this Activity received an unknown requestCode
			Log.d(GeofenceUtils.APPTAG,
					getString(R.string.unknown_activity_request_code,
							requestCode));

			break;
		}
	}

	/**
	 * Verify that Google Play services is available before making a request.
	 * 
	 * @return true if Google Play services is available, otherwise false
	 */
	private boolean servicesConnected() {

		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);

		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {

			// In debug mode, log the status
			Log.d(GeofenceUtils.APPTAG,
					getString(R.string.play_services_available));

			// Continue
			return true;

			// Google Play services was not available for some reason
		}
		else {

			// Display an error dialog
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode,
					this, 0);
			if (dialog != null) {
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				errorFragment.setDialog(dialog);
				errorFragment.show(getFragmentManager(), GeofenceUtils.APPTAG);
			}
			return false;
		}
	}

	/**
	 * Define a Broadcast receiver that receives updates from connection
	 * listeners and the geofence transition service.
	 */
	public class GeofenceSampleReceiver extends BroadcastReceiver {
		/*
		 * Define the required method for broadcast receivers This method is
		 * invoked when a broadcast Intent triggers the receiver
		 */
		@Override
		public void onReceive(Context context, Intent intent) {

			// Check the action code and determine what to do
			String action = intent.getAction();

			// Intent contains information about errors in adding or removing
			// geofences
			if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR)) {

				handleGeofenceError(context, intent);

				// Intent contains information about successful addition or
				// removal of geofences
			}
			else if (TextUtils.equals(action,
					GeofenceUtils.ACTION_GEOFENCES_ADDED)
					|| TextUtils.equals(action,
							GeofenceUtils.ACTION_GEOFENCES_REMOVED)) {

				handleGeofenceStatus(context, intent);

				// Intent contains information about a geofence transition
			}
			else if (TextUtils.equals(action,
					GeofenceUtils.ACTION_GEOFENCE_TRANSITION)) {

				handleGeofenceTransition(context, intent);

				// The Intent contained an invalid action
			}
			else {
				Log.e(GeofenceUtils.APPTAG,
						getString(R.string.invalid_action_detail, action));
				Toast.makeText(context, R.string.invalid_action,
						Toast.LENGTH_LONG).show();
			}
		}

		/**
		 * If you want to display a UI message about adding or removing
		 * geofences, put it here.
		 * 
		 * @param context
		 *        A Context for this component
		 * @param intent
		 *        The received broadcast Intent
		 */
		private void handleGeofenceStatus(Context context, Intent intent) {
			returnAndFinish();
		}

		/**
		 * Report geofence transitions to the UI
		 * 
		 * @param context
		 *        A Context for this component
		 * @param intent
		 *        The Intent containing the transition
		 */
		private void handleGeofenceTransition(Context context, Intent intent) {
			/*
			 * If you want to change the UI when a transition occurs, put the
			 * code here. The current design of the app uses a notification to
			 * inform the user that a transition has occurred.
			 */
		}

		/**
		 * Report addition or removal errors to the UI, using a Toast
		 * 
		 * @param intent
		 *        A broadcast Intent sent by ReceiveTransitionsIntentService
		 */
		private void handleGeofenceError(Context context, Intent intent) {
			String msg = intent
					.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
			Log.e(GeofenceUtils.APPTAG, msg);
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Define a DialogFragment to display the error dialog generated in
	 * showErrorDialog.
	 */
	public static class ErrorDialogFragment extends DialogFragment {

		// Global field to contain the error dialog
		private Dialog mDialog;

		/**
		 * Default constructor. Sets the dialog field to null
		 */
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		/**
		 * Set the dialog to display
		 * 
		 * @param dialog
		 *        An error dialog
		 */
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		/*
		 * This method must return a Dialog to the DialogFragment.
		 */
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}
}
