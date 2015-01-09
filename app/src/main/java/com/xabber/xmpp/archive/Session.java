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
package com.xabber.xmpp.archive;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.Instance;
import com.xabber.xmpp.SerializerUtils;

/**
 * Session settings item inside the {@link Pref}.
 * 
 * @author alexander.ivanov
 * 
 */
public class Session implements Instance {

	public static final String ELEMENT_NAME = "session";

	public static final String TIMEOUT_ATTRIBUTE = "timeout";
	public static final String THREAD_ATTRIBUTE = "thread";
	public static final String SAVE_ATTRIBUTE = "save";

	private Integer timeout;
	private String thread;
	private SaveMode save;

	@Override
	public boolean isValid() {
		return thread != null && save != null;
	}

	@Override
	public void serialize(XmlSerializer serializer) throws IOException {
		serializer.startTag(null, ELEMENT_NAME);
		if (timeout != null)
			SerializerUtils.setIntegerAttribute(serializer, TIMEOUT_ATTRIBUTE,
					timeout);
		SerializerUtils.setTextAttribute(serializer, THREAD_ATTRIBUTE, thread);
		if (save != null)
			SerializerUtils.setTextAttribute(serializer, SAVE_ATTRIBUTE,
					save.toString());
		serializer.endTag(null, ELEMENT_NAME);
	}

	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public String getThread() {
		return thread;
	}

	public void setThread(String thread) {
		this.thread = thread;
	}

	public SaveMode getSave() {
		return save;
	}

	public void setSave(SaveMode save) {
		this.save = save;
	}

}
