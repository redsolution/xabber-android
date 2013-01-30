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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.capability.ClientInfo;
import com.xabber.android.data.extension.vcard.OnVCardListener;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.ResourceItem;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.ManagedPreferenceActivity;
import com.xabber.android.ui.widget.StatusPreference;
import com.xabber.androiddev.R;
import com.xabber.xmpp.address.Jid;
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

public class ContactViewer extends ManagedPreferenceActivity implements
		OnVCardListener, OnContactChangedListener, OnAccountChangedListener {

	private static final String SAVED_VCARD = "com.xabber.android.ui.ContactViewer.SAVED_VCARD";
	private static final String SAVED_VCARD_ERROR = "com.xabber.android.ui.ContactViewer.SAVED_VCARD_ERROR";

	private String account;
	private String bareAddress;
	private VCard vCard;
	private boolean vCardError;
	private List<PreferenceCategory> addresses;
	private List<PreferenceCategory> telephones;
	private List<PreferenceCategory> emails;

	private static final Map<AddressType, Integer> ADDRESS_TYPE_MAP = new HashMap<AddressType, Integer>();
	private static final Map<AddressProperty, Integer> ADDRESS_PROPERTY_MAP = new HashMap<AddressProperty, Integer>();
	private static final Map<TelephoneType, Integer> TELEPHONE_TYPE_MAP = new HashMap<TelephoneType, Integer>();
	private static final Map<EmailType, Integer> EMAIL_TYPE_MAP = new HashMap<EmailType, Integer>();

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.contact_viewer);
		addresses = new ArrayList<PreferenceCategory>();
		telephones = new ArrayList<PreferenceCategory>();
		emails = new ArrayList<PreferenceCategory>();

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
						for (RosterContact rosterContact : RosterManager
								.getInstance().getContacts())
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
		setTitle(getString(R.string.contact_viewer_for, bareAddress));
	}

	@Override
	protected void onResume() {
		super.onResume();
		Application.getInstance().addUIListener(OnVCardListener.class, this);
		Application.getInstance().addUIListener(OnContactChangedListener.class,
				this);
		Application.getInstance().addUIListener(OnAccountChangedListener.class,
				this);
		if (vCard == null && !vCardError)
			VCardManager.getInstance().request(account, bareAddress, null);
		updateContact();
		updateVCard();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Application.getInstance().removeUIListener(OnVCardListener.class, this);
		Application.getInstance().removeUIListener(
				OnContactChangedListener.class, this);
		Application.getInstance().removeUIListener(
				OnAccountChangedListener.class, this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SAVED_VCARD_ERROR, vCardError);
		if (vCard != null)
			outState.putString(SAVED_VCARD, vCard.getChildElementXML());
	}

	@Override
	public void onVCardReceived(String account, String bareAddress, VCard vCard) {
		if (!this.account.equals(account)
				|| !this.bareAddress.equals(bareAddress))
			return;
		this.vCard = vCard;
		this.vCardError = false;
		updateVCard();
	}

	@Override
	public void onVCardFailed(String account, String bareAddress) {
		if (!this.account.equals(account)
				|| !this.bareAddress.equals(bareAddress))
			return;
		this.vCard = null;
		this.vCardError = true;
		updateVCard();
		Application.getInstance().onError(R.string.XMPP_EXCEPTION);
	}

	@Override
	public void onContactsChanged(Collection<BaseEntity> entities) {
		for (BaseEntity entity : entities)
			if (entity.equals(account, bareAddress)) {
				updateContact();
				break;
			}
	}

	@Override
	public void onAccountsChanged(Collection<String> accounts) {
		if (accounts.contains(account))
			updateContact();
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

	private void updateContact() {
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
				StatusPreference preference = new StatusPreference(this);
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

	private void updateVCard() {
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
				types = addString(types, getString(ADDRESS_TYPE_MAP.get(type)),
						", ");
			String value = null;
			for (AddressProperty property : AddressProperty.values())
				value = addString(value, address.getProperties().get(property),
						"\n");
			PreferenceScreen addressScreen = createTypedCategory(
					R.string.vcard_address, types, value);
			for (AddressProperty property : AddressProperty.values())
				addPreferenceScreen(addressScreen,
						ADDRESS_PROPERTY_MAP.get(property), address
								.getProperties().get(property));
		}
		for (Telephone telephone : vCard.getTelephones()) {
			String types = null;
			for (TelephoneType type : telephone.getTypes())
				types = addString(types,
						getString(TELEPHONE_TYPE_MAP.get(type)), ", ");
			createTypedCategory(R.string.vcard_telephone, types,
					telephone.getValue());
		}
		for (Email email : vCard.getEmails()) {
			String types = null;
			for (EmailType type : email.getTypes())
				types = addString(types, getString(EMAIL_TYPE_MAP.get(type)),
						", ");
			createTypedCategory(R.string.vcard_email, types, email.getValue());
		}
	}

	private PreferenceScreen createTypedCategory(int title, String types,
			String value) {
		PreferenceCategory preferenceCategory = new PreferenceCategory(this);
		preferenceCategory.setTitle(title);
		getPreferenceScreen().addPreference(preferenceCategory);

		addPreferenceScreen(preferenceCategory, R.string.vcard_type, types);
		return addPreferenceScreen(preferenceCategory, title, value);
	}

	private PreferenceScreen addPreferenceScreen(PreferenceGroup container,
			int title, String summary) {
		PreferenceScreen preference = getPreferenceManager()
				.createPreferenceScreen(this);
		preference.setLayoutResource(R.layout.info_preference);
		preference.setTitle(title);
		preference.setSummary(summary == null ? "" : summary);
		container.addPreference(preference);
		return preference;
	}

	public static Intent createIntent(Context context, String account,
			String user) {
		return new EntityIntentBuilder(context, ContactViewer.class)
				.setAccount(account).setUser(user).build();
	}

	private static String getAccount(Intent intent) {
		return AccountIntentBuilder.getAccount(intent);
	}

	private static String getUser(Intent intent) {
		return EntityIntentBuilder.getUser(intent);
	}

}
