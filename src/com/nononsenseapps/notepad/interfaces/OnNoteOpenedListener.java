package com.nononsenseapps.notepad.interfaces;

public interface OnNoteOpenedListener {
	public void onNoteOpened(long id, boolean created);
	
	public void onNewNoteDeleted(long id);
}
