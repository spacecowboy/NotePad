/*
 * Copyright (C) 2010 Eric Harlow
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

package com.ericharlow.DragNDrop;

import android.view.View;
import android.widget.ListView;

/**
 * Implement to handle an item being dragged.
 *  
 * @author Eric Harlow
 */
public interface DragListener {
	/**
	 * Called when a drag starts.
	 * @param itemView - the view of the item to be dragged i.e. the drag view
	 */
	void onStartDrag(View itemView);
	
	/**
	 * Called when a drag is to be performed.
	 * @param x - horizontal coordinate of MotionEvent.
	 * @param y - verital coordinate of MotionEvent.
	 * @param listView - the listView
	 */
	void onDrag(int x, int y, ListView listView);
	
	/**
	 * Called when a drag stops.
	 * Any changes in onStartDrag need to be undone here 
	 * so that the view can be used in the list again.
	 * @param itemView - the view of the item to be dragged i.e. the drag view
	 */
	void onStopDrag(View itemView);
}
