package com.nononsenseapps.notepad;

import java.io.IOException;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nononsenseapps.notepad.database.TaskList;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;

@EActivity(R.layout.activity_map)
public class ActivityLocation extends Activity {
	
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String RADIUS = "radius";
	public static final String LOCATION_NAME = "location_name";

	final static int fillColor = 0xaa33b5e5;
	final static int lineColor = 0xff33b5e5;

	@SystemService
	LocationManager locationManager;

	@FragmentById
	MapFragment mapFragment;
	GoogleMap mMap;

	int radius = 200;
	String locationName = "";

	Circle circle = null;
	Marker marker = null;

	@AfterViews
	protected void setupMap() {
		mMap = mapFragment.getMap();
		mMap.setMyLocationEnabled(true);
		// TODO start with provided location
		// Set default view to current location
		final Location startLocation = locationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (startLocation != null) {
			// Zoom 14 is good
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
					startLocation.getLatitude(), startLocation.getLongitude()),
					14));
		}

		// Handle clicks
		mMap.setOnMapLongClickListener(new OnMapLongClickListener() {

			@Override
			public void onMapLongClick(LatLng point) {
				// Remove previous markers
				mMap.clear();
				locationName = "";

				CircleOptions co = new CircleOptions();
				co.center(point).radius(radius).fillColor(fillColor)
						.strokeColor(lineColor).strokeWidth(2);
				// Add radius
				circle = mMap.addCircle(co);

				// Add marker
				marker = mMap.addMarker(new MarkerOptions().position(point)
						.draggable(true).title("Hello world"));
				
				// TODO jsut testing here
				Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());                 
				try {
				    List<Address> listAddresses = geocoder.getFromLocation(point.latitude, point.longitude, 1);
				    if(null!=listAddresses&&listAddresses.size()>0){
				        String _Location = "";
				        _Location += listAddresses.get(0).getAdminArea();
				        _Location += " " + listAddresses.get(0).getFeatureName();
				        _Location += " " + listAddresses.get(0).getThoroughfare();
				        for (int i = 0; i < listAddresses.get(0).getMaxAddressLineIndex(); i++) {
				        	_Location +=" " + listAddresses.get(0).getAddressLine(i);
				        }
				        Toast.makeText(getApplicationContext(), _Location, Toast.LENGTH_SHORT).show();
				    }
				} catch (IOException e) {
				}

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

	@SeekBarTouchStart(R.id.radiusSeekBar)
	void onRadiusChangeStart(final SeekBar seekBar) {
		if (circle != null) {
			circle.setFillColor(Color.TRANSPARENT);
		}
	}

	@SeekBarProgressChange(R.id.radiusSeekBar)
	void onRadiusChanging(final SeekBar seekBar, final int progress) {
		if (progress > 0) radius = progress;
		if (circle != null) {
			circle.setRadius(radius);
		}
	}

	@SeekBarTouchStop(R.id.radiusSeekBar)
	void onRadiusChanged(final SeekBar seekBar) {
		if (seekBar.getProgress() > 0) radius = seekBar.getProgress();
		if (circle != null) {
			circle.setFillColor(fillColor);
			circle.setRadius(radius);
		}
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
						if (marker != null) {
							final Intent data = new Intent();
							data.putExtra(LATITUDE, marker.getPosition().latitude)
							.putExtra(LONGITUDE, marker.getPosition().longitude)
							.putExtra(RADIUS, radius)
							.putExtra(LOCATION_NAME, locationName);
							
							setResult(Activity.RESULT_OK, data);
						}
						finish();

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
		actionBar.setCustomView(customActionBarView);
	}
	
	@Background
	void getLocationName(final Marker marker) {
		
	}
}
