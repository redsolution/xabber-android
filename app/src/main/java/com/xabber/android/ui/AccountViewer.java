package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.xabber.android.R;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.vcard.VCardProperty;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class AccountViewer extends ContactViewer implements Toolbar.OnMenuItemClickListener {

    public static final int ACCOUNT_VIEWER_MENU = R.menu.account_viewer;
    public static final int ACCOUNT_INFO_EDITOR_REQUEST_CODE = 1;

    public static Intent createIntent(Context context, String account) {
        return new EntityIntentBuilder(context, AccountViewer.class)
                .setAccount(account).setUser(GroupManager.IS_ACCOUNT).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = contactTitleExpandableToolbarInflater.getToolbar();
        toolbar.inflateMenu(ACCOUNT_VIEWER_MENU);
        toolbar.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(ACCOUNT_VIEWER_MENU, menu);

        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_account_user_info:
                VCard vCard = ((ContactVcardViewerFragment) getFragmentManager().findFragmentById(R.id.scrollable_container)).getvCard();
                if (vCard != null) {
                    Intent intent = AccountInfoEditor.createIntent(this, getAccount(), vCard.getChildElementXML().toString());
                    startActivityForResult(intent, ACCOUNT_INFO_EDITOR_REQUEST_CODE);
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != ACCOUNT_INFO_EDITOR_REQUEST_CODE) {
            return;
        }

        if (resultCode == AccountInfoEditorFragment.ACCOUNT_INFO_EDITOR_RESULT_NEED_VCARD_REQUEST) {
            ((ContactVcardViewerFragment)getFragmentManager().findFragmentById(R.id.scrollable_container)).requestVCard();
        }

        if (resultCode == RESULT_OK) {
            String vCardXml = data.getStringExtra(AccountInfoEditorFragment.ARGUMENT_VCARD);

            VCard vCard = null;
            if (vCardXml != null) {
                try {
                    vCard = ContactVcardViewerFragment.parseVCard(vCardXml);
                } catch (XmlPullParserException | IOException | SmackException e) {
                    e.printStackTrace();
                }
            }

            if (vCard != null) {
                vCard.getField(VCardProperty.NICKNAME.name());
                ((ContactVcardViewerFragment) getFragmentManager().findFragmentById(R.id.scrollable_container)).onVCardReceived(getAccount(), Jid.getBareAddress(getAccount()), vCard);
            } else {
                ((ContactVcardViewerFragment)getFragmentManager().findFragmentById(R.id.scrollable_container)).requestVCard();
            }
        }
    }
}
