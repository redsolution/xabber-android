/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 * 
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 * 
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Map of map with string value as keys for both maps.
 * 
 * @author alexander.ivanov
 * 
 * @param <T>
 */
public class NestedMap<T> implements Iterable<NestedMap.Entry<T>> {

	private final Map<String, Map<String, T>> map;

	public NestedMap() {
		map = new HashMap<String, Map<String, T>>();
	}

	/**
	 * @param first
	 * @param second
	 * @return <code>null</code> if there is no such first or second level.
	 */
	public T get(String first, String second) {
		Map<String, T> nested = map.get(first);
		if (nested == null)
			return null;
		return nested.get(second);
	}

	/**
	 * Puts value. Nested map will be created if necessary.
	 * 
	 * @param first
	 * @param second
	 * @param value
	 */
	synchronized public void put(String first, String second, T value) {
		Map<String, T> nested = map.get(first);
		if (nested == null) {
			nested = new HashMap<String, T>();
			map.put(first, nested);
		}
		nested.put(second, value);
	}

	/**
	 * Removes value. Nested map will be removed if necessary.
	 * 
	 * @param first
	 * @param second
	 */
	synchronized public T remove(String first, String second) {
		Map<String, T> nested = map.get(first);
		if (nested == null)
			return null;
		T value = nested.remove(second);
		if (nested.isEmpty())
			map.remove(first);
		return value;
	}

	/**
	 * Removes all information associated with first level.
	 * 
	 * @param first
	 */
	synchronized public void clear(String first) {
		map.remove(first);
	}

	/**
	 * Removes all information.
	 */
	synchronized public void clear() {
		map.clear();
	}

	/**
	 * @return Whether there is no values.
	 */
	synchronized public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Returns an {@link Iterator} for the elements in this object.
	 * 
	 * Iterators are designed to be used by only one thread at a time.
	 * 
	 * @return
	 */
	@Override
	public Iterator<Entry<T>> iterator() {
		return new EntryIterator();
	}

	/**
	 * Returns nested map.
	 * 
	 * @param first
	 * @return empty map if there is no such first level.
	 */
	public Map<String, T> getNested(String first) {
		Map<String, T> nested = map.get(first);
		if (nested == null)
			return Collections.emptyMap();
		return Collections.unmodifiableMap(nested);
	}

	/**
	 * Collection with values.
	 * 
	 * ONLY {@link Collection#iterator()} FUNCTION IS SUPPORTED.
	 * 
	 * @return
	 */
	public Collection<T> values() {
		return new Values();
	}

	/**
	 * Adds all elements from another {@link NestedMap}.
	 * 
	 * @param nestedMap
	 */
	public void addAll(NestedMap<T> nestedMap) {
		for (NestedMap.Entry<T> entry : nestedMap)
			put(entry.getFirst(), entry.getSecond(), entry.getValue());
	}

	/**
	 * Entry stored in {@link NestedMap}.
	 * 
	 * @author alexander.ivanov
	 * 
	 * @param <T>
	 */
	public static class Entry<T> {

		private final String first;
		private final String second;
		private final T value;

		public Entry(String first, String second, T value) {
			super();
			this.first = first;
			this.second = second;
			this.value = value;
		}

		public String getFirst() {
			return first;
		}

		public String getSecond() {
			return second;
		}

		public T getValue() {
			return value;
		}

	}

	private class EntryIterator implements Iterator<Entry<T>> {

		private final Iterator<java.util.Map.Entry<String, Map<String, T>>> firstIterator;

		private java.util.Map.Entry<String, Map<String, T>> nested;

		private Iterator<java.util.Map.Entry<String, T>> secondIterator;

		private EntryIterator() {
			firstIterator = map.entrySet().iterator();
			nested = null;
			secondIterator = null;
		}

		@Override
		public boolean hasNext() {
			if (secondIterator != null && secondIterator.hasNext())
				return true;
			while (firstIterator.hasNext()) {
				nested = firstIterator.next();
				secondIterator = nested.getValue().entrySet().iterator();
				if (secondIterator.hasNext()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Entry<T> next() throws NoSuchElementException {
			if (!hasNext())
				throw new NoSuchElementException();
			java.util.Map.Entry<String, T> entry = secondIterator.next();
			return new Entry<T>(nested.getKey(), entry.getKey(),
					entry.getValue());
		}

		@Override
		public void remove() throws IllegalStateException {
			if (secondIterator == null)
				throw new IllegalStateException();
			secondIterator.remove();
			if (nested.getValue().isEmpty())
				firstIterator.remove();
		}

	}

	private class Values implements Collection<T> {

		@Override
		public boolean add(T object) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends T> arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean contains(Object object) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsAll(Collection<?> arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isEmpty() {
			return iterator().hasNext();
		}

		@Override
		public Iterator<T> iterator() {
			return new ValuesIterator();
		}

		@Override
		public boolean remove(Object object) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int size() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <Type> Type[] toArray(Type[] array) {
			throw new UnsupportedOperationException();
		}

		private class ValuesIterator implements Iterator<T> {

			private final Iterator<Entry<T>> iterator;

			private ValuesIterator() {
				this.iterator = NestedMap.this.iterator();
			}

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public T next() {
				return iterator.next().getValue();
			}

			@Override
			public void remove() {
				iterator.remove();
			}

		}

	}

}
