package com.xabber.android.data.database.realmobjects;

import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.StructuredName;
import com.xabber.xmpp.vcard.VCard;
import com.xabber.xmpp.vcard.VCardProperty;

import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

import io.realm.RealmObject;

public class VCardRealmObject extends RealmObject {

    private static final String LOG_TAG = VCardRealmObject.class.getSimpleName();
    private static final byte[] EMPTY_AVATAR = new byte[] {(byte) 0x00};

    public static final class Fields{
        public static final String CONTACT_JID = "contactJid";
        public static final String VCARD_STRING = "vCardString";
        public static final String NICK_NAME = "nickName";
        public static final String FORMATTED_NAME = "formattedName";
        public static final String FIRST_NAME = "firstName";
        public static final String LAST_NAME = "lastName";
        public static final String MIDDLE_NAME = "middleName";
    }

    private String contactJid;
    private String vCardString;
    private String nickName;
    private String formattedName;
    private String firstName;
    private String lastName;
    private String middleName;

    public VCardRealmObject(){ }

    public VCardRealmObject(ContactJid contactJid, VCard vCard){
        this.contactJid = contactJid.getBareJid().toString();
        vCard.setAvatar(EMPTY_AVATAR);
        this.vCardString = vCard.toXML().toString();
        this.nickName = vCard.getNickName();
        this.formattedName = vCard.getField(VCardProperty.FN.name());
        this.firstName = vCard.getFirstName();
        this.lastName = vCard.getLastName();
        this.middleName = vCard.getMiddleName();
    }

    public String getFormattedName() { return formattedName; }

    public String getFirstName() { return firstName; }

    public String getLastName() { return lastName; }

    public String getMiddleName() { return middleName; }

    public String getNickName() { return nickName; }

    public StructuredName getStructuredName(){
        return new StructuredName(getNickName(), getFormattedName(), getFirstName(),
                getMiddleName(), getLastName());
    }

    public void setContactJid(ContactJid contactJid) {
        this.contactJid = contactJid.getBareJid().toString();
    }
    public ContactJid getContactJid() {
        try {
            return ContactJid.from(contactJid);
        } catch (ContactJid.ContactJidCreateException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setVCard(VCard vCard) {
        vCard.setAvatar(EMPTY_AVATAR);
        this.vCardString = vCard.toXML().toString();
        this.vCardString = vCard.toXML().toString();
        this.nickName = vCard.getNickName();
        this.formattedName = vCard.getField(VCardProperty.FN.name());
        this.firstName = vCard.getFirstName();
        this.lastName = vCard.getLastName();
        this.middleName = vCard.getMiddleName();
    }
    public VCard getVCard() {
        VCard result = null;
        try{
            XmlPullParser xmlPullParser = XmlPullParserFactory.newInstance().newPullParser();
            xmlPullParser.setInput(new StringReader(vCardString));
            result = (VCard) new VCardProvider().parse(xmlPullParser);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        }
        return result;
    }
}
