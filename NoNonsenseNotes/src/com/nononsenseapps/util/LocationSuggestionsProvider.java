package com.nononsenseapps.util;

import android.content.SearchRecentSuggestionsProvider;

public class LocationSuggestionsProvider extends SearchRecentSuggestionsProvider { 
	   
	   public static final String AUTHORITY = "com.nononsenseapps.LocationSuggestions";
			   //LocationSuggestionsProvider.class.getName(); 

	   public static final int MODE = DATABASE_MODE_QUERIES | DATABASE_MODE_2LINES; 

	   public LocationSuggestionsProvider() { 
	      setupSuggestions(AUTHORITY, MODE); 
	   } 
	}