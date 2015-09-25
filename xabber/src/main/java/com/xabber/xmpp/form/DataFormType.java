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
package com.xabber.xmpp.form;

import java.util.NoSuchElementException;

/**
 * Form type for the Date Forms.
 * <p/>
 * http://xmpp.org/extensions/xep-0004.html#protocol-formtypes
 *
 * @author alexander.ivanov
 */
public enum DataFormType {

    cancel,

    form,

    result,

    submit;

    public static DataFormType fromString(String value)
            throws NoSuchElementException {
        if (value == null)
            throw new NoSuchElementException();
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new NoSuchElementException();
        }
    }

}