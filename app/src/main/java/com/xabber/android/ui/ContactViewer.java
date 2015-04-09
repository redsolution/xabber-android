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
package com.xabber.android.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.vcard.OnVCardListener;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.dialog.ContactDeleteDialogFragment;
import com.xabber.android.ui.helper.ContactTitleExpandableToolbarInflater;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.vcard.AddressProperty;
import com.xabber.xmpp.vcard.AddressType;
import com.xabber.xmpp.vcard.EmailType;
import com.xabber.xmpp.vcard.TelephoneType;
import com.xabber.xmpp.vcard.VCard;
import com.xabber.xmpp.vcard.VCardProvider;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactViewer extends ManagedActivity implements
        OnVCardListener, OnContactChangedListener, OnAccountChangedListener {

    private static final String SAVED_VCARD = "com.xabber.android.ui.ContactViewer.SAVED_VCARD";
    private static final String SAVED_VCARD_ERROR = "com.xabber.android.ui.ContactViewer.SAVED_VCARD_ERROR";

    private String account;
    private String bareAddress;
    private VCard vCard;
    private boolean vCardError;


    private static final Map<AddressType, Integer> ADDRESS_TYPE_MAP = new HashMap<>();
    private static final Map<AddressProperty, Integer> ADDRESS_PROPERTY_MAP = new HashMap<>();
    private static final Map<TelephoneType, Integer> TELEPHONE_TYPE_MAP = new HashMap<>();
    private static final Map<EmailType, Integer> EMAIL_TYPE_MAP = new HashMap<>();

    private ContactTitleExpandableToolbarInflater contactTitleExpandableToolbarInflater;

    static {
        ADDRESS_TYPE_MAP.put(AddressType.DOM, R.string.vcard_type_dom);
        ADDRESS_TYPE_MAP.put(AddressType.HOME, R.string.vcard_type_home);
        ADDRESS_TYPE_MAP.put(AddressType.INTL, R.string.vcard_type_intl);
        ADDRESS_TYPE_MAP.put(AddressType.PARCEL, R.string.vcard_type_parcel);
        ADDRESS_TYPE_MAP.put(AddressType.POSTAL, R.string.vcard_type_postal);
        ADDRESS_TYPE_MAP.put(AddressType.PREF, R.string.vcard_type_pref);
        ADDRESS_TYPE_MAP.put(AddressType.WORK, R.string.vcard_type_work);
        if (ADDRESS_TYPE_MAP.size() != AddressType.values().length)
            throw new IllegalStateException();

        ADDRESS_PROPERTY_MAP.put(AddressProperty.CTRY,
                R.string.vcard_address_ctry);
        ADDRESS_PROPERTY_MAP.put(AddressProperty.EXTADR,
                R.string.vcard_address_extadr);
        ADDRESS_PROPERTY_MAP.put(AddressProperty.LOCALITY,
                R.string.vcard_address_locality);
        ADDRESS_PROPERTY_MAP.put(AddressProperty.PCODE,
                R.string.vcard_address_pcode);
        ADDRESS_PROPERTY_MAP.put(AddressProperty.POBOX,
                R.string.vcard_address_pobox);
        ADDRESS_PROPERTY_MAP.put(AddressProperty.REGION,
                R.string.vcard_address_region);
        ADDRESS_PROPERTY_MAP.put(AddressProperty.STREET,
                R.string.vcard_address_street);
        if (ADDRESS_PROPERTY_MAP.size() != AddressProperty.values().length)
            throw new IllegalStateException();

        TELEPHONE_TYPE_MAP.put(TelephoneType.BBS, R.string.vcard_type_bbs);
        TELEPHONE_TYPE_MAP.put(TelephoneType.CELL, R.string.vcard_type_cell);
        TELEPHONE_TYPE_MAP.put(TelephoneType.FAX, R.string.vcard_type_fax);
        TELEPHONE_TYPE_MAP.put(TelephoneType.HOME, R.string.vcard_type_home);
        TELEPHONE_TYPE_MAP.put(TelephoneType.ISDN, R.string.vcard_type_isdn);
        TELEPHONE_TYPE_MAP.put(TelephoneType.MODEM, R.string.vcard_type_modem);
        TELEPHONE_TYPE_MAP.put(TelephoneType.MSG, R.string.vcard_type_msg);
        TELEPHONE_TYPE_MAP.put(TelephoneType.PAGER, R.string.vcard_type_pager);
        TELEPHONE_TYPE_MAP.put(TelephoneType.PCS, R.string.vcard_type_pcs);
        TELEPHONE_TYPE_MAP.put(TelephoneType.PREF, R.string.vcard_type_pref);
        TELEPHONE_TYPE_MAP.put(TelephoneType.VIDEO, R.string.vcard_type_video);
        TELEPHONE_TYPE_MAP.put(TelephoneType.VOICE, R.string.vcard_type_voice);
        TELEPHONE_TYPE_MAP.put(TelephoneType.WORK, R.string.vcard_type_work);
        if (TELEPHONE_TYPE_MAP.size() != TelephoneType.values().length)
            throw new IllegalStateException();

        EMAIL_TYPE_MAP.put(EmailType.HOME, R.string.vcard_type_home);
        EMAIL_TYPE_MAP.put(EmailType.INTERNET, R.string.vcard_type_internet);
        EMAIL_TYPE_MAP.put(EmailType.PREF, R.string.vcard_type_pref);
        EMAIL_TYPE_MAP.put(EmailType.WORK, R.string.vcard_type_work);
        EMAIL_TYPE_MAP.put(EmailType.X400, R.string.vcard_type_x400);
        if (EMAIL_TYPE_MAP.size() != EmailType.values().length)
            throw new IllegalStateException();
    }

    private boolean isAccount = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            // View information about contact from system contact list
            Uri data = getIntent().getData();
            if (data != null && "content".equals(data.getScheme())) {
                List<String> segments = data.getPathSegments();
                if (segments.size() == 2 && "data".equals(segments.get(0))) {
                    Long id;
                    try {
                        id = Long.valueOf(segments.get(1));
                    } catch (NumberFormatException e) {
                        id = null;
                    }
                    if (id != null)
                        // FIXME: Will be empty while application is loading
                        for (RosterContact rosterContact : RosterManager.getInstance().getContacts())
                            if (id.equals(rosterContact.getViewId())) {
                                account = rosterContact.getAccount();
                                bareAddress = rosterContact.getUser();
                                break;
                            }
                }
            }
        } else {
            account = getAccount(getIntent());
            bareAddress = Jid.getBareAddress(getUser(getIntent()));
        }

        if (bareAddress != null && bareAddress.equalsIgnoreCase(GroupManager.IS_ACCOUNT)) {
            isAccount = true;
            bareAddress = Jid.getBareAddress(AccountManager.getInstance().getAccount(account).getRealJid());
        }

        if (account == null || bareAddress == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }
        vCard = null;
        vCardError = false;
        if (savedInstanceState != null) {
            vCardError = savedInstanceState
                    .getBoolean(SAVED_VCARD_ERROR, false);
            String xml = savedInstanceState.getString(SAVED_VCARD);
            if (xml != null)
                try {
                    XmlPullParser parser = XmlPullParserFactory.newInstance()
                            .newPullParser();
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,
                            true);
                    parser.setInput(new StringReader(xml));
                    int eventType = parser.next();
                    if (eventType != XmlPullParser.START_TAG)
                        throw new IllegalStateException(
                                String.valueOf(eventType));
                    if (!VCard.ELEMENT_NAME.equals(parser.getName()))
                        throw new IllegalStateException(parser.getName());
                    if (!VCard.NAMESPACE.equals(parser.getNamespace()))
                        throw new IllegalStateException(parser.getNamespace());
                    vCard = (VCard) (new VCardProvider()).parseIQ(parser);
                } catch (Exception e) {
                    LogManager.exception(this, e);
                }
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.scrollable_container, new ContactViewerFragment()).commit();
        }


        contactTitleExpandableToolbarInflater = new ContactTitleExpandableToolbarInflater(this);
        AbstractContact bestContact = RosterManager.getInstance().getBestContact(account, bareAddress);
        contactTitleExpandableToolbarInflater.onCreate(bestContact);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnVCardListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        if (vCard == null && !vCardError) {
            VCardManager.getInstance().request(account, bareAddress, null);
        }

        contactTitleExpandableToolbarInflater.onResume();

        ContactViewerFragment contactViewerFragment = getFragment();

        contactViewerFragment.updateContact(account, bareAddress);
        contactViewerFragment.updateVCard(vCard);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isAccount || MUCManager.getInstance().hasRoom(account, bareAddress)) {
            return true;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contact_viewer, menu);

        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, bareAddress);
        if (rosterContact == null) {
            menu.findItem(R.id.action_edit_alias).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_alias:
                editAlias();
                return true;

            case R.id.action_edit_groups:
                startActivity(GroupEditor.createIntent(this, account, bareAddress));
                return true;

            case R.id.action_remove_contact:
                ContactDeleteDialogFragment.newInstance(account, bareAddress)
                        .show(getFragmentManager(), "CONTACT_DELETE");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void editAlias() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_alias);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, bareAddress);
        input.setText(rosterContact.getName());
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    RosterManager.getInstance().setName(account, bareAddress, input.getText().toString());
                } catch (NetworkException e) {
                    Application.getInstance().onError(e);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private ContactViewerFragment getFragment() {
        return (ContactViewerFragment) getFragmentManager().findFragmentById(R.id.scrollable_container);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnVCardListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_VCARD_ERROR, vCardError);
        if (vCard != null) {
            outState.putString(SAVED_VCARD, vCard.getChildElementXML());
        }
    }

    @Override
    public void onVCardReceived(String account, String bareAddress, VCard vCard) {
        if (!this.account.equals(account) || !this.bareAddress.equals(bareAddress)) {
            return;
        }
        this.vCard = vCard;
        this.vCardError = false;
        getFragment().updateVCard(vCard);
    }

    @Override
    public void onVCardFailed(String account, String bareAddress) {
        if (!this.account.equals(account)
                || !this.bareAddress.equals(bareAddress))
            return;
        this.vCard = null;
        this.vCardError = true;
        getFragment().updateVCard(vCard);
        Application.getInstance().onError(R.string.XMPP_EXCEPTION);
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {
        for (BaseEntity entity : entities)
            if (entity.equals(account, bareAddress)) {
                getFragment().updateContact(account, bareAddress);
                break;
            }
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        if (accounts.contains(account))
            getFragment().updateContact(account, bareAddress);
    }


    public static Intent createIntent(Context context, String account, String user) {
        return new EntityIntentBuilder(context, ContactViewer.class)
                .setAccount(account).setUser(user).build();
    }

    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static String getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    public static Map<AddressType, Integer> getAddressTypeMap() {
        return ADDRESS_TYPE_MAP;
    }

    public static Map<AddressProperty, Integer> getAddressPropertyMap() {
        return ADDRESS_PROPERTY_MAP;
    }

    public static Map<TelephoneType, Integer> getTelephoneTypeMap() {
        return TELEPHONE_TYPE_MAP;
    }

    public static Map<EmailType, Integer> getEmailTypeMap() {
        return EMAIL_TYPE_MAP;
    }


    public View getContactTitleView() {
        return findViewById(R.id.expandable_contact_title);
    }

}
