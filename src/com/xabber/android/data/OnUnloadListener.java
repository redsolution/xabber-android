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
 * Listen for application to being closed.
 * 
 * @author alexander.ivanov
 * 
 */
public interface OnUnloadListener extends BaseManagerInterface {

	/**
	 * Called before application to be killed after
	 * {@link OnCloseListener#onClose()} has been called.
	 * 
	 * WILL BE CALLED FROM BACKGROUND THREAD. DON'T CHANGE OR ACCESS
	 * APPLICATION'S DATA HERE!
	 */
	public void onUnload();

}
