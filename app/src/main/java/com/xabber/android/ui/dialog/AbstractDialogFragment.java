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
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

import java.util.ArrayList;

/**
 * Base dialog fragment.
 * <p/>
 * CONVENTION: Subclass should implement <code>newInstance</code> static method.
 * Activities or Fragments should use this method to instantiate DialogFragment.
 *
 * @author alexander.ivanov
 */
public abstract class AbstractDialogFragment extends DialogFragment {

    private Bundle initArguments() {
        Bundle bundle = getArguments();
        if (bundle == null) {
            bundle = new Bundle();
            setArguments(bundle);
        }
        return bundle;
    }

    protected AbstractDialogFragment putAgrument(String key, String value) {
        initArguments().putString(key, value);
        return this;
    }

    protected AbstractDialogFragment putAgrument(String key,
                                                 ArrayList<String> value) {
        initArguments().putStringArrayList(key, value);
        return this;
    }

    protected AbstractDialogFragment putAgrument(String key, boolean value) {
        initArguments().putBoolean(key, value);
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = getBuilder();
        return getDialog(builder);
    }

    /**
     * Constructs {@link AlertDialog.Builder} instance.
     *
     * @return
     */
    protected abstract Builder getBuilder();

    /**
     * Constructs {@link Dialog} instance.
     *
     * @param builder
     * @return
     */
    protected Dialog getDialog(Builder builder) {
        return builder.create();
    }

}
