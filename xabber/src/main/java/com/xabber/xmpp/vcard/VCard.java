package com.xabber.xmpp.vcard;

import java.util.HashMap;
import java.util.Map;

public class VCard extends org.jivesoftware.smackx.vcardtemp.packet.VCard {

    /**
     * Phone types:
     * VOICE?, FAX?, PAGER?, MSG?, CELL?, VIDEO?, BBS?, MODEM?, ISDN?, PCS?, PREF?
     */
    private Map<String, String> mobilePhones = new HashMap<String, String>();

    public VCard() {
        super();
    }

    /**
     * Set work phone number.
     *
     * @param phoneType one of VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF
     * @param phoneNum  phone number
     */
    public void setPhoneMobile(String phoneType, String phoneNum) {
        mobilePhones.put(phoneType, phoneNum);
    }

    /**
     * Get work phone number.
     *
     * @param phoneType one of VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF
     */
    public String getPhoneMobile(String phoneType) {
        return mobilePhones.get(phoneType);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        if (hasContent()) {
            xml.rightAngleBracket();
            for (Map.Entry<String, String> phone : mobilePhones.entrySet()) {
                final String number = phone.getValue();
                if (number == null) {
                    continue;
                }
                xml.openElement("TEL");
                //xml.emptyElement(phone.getKey());
                xml.element("NUMBER", number);
                //xml.closeElement("TEL");
                xml.append("</TEL");
            }
        }
        super.getIQChildElementBuilder(xml);
        return xml;
    }

    private boolean hasContent() {
        return mobilePhones.size()>0;
    }
}
