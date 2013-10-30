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
package com.xabber.android.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.Spannable;
import android.text.Spannable.Factory;
import android.text.style.ImageSpan;

import com.xabber.android.data.SettingsManager;
import com.xabber.androiddev.R;

/**
 * Emoticons.
 * 
 * @author alexander.ivanov
 * 
 */
public class Emoticons {

	public static final Map<Pattern, Integer> ANDROID_EMOTICONS = new HashMap<Pattern, Integer>();
	public static final Map<Pattern, Integer> NONE_EMOTICONS = new HashMap<Pattern, Integer>();

	private static final Factory spannableFactory = Spannable.Factory
			.getInstance();

	static {
		addPattern(ANDROID_EMOTICONS, ":mad:", R.drawable.mad);
		addPattern(ANDROID_EMOTICONS, ";)", R.drawable.wink);
		addPattern(ANDROID_EMOTICONS, ":o", R.drawable.redface);
		addPattern(ANDROID_EMOTICONS, ":(", R.drawable.frown);
		addPattern(ANDROID_EMOTICONS, ":rolleyes:", R.drawable.rolleyes);
		addPattern(ANDROID_EMOTICONS, ":eek:", R.drawable.eek);
		addPattern(ANDROID_EMOTICONS, ":p", R.drawable.tongue);
		addPattern(ANDROID_EMOTICONS, ":D", R.drawable.biggrin);
		addPattern(ANDROID_EMOTICONS, ":)", R.drawable.smile);
		addPattern(ANDROID_EMOTICONS, ":confused:", R.drawable.confused);
		addPattern(ANDROID_EMOTICONS, ":cool:", R.drawable.cool);
		addPattern(ANDROID_EMOTICONS, ":comp:", R.drawable.comp);
		addPattern(ANDROID_EMOTICONS, ":think:", R.drawable.think);
		addPattern(ANDROID_EMOTICONS, ":psmoke:", R.drawable.porvarismoke);
		addPattern(ANDROID_EMOTICONS, ":eat1:", R.drawable.eat);
		addPattern(ANDROID_EMOTICONS, ":kahvi:", R.drawable.kahvi3);
		addPattern(ANDROID_EMOTICONS, ":kippis:", R.drawable.kippis);
		addPattern(ANDROID_EMOTICONS, ":smoke:", R.drawable.smoker);
		addPattern(ANDROID_EMOTICONS, ":hammer:", R.drawable.hammer);
		addPattern(ANDROID_EMOTICONS, ":hammer2:", R.drawable.hammer2);
		addPattern(ANDROID_EMOTICONS, ":hammer3:", R.drawable.hammer3);
		addPattern(ANDROID_EMOTICONS, ":facepalm:", R.drawable.facepalm);
		addPattern(ANDROID_EMOTICONS, ":mad2:", R.drawable.mad2);
		addPattern(ANDROID_EMOTICONS, ":comp2:", R.drawable.comp2);
		addPattern(ANDROID_EMOTICONS, ":conf:", R.drawable.conf);
		addPattern(ANDROID_EMOTICONS, ":gadne:", R.drawable.gadne);
		addPattern(ANDROID_EMOTICONS, ":gcomp:", R.drawable.gcomp);
		addPattern(ANDROID_EMOTICONS, ":gwalk:", R.drawable.gwalk);
		addPattern(ANDROID_EMOTICONS, ":gkippis:", R.drawable.gkippis);
		addPattern(ANDROID_EMOTICONS, ":ghammer:", R.drawable.ghammer);
		addPattern(ANDROID_EMOTICONS, ":ghammer3:", R.drawable.ghammer3);
		addPattern(ANDROID_EMOTICONS, ":gthink:", R.drawable.gthink);
		addPattern(ANDROID_EMOTICONS, ":apple:", R.drawable.apple);
		addPattern(ANDROID_EMOTICONS, ":play:", R.drawable.play);
		addPattern(ANDROID_EMOTICONS, ":thumb:", R.drawable.thumb);
		addPattern(ANDROID_EMOTICONS, ":laihduta:", R.drawable.laihduta);
	}

	private static void addPattern(Map<Pattern, Integer> map, String smile,
			int resource) {
		map.put(Pattern.compile(Pattern.quote(smile)), resource);
	}

	private Emoticons() {
	}

	/**
	 * @param text
	 * @return new spannable.
	 */
	public static Spannable newSpannable(CharSequence text) {
		return spannableFactory.newSpannable(text);
	}

	/**
	 * @param context
	 * @param spannable
	 * @return Whether smiles have been added into <code>spannable</code>.
	 */
	public static boolean getSmiledText(Context context, Spannable spannable) {
		boolean hasChanges = false;
		Map<Pattern, Integer> emoticons = SettingsManager.interfaceSmiles();
		for (Entry<Pattern, Integer> entry : emoticons.entrySet()) {
			Matcher matcher = entry.getKey().matcher(spannable);
			while (matcher.find()) {
				boolean set = true;
				for (ImageSpan span : spannable.getSpans(matcher.start(),
						matcher.end(), ImageSpan.class))
					if (spannable.getSpanStart(span) >= matcher.start()
							&& spannable.getSpanEnd(span) <= matcher.end())
						spannable.removeSpan(span);
					else {
						set = false;
						break;
					}
				if (set) {
					spannable.setSpan(new ImageSpan(context, entry.getValue()),
							matcher.start(), matcher.end(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					hasChanges = true;
				}
			}
		}
		return hasChanges;
	}

	/**
	 * @param context
	 * @param text
	 * @return New spannable with added smiles if needed.
	 */
	public static Spannable getSmiledText(Context context, CharSequence text) {
		Spannable spannable = spannableFactory.newSpannable(text);
		getSmiledText(context, spannable);
		return spannable;
	}

}
