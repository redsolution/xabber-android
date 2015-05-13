package com.xabber.android.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.VcardMaps;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.capability.ClientInfo;
import com.xabber.android.data.extension.vcard.OnVCardListener;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.ResourceItem;
import com.xabber.xmpp.vcard.Address;
import com.xabber.xmpp.vcard.AddressProperty;
import com.xabber.xmpp.vcard.AddressType;
import com.xabber.xmpp.vcard.Email;
import com.xabber.xmpp.vcard.EmailType;
import com.xabber.xmpp.vcard.NameProperty;
import com.xabber.xmpp.vcard.Organization;
import com.xabber.xmpp.vcard.Telephone;
import com.xabber.xmpp.vcard.TelephoneType;
import com.xabber.xmpp.vcard.VCard;
import com.xabber.xmpp.vcard.VCardProperty;
import com.xabber.xmpp.vcard.VCardProvider;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContactVcardViewerFragment extends Fragment implements OnVCardListener, OnContactChangedListener, OnAccountChangedListener {
    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.ContactVcardViewerFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.xabber.android.ui.ContactVcardViewerFragment.ARGUMENT_USER";
    private static final String SAVED_VCARD = "com.xabber.android.ui.ContactVcardViewerFragment.SAVED_VCARD";
    private static final String SAVED_VCARD_ERROR = "com.xabber.android.ui.ContactVcardViewerFragment.SAVED_VCARD_ERROR";
    String account;
    String user;
    private LinearLayout xmppItems;
    private LinearLayout contactInfoItems;
    private VCard vCard;
    private boolean vCardError;

    public static ContactVcardViewerFragment newInstance(String account, String user) {
        ContactVcardViewerFragment fragment = new ContactVcardViewerFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_ACCOUNT, account);
        arguments.putString(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        account = args.getString(ARGUMENT_ACCOUNT, null);
        user = args.getString(ARGUMENT_USER, null);

        vCard = null;
        vCardError = false;
        if (savedInstanceState != null) {
            vCardError = savedInstanceState.getBoolean(SAVED_VCARD_ERROR, false);
            String xml = savedInstanceState.getString(SAVED_VCARD);
            if (xml != null)
                try {
                    XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
                    parser.setInput(new StringReader(xml));
                    int eventType = parser.next();
                    if (eventType != XmlPullParser.START_TAG) {
                        throw new IllegalStateException(String.valueOf(eventType));
                    }
                    if (!VCard.ELEMENT_NAME.equals(parser.getName())) {
                        throw new IllegalStateException(parser.getName());
                    }
                    if (!VCard.NAMESPACE.equals(parser.getNamespace())) {
                        throw new IllegalStateException(parser.getNamespace());
                    }
                    vCard = (VCard) (new VCardProvider()).parseIQ(parser);
                } catch (Exception e) {
                    LogManager.exception(this, e);
                }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.contact_vcard_viewer_fragment, container, false);

        xmppItems = (LinearLayout) view.findViewById(R.id.xmpp_items);
        contactInfoItems = (LinearLayout) view.findViewById(R.id.contact_info_items);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnVCardListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        if (vCard == null && !vCardError) {
            VCardManager.getInstance().request(account, user, null);
        }

        updateContact(account, user);
        updateVCard(vCard);
    }

    @Override
    public void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnVCardListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_VCARD_ERROR, vCardError);
        if (vCard != null) {
            outState.putString(SAVED_VCARD, vCard.getChildElementXML());
        }
    }

    @Override
    public void onVCardReceived(String account, String bareAddress, VCard vCard) {
        if (!this.account.equals(account) || !this.user.equals(bareAddress)) {
            return;
        }
        this.vCard = vCard;
        this.vCardError = false;
        updateVCard(vCard);
    }

    @Override
    public void onVCardFailed(String account, String bareAddress) {
        if (!this.account.equals(account) || !this.user.equals(bareAddress)) {
            return;
        }
        this.vCard = null;
        this.vCardError = true;
        Application.getInstance().onError(R.string.XMPP_EXCEPTION);
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {
        for (BaseEntity entity : entities) {
            if (entity.equals(account, user)) {
                updateContact(account, user);
                break;
            }
        }
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        if (accounts.contains(account)) {
            updateContact(account, user);
        }
    }

    /**
     * @param source
     * @param value
     * @param splitter
     * @return Concatenated source and value with splitter if necessary.
     */
    private String addString(String source, String value, String splitter) {
        if (value == null || "".equals(value)) {
            return source;
        }
        if (source == null || "".equals(source)) {
            return value;
        }
        return source + splitter + value;
    }

    public void updateContact(String account, String bareAddress) {
        this.account = account;
        this.user = bareAddress;

        xmppItems.removeAllViews();

        View jabberIdView = createItemView(xmppItems, getString(R.string.contact_viewer_jid),
                bareAddress, R.drawable.ic_vcard_xmpp_24dp);

        if (jabberIdView != null) {
            xmppItems.addView(jabberIdView);
        }

        List<View> resourcesList = new ArrayList<>();

        for (ResourceItem resourceItem
                : PresenceManager.getInstance().getResourceItems(account, bareAddress)) {

            String user = resourceItem.getUser(bareAddress);
            ClientInfo clientInfo = CapabilitiesManager.getInstance().getClientInfo(account, user);

            String client = "";
            if (clientInfo == null) {
                CapabilitiesManager.getInstance().request(account, user);
                client = getString(R.string.please_wait);
            } else if (clientInfo == CapabilitiesManager.INVALID_CLIENT_INFO) {
                client = getString(R.string.unknown);
            } else {
                String name = clientInfo.getName();
                if (name != null) {
                    client = name;
                }

                String type = clientInfo.getType();
                if (type != null) {
                    if (client.isEmpty()) {
                        client = type;
                    } else {
                        client = client + "/" + type;
                    }
                }
            }

            String priority = getString(R.string.account_priority) + ": " + resourceItem.getPriority();

            String label = "";
            if (!client.isEmpty()) {
                label = getString(R.string.contact_viewer_client) + ": " + client + ", ";
            }

            label += priority;

            String resource = getString(R.string.account_resource) + ": " + resourceItem.getVerbose();

            String status = resourceItem.getStatusText().trim();
            if (status.isEmpty()) {
                status = getString(resourceItem.getStatusMode().getStringID());
            }

            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

            View resourceView = inflater.inflate(R.layout.contact_info_item, xmppItems, false);

            ((TextView)resourceView.findViewById(R.id.contact_info_item_secondary)).setText(label);
            ((TextView)resourceView.findViewById(R.id.contact_info_item_main)).setText(status);

            ((TextView)resourceView.findViewById(R.id.contact_info_item_secondary_second_line)).setText(resource);
            resourceView.findViewById(R.id.contact_info_item_secondary_second_line).setVisibility(View.VISIBLE);

            ImageView statusIcon = (ImageView) resourceView.findViewById(R.id.contact_info_right_icon);
            statusIcon.setVisibility(View.VISIBLE);

            statusIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_status));
            statusIcon.setImageLevel(resourceItem.getStatusMode().getStatusLevel());

            resourcesList.add(resourceView);
        }

        addItemGroup(resourcesList, xmppItems, R.drawable.ic_vcard_jabber_24dp);
    }

    public void updateVCard(VCard vCard) {
        if (vCard == null) {
            return;
        }

        contactInfoItems.removeAllViews();

        addNameInfo(vCard);

        List<View> birthDayList = new ArrayList<>();
        addItem(birthDayList, contactInfoItems, getString(R.string.vcard_birth_date), vCard.getField(VCardProperty.BDAY));
        addItemGroup(birthDayList, contactInfoItems, R.drawable.ic_vcard_birthday_24dp);

        addOrganizationInfo(vCard);

        List<View> webList = new ArrayList<>();
        addItem(webList, contactInfoItems, getString(R.string.vcard_url), vCard.getField(VCardProperty.URL));
        addItemGroup(webList, contactInfoItems, R.drawable.ic_vcard_web_24dp);

        addAdditionalInfo(vCard);
        addAddresses(vCard);
        addPhones(vCard);
        addEmails(vCard);
    }

    private void addEmails(VCard vCard) {
        List<View> emailList = new ArrayList<>();
        for (Email email : vCard.getEmails()) {
            String types = null;
            for (EmailType type : email.getTypes()) {
                types = addString(types, getString(VcardMaps.getEmailTypeMap().get(type)), ", ");
            }

            addItem(emailList, contactInfoItems, types, email.getValue());
        }

        addItemGroup(emailList, contactInfoItems, R.drawable.ic_vcard_email_24dp);
    }

    private void addPhones(VCard vCard) {
        List<View> phoneList = new ArrayList<>();
        for (Telephone telephone : vCard.getTelephones()) {
            String types = null;
            for (TelephoneType type : telephone.getTypes()) {
                types = addString(types, getString(VcardMaps.getTelephoneTypeMap().get(type)), ", ");
            }

            addItem(phoneList, contactInfoItems, types, telephone.getValue());
        }

        addItemGroup(phoneList, contactInfoItems, R.drawable.ic_vcard_phone_24dp);
    }

    private void addAddresses(VCard vCard) {
        List<View> addressList = new ArrayList<>();

        for (Address address : vCard.getAddresses()) {
            String types = null;
            for (AddressType type : address.getTypes()) {
                types = addString(types, getString(VcardMaps.getAddressTypeMap().get(type)), ", ");
            }

            String value = null;
            for (AddressProperty property : AddressProperty.values()) {
                value = addString(value, address.getProperties().get(property), "\n");
            }

            addItem(addressList, contactInfoItems, types, value);
        }

        addItemGroup(addressList, contactInfoItems, R.drawable.ic_vcard_address_24dp);
    }

    private void addAdditionalInfo(VCard vCard) {
        String categories = null;
        for (String category : vCard.getCategories()) {
            categories = addString(categories, category, "\n");
        }

        List<View> notesList = new ArrayList<>();
        addItem(notesList, contactInfoItems, getString(R.string.vcard_categories), categories);
        addItem(notesList, contactInfoItems, getString(R.string.vcard_note), vCard.getField(VCardProperty.NOTE));
        addItem(notesList, contactInfoItems, getString(R.string.vcard_decsription), vCard.getField(VCardProperty.DESC));
        addItemGroup(notesList, contactInfoItems, R.drawable.ic_vcard_notes_24dp);
    }

    private void addOrganizationInfo(VCard vCard) {
        List<View> organizationList = new ArrayList<>();

        addItem(organizationList, contactInfoItems, getString(R.string.vcard_title), vCard.getField(VCardProperty.TITLE));
        addItem(organizationList, contactInfoItems, getString(R.string.vcard_role), vCard.getField(VCardProperty.ROLE));

        List<Organization> organizations = vCard.getOrganizations();
        String organization;
        if (organizations.isEmpty()) {
            organization = null;
        }  else {
            organization = organizations.get(0).getName();
            for (String unit : organizations.get(0).getUnits()) {
                organization = addString(organization, unit, "\n");
            }
        }
        addItem(organizationList, contactInfoItems, getString(R.string.vcard_organization), organization);

        addItemGroup(organizationList, contactInfoItems, R.drawable.ic_vcard_job_title_24dp);
    }

    private void addNameInfo(VCard vCard) {
        List<View> nameList = new ArrayList<>();

        addItem(nameList, contactInfoItems, getString(R.string.vcard_nick_name), vCard.getField(VCardProperty.NICKNAME));
        addItem(nameList, contactInfoItems, getString(R.string.vcard_formatted_name), vCard.getFormattedName());
        addItem(nameList, contactInfoItems, getString(R.string.vcard_prefix_name), vCard.getField(NameProperty.PREFIX));
        addItem(nameList, contactInfoItems, getString(R.string.vcard_given_name), vCard.getField(NameProperty.GIVEN));
        addItem(nameList, contactInfoItems, getString(R.string.vcard_middle_name), vCard.getField(NameProperty.MIDDLE));
        addItem(nameList, contactInfoItems, getString(R.string.vcard_family_name), vCard.getField(NameProperty.FAMILY));
        addItem(nameList, contactInfoItems, getString(R.string.vcard_suffix_name), vCard.getField(NameProperty.SUFFIX));

        addItemGroup(nameList, contactInfoItems, R.drawable.ic_vcard_contact_info_24dp);
    }

    private void addItemGroup(List<View> nameList, LinearLayout itemList, int groupIcon) {
        if (nameList.isEmpty()) {
            return;
        }

        addSeparator(itemList);
        ((ImageView) nameList.get(0).findViewById(R.id.contact_info_group_icon)).setImageResource(groupIcon);

        for (View view : nameList) {
            itemList.addView(view);
        }
    }

    private void addItem(List<View> nameList, ViewGroup rootView, String label, String value) {
        View itemView = createItemView(rootView, label, value, null);
        if (itemView != null) {
            Linkify.addLinks((TextView)itemView.findViewById(R.id.contact_info_item_main), Linkify.ALL);
            nameList.add(itemView);
        }
    }

    private void addSeparator(LinearLayout rootView) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        rootView.addView(inflater.inflate(R.layout.contact_info_separator, rootView, false));
    }

    private View createItemView(ViewGroup rootView, String label, String value, Integer iconResource) {
        if (value == null || value.isEmpty() ) {
            return null;
        }

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        View contactInfoItem = inflater.inflate(R.layout.contact_info_item, rootView, false);

        if (label == null || label.trim().isEmpty()) {
            contactInfoItem.findViewById(R.id.contact_info_item_secondary).setVisibility(View.GONE);
        } else {
            ((TextView) contactInfoItem.findViewById(R.id.contact_info_item_secondary)).setText(label);
        }
        ((TextView)contactInfoItem.findViewById(R.id.contact_info_item_main)).setText(value);

        if (iconResource != null) {
            ((ImageView) contactInfoItem.findViewById(R.id.contact_info_group_icon)).setImageResource(iconResource);
        }
        return contactInfoItem;
    }

}
