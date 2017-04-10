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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.helper.OrbotHelper;

public class OrbotInstallerDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private final static String MARKET_SEARCH = "market://search?q=pname:%s";
    private static final String LOG_TAG = OrbotInstallerDialog.class.getSimpleName();

    public static DialogFragment newInstance() {
        return new OrbotInstallerDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.orbot_required_title)
                .setMessage(R.string.orbot_required_message)
                .setPositiveButton(R.string.install_orbot, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setCancelable(false)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            Uri uri = Uri.parse(String.format(MARKET_SEARCH, OrbotHelper.URI_ORBOT));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            try {
                getActivity().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                LogManager.exception(LOG_TAG, e);
            }
        }
    }
}
