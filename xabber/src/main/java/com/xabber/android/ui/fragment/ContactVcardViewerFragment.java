package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.VcardMaps;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.capability.ClientInfo;
import com.xabber.android.data.extension.vcard.OnVCardListener;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.AccountInfoEditorActivity;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.xmpp.vcard.AddressProperty;
import com.xabber.xmpp.vcard.AddressType;
import com.xabber.xmpp.vcard.EmailType;
import com.xabber.xmpp.vcard.TelephoneType;
import com.xabber.xmpp.vcard.VCardProperty;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContactVcardViewerFragment extends Fragment implements OnContactChangedListener, OnAccountChangedListener, OnVCardListener {
    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.fragment.ContactVcardViewerFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.xabber.android.ui.fragment.ContactVcardViewerFragment.ARGUMENT_USER";
    private static final String SAVED_VCARD = "com.xabber.android.ui.fragment.ContactVcardViewerFragment.SAVED_VCARD";
    private static final String SAVED_VCARD_ERROR = "com.xabber.android.ui.fragment.ContactVcardViewerFragment.SAVED_VCARD_ERROR";
    private static final String LOG_TAG = ContactVcardViewerFragment.class.getSimpleName();
    public static final int REQUEST_CODE_EDIT_VCARD = 1;

    AccountJid account;
    UserJid user;
    private LinearLayout xmppItems;
    private LinearLayout contactInfoItems;
    private VCard vCard;
    private boolean vCardError;
    private View progressBar;
    private Listener listener;
    private Button editButton;

    public interface Listener {
        void onVCardReceived();
        void registerVCardFragment(ContactVcardViewerFragment fragment);
    }

    public static ContactVcardViewerFragment newInstance(AccountJid account, UserJid user) {
        ContactVcardViewerFragment fragment = new ContactVcardViewerFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putParcelable(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    public static ContactVcardViewerFragment newInstance(AccountJid account) {
        try {
            return newInstance(account, UserJid.from(account.getFullJid().asBareJid()));
        } catch (UserJid.UserJidCreateException e) {
            throw new IllegalStateException("Cannot convert account to user. Account: " + account, e);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Listener) {
            listener = (Listener) activity;
            listener.registerVCardFragment(this);
        }
        else throw new RuntimeException(activity.toString()
                + " must implement ContactVcardViewerFragment.Listener");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        account = args.getParcelable(ARGUMENT_ACCOUNT);
        user = args.getParcelable(ARGUMENT_USER);

        vCard = null;
        vCardError = false;
        if (savedInstanceState != null) {
            vCardError = savedInstanceState.getBoolean(SAVED_VCARD_ERROR, false);
            String xml = savedInstanceState.getString(SAVED_VCARD);
            if (xml != null) {
                try {
                    vCard = parseVCard(xml);
                } catch (Exception e) {
                    LogManager.exception(this, e);
                }
            }
        }
    }

    public static VCard parseVCard(String xml) throws Exception {
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

        View view = inflater.inflate(R.layout.fragment_contact_vcard, container, false);

        xmppItems = (LinearLayout) view.findViewById(R.id.xmpp_items);
        contactInfoItems = (LinearLayout) view.findViewById(R.id.contact_info_items);
        progressBar = view.findViewById(R.id.contact_info_progress_bar);

        editButton = (Button) view.findViewById(R.id.contact_info_edit_button);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (vCard != null) {
                    Intent intent = AccountInfoEditorActivity.createIntent(getActivity(), account,
                            vCard.getChildElementXML().toString());

                    startActivityForResult(intent, REQUEST_CODE_EDIT_VCARD);
                }
            }
        });


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (AccountManager.getInstance().getAccount(account) == null) {
            // in case if account was removed
            return;
        }

        Application.getInstance().addUIListener(OnVCardListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);

        if (vCard == null && !vCardError) {
            requestVCard();
        } else {
            updateVCard();
        }

        updateContact(account, user);
    }

    public void requestVCard() {
        progressBar.setVisibility(View.VISIBLE);
        VCardManager.getInstance().requestByUser(account, user.getJid());
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
    public void onVCardReceived(AccountJid account, Jid bareAddress, VCard vCard) {
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
    public void onVCardFailed(AccountJid account, Jid bareAddress) {
        if (!this.account.equals(account) || !this.user.equals(bareAddress)) {
            return;
        }
        this.vCard = null;
        this.vCardError = true;
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        for (BaseEntity entity : entities) {
            if (entity.equals(account, user)) {
                updateContact(account, user);
                break;
            }
        }
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        if (accounts.contains(account)) {
            updateContact(account, user);
            if (account.getFullJid().asBareJid().equals(user.getJid().asBareJid())) {
                AccountItem accountItem = AccountManager.getInstance().getAccount(this.account);
                if (accountItem != null && accountItem.getFactualStatusMode().isOnline()) {
                    VCardManager.getInstance().request(this.account, this.account.getFullJid().asBareJid());
                }
            }
        }
    }

    /**
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

    public void updateContact(AccountJid account, UserJid bareAddress) {
        this.account = account;
        this.user = bareAddress;

        if (!isAdded()) {
            return;
        }

        xmppItems.removeAllViews();

        List<View> resourcesList = new ArrayList<>();

        fillResourceList(account, bareAddress.getJid(), resourcesList);

        if (!resourcesList.isEmpty()) {
            addHeader(xmppItems, getString(R.string.contact_info_connected_clients_header));
        }

        addItemGroup(resourcesList, xmppItems, R.drawable.ic_vcard_jabber_24dp, false);

        addHeader(xmppItems, getString(R.string.contact_info_visiting_card_header));

        View jabberIdView = createItemView(xmppItems, getString(R.string.jabber_id),
                bareAddress.toString(), R.drawable.ic_vcard_xmpp_24dp);

        if (jabberIdView != null) {
            xmppItems.addView(jabberIdView);
        }
    }

    private void addHeader(LinearLayout rootView, String text) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View contactInfoHeader = inflater.inflate(R.layout.item_contact_info_header, rootView, false);
        TextView headerView = (TextView) contactInfoHeader.findViewById(R.id.contact_info_header_text_view);
        headerView.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountSendButtonColor(account));
        headerView.setText(text);

        rootView.addView(contactInfoHeader);
    }

    private void fillResourceList(AccountJid account, Jid bareAddress, List<View> resourcesList) {
        final List<Presence> allPresences = RosterManager.getInstance().getPresences(account, bareAddress);

        boolean isAccount = account.getFullJid().asBareJid().equals(user.getBareJid());
        Resourcepart accountResource = null;
        if (isAccount) {
            // TODO: probably not the best way to get own resource
            AccountItem accountItem = AccountManager.getInstance().getAccount(account);
            if (accountItem != null) {
                accountResource = accountItem.getConnection().getConfiguration().getResource();
            }
        }

        PresenceManager.sortPresencesByPriority(allPresences);

        for (Presence presence : allPresences) {
            Jid fromJid = presence.getFrom();

            ClientInfo clientInfo = CapabilitiesManager.getInstance().getCachedClientInfo(fromJid);

            String client = "";
            if (clientInfo == null) {
                client = getString(R.string.please_wait);
                CapabilitiesManager.getInstance().requestClientInfoByUser(account, fromJid);
            } else if (clientInfo == ClientInfo.INVALID_CLIENT_INFO) {
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

            if (!client.isEmpty()) {
                client = getString(R.string.contact_viewer_client) + ": " + client;
            }

            Resourcepart resourceOrNull = fromJid.getResourceOrNull();
            String resource = getString(R.string.account_resource) + ": " + resourceOrNull;

            final StatusMode statusMode = StatusMode.createStatusMode(presence);

            String status = presence.getStatus();
            if (TextUtils.isEmpty(status)) {
                status = getString(statusMode.getStringID());
            }

            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

            View resourceView = inflater.inflate(R.layout.item_contact_info, xmppItems, false);

            resourceView.findViewById(R.id.contact_info_item_secondary);

            ((TextView)resourceView.findViewById(R.id.contact_info_item_secondary)).setText(client);
            ((TextView)resourceView.findViewById(R.id.contact_info_item_main)).setText(status);

            ((TextView)resourceView.findViewById(R.id.contact_info_item_secondary_second_line)).setText(resource);
            resourceView.findViewById(R.id.contact_info_item_secondary_second_line).setVisibility(View.VISIBLE);

            ((TextView)resourceView.findViewById(R.id.contact_info_item_secondary_third_line)).setText(priorityString);
            resourceView.findViewById(R.id.contact_info_item_secondary_third_line).setVisibility(View.VISIBLE);

            if (isAccount &&resourceOrNull != null
                    && resourceOrNull.equals(accountResource)) {
                TextView thisDeviceIndicatorTextView
                        = (TextView) resourceView.findViewById(R.id.contact_info_item_secondary_forth_line);

                thisDeviceIndicatorTextView.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountSendButtonColor(account));
                thisDeviceIndicatorTextView.setText(R.string.contact_viewer_this_device);
                thisDeviceIndicatorTextView.setVisibility(View.VISIBLE);
            }


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

        if (account.getFullJid().asBareJid().equals(user.getBareJid())) {
            editButton.setVisibility(View.VISIBLE);
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
        addItemGroup(nameList, itemList, groupIcon, true);
    }


    private void addItemGroup(List<View> nameList, LinearLayout itemList, int groupIcon, boolean addSeparator) {
        if (nameList.isEmpty()) {
            return;
        }

        if (addSeparator) {
            addSeparator(itemList);
        }

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

        View contactInfoItem = inflater.inflate(R.layout.item_contact_info, rootView, false);

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_VCARD
                && resultCode == Activity.RESULT_OK) {
            requestVCard();
        }

    }
}
