package com.nononsenseapps.notepad.interfaces;

/**
 * Used to control the menu items for the navigation drawer.
 * 
 */
public interface MenuStateController {

	/**
	 * If true, menu items should be hidden/removed. Items relevant to the
	 * navigation drawer should be visible
	 */
	public boolean childItemsVisible();
}
