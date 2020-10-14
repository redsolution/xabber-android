package com.xabber.android.data.extension.groupchat;

import androidx.annotation.NonNull;

import com.xabber.android.data.message.chat.groupchat.GroupchatManager;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatExtensionElement implements ExtensionElement {

    public static final String NAMESPACE = GroupchatManager.NAMESPACE;
    public static final String SYSTEM_MESSAGE_NAMESPACE = GroupchatManager.SYSTEM_MESSAGE_NAMESPACE;
    public static final String ELEMENT = "x";
    public static final String TYPE_ATTRIBUTE = "type";

    private Type type = Type.none;

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute(TYPE_ATTRIBUTE, type.toString());
        xml.rightAngleBracket();
        appendToXML(xml);
        xml.closeElement(this);
        return xml;
    }

    public void appendToXML(XmlStringBuilder xml) {
    }

    public enum Type {
        join,
        kick,
        block,
        left,
        echo,
        create,
        none;

        public Type fromString(String string){
            switch (string){
                case "join": return join;
                case "kick": return kick;
                case "block": return block;
                case "left": return left;
                case "echo": return echo;
                case "create": return create;
                default: return none;
            }
        }

        @NonNull
        @Override
        public String toString() {
            switch (this){
                case join: return "join";
                case kick: return "kick";
                case block: return "block";
                case left: return "left";
                case echo: return "echo";
                case create: return "create";
                case none:
                default: return "";
            }
        }
    }

}
