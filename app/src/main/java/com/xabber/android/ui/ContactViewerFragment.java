package com.xabber.android.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.capability.ClientInfo;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.ResourceItem;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.androiddev.R;
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

import java.util.List;

public class ContactViewerFragment extends Fragment {
    private LinearLayout xmppItems;
    private LinearLayout contactInfoItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.contact_viewer_fragment, container, false);

        xmppItems = (LinearLayout) view.findViewById(R.id.xmpp_items);
        contactInfoItems = (LinearLayout) view.findViewById(R.id.contact_info_items);

        return view;
    }

    /**
     * @param source
     * @param value
     * @param splitter
     * @return Concatenated source and value with splitter if necessary.
     */
    private String addString(String source, String value, String splitter) {
        if (value == null || "".equals(value))
            return source;
        if (source == null || "".equals(source))
            return value;
        return source + splitter + value;
    }

    public void updateContact(String account, String bareAddress) {
        xmppItems.removeAllViews();

        addXmppItem(getString(R.string.contact_viewer_jid), bareAddress, null);
        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, bareAddress);
        addXmppItem(getString(R.string.contact_viewer_name), rosterContact == null ? null : rosterContact.getRealName(), null);

        if (rosterContact != null && rosterContact.isConnected())
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

                String label = String.valueOf(resourceItem.getPriority());
                if (!client.isEmpty()) {
                    label = label + ", " + client;
                }

                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

                View contactInfoItem = inflater.inflate(R.layout.contact_info_item, xmppItems, false);

                ((TextView)contactInfoItem.findViewById(R.id.contact_info_item_name)).setText(label);
                ((TextView)contactInfoItem.findViewById(R.id.contact_info_item_value)).setText(resourceItem.getVerbose());

                ((ImageView) contactInfoItem.findViewById(R.id.contact_info_group_icon)).setImageResource(R.drawable.ic_xmpp_24dp);

                ImageView statusIcon = (ImageView) contactInfoItem.findViewById(R.id.contact_info_right_icon);
                statusIcon.setVisibility(View.VISIBLE);

                statusIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_status));
                statusIcon.setImageLevel(resourceItem.getStatusMode().getStatusLevel());

                xmppItems.addView(contactInfoItem);
            }
    }

    public void updateVCard(VCard vCard) {
        if (vCard == null) {
            return;
        }

        contactInfoItems.removeAllViews();

        addContactInfoItem(R.string.vcard_nick_name, vCard.getField(VCardProperty.NICKNAME));
        addContactInfoItem(getString(R.string.vcard_formatted_name), vCard.getFormattedName(), R.drawable.ic_contact_info_24dp);
        addContactInfoItem(R.string.vcard_prefix_name, vCard.getField(NameProperty.PREFIX));
        addContactInfoItem(R.string.vcard_given_name, vCard.getField(NameProperty.GIVEN));
        addContactInfoItem(R.string.vcard_middle_name, vCard.getField(NameProperty.MIDDLE));
        addContactInfoItem(R.string.vcard_family_name, vCard.getField(NameProperty.FAMILY));
        addContactInfoItem(R.string.vcard_suffix_name, vCard.getField(NameProperty.SUFFIX));
        addContactInfoItem(getString(R.string.vcard_birth_date), vCard.getField(VCardProperty.BDAY), R.drawable.ic_birthday_24dp);
        addContactInfoItem(getString(R.string.vcard_title), vCard.getField(VCardProperty.TITLE), R.drawable.ic_job_title_24dp);
        addContactInfoItem(R.string.vcard_role, vCard.getField(VCardProperty.ROLE));

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
        addContactInfoItem(getString(R.string.vcard_organization), organization, R.drawable.ic_organization_24dp);
        addContactInfoItem(getString(R.string.vcard_url), vCard.getField(VCardProperty.URL), R.drawable.ic_web_24dp);

        String categories = null;
        for (String category : vCard.getCategories()) {
            categories = addString(categories, category, "\n");
        }

        addContactInfoItem(R.string.vcard_categories, categories);
        addContactInfoItem(R.string.vcard_note, vCard.getField(VCardProperty.NOTE));
        addContactInfoItem(R.string.vcard_decsription, vCard.getField(VCardProperty.DESC));

        for (Address address : vCard.getAddresses()) {
            String types = null;
            for (AddressType type : address.getTypes()) {
                types = addString(types, getString(ContactViewer.getAddressTypeMap().get(type)), ", ");
            }

            String value = null;
            for (AddressProperty property : AddressProperty.values()) {
                value = addString(value, address.getProperties().get(property), "\n");
            }

            addContactInfoItem(types, value, R.drawable.ic_address_24dp);

            for (AddressProperty property : AddressProperty.values()) {
                ContactViewer.getAddressPropertyMap().get(property);
                address.getProperties().get(property);
            }
        }

        for (Telephone telephone : vCard.getTelephones()) {
            String types = null;
            for (TelephoneType type : telephone.getTypes()) {
                types = addString(types, getString(ContactViewer.getTelephoneTypeMap().get(type)), ", ");
            }

            addContactInfoItem(types, telephone.getValue(), R.drawable.ic_phone_24dp);
        }

        for (Email email : vCard.getEmails()) {
            String types = null;
            for (EmailType type : email.getTypes()) {
                types = addString(types, getString(ContactViewer.getEmailTypeMap().get(type)), ", ");
            }

            addContactInfoItem(types, email.getValue(), R.drawable.ic_email_24dp);
        }
    }

    private void addContactInfoItem(int labelId, String value) {
        addContactInfoItem(getString(labelId), value, null);
    }

    private void addContactInfoItem(String label, String value, Integer iconResource) {
        View contactInfoItem = createItemView(contactInfoItems, label, value, iconResource);
        if (contactInfoItem != null) {
            contactInfoItems.addView(contactInfoItem);
        }
    }

    private void addXmppItem(String label, String value, Integer iconResource) {
        View contactInfoItem = createItemView(xmppItems, label, value, iconResource);
        if (contactInfoItem != null) {
            xmppItems.addView(contactInfoItem);
        }
    }


    private View createItemView(ViewGroup rootView, String label, String value, Integer iconResource) {
        if (value == null || value.isEmpty() ) {
            return null;
        }

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        View contactInfoItem = inflater.inflate(R.layout.contact_info_item, rootView, false);

        ((TextView)contactInfoItem.findViewById(R.id.contact_info_item_name)).setText(label);
        ((TextView)contactInfoItem.findViewById(R.id.contact_info_item_value)).setText(value);

        if (iconResource != null) {
            ((ImageView) contactInfoItem.findViewById(R.id.contact_info_group_icon)).setImageResource(iconResource);
        }
        return contactInfoItem;
    }

}
