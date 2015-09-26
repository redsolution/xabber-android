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
package com.xabber.android.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import com.xabber.android.R;
import com.xabber.android.ui.helper.OrbotHelper;

/**
 * Orbot installer dialog builder.
 *
 * @author alexander.ivanov
 */
public class OrbotInstallerDialogBuilder {

    private final static String MARKET_SEARCH = "market://search?q=pname:%s";

    public static void show(final Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.orbot_required_title)
                .setMessage(R.string.orbot_required_message)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int w) {
                                Uri uri = Uri.parse(String.format(MARKET_SEARCH, OrbotHelper.URI_ORBOT));
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                activity.startActivity(intent);
                            }
                        })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

}
