package com.xabber.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.ui.activity.BlockedListActivity;
import com.xabber.android.ui.color.ColorManager;

public class BlockByJidDialog extends DialogFragment implements BlockingManager.BlockContactListener, View.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.JidBlocker.ARGUMENT_ACCOUNT";

    private AccountJid account;

    private EditText blockJid;
    private Button block;

    public static BlockByJidDialog newInstance(AccountJid account) {
        BlockByJidDialog fragment = new BlockByJidDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Context context = builder.getContext();
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.dialog_block_jid, null);

        Bundle args = getArguments();

        if (args == null) dismiss();

        account = args.getParcelable(ARGUMENT_ACCOUNT);
        int colorIndicator = ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(account);

        block = (Button) view.findViewById(R.id.block);
        block.setTextColor(colorIndicator);

        blockJid = view.findViewById(R.id.block_jid);
        blockJid.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals("")) {
                    block.setEnabled(true);
                }
            }
        });

        view.findViewById(R.id.cancel_block).setOnClickListener(this);
        view.findViewById(R.id.block).setOnClickListener(this);

        return builder.setTitle(R.string.contact_bar_block)
                .setView(view)
                .create();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.block:
                if (validationIsSuccess()) {
                    try {
                        BlockingManager.getInstance().blockContact(account, ContactJid.from(blockJid.getText().toString()), this);
                    } catch (ContactJid.ContactJidCreateException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.cancel_block:
                dismiss();
                break;
        }
    }

    @Override
    public void onSuccessBlock() {
        Toast.makeText(Application.getInstance(), R.string.contact_blocked_successfully, Toast.LENGTH_SHORT).show();
        dismiss();
        if (getActivity() instanceof BlockedListActivity) {
            ((BlockedListActivity)getActivity()).update();
        }
    }

    @Override
    public void onErrorBlock() {
        Toast.makeText(Application.getInstance(), R.string.error_blocking_contact, Toast.LENGTH_SHORT).show();
    }

    private void setError(String error) {
        block.setEnabled(false);
        blockJid.setError(error);
    }

    private boolean validationIsSuccess(){
        String contactString = blockJid.getText().toString();
        contactString = contactString.trim();

        if (contactString.contains(" ")) {
            setError(getString(R.string.INCORRECT_USER_NAME));
            return false;
        }

        if (TextUtils.isEmpty(contactString)) {
            setError(getString(R.string.EMPTY_USER_NAME));
            return false;
        }

        int atChar = contactString.indexOf('@');
        int slashIndex = contactString.indexOf('/');

        String domainName;
        String localName;
        String resourceName;

        if (slashIndex > 0) {
            resourceName = contactString.substring(slashIndex + 1);
            if (atChar > 0 && atChar < slashIndex) {
                localName = contactString.substring(0, atChar);
                domainName = contactString.substring(atChar + 1, slashIndex);
            } else {
                localName = "";
                domainName = contactString.substring(0, slashIndex);
            }
        } else {
            resourceName = "";
            if (atChar > 0) {
                localName = contactString.substring(0, atChar);
                domainName = contactString.substring(atChar + 1);
            } else {
                localName = "";
                domainName = contactString;
            }
        }

        //
        if (!resourceName.equals("")) {
            setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_RESOURCE));
            return false;
        }
        //Invalid when domain is empty
        if (domainName.equals("")) {
            setError(getString(R.string.INCORRECT_USER_NAME));
            return false;
        }

        //Invalid when "@" is present but localPart is empty
        if (atChar == 0) {
            setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_LOCAL_AT));
            return false;
        }

        //Invalid when "@" is present in a domainPart
        if (atChar > 0) {
            if (domainName.contains("@")) {
                setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_AT));
                return false;
            }
        }

        //Invalid when domain has "." at the start/end
        if (domainName.charAt(domainName.length()-1)=='.' || domainName.charAt(0)=='.'){
            setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_DOMAIN));
            return false;
        }
        //Invalid when domain does not have a "." in the middle, when paired with the last check
        if (!domainName.contains(".")) {
            setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_DOMAIN));
            return false;
        }
        //Invalid when domain has multiple dots in a row
        if(domainName.contains("..")) {
            setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_DOMAIN));
            return false;
        }

        if (!localName.equals("")) {
            //Invalid when localPart is NOT empty, and HAS "." at the start/end
            if (localName.charAt(localName.length() - 1) == '.' || localName.charAt(0) == '.') {
                setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_LOCAL));
                return false;
            }
            //Invalid when localPart is NOT empty, and contains ":" or "/" symbol. Other restricted localPart symbols get checked during the creation of the jid/userJid.
            if (localName.contains(":")) {
                setError(getString(R.string.INCORRECT_USER_NAME) + String.format(getString(R.string.INCORRECT_USER_NAME_ADDENDUM_LOCAL_SYMBOL), ":"));
                return false;
            }
            if (localName.contains("/")) {
                setError(getString(R.string.INCORRECT_USER_NAME) + String.format(getString(R.string.INCORRECT_USER_NAME_ADDENDUM_LOCAL_SYMBOL), "/"));
                return false;
            }
            //Invalid when localPart is NOT empty, and has multiple dots in a row
            if(localName.contains("..")) {
                setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_LOCAL));
                return false;
            }
        }
        return true;
    }

}
