package com.xabber.android.data.database.realmobjects;

import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.xmpp.vcard.VCard;

import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

import io.realm.RealmObject;

public class VCardRealmObject extends RealmObject {

    private static final String LOG_TAG = VCardRealmObject.class.getSimpleName();

    public static final class Fields{
        public static final String CONTACT_JID = "contactJid";
        public static final String VCARD_STRING = "vCardString";
    }

    private String contactJid;
    private String vCardString;

    public VCardRealmObject(){ }

    public VCardRealmObject(ContactJid contactJid, VCard vCard){
        this.contactJid = contactJid.getBareJid().toString();
        this.vCardString = vCard.toString();
    }

    public void setContactJid(ContactJid contactJid) {
        this.contactJid = contactJid.getBareJid().toString();
    }
    public ContactJid getContactJid() {
        try {
            return ContactJid.from(contactJid);
        } catch (ContactJid.UserJidCreateException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setVCard(VCard vCard) { this.vCardString = vCard.toString(); }
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
