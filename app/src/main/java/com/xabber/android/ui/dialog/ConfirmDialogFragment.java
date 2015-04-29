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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.os.Build;
import android.view.View;
import android.widget.Button;

/**
 * Base yes / no dialog fragment.
 * <p/>
 * Provides callback methods and option to abort dialog dismissing on button
 * click (starting from FROYO version).
 *
 * @author alexander.ivanov
 */
public abstract class ConfirmDialogFragment extends AbstractDialogFragment {

    private final OnClickListener positiveListener = new OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (onPositiveClick())
                supportDismiss(dialog);
        }

    };

    private final OnClickListener neutralListener = new OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (onNegativeClicked())
                supportDismiss(dialog);
        }

    };

    private final OnClickListener negativeListener = new OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (onNegativeClicked())
                supportDismiss(dialog);
        }

    };

    @Override
    protected Dialog getDialog(Builder builder) {
        if (hasPositiveButton())
            builder.setPositiveButton(getPositiveTextId(), positiveListener);
        if (hasNeutralButton())
            builder.setNeutralButton(getNeutralTextId(), neutralListener);
        if (hasNegativeButton())
            builder.setNegativeButton(getNegativeTextId(), negativeListener);
        AlertDialog dialog = builder.create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
            setOnShowListener(dialog);
        return dialog;
    }

    private void supportDismiss(DialogInterface dialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
            dialog.dismiss();
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void setOnShowListener(final AlertDialog alertDialog) {
        alertDialog.setOnShowListener(new OnShowListener() {

            private void setListener(final int whichButton,
                                     final OnClickListener listener) {
                Button button = alertDialog.getButton(whichButton);
                if (button == null)
                    return;
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        listener.onClick(alertDialog, whichButton);
                    }
                });
            }

            @Override
            public void onShow(DialogInterface dialog) {
                setListener(AlertDialog.BUTTON_POSITIVE, positiveListener);
                setListener(AlertDialog.BUTTON_NEUTRAL, neutralListener);
                setListener(AlertDialog.BUTTON_NEGATIVE, negativeListener);
            }

        });
    }

    protected boolean hasPositiveButton() {
        return true;
    }

    protected boolean hasNeutralButton() {
        return false;
    }

    protected boolean hasNegativeButton() {
        return true;
    }

    protected int getPositiveTextId() {
        return android.R.string.ok;
    }

    protected int getNeutralTextId() {
        return android.R.string.unknownName;
    }

    protected int getNegativeTextId() {
        return android.R.string.cancel;
    }

    /**
     * Processes positive button click.
     *
     * @return Whether dialog can be dismissed.
     */
    protected boolean onPositiveClick() {
        return true;
    }

    /**
     * Processes neutral button click.
     *
     * @return Whether dialog can be dismissed.
     */
    protected boolean onNeutralClicked() {
        return true;
    }

    /**
     * Processes negative button click.
     *
     * @return Whether dialog can be dismissed.
     */
    protected boolean onNegativeClicked() {
        return true;
    }

}
