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

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Exception with specified error's string resource.
 * 
 * @author alexander.ivanov
 * 
 */
public class NetworkException extends Exception {
	private static final long serialVersionUID = 1L;
	private final int resourceId;
	private final Throwable wrappedThrowable;

	public NetworkException(int resourceId) {
		this(resourceId, null);
	}

	public NetworkException(int resourceId, Throwable wrappedThrowable) {
		super();
		this.resourceId = resourceId;
		this.wrappedThrowable = wrappedThrowable;
	}

	public int getResourceId() {
		return resourceId;
	}

	@Override
	public void printStackTrace(PrintStream out) {
		super.printStackTrace(out);
		if (wrappedThrowable != null) {
			out.println("Nested Exception: ");
			wrappedThrowable.printStackTrace(out);
		}
	}

	@Override
	public void printStackTrace(PrintWriter out) {
		super.printStackTrace(out);
		if (wrappedThrowable != null) {
			out.println("Nested Exception: ");
			wrappedThrowable.printStackTrace(out);
		}
	}

}