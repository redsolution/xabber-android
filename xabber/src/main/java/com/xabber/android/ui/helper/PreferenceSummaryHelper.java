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
package com.xabber.android.ui.helper;

import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;

/**
 * Update preference's title and summary based on multiline title.
 *
 * @author alexander.ivanov
 */
public final class PreferenceSummaryHelper {

    private PreferenceSummaryHelper() {
    }

    public static void updateSummary(PreferenceGroup group) {
        for (int index = 0; index < group.getPreferenceCount(); index++) {
            Preference preference = group.getPreference(index);
            if (preference instanceof PreferenceGroup) {
                updateSummary((PreferenceGroup) preference);
            }
            String titleAndSummary = preference.getTitle().toString();

            if (!isTitleAndSummary(titleAndSummary)) {
                continue;
            }

            preference.setTitle(getPreferenceTitle(titleAndSummary));
            if (preference instanceof DialogPreference) {
                ((DialogPreference) preference).setDialogTitle(preference.getTitle());
            }
            preference.setSummary(getPreferenceSummary(titleAndSummary));
        }
    }

    private static boolean isTitleAndSummary(String titleAndSummary) {
        return titleAndSummary.contains("\n");
    }

    public static String getPreferenceTitle(String titleAndSummary) {
        int delimiter = titleAndSummary.indexOf("\n");
        if (delimiter == -1) {
            return titleAndSummary;
        }
        return titleAndSummary.substring(0, delimiter);
    }

    private static String getPreferenceSummary(String titleAndSummary) {
        int delimiter = titleAndSummary.indexOf("\n");
        if (delimiter == -1) {
            return "";
        }
        return titleAndSummary.substring(delimiter + 1, titleAndSummary.length());
    }

}
