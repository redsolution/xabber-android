package com.xabber.android.data;

import com.xabber.android.R;
import com.xabber.xmpp.vcard.AddressProperty;
import com.xabber.xmpp.vcard.AddressType;
import com.xabber.xmpp.vcard.EmailType;
import com.xabber.xmpp.vcard.TelephoneType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by grigory.fedorov on 13.04.15.
 */
public class VcardMaps {

    private static final Map<AddressType, Integer> ADDRESS_TYPE_MAP = new HashMap<>();
    private static final Map<AddressProperty, Integer> ADDRESS_PROPERTY_MAP = new HashMap<>();
    private static final Map<TelephoneType, Integer> TELEPHONE_TYPE_MAP = new HashMap<>();
    private static final Map<EmailType, Integer> EMAIL_TYPE_MAP = new HashMap<>();

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
}
