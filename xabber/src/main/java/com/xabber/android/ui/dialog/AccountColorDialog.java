package com.xabber.android.ui.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.adapter.AccountColorPickerAdapter;

public class AccountColorDialog extends DialogFragment implements AccountColorPickerAdapter.Listener{
    private static final String ARGUMENT_ACCOUNT = AccountColorDialog.class.getName();
    private Dialog dialogEntity;
    AccountJid accountJid;

    public static DialogFragment newInstance(AccountJid account) {
        AccountColorDialog fragment = new AccountColorDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        accountJid = args.getParcelable(ARGUMENT_ACCOUNT);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        int colorIndex = AccountManager.INSTANCE.getColorLevel(accountJid);
        dialog.setTitle(getString(R.string.account_color));

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_color_picker, null);

        RecyclerView rv = view.findViewById(R.id.color_list);
        TypedArray colors = getResources().obtainTypedArray(R.array.account_500); /*getResources().getIntArray(R.array.account_500)*/
        //TypedArray darkColors = getResources().obtainTypedArray(R.array.account_700);
        AccountColorPickerAdapter adapter = new AccountColorPickerAdapter(getResources().getStringArray(R.array.account_color_names),
                 colors, /*darkColors,*/ colorIndex, this);
        rv.setLayoutManager(new LinearLayoutManager(getActivity()));
        rv.setAdapter(adapter);
        dialog.setView(view);
        dialog.setPositiveButton(null, null);
        dialog.setNegativeButton(android.R.string.cancel, null);

        dialogEntity = dialog.create();
        return dialogEntity;
    }

    @Override
    public void onColorClickListener(int position) {
        AccountManager.INSTANCE.setColor(accountJid, position);
        AccountManager.INSTANCE.setTimestamp(accountJid, (int) (System.currentTimeMillis() / 1000L));
        AccountManager.INSTANCE.onAccountChanged(accountJid);

        if (XabberAccountManager.getInstance().getAccount() != null)
            XabberAccountManager.getInstance().updateRemoteAccountSettings();
        dialogEntity.dismiss();
    }
}
