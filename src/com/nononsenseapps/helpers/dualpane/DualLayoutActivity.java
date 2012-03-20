package com.nononsenseapps.helpers.dualpane;

/*
 * Copyright (C) 2012 Jonas Kalderstam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.R;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

public abstract class DualLayoutActivity extends Activity {
	public static final String SHOWRIGHT = "showRight";

	public enum CONTENTVIEW {
		DUAL, LEFT, RIGHT
	};

	protected CONTENTVIEW currentContent;

	protected Fragment leftFragment;
	protected Fragment rightFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set content view depending on the intent
		decideContentView();

		// Can potentially be null so plan for that
		leftFragment = getFragmentManager().findFragmentById(R.id.leftFragment);
		// Can potentially be null so plan for that
		rightFragment = getFragmentManager().findFragmentById(
				R.id.rightFragment);

		// Set up navigation if in right fragment mode
		if (currentContent.equals(CONTENTVIEW.RIGHT)) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	/**
	 * Sets the contentview depending on the screen size and possibly what
	 * intent was used to start this activity
	 */
	protected void decideContentView() {
		if (getResources().getBoolean(R.bool.tabletLayout)) {
			currentContent = CONTENTVIEW.DUAL;
			setContentView(R.layout.dual_layout);
		} else {
			Intent intent = getIntent();
			setIntent(intent);
			if (null != intent
					&& null != intent.getData()
					&& (intent.getData().getPath()
							.startsWith(NotePad.Notes.PATH_VISIBLE_NOTE_ID) || intent
							.getData().getPath()
							.startsWith(NotePad.Notes.PATH_NOTE_ID))) {
				currentContent = CONTENTVIEW.RIGHT;
				// Display right fragment
				setRightContentView();
			} else {
				currentContent = CONTENTVIEW.LEFT;
				// Left or Dual layout is handled automatically
				Log.d("DUalActivity", "SetLeft");
				setContentView(R.layout.dual_layout);
			}
		}
	}

	/**
	 * Override this method to show a different layout when right fragment is
	 * shown alone.
	 */
	protected void setRightContentView() {
		Log.d("DUalActivity", "Set Right");
		setContentView(R.layout.right_layout);
	}

	/**
	 * Besides showing/hiding the left fragment, this method specifies that a
	 * layout animation should be used. It is defined as a sliding animation.
	 * The right fragment will use the default animation, which is a sliding
	 * animation also.
	 * 
	 * If the left fragment's animation is removed from this method, the default
	 * animation will be used which is a fading animation.
	 * 
	 * Please note that this method will only have an effect in those screen
	 * configurations where the list is hideable; by default, a width between
	 * 600 and 1024 dip which corresponds to a portrait view on tablets. Change
	 * the boolean value in layout_constants.xml to allow for it in other screen
	 * sizes.
	 * 
	 * @param visible
	 */
	protected void setLeftFragmentVisible(boolean visible) {
		if (leftFragment != null && (leftFragment.isVisible() || visible)
				&& getResources().getBoolean(R.bool.leftHideable)) {
			final float listWidth = getLeftFragment().getView().getWidth();
			ViewGroup container = (ViewGroup) findViewById(R.id.dual_layout);
			// Don't clip the children, we want to draw the entire fragment even
			// if it is partially off-screen.
			container.setClipChildren(false);
			final LayoutTransition trans = container.getLayoutTransition();
			/**
			 * This specifies the delay before the leftFragment will appear.
			 * Change if you want the right fragment to move before.
			 */
			trans.setStartDelay(LayoutTransition.APPEARING, 0);
			/**
			 * This is the delay before the right fragment will start to occupy
			 * the space left by the left fragment
			 */
			trans.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 100);

			/**
			 * Adding, specifies that the left fragment should animate by
			 * sliding into view.
			 */
			ObjectAnimator animIn = ObjectAnimator.ofFloat(null, "x",
					-listWidth, 0f).setDuration(
					trans.getDuration(LayoutTransition.CHANGE_APPEARING));
			trans.setAnimator(LayoutTransition.APPEARING, animIn);

			/**
			 * Removing, specifies that the left fragment should animate by
			 * sliding out of view.
			 */
			ObjectAnimator animOut = ObjectAnimator.ofFloat(null, "x", 0f,
					-listWidth).setDuration(
					trans.getDuration(LayoutTransition.CHANGE_DISAPPEARING));
			trans.setAnimator(LayoutTransition.DISAPPEARING, animOut);

			FragmentManager fragmentManager = getFragmentManager();
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();
			if (getLeftFragment().isVisible()) {
				fragmentTransaction.hide(getLeftFragment());
			} else {
				fragmentTransaction.show(getLeftFragment());
			}
			// The hiding/showing will automatically initiate the animations
			// since
			// we have specified that we want layout animations in the layout
			// xml
			fragmentTransaction.commit();

			/*
			 * Display home as up to be able to view the list
			 */
			getActionBar().setDisplayHomeAsUpEnabled(!visible);
		}
	}

	/**
	 * Can be null if not dual pane!
	 */
	public Fragment getLeftFragment() {
		return leftFragment;
	}

	/**
	 * Can be null if not dual pane!
	 */
	public Fragment getRightFragment() {
		return rightFragment;
	}

	public CONTENTVIEW getCurrentContent() {
		return currentContent;
	}

	/**
	 * Handle up navigation
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onHomePressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Home icon has two behaviours. If this is a phone, home should navigate to
	 * list If tablet, and portrait mode, home should show the list.
	 */
	private void onHomePressed() {
		if (currentContent.equals(DualLayoutActivity.CONTENTVIEW.DUAL)) {
			setLeftFragmentVisible(true);
		} else {
			goUp();
		}
	}

	/**
	 * Must implement this to launch your main activity. Abstract since your
	 * intents and classes will be unique to your app
	 */
	protected abstract void goUp();

	/**
	 * Catches any clicks inside right fragment when left fragment can be
	 * hidden. A click in the right fragment will hide the left fragment
	 */
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// If user clicked inside right fragment, and this left fragment can be
		// hidden, then hide it
		if (rightFragment != null && leftFragment != null
				&& leftFragment.isVisible()
				&& getResources().getBoolean(R.bool.leftHideable)) {
			View r = rightFragment.getView();
			float[] xy = getRelativeCoords(ev);
			if (xy[0] < r.getRight() && xy[0] > r.getLeft()
					&& xy[1] < r.getBottom() && xy[1] > r.getTop()) {
				setLeftFragmentVisible(false);
				return true;
			}
		}

		// Else normally
		return super.dispatchTouchEvent(ev);
	}

	/**
	 * Needed because touch event will report absolute coordinates and we only
	 * have access to relative to our window.
	 */
	public float[] getRelativeCoords(MotionEvent e) {
		// MapView
		View contentView = getWindow().findViewById(Window.ID_ANDROID_CONTENT);
		return new float[] { e.getRawX() - contentView.getLeft(),
				e.getRawY() - contentView.getTop() };
	}
}
