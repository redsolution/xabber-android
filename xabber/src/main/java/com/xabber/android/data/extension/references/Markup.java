package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class Markup extends ReferenceElement {
    private final boolean bold;
    private final boolean italic;
    private final boolean underline;
    private final boolean strike;
    private final String url;

    public Markup(int begin, int end, boolean bold, boolean italic, boolean underline, boolean strike, String url) {
        super(begin, end);
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strike = strike;
        this.url = url;
    }

    @Override
    public Type getType() {
        return Type.markup;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        if (bold) xml.emptyElement(ELEMENT_BOLD);
        if (italic) xml.emptyElement(ELEMENT_ITALIC);
        if (underline) xml.emptyElement(ELEMENT_UNDERLINE);
        if (strike) xml.emptyElement(ELEMENT_STRIKE);
        if (url != null && url.isEmpty()) {
            xml.element(ELEMENT_URL, url);
        }
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

    public String getUrl() {
        return url;
    }
}
