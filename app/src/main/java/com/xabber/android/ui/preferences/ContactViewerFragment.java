package com.xabber.android.ui.preferences;

import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.capability.ClientInfo;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.ResourceItem;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.PreferenceSummaryHelper;
import com.xabber.android.ui.widget.StatusPreference;
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

import java.util.ArrayList;
import java.util.List;

public class ContactViewerFragment extends android.preference.PreferenceFragment {
    private List<PreferenceCategory> addresses;
    private List<PreferenceCategory> telephones;
    private List<PreferenceCategory> emails;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addresses = new ArrayList<>();
        telephones = new ArrayList<>();
        emails = new ArrayList<>();

        addPreferencesFromResource(R.xml.contact_viewer);

        PreferenceSummaryHelper.updateSummary(getPreferenceScreen());
    }

    /**
     * Sets value for the preference by its id.
     *
     * @param resourceId
     * @param value
     */
    private void setValue(int resourceId, String value) {
        if (value == null)
            value = "";
        findPreference(getString(resourceId)).setSummary(value);
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
        setValue(R.string.contact_viewer_jid, bareAddress);
        RosterContact rosterContact = RosterManager.getInstance()
                .getRosterContact(account, bareAddress);
        setValue(R.string.contact_viewer_name, rosterContact == null ? null
                : rosterContact.getRealName());
        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference(getString(R.string.contact_viewer_resources));
        preferenceCategory.removeAll();
        if (rosterContact != null && rosterContact.isConnected())
            for (ResourceItem resourceItem : PresenceManager.getInstance()
                    .getResourceItems(account, bareAddress)) {
                StatusPreference preference = new StatusPreference(getActivity());
                preference.setLayoutResource(R.layout.info_preference);
                preference.setStatusMode(resourceItem.getStatusMode());
                String user = resourceItem.getUser(bareAddress);
                ClientInfo clientInfo = CapabilitiesManager.getInstance()
                        .getClientInfo(account, user);
                String client;
                if (clientInfo == null) {
                    CapabilitiesManager.getInstance().request(account, user);
                    client = getString(R.string.please_wait);
                } else if (clientInfo == CapabilitiesManager.INVALID_CLIENT_INFO) {
                    client = getString(R.string.unknown);
                } else {
                    String name = clientInfo.getName();
                    if (name == null)
                        name = getString(R.string.unknown);
                    String type = clientInfo.getType();
                    if (type == null)
                        type = getString(R.string.unknown);
                    client = getString(R.string.contact_viewer_client_info,
                            name, type);
                }
                preference.setTitle(getString(
                        R.string.contact_viewer_resource_summary,
                        resourceItem.getVerbose(), resourceItem.getPriority(),
                        client));
                preference.setSummary(resourceItem.getStatusText());
                preferenceCategory.addPreference(preference);
            }
    }

    public void updateVCard(VCard vCard) {

        if (vCard == null)
            return;
        setValue(R.string.vcard_nick_name,
                vCard.getField(VCardProperty.NICKNAME));
        setValue(R.string.vcard_formatted_name, vCard.getFormattedName());
        setValue(R.string.vcard_prefix_name,
                vCard.getField(NameProperty.PREFIX));
        setValue(R.string.vcard_given_name, vCard.getField(NameProperty.GIVEN));
        setValue(R.string.vcard_middle_name,
                vCard.getField(NameProperty.MIDDLE));
        setValue(R.string.vcard_family_name,
                vCard.getField(NameProperty.FAMILY));
        setValue(R.string.vcard_suffix_name,
                vCard.getField(NameProperty.SUFFIX));
        setValue(R.string.vcard_birth_date, vCard.getField(VCardProperty.BDAY));
        setValue(R.string.vcard_title, vCard.getField(VCardProperty.TITLE));
        setValue(R.string.vcard_role, vCard.getField(VCardProperty.ROLE));
        List<Organization> organizations = vCard.getOrganizations();
        String organization;
        if (organizations.isEmpty())
            organization = null;
        else {
            organization = organizations.get(0).getName();
            for (String unit : organizations.get(0).getUnits())
                organization = addString(organization, unit, "\n");
        }
        setValue(R.string.vcard_organization, organization);
        setValue(R.string.vcard_url, vCard.getField(VCardProperty.URL));
        String categories = null;
        for (String category : vCard.getCategories())
            categories = addString(categories, category, "\n");
        setValue(R.string.vcard_categories, categories);
        setValue(R.string.vcard_note, vCard.getField(VCardProperty.NOTE));
        setValue(R.string.vcard_decsription, vCard.getField(VCardProperty.DESC));
        PreferenceScreen screen = getPreferenceScreen();
        for (PreferenceCategory category : addresses)
            screen.removePreference(category);
        for (PreferenceCategory category : telephones)
            screen.removePreference(category);
        for (PreferenceCategory category : emails)
            screen.removePreference(category);
        for (Address address : vCard.getAddresses()) {
            String types = null;
            for (AddressType type : address.getTypes())
                types = addString(types, getString(ContactViewer.getAddressTypeMap().get(type)),
                        ", ");
            String value = null;
            for (AddressProperty property : AddressProperty.values())
                value = addString(value, address.getProperties().get(property),
                        "\n");
            PreferenceScreen addressScreen = createTypedCategory(
                    R.string.vcard_address, types, value);
            for (AddressProperty property : AddressProperty.values())
                addPreferenceScreen(addressScreen,
                        ContactViewer.getAddressPropertyMap().get(property), address
                                .getProperties().get(property));
        }
        for (Telephone telephone : vCard.getTelephones()) {
            String types = null;
            for (TelephoneType type : telephone.getTypes())
                types = addString(types,
                        getString(ContactViewer.getTelephoneTypeMap().get(type)), ", ");
            createTypedCategory(R.string.vcard_telephone, types,
                    telephone.getValue());
        }
        for (Email email : vCard.getEmails()) {
            String types = null;
            for (EmailType type : email.getTypes())
                types = addString(types, getString(ContactViewer.getEmailTypeMap().get(type)),
                        ", ");
            createTypedCategory(R.string.vcard_email, types, email.getValue());
        }
    }

    private PreferenceScreen createTypedCategory(int title, String types,
                                                 String value) {
        PreferenceCategory preferenceCategory = new PreferenceCategory(getActivity());
        preferenceCategory.setTitle(title);
        getPreferenceScreen().addPreference(preferenceCategory);

        addPreferenceScreen(preferenceCategory, R.string.vcard_type, types);
        return addPreferenceScreen(preferenceCategory, title, value);
    }

    private PreferenceScreen addPreferenceScreen(PreferenceGroup container,
                                                 int title, String summary) {
        PreferenceScreen preference = getPreferenceManager().createPreferenceScreen(getActivity());
        preference.setLayoutResource(R.layout.info_preference);
        preference.setTitle(title);
        preference.setSummary(summary == null ? "" : summary);
        container.addPreference(preference);
        return preference;
    }
}
