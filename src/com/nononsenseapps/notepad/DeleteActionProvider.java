package com.nononsenseapps.notepad;

import com.nononsenseapps.notepad.interfaces.DeleteActionListener;

import android.content.Context;
import android.util.Log;
import android.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.widget.Toast;

public class DeleteActionProvider extends ActionProvider implements
		OnMenuItemClickListener {

	protected Context context;
	
	protected DeleteActionListener listener;

	public DeleteActionProvider(Context context) {
		super(context);
		this.context = context;
	}
	
	public void setDeleteActionListener(DeleteActionListener d) {
		listener = d;
	}

	@Override
	public View onCreateActionView() {
		Log.d(this.getClass().getSimpleName(), "onCreateActionView");

		// Inflate the action view to be shown on the action bar.
		// LayoutInflater layoutInflater = LayoutInflater.from( );
		// View view = layoutInflater.inflate(R.menu.action_delete_menu, null);

		return null;
	}

	@Override
	public boolean onPerformDefaultAction() {
		Log.d(this.getClass().getSimpleName(), "onPerformDefaultAction");

		return super.onPerformDefaultAction();
	}

	@Override
	public boolean hasSubMenu() {
		Log.d(this.getClass().getSimpleName(), "hasSubMenu");

		return true;
	}

	@Override
	public void onPrepareSubMenu(SubMenu subMenu) {
		Log.d(this.getClass().getSimpleName(), "onPrepareSubMenu");

		subMenu.clear();

		// Add delete button
		subMenu.add(subMenu.NONE, R.id.menu_delete, 0, R.string.menu_delete)
				//.setIcon()
				.setOnMenuItemClickListener(this);

		// Add cancel button
		subMenu.add(subMenu.NONE, R.id.menu_cancel, 0, R.string.menu_cancel)
				.setOnMenuItemClickListener(this);
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		Toast.makeText(context, item.getTitle(), Toast.LENGTH_SHORT).show();
			switch (item.getItemId()) {
			case R.id.menu_delete:
				if (listener != null) {
					listener.onDeleteAction();
				}
				return true;
			}
		return false;
	}
}
