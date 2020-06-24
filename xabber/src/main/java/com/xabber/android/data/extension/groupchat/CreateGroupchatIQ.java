package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.message.chat.groupchat.GroupchatManager;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.NamedElement;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;

public class CreateGroupchatIQ extends IQ {

    private static final String NAMESPACE = GroupchatManager.NAMESPACE;
    private static final String ELEMENT = "create";
    private static final String NAME_ELEMENT = "name";
    private static final String PRIVACY_ELEMENT = "privacy";
    private static final String INDEX_ELEMENT = "index";
    private static final String MEMBERSHIP_ELEMENT = "membership";
    //todo implement contacts, domains, languages

    private Jid to;
    private Jid from;
    private Collection<NamedElement> elements = new ArrayList<>();

    @Override
    public Type getType() {
        return Type.set;
    }

    @Override
    public Jid getTo() {
        return to;
    }

    public CreateGroupchatIQ(Jid from, Jid to){
        super(ELEMENT, NAMESPACE);
        this.setType(Type.set);
        this.setFrom(from);
        this.setTo(to);

        this.from = from;
        this.to = to;
    }

    public void addElement(NamedElement element){
        elements.add(element);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        if (elements.size() != 0)
            for (NamedElement element : elements)
                xml.append(element.toXML());
        return xml;
    }

}
