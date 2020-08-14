package com.xabber.android.data.extension.groupchat;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.NamedElement;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collection;

public class GroupchatUpdateIQ extends IQ  {

    public static final String NAMESPACE = "http://xabber.com/protocol/groupchat";
    public static final String ELEMENT = "update";

    private Jid from;
    private Jid to;
    private Collection<NamedElement> elements = new ArrayList<>();

    public String getNamespace(){ return NAMESPACE; }
    public String getElementName(){ return ELEMENT; }

    public GroupchatUpdateIQ(Jid from, Jid to){
        super(ELEMENT, NAMESPACE);
        this.setType(Type.set);
        this.setFrom(from);
        this.setTo(to);

        this.from = from;
        this.to = to;
    }

    public GroupchatUpdateIQ(Jid from, Jid to, NamedElement element){
        super(ELEMENT, NAMESPACE);
        this.setType(Type.set);
        this.setFrom(from);
        this.setTo(to);

        this.from = from;
        this.to = to;

        this.addElement(element);
    }

    public GroupchatUpdateIQ(Jid from, Jid to, Collection<NamedElement> elements){
        super(ELEMENT, NAMESPACE);
        this.setType(Type.set);
        this.setFrom(from);
        this.setTo(to);

        this.from = from;
        this.to = to;

        this.addElements(elements);
    }

    public void addElement(NamedElement element){
        elements.add(element);
    }

    public void addElements(Collection<NamedElement> elements){
        for (NamedElement element : elements)
            addElement(element);
    }

    @Override
    public Type getType() { return Type.set; }

    @Override
    public Jid getTo() {
        return to;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        if (elements.size() != 0)
            for (NamedElement element : elements)
                xml.append(element.toXML());
        return xml;
    }

    public interface UpdateGroupchatSettingsIqResultListener{
        void onError();
        void onSuccess();
    }

}
