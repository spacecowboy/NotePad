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

/**
 * Implement to handle an item being dropped.
 * An adapter handling the underlying data 
 * will most likely handle this interface.
 *  
 * @author Eric Harlow
 */
public interface DropListener {
	
	/**
	 * Called when an item is to be dropped.
	 * @param from - index item started at.
	 * @param to - index to place item at.
	 */
	void onDrop(int from, int to);
}
