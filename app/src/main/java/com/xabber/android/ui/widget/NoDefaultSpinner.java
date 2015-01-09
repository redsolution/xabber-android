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
package com.xabber.android.ui.widget;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

/**
 * A modified Spinner that doesn't automatically select the first entry in the
 * list.
 * 
 * Shows the prompt if nothing is selected.
 * 
 * Limitations: does not display prompt if the entry list is empty.
 * 
 * http://stackoverflow.com/a/3427058
 * 
 * @author emmby
 */
public class NoDefaultSpinner extends Spinner {

	public NoDefaultSpinner(Context context) {
		super(context);
	}

	public NoDefaultSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NoDefaultSpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void setAdapter(SpinnerAdapter orig) {
		final SpinnerAdapter adapter = newProxy(orig);

		super.setAdapter(adapter);

		try {
			final Method m = AdapterView.class.getDeclaredMethod(
					"setNextSelectedPositionInt", int.class);
			m.setAccessible(true);
			m.invoke(this, -1);

			final Method n = AdapterView.class.getDeclaredMethod(
					"setSelectedPositionInt", int.class);
			n.setAccessible(true);
			n.invoke(this, -1);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected SpinnerAdapter newProxy(SpinnerAdapter obj) {
		return (SpinnerAdapter) java.lang.reflect.Proxy.newProxyInstance(obj
				.getClass().getClassLoader(),
				new Class[] { SpinnerAdapter.class }, new SpinnerAdapterProxy(
						obj));
	}

	/**
	 * Intercepts getView() to display the prompt if position < 0
	 */
	protected class SpinnerAdapterProxy implements InvocationHandler {

		protected SpinnerAdapter obj;
		protected Method getView;

		protected SpinnerAdapterProxy(SpinnerAdapter obj) {
			this.obj = obj;
			try {
				this.getView = SpinnerAdapter.class.getMethod("getView",
						int.class, View.class, ViewGroup.class);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Object invoke(Object proxy, Method m, Object[] args)
				throws Throwable {
			try {
				return m.equals(getView) && (Integer) (args[0]) < 0 ? getView(
						(Integer) args[0], (View) args[1], (ViewGroup) args[2])
						: m.invoke(obj, args);
			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		protected View getView(int position, View convertView, ViewGroup parent)
				throws IllegalAccessException {
			if (position < 0) {
				final TextView v = (TextView) ((LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(android.R.layout.simple_spinner_item, parent,
								false);
				v.setText(getPrompt());
				return v;
			}

			return obj.getView(position, convertView, parent);
		}

	}
}
