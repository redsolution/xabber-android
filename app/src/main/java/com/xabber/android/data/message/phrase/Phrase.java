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
package com.xabber.android.data.message.phrase;

import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.net.Uri;

/**
 * Settings for phrase search.
 * 
 * @author alexander.ivanov
 * 
 */
public class Phrase {

	/**
	 * Database ID. Should be used from background thread only.
	 */
	private Long id;

	/**
	 * Part of message body.
	 */
	private String text;

	/**
	 * Part of sender's JID.
	 */
	private String user;

	/**
	 * Part of one of the sender's roster groups.
	 */
	private String group;

	private boolean regexp;
	private Uri sound;

	private Pattern textPattern;
	private Pattern userPattern;
	private Pattern groupPattern;

	public Phrase(Long id, String value, String user, String group,
			boolean regexp, Uri sound) {
		super();
		setId(id);
		update(value, user, group, regexp, sound);
	}

	/**
	 * @param text
	 * @param user
	 * @param groups
	 * @return Whether phrase was found in specified text for user in specified
	 *         groups.
	 */
	public boolean matches(String text, String user, Collection<String> groups) {
		if (textPattern.matcher(text).find()
				&& userPattern.matcher(user).find()) {
			if (groups.isEmpty())
				return groupPattern.matcher("").find();
			for (String group : groups)
				if (groupPattern.matcher(group).find())
					return true;
		}
		return false;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public String getUser() {
		return user;
	}

	public String getGroup() {
		return group;
	}

	public boolean isRegexp() {
		return regexp;
	}

	public Uri getSound() {
		return sound;
	}

	void update(String text, String user, String group, boolean regexp,
			Uri sound) {
		this.text = text;
		this.user = user;
		this.group = group;
		this.regexp = regexp;
		this.sound = sound;
		if (!regexp) {
			text = Pattern.quote(text);
			user = Pattern.quote(user);
			group = Pattern.quote(group);
		}
		textPattern = compile(text);
		userPattern = compile(user);
		groupPattern = compile(group);
	}

	public static Pattern compile(String value) throws PatternSyntaxException {
		return Pattern.compile(value, Pattern.CASE_INSENSITIVE
				| Pattern.MULTILINE);
	}

}
