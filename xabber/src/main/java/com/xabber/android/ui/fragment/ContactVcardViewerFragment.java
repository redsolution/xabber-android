package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
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
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.capability.ClientInfo;
import com.xabber.android.data.extension.vcard.OnVCardListener;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.vcard.AddressProperty;
import com.xabber.xmpp.vcard.AddressType;
import com.xabber.xmpp.vcard.EmailType;
import com.xabber.xmpp.vcard.TelephoneType;
import com.xabber.xmpp.vcard.VCardProperty;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContactVcardViewerFragment extends Fragment implements OnContactChangedListener, OnAccountChangedListener, OnVCardListener {
    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.fragment.ContactVcardViewerFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.xabber.android.ui.fragment.ContactVcardViewerFragment.ARGUMENT_USER";
    private static final String SAVED_VCARD = "com.xabber.android.ui.fragment.ContactVcardViewerFragment.SAVED_VCARD";
    private static final String SAVED_VCARD_ERROR = "com.xabber.android.ui.fragment.ContactVcardViewerFragment.SAVED_VCARD_ERROR";
    String account;
    String user;
    private LinearLayout xmppItems;
    private LinearLayout contactInfoItems;
    private VCard vCard;
    private boolean vCardError;
    private View progressBar;
    private Listener listener;

    public interface Listener {
        void onVCardReceived();
    }

    public static ContactVcardViewerFragment newInstance(String account, String user) {
        ContactVcardViewerFragment fragment = new ContactVcardViewerFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_ACCOUNT, account);
        arguments.putString(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        listener = (Listener) activity;
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
            if (xml != null) {
                try {
                    vCard = parseVCard(xml);
                } catch (XmlPullParserException | IOException | SmackException e) {
                    LogManager.exception(this, e);
                }
            }
        }
    }

    public static VCard parseVCard(String xml) throws XmlPullParserException, IOException, SmackException {
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(xml));
        int eventType = parser.next();
        if (eventType != XmlPullParser.START_TAG) {
            throw new IllegalStateException(String.valueOf(eventType));
        }
        if (!VCard.ELEMENT.equals(parser.getName())) {
            throw new IllegalStateException(parser.getName());
        }
        if (!VCard.NAMESPACE.equals(parser.getNamespace())) {
            throw new IllegalStateException(parser.getNamespace());
        }
        return (new VCardProvider()).parse(parser);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.contact_vcard_viewer_fragment, container, false);

        xmppItems = (LinearLayout) view.findViewById(R.id.xmpp_items);
        contactInfoItems = (LinearLayout) view.findViewById(R.id.contact_info_items);
        progressBar = view.findViewById(R.id.contact_info_progress_bar);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnVCardListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);

        updateContact(account, user);

        if (vCard == null && !vCardError) {
            requestVCard();
        } else {
            updateVCard();
        }
    }

    public void requestVCard() {
        progressBar.setVisibility(View.VISIBLE);
        VCardManager.getInstance().request(account, user);
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
            outState.putString(SAVED_VCARD, vCard.getChildElementXML().toString());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        listener = null;
    }

    @Override
    public void onVCardReceived(String account, String bareAddress, VCard vCard) {
        if (!this.account.equals(account) || !this.user.equals(bareAddress)) {
            return;
        }
        this.vCard = vCard;
        this.vCardError = false;
        updateVCard();
        listener.onVCardReceived();
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onVCardFailed(String account, String bareAddress) {
        if (!this.account.equals(account) || !this.user.equals(bareAddress)) {
            return;
        }
        this.vCard = null;
        this.vCardError = true;
        progressBar.setVisibility(View.GONE);
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
            if (Jid.getBareAddress(account).equals(Jid.getBareAddress(user))) {
                if (AccountManager.getInstance().getAccount(account).getFactualStatusMode().isOnline()) {
                    VCardManager.getInstance().request(account, Jid.getBareAddress(account));
                }
            }
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

        View jabberIdView = createItemView(xmppItems, getString(R.string.jabber_id),
                bareAddress, R.drawable.ic_vcard_xmpp_24dp);

        if (jabberIdView != null) {
            xmppItems.addView(jabberIdView);
        }

        List<View> resourcesList = new ArrayList<>();

        fillResourceList(account, bareAddress, resourcesList);

        addItemGroup(resourcesList, xmppItems, R.drawable.ic_vcard_jabber_24dp);
    }

    private void fillResourceList(String account, String bareAddress, List<View> resourcesList) {
        final List<Presence> allPresences = RosterManager.getInstance().getPresences(account, bareAddress);

        for (Presence presence : allPresences) {
            String user = presence.getFrom();
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
            int priorityValue = presence.getPriority();
            String priorityString;
            if (priorityValue == Integer.MIN_VALUE) {
                priorityString = getString(R.string.account_priority) + ": " + getString(R.string.unknown);
            } else {
                priorityString = getString(R.string.account_priority) + ": " + priorityValue;
            }

            String label = "";
            if (!client.isEmpty()) {
                label = getString(R.string.contact_viewer_client) + ": " + client + ", ";
            }

            label += priorityString;

            String resource = getString(R.string.account_resource) + ": " + Jid.getResource(user);

            final StatusMode statusMode = StatusMode.createStatusMode(presence);

            String status = presence.getStatus();
            if (TextUtils.isEmpty(status)) {
                status = getString(statusMode.getStringID());
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
            statusIcon.setImageLevel(statusMode.getStatusLevel());

            resourcesList.add(resourceView);
        }
    }

    public void updateVCard() {
        if (vCard == null) {
            return;
        }

        contactInfoItems.removeAllViews();

        addNameInfo(vCard);

        List<View> birthDayList = new ArrayList<>();
        addItem(birthDayList, contactInfoItems, getString(R.string.vcard_birth_date), vCard.getField(VCardProperty.BDAY.toString()));
        addItemGroup(birthDayList, contactInfoItems, R.drawable.ic_vcard_birthday_24dp);

        addOrganizationInfo(vCard);

        List<View> webList = new ArrayList<>();
        addItem(webList, contactInfoItems, getString(R.string.vcard_url), vCard.getField(VCardProperty.URL.toString()));
        addItemGroup(webList, contactInfoItems, R.drawable.ic_vcard_web_24dp);

        addAdditionalInfo(vCard);
        addAddresses(vCard);
        addPhones(vCard);
        addEmails(vCard);
    }

    private void addEmails(VCard vCard) {
        List<View> emailList = new ArrayList<>();

        String emailHome = vCard.getEmailHome();
        if (!"".equals(emailHome)) {
            addItem(emailList, contactInfoItems, getString(VcardMaps.getEmailTypeMap().get(EmailType.HOME)), emailHome);
        }

        String emailWork = vCard.getEmailWork();
        if (!"".equals(emailWork)) {
            addItem(emailList, contactInfoItems, getString(VcardMaps.getEmailTypeMap().get(EmailType.WORK)), emailWork);
        }

        addItemGroup(emailList, contactInfoItems, R.drawable.ic_vcard_email_24dp);
    }

    private void addPhones(VCard vCard) {
        List<View> phoneList = new ArrayList<>();

        for (TelephoneType type : TelephoneType.values()) {
            String types = getString(VcardMaps.getTelephoneTypeMap().get(TelephoneType.HOME));

            String phoneHome = vCard.getPhoneHome(type.name());

            if (!"".equals(phoneHome)) {
                types = addString(types, getString(VcardMaps.getTelephoneTypeMap().get(type)), ", ");
                addItem(phoneList, contactInfoItems, types, phoneHome);
            }
        }

        for (TelephoneType type : TelephoneType.values()) {
            String types = getString(VcardMaps.getTelephoneTypeMap().get(TelephoneType.WORK));

            String phoneHome = vCard.getPhoneWork(type.name());

            if (!"".equals(phoneHome)) {
                types = addString(types, getString(VcardMaps.getTelephoneTypeMap().get(type)), ", ");
                addItem(phoneList, contactInfoItems, types, phoneHome);
            }
        }

        addItemGroup(phoneList, contactInfoItems, R.drawable.ic_vcard_phone_24dp);
    }

    private void addAddresses(VCard vCard) {
        List<View> addressList = new ArrayList<>();

        String homeAddress = null;
        for (AddressProperty property : AddressProperty.values()) {
            homeAddress = addString(homeAddress, vCard.getAddressFieldHome(property.name()), "\n");
        }

        addItem(addressList, contactInfoItems,  getString(VcardMaps.getAddressTypeMap().get(AddressType.HOME)), homeAddress);

        String workAddress = null;
        for (AddressProperty property : AddressProperty.values()) {
            workAddress = addString(workAddress, vCard.getAddressFieldWork(property.name()), "\n");
        }

        addItem(addressList, contactInfoItems, getString(VcardMaps.getAddressTypeMap().get(AddressType.WORK)), workAddress);

        addItemGroup(addressList, contactInfoItems, R.drawable.ic_vcard_address_24dp);
    }

    private void addAdditionalInfo(VCard vCard) {
        List<View> notesList = new ArrayList<>();
        addItem(notesList, contactInfoItems, getString(R.string.vcard_note), vCard.getField(VCardProperty.NOTE.name()));
        addItem(notesList, contactInfoItems, getString(R.string.vcard_decsription), vCard.getField(VCardProperty.DESC.name()));
        addItemGroup(notesList, contactInfoItems, R.drawable.ic_vcard_notes_24dp);
    }

    private void addOrganizationInfo(VCard vCard) {
        List<View> organizationList = new ArrayList<>();

        addItem(organizationList, contactInfoItems, getString(R.string.vcard_title), vCard.getField(VCardProperty.TITLE.toString()));
        addItem(organizationList, contactInfoItems, getString(R.string.vcard_role), vCard.getField(VCardProperty.ROLE.toString()));

        String organization = vCard.getOrganization();
        String unit = vCard.getOrganizationUnit();

        addItem(organizationList, contactInfoItems, getString(R.string.vcard_organization), addString(organization, unit, "\n"));

        addItemGroup(organizationList, contactInfoItems, R.drawable.ic_vcard_job_title_24dp);
    }

    private void addNameInfo(VCard vCard) {
        List<View> nameList = new ArrayList<>();

        addItem(nameList, contactInfoItems, getString(R.string.vcard_nick_name), vCard.getField(VCardProperty.NICKNAME.name()));
        addItem(nameList, contactInfoItems, getString(R.string.vcard_formatted_name), vCard.getField(VCardProperty.FN.name()));
        addItem(nameList, contactInfoItems, getString(R.string.vcard_prefix_name), vCard.getPrefix());

        addItem(nameList, contactInfoItems, getString(R.string.vcard_given_name), vCard.getFirstName());
        addItem(nameList, contactInfoItems, getString(R.string.vcard_middle_name), vCard.getMiddleName());
        addItem(nameList, contactInfoItems, getString(R.string.vcard_family_name), vCard.getLastName());
        addItem(nameList, contactInfoItems, getString(R.string.vcard_suffix_name), vCard.getSuffix());

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

    public VCard getvCard() {
        return vCard;
    }

}
