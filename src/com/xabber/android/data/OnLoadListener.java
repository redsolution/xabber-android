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
 * Listen for application to be ready to load data.
 * 
 * @author alexander.ivanov
 * 
 */
public interface OnLoadListener extends BaseManagerInterface {

	/**
	 * Called after service has been started before
	 * {@link OnInitializedListener}.
	 * 
	 * WILL BE CALLED FROM BACKGROUND THREAD. DON'T CHANGE OR ACCESS
	 * APPLICATION'S DATA HERE!
	 * 
	 * Used to load data from DB and post request to UI thread to update data.
	 */
	void onLoad();

}
