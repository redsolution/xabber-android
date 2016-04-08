package com.xabber.android.ui.dialog;

import android.app.DialogFragment;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.blocking.PrivateMucChatBlockingManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.ui.activity.ChatViewer;

public class MucPrivateChatInvitationDialog extends BaseContactDialog {

    public static DialogFragment newInstance(AccountJid account, UserJid contact) {
        DialogFragment fragment = new MucPrivateChatInvitationDialog();
        setArguments(account, contact, fragment);
        return fragment;
    }

    @Override
    protected int getDialogTitleTextResource() {
        return R.string.conference_private_chat;
    }

    @Override
    protected String getMessage() {
        return String.format(getString(R.string.conference_private_chat_invitation),
                getContact().getJid().getResourceOrNull().toString(), getContact().getJid().asBareJid().toString());
    }

    @Override
    protected int getNegativeButtonTextResource() {
        return R.string.decline_contact;
    }

    @Override
    protected int getPositiveButtonTextResource() {
        return R.string.accept_muc_private_chat;
    }

    @Override
    protected Integer getNeutralButtonTextResourceOrNull() {
        return R.string.block_muc_private_chat;
    }

    @Override
    protected void onPositiveButtonClick() {
        try {
            MessageManager.getInstance().acceptMucPrivateChat(getAccount(), getContact());
            startActivity(ChatViewer.createSpecificChatIntent(Application.getInstance(), getAccount(), getContact()));
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(this, e);
        }

    }

    @Override
    protected void onNegativeButtonClick() {
        MessageManager.getInstance().discardMucPrivateChat(getAccount(), getContact());
    }

    @Override
    protected void onNeutralButtonClick() {
        PrivateMucChatBlockingManager.getInstance().blockContact(getAccount(), getContact());
        Toast.makeText(Application.getInstance(), R.string.contact_blocked_successfully, Toast.LENGTH_SHORT).show();
        MessageManager.getInstance().discardMucPrivateChat(getAccount(), getContact());
    }
}
