package com.nononsenseapps.utils;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.ActivityMain;
import com.nononsenseapps.notepad.R;

/**
 * Holds all code related to the showcase view, which for now is provided
 * by the {@link TapTargetView} library
 */
public final class ShowcaseHelper {

	// TODO is there a better place to put this file ?

	/**
	 * Create, configure and show a view to highlight a functionality, using an appropriate
	 * library. The view is shown above the given {@link Activity} and features a title and a
	 * short description
	 */
	public static void showForView(Activity activity, View targetToHighlight,
								   int titleStringId, int descriptionStringId) {
		// always a good idea to check
		if (targetToHighlight == null) {
			NnnLogger.error(ActivityMain.class,
					"Can't show the TapTargetView for the given view");
			return;
		}

		var target2 = TapTarget.forView(targetToHighlight,
				activity.getString(titleStringId), activity.getString(descriptionStringId));

		finishConfiguringAndShow(target2, activity);
	}

	public static void showForOverflowMenu(@NonNull Activity activity,
										   int titleStringId, int descriptionStringId) {
		// get the toolbar from the activity
		Toolbar tBar = activity.findViewById(androidx.appcompat.R.id.action_bar);

		// always a good idea to check
		if (tBar == null) {
			NnnLogger.error(ShowcaseHelper.class,
					"Can't show the TapTargetView, the Toolbar is unavailable");
			return;
		}

		var target2 = TapTarget.forToolbarOverflow(tBar,
				activity.getString(titleStringId), activity.getString(descriptionStringId));

		finishConfiguringAndShow(target2, activity);
	}

	/**
	 * All functions share this common configuration for the TapTargetView
	 */
	private static void finishConfiguringAndShow(TapTarget target, Activity activity) {
		// TODO can *you* make it prettier ? See also https://github.com/KeepSafe/TapTargetView
		target.outerCircleAlpha(0.9f)
				.drawShadow(true)
				.cancelable(true) // tap outside the circle to dismiss the showcaseView
				.textColor(R.color.accent);

		TapTargetView.showFor(activity, target);
	}
}
