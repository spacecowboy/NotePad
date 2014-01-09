package com.nononsenseapps.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;
import com.nononsenseapps.helpers.ActivityHelper;
import com.nononsenseapps.notepad.ActivityLocation;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

public class LocationSuggestionsAdapter extends ArrayAdapter<String> implements
		Filterable {
	private ArrayList<String> resultList;

	private Geocoder geocoder = null;
	private List<Address> places = null;

	public LocationSuggestionsAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);

		geocoder = new Geocoder(context.getApplicationContext(),
				ActivityHelper.getUserLocale(context));
		
		resultList = new ArrayList<String>();
	}

	@Override
	public int getCount() {
		return resultList.size();
	}

	@Override
	public String getItem(int index) {
		return resultList.get(index);
	}

	@Override
	public Filter getFilter() {
		Filter filter = new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults filterResults = new FilterResults();
				if (constraint != null) {
					// Retrieve the autocomplete results.
					resultList = getSuggestions(constraint.toString());

					// Assign the data to the FilterResults
					filterResults.values = resultList;
					filterResults.count = resultList.size();
				}
				return filterResults;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				if (results != null && results.count > 0) {
					notifyDataSetChanged();
				}
				else {
					notifyDataSetInvalidated();
				}
			}
		};
		return filter;
	}
	
	public static String getPlaceName(final Address place) {
		String name = "";
		// _Location += listAddresses.get(0).getAdminArea();
		// _Location += " " + listAddresses.get(0).getFeatureName();
		// _Location += " " + listAddresses.get(0).getThoroughfare();
		for (int i = 0; i < place.getMaxAddressLineIndex(); i++) {
			if (name.length() > 0) name += " ";
			name += place.getAddressLine(i);
		}

		return name;
	}

	ArrayList<String> getSuggestions(final String query) {
		ArrayList<String> resultList = new ArrayList<String>();
		
		try {
			places = geocoder.getFromLocationName(query, 10);

			if (null == places || places.isEmpty()) {
			}
			else {
				for (Address place: places) {
					resultList.add(getPlaceName(place));
				}
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return resultList;
	}
}