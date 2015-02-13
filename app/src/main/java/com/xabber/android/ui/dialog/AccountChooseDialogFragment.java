package com.xabber.android.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.AccountChooseAdapter;

import java.util.ArrayList;

public class AccountChooseDialogFragment extends AbstractDialogFragment {

    private static final String USER = "USER";
    private static final String TEXT = "TEXT";

    /**
     * @param user
     * @param text
     * @return
     */
    public static DialogFragment newInstance(String user, String text) {
        return new AccountChooseDialogFragment().putAgrument(USER, user)
                .putAgrument(TEXT, text);
    }

    private String user;
    private String text;

    @Override
    protected Builder getBuilder() {
        user = getArguments().getString(USER);
        text = getArguments().getString(TEXT);
        final Adapter adapter = new Adapter(getActivity());
        Builder builder = new Builder(getActivity());
        builder.setSingleChoiceItems(adapter, -1, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String account = (String) adapter.getItem(which);
                OnChoosedListener listener = (OnChoosedListener) getActivity();
                listener.onChoose(account, user, text);
            }
        });
        return builder;
    }

    private class Adapter extends AccountChooseAdapter {

        public Adapter(Activity activity) {
            super(activity);
            ArrayList<String> available = new ArrayList<String>();
            for (RosterContact check : RosterManager.getInstance()
                    .getContacts())
                if (check.isEnabled() && check.getUser().equals(user))
                    available.add(check.getAccount());
            if (!available.isEmpty()) {
                accounts.clear();
                accounts.addAll(available);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getDropDownView(position, convertView, parent);
        }

    }

    public interface OnChoosedListener {

        void onChoose(String account, String user, String text);

    }

}
