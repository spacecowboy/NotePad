package com.nononsenseapps.ui;

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

		// this listener will always dismiss the taptargetview, regardless of where you click.
		// It's less frustrating to use, and above all it makes the espresso tests work.
		var listener = new TapTargetView.Listener() {

			@Override
			public void onTargetClick(TapTargetView view) {
				view.dismiss(true);
			}

			@Override
			public void onTargetLongClick(TapTargetView view) {
				this.onTargetClick(view);
			}

			@Override
			public void onTargetCancel(TapTargetView view) {
				view.dismiss(false);
			}

			@Override
			public void onOuterCircleClick(TapTargetView view) {
				// this is probably the only important method to override, the rest is boilerplate
				view.dismiss(false);
			}

			@Override
			public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
				// don't .dismiss(), because that causes a NullPointerException
				// in android.view.View.dispatchDetachedFromWindow(). See
				// https://github.com/KeepSafe/TapTargetView/issues/395#issuecomment-987952528
				view.setVisibility(View.GONE);
				super.onTargetDismissed(view, userInitiated);
			}
		};

		TapTargetView.showFor(activity, target, listener);
	}
}
