package com.nononsenseapps.notepad;

public class DeleteActionProvider extends ActionProvider {
	
	@Override
	public View onCreateActionView() {
	    // Inflate the action view to be shown on the action bar.
	    LayoutInflater layoutInflater = LayoutInflater.from(R.id.action_delete_menu);
	    View view = layoutInflater.inflate(R.layout.action_provider, null);
	    
	    return view;
	}

}
