package com.xabber.android.data.extension.references.decoration;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class Markup extends Decoration {

    private final boolean bold;
    private final boolean italic;
    private final boolean underline;
    private final boolean strike;
    private final boolean quote;
    private final String link;

    public Markup(int begin, int end, boolean bold, boolean italic, boolean underline, boolean strike, boolean quote, String link) {
        super(begin, end);
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strike = strike;
        this.quote = quote;
        this.link = link;
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public boolean isUnderline() {
        return underline;
    }

    public boolean isStrike() {
        return strike;
    }

    public boolean isQuote() {
        return quote;
    }

    public String getLink() {
        return link;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        if (bold) addMarkupElement(xml, ELEMENT_BOLD, null);
        if (italic) addMarkupElement(xml, ELEMENT_ITALIC, null);
        if (underline) addMarkupElement(xml, ELEMENT_UNDERLINE, null);
        if (strike) addMarkupElement(xml, ELEMENT_STRIKE, null);
        if (quote) addMarkupElement(xml, ELEMENT_QUOTE, null);
        if (link != null && !link.isEmpty()) {
            addMarkupElement(xml, ELEMENT_LINK, link);
        }
    }

    private void addMarkupElement(XmlStringBuilder xml, String element, String content) {
        xml.prelude(element, MARKUP_NAMESPACE);
        if (content != null) {
            xml.rightAngleBracket();
            xml.append(content);
            xml.closeElement(element);
        } else {
            xml.closeEmptyElement();
        }
    }
}
