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

package com.nononsenseapps.notepad;

import com.nononsenseapps.notepad.interfaces.DeleteActionListener;

import android.content.Context;
import android.util.Log;
import android.view.ActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;

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
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(this.getClass().getSimpleName(), "onCreateActionView");

		// Inflate the action view to be shown on the action bar.
		// LayoutInflater layoutInflater = LayoutInflater.from( );
		// View view = layoutInflater.inflate(R.menu.action_delete_menu, null);

		return null;
	}

	@Override
	public boolean onPerformDefaultAction() {
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(this.getClass().getSimpleName(), "onPerformDefaultAction");

		return super.onPerformDefaultAction();
	}

	@Override
	public boolean hasSubMenu() {
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(this.getClass().getSimpleName(), "hasSubMenu");

		return true;
	}

	@Override
	public void onPrepareSubMenu(SubMenu subMenu) {
		if (FragmentLayout.UI_DEBUG_PRINTS) Log.d(this.getClass().getSimpleName(), "onPrepareSubMenu");

		subMenu.clear();

		// Add delete button
		subMenu.add(Menu.NONE, R.id.menu_delete, 0, R.string.menu_delete)
				//.setIcon()
				.setOnMenuItemClickListener(this);

		// Add cancel button
		subMenu.add(Menu.NONE, Menu.NONE, 0, R.string.menu_cancel)
				.setOnMenuItemClickListener(this);
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
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
