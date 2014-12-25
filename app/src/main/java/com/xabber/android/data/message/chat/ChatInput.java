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
package com.xabber.android.data.message.chat;

/**
 * Represents information about chat input.
 * 
 * @author alexander.ivanov
 * 
 */
class ChatInput {

	/**
	 * Typed text.
	 */
	private String typedMessage;

	/**
	 * Start selection position.
	 */
	private int selectionStart;

	/**
	 * End selection position.
	 */
	private int selectionEnd;

	public ChatInput() {
		typedMessage = "";
		selectionStart = 0;
		selectionEnd = 0;
	}

	public String getTypedMessage() {
		return typedMessage;
	}

	public int getSelectionStart() {
		return selectionStart;
	}

	public int getSelectionEnd() {
		return selectionEnd;
	}

	public void setTyped(String typedMessage, int selectionStart,
			int selectionEnd) {
		this.typedMessage = typedMessage;
		this.selectionStart = selectionStart;
		this.selectionEnd = selectionEnd;
	}

}
