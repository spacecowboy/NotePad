/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.util;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is a Bi-directional Map. It works just like a HashMap with the added restriction that both keys and values must be unique.
 * This means you can query the Map for a value and get the key.
 * 
 * Implemented with two HashMaps and validity checks on the arguments.
 *
 */
public class BiMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = 1207460403477674827L;
	private final HashMap<V, K> valueToKey;
	
	public BiMap() {
		super();
		
		valueToKey = new HashMap<V, K>();
	}
	
	/**
	 * Reverse GET
	 * 
	 * @param value to retrieve the key for
	 * @return the key associated with the value or NULL if none exists
	 */
	public K getKey(Object value) {
		return valueToKey.get(value);
	}
	
	/**
	 * Just for grammatical consistency. Maps directly to GET.
	 * @param key to retrieve the value for
	 * @return the value the key is associated with or NULL if none exists
	 */
	public V getValue(K key) {
		return get(key);
	}

	@Override
	public void clear() {
		super.clear();
		valueToKey.clear();
	}

	@Override
	public V put(K key, V value) {
		if (containsValue(value)) {
			remove(getKey(value));
		}
		valueToKey.put(value, key);
		return super.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> arg0) {
		for (K key : arg0.keySet()) {
			put(key, arg0.get(key));
		}
	}

	@Override
	public V remove(Object key) {
		if (containsKey(key))
			valueToKey.remove(get(key));
		return super.remove(key);
	}
}
