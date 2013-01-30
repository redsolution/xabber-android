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
package com.xabber.android.data;

/**
 * Listener for the periodically call.
 * 
 * At least {@link #DELAY} milliseconds will pass before the next call.
 * 
 * First time it will be called after at least {@link #DELAY} milliseconds after
 * {@link OnInitializedListener#onInitialized()}.
 * 
 * @author alexander.ivanov
 * 
 */
public interface OnTimerListener extends BaseManagerInterface {

	public final static int DELAY = 1000;

	/**
	 * Called after at least {@link #DELAY} milliseconds.
	 */
	void onTimer();

}
