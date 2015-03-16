package com.xabber.android.ui.preferences;

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

import com.xabber.android.data.LogManager;
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
        addContactInfoItem(R.string.contact_viewer_jid, bareAddress);
        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, bareAddress);
        addContactInfoItem(R.string.contact_viewer_name, rosterContact == null ? null : rosterContact.getRealName());

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

                resourceItem.getStatusMode().getStatusLevel();

                String label = String.valueOf(resourceItem.getPriority());
                if (!client.isEmpty()) {
                    label = label + ", " + client;
                }

                addContactInfoItem(label, resourceItem.getVerbose(), null);
            }
    }

    public void updateVCard(VCard vCard) {
        LogManager.i(this, "updateVCard.");

        if (vCard == null) {
            return;
        }
        addContactInfoItem(R.string.vcard_nick_name, vCard.getField(VCardProperty.NICKNAME));
        addContactInfoItem(R.string.vcard_formatted_name, vCard.getFormattedName());
        addContactInfoItem(R.string.vcard_prefix_name, vCard.getField(NameProperty.PREFIX));
        addContactInfoItem(R.string.vcard_given_name, vCard.getField(NameProperty.GIVEN));
        addContactInfoItem(R.string.vcard_middle_name, vCard.getField(NameProperty.MIDDLE));
        addContactInfoItem(R.string.vcard_family_name, vCard.getField(NameProperty.FAMILY));
        addContactInfoItem(R.string.vcard_suffix_name, vCard.getField(NameProperty.SUFFIX));
        addContactInfoItem(R.string.vcard_birth_date, vCard.getField(VCardProperty.BDAY));
        addContactInfoItem(R.string.vcard_title, vCard.getField(VCardProperty.TITLE));
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

        addContactInfoItem(R.string.vcard_url, vCard.getField(VCardProperty.URL));

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

            addContactInfoItem(types, value, null);

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

            addContactInfoItem(types, telephone.getValue(), R.drawable.ic_telephone_24dp);
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
        LogManager.i(this, label + ": " + value);

        if (value == null || value.isEmpty() ) {
            return;
        }

        final LayoutInflater inflater
                = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View contactInfoItem = inflater.inflate(R.layout.contact_info_item, contactInfoItems, false);

        ((TextView)contactInfoItem.findViewById(R.id.contact_info_item_name)).setText(label);
        ((TextView)contactInfoItem.findViewById(R.id.contact_info_item_value)).setText(value);

        if (iconResource != null) {
            ((ImageView) contactInfoItem.findViewById(R.id.contact_info_group_icon)).setImageResource(iconResource);
        }

        contactInfoItems.addView(contactInfoItem);
    }

}
