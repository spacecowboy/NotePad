package com.nononsenseapps.ui;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.helpers.ThemeHelper;

/**
 * Holds all code related to the showcase view, which for now is provided
 * by the {@link TapTargetView} library
 */
public final class ShowcaseHelper {

	/**
	 * Create, configure and show a view to highlight the overflow menu, using a library.
	 * The view is shown above the given {@link FragmentActivity} and features a
	 * title and a short description
	 */
	public static void showForOverflowMenu(@NonNull FragmentActivity activity,
										   @StringRes int titleStringId,
										   @StringRes int descriptionStringId) {
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
	 * All functions in this class share this common configuration for the TapTargetView, but to
	 * highlight a {@link View} on the {@link FragmentActivity} you would implement an alternative
	 * to {@link #showForOverflowMenu}
	 */
	private static void finishConfiguringAndShow(TapTarget target, FragmentActivity activity) {
		// TODO can *you* make it prettier ? See also https://github.com/KeepSafe/TapTargetView
		target.outerCircleAlpha(0.9f)
				.drawShadow(true)
				.cancelable(true) // tap outside the circle to dismiss the showcaseView
				.tintTarget(false)
				// TODO not good for Material YOU themes. shows green text on a green circle, for example
				.textColorInt(ThemeHelper.getThemeAccentColor(activity));

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
