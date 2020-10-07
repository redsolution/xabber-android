package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.message.chat.groupchat.GroupchatIndexType;
import com.xabber.android.data.message.chat.groupchat.GroupchatManager;
import com.xabber.android.data.message.chat.groupchat.GroupchatMembershipType;
import com.xabber.android.data.message.chat.groupchat.GroupchatPrivacyType;
import com.xabber.xmpp.SimpleNamedElement;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CreateGroupchatIQ extends IQ {

    public static final String HASH_BLOCK = "#create";
    public static final String NAMESPACE = GroupchatManager.NAMESPACE.concat(HASH_BLOCK);
    public static final String QUERY_ELEMENT = "query";
    public static final String JID_ELEMENT = "jid";
    private static final String NAME_ELEMENT = "name";
    private static final String DESCRIPTION_ELEMENT = "description";
    private static final String LOCALPART_ELEMENT = "localpart";
    private static final String PRIVACY_ELEMENT = "privacy";
    private static final String INDEX_ELEMENT = "index";
    private static final String MEMBERSHIP_ELEMENT = "membership";
    private static final String CONTACTS_ELEMENT = "contacts";
    private static final String CONTACT_ELEMENT = "contact";

    private String to;
    private Jid from;
    private Collection<NamedElement> elements = new ArrayList<>();

    @Override
    public Jid getFrom() {
        return from;
    }

    @Override
    public Type getType() {
        return Type.set;
    }

    public CreateGroupchatIQ(Jid from, String to, String groupName, String groupLocalpart,
                             String description, GroupchatMembershipType membershipType,
                             GroupchatPrivacyType privacyType, GroupchatIndexType indexType){
        super(QUERY_ELEMENT, NAMESPACE);
        this.setType(Type.set);
        this.setFrom(from);
        this.setTo(to);

        elements.add(new SimpleNamedElement(NAME_ELEMENT, groupName));
        elements.add(new SimpleNamedElement(DESCRIPTION_ELEMENT, description));
        elements.add(new SimpleNamedElement(MEMBERSHIP_ELEMENT, membershipType.toXml()));
        elements.add(new SimpleNamedElement(PRIVACY_ELEMENT, privacyType.toXml()));
        elements.add(new SimpleNamedElement(INDEX_ELEMENT, indexType.toXml()));
        elements.add(new MSimpleParentElement(CONTACTS_ELEMENT,
                new SimpleNamedElement(CONTACT_ELEMENT, from.asBareJid().toString())));
        if (groupLocalpart != null && !groupLocalpart.isEmpty())
            elements.add(new SimpleNamedElement(LOCALPART_ELEMENT, groupLocalpart));

        this.from = from;
        this.to = to;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        if (elements.size() != 0)
            for (NamedElement element : elements)
                xml.append(element.toXML());
        return xml;
    }

    private static class MSimpleParentElement implements NamedElement{
        String elementName;
        List<NamedElement> nestedElements = new ArrayList<>();

        MSimpleParentElement(String elementName, List<NamedElement> nestedElements){
            this.elementName = elementName;
            this.nestedElements.addAll(nestedElements);
        }

        MSimpleParentElement(String elementName, NamedElement nestedElement){
            this.elementName = elementName;
            this.nestedElements.add(nestedElement);
        }

        @Override
        public String getElementName() { return elementName; }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xmlStringBuilder = new XmlStringBuilder(this);
            xmlStringBuilder.rightAngleBracket();
            for (NamedElement nestedElement : nestedElements)
                xmlStringBuilder.append(nestedElement.toXML());
            xmlStringBuilder.closeElement(this);
            return xmlStringBuilder;
        }
    }

    public interface CreateGroupchatIqResultListener{
        void onJidConflict();
        void onOtherError();
        void onSuccessfullyCreated(AccountJid accountJid, ContactJid contactJid);
    }

}
