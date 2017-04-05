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
package com.xabber.android.ui.adapter;

import java.util.Comparator;
import java.util.Date;

import com.xabber.android.data.message.AbstractChat;

public class ChatComparator implements Comparator<AbstractChat> {

    public static final ChatComparator CHAT_COMPARATOR = new ChatComparator();

    @Override
    public int compare(AbstractChat chat1, AbstractChat chat2) {
        Date lastTime1 = chat1.getLastTime();
        Date lastTime2 = chat2.getLastTime();

        if (lastTime1 == null && lastTime2 == null) {
            return 0;
        }

        if (lastTime1 == null) {
            return 1;
        }

        if (lastTime2 == null) {
            return -1;
        }

        return -lastTime1.compareTo(lastTime2);
    }

}