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
package com.xabber.xmpp.rsm;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.PacketExtension;
import com.xabber.xmpp.SerializerUtils;

/**
 * Result Set Management extension.
 * 
 * http://xmpp.org/extensions/xep-0085.html
 * 
 * @author alexander.ivanov
 * 
 */
public class Set extends PacketExtension {

	public static final String NAMESPACE = "http://jabber.org/protocol/rsm";
	public static final String ELEMENT_NAME = "set";

	static final String AFTER_NAME = "after";
	static final String BEFORE_NAME = "before";
	static final String COUNT_NAME = "count";
	static final String FIRST_NAME = "first";
	static final String INDEX_ATTRIBUTE = "index";
	static final String INDEX_NAME = "index";
	static final String LAST_NAME = "last";
	static final String MAX_NAME = "max";

	private String after;
	private String before;
	private Integer count;
	private String first;
	private Integer firstIndex;
	private Integer index;
	private String last;
	private Integer max;

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		if (after != null)
			SerializerUtils.addTextTag(serializer, AFTER_NAME, after);
		if (before != null)
			SerializerUtils.addTextTag(serializer, BEFORE_NAME, before);
		if (count != null)
			SerializerUtils.addIntegerTag(serializer, COUNT_NAME, count);
		if (first != null) {
			serializer.startTag(null, FIRST_NAME);
			if (firstIndex != null)
				SerializerUtils.setIntegerAttribute(serializer,
						INDEX_ATTRIBUTE, firstIndex);
			serializer.text(first);
			serializer.endTag(null, FIRST_NAME);
		}
		if (index != null)
			SerializerUtils.addIntegerTag(serializer, INDEX_NAME, index);
		if (last != null)
			SerializerUtils.addTextTag(serializer, LAST_NAME, last);
		if (max != null)
			SerializerUtils.addIntegerTag(serializer, MAX_NAME, max);
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public String getElementName() {
		return ELEMENT_NAME;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	public String getAfter() {
		return after;
	}

	public void setAfter(String after) {
		this.after = after;
	}

	public String getBefore() {
		return before;
	}

	public void setBefore(String before) {
		this.before = before;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public Integer getFirstIndex() {
		return firstIndex;
	}

	public void setFirstIndex(Integer firstIndex) {
		this.firstIndex = firstIndex;
	}

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public String getLast() {
		return last;
	}

	public void setLast(String last) {
		this.last = last;
	}

	public Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
	}

	/**
	 * @param received
	 *            number of currently received elements.
	 * @return <code>true</code> if forward pagination willn't receive any more
	 *         elements.
	 */
	public boolean isForwardFinished(int received) {
		return last == null
				|| (firstIndex != null && count != null && count - firstIndex == received);
	}

	/**
	 * @param received
	 *            number of currently received elements.
	 * @return <code>true</code> if backward pagination willn't receive any more
	 *         elements.
	 */
	public boolean isBackwardFinished(int received) {
		return first == null || (firstIndex != null && firstIndex == 0);
	}

}
