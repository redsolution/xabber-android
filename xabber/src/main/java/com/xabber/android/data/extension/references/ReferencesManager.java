package com.xabber.android.data.extension.references;

import android.text.Html;
import android.util.Pair;

import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.ui.text.ClickSpan;
import com.xabber.android.utils.Utils;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.forward.packet.Forwarded;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ReferencesManager {

    @NonNull
    public static List<Forwarded> getForwardedFromReferences(Stanza packet) {
        List<ExtensionElement> elements = packet.getExtensions(ReferenceElement.ELEMENT, ReferenceElement.NAMESPACE);
        if (elements == null || elements.size() == 0) return Collections.emptyList();

        List<Forwarded> forwarded = new ArrayList<>();
        for (ExtensionElement element : elements) {
            if (element instanceof Forward) {
                forwarded.addAll(((Forward) element).getForwarded());
            }
        }
        return forwarded;
    }

    public static Media createMediaReferences(Attachment attachment, int begin, int end) {
        List<RefMedia> mediaList = new ArrayList<>();
        RefFile.Builder builder = RefFile.newBuilder();
        builder.setName(attachment.getTitle());
        builder.setMediaType(attachment.getMimeType());
        builder.setDuration(attachment.getDuration());
        builder.setSize(attachment.getFileSize());
        if (attachment.getImageHeight() != null)
            builder.setHeight(attachment.getImageHeight());
        if (attachment.getImageWidth() != null)
            builder.setWidth(attachment.getImageWidth());
        RefMedia media = new RefMedia(builder.build(), attachment.getFileUrl());
        mediaList.add(media);

        return new Media(begin, end, mediaList);
    }

    public static Forward createForwardReference(MessageItem item, int begin, int end) {
        List<Forwarded> forwardedList = new ArrayList<>();
        try {
            Message forwarded = PacketParserUtils.parseStanza(item.getOriginalStanza());
            forwardedList.add(new Forwarded(new DelayInformation(new Date(item.getTimestamp())), forwarded));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Forward(begin, end, forwardedList);
    }

    @NonNull
    public static List<RefMedia> getMediaFromReferences(Stanza packet) {
        List<ExtensionElement> elements = packet.getExtensions(ReferenceElement.ELEMENT, ReferenceElement.NAMESPACE);
        if (elements == null || elements.size() == 0) return Collections.emptyList();

        List<RefMedia> media = new ArrayList<>();
        for (ExtensionElement element : elements) {
            if (element instanceof Media) {
                media.addAll(((Media) element).getMedia());
            }
        }
        return media;
    }

    @Nullable
    public static RefUser getGroupchatUserFromReferences(Stanza packet) {
        List<ExtensionElement> elements = packet.getExtensions(ReferenceElement.ELEMENT, ReferenceElement.NAMESPACE);
        if (elements == null || elements.size() == 0) return null;

        for (ExtensionElement element : elements) {
            if (element instanceof Groupchat) {
                return ((Groupchat) element).getUser();
            }
        }
        return null;
    }

    public static Pair<String, String> modifyBodyWithReferences(Message message, String body) {
        if (body == null || body.isEmpty() || body.trim().isEmpty()) return new Pair<>(body, null);

        List<ExtensionElement> elements = message.getExtensions(ReferenceElement.ELEMENT, ReferenceElement.NAMESPACE);
        if (elements == null || elements.size() == 0) return new Pair<>(body, null);

        List<ReferenceElement> references = getReferences(elements);
        if (references.isEmpty()) return new Pair<>(body, null);

        // encode HTML and split into chars
        String[] chars = stringToChars(Utils.xmlEncode(body));

        // modify chars with references except markup and mention
        for (ReferenceElement reference : references) {
            if (!(reference instanceof Markup) && !(reference instanceof Mention) && !(reference instanceof Quote))
                chars = modifyBodyWithReferences(chars, reference);
        }

        // chars to string and decode from html
        String regularBody = Html.fromHtml(charsToString(chars).replace("\n", "<br/>")).toString();
        String markupBody = null;

        // modify chars with markup and mention references
        for (ReferenceElement reference : references) {
            if (reference instanceof Markup || reference instanceof Mention || reference instanceof Quote)
                chars = modifyBodyWithReferences(chars, reference);
        }
        markupBody = charsToString(chars);
        if (regularBody.equals(markupBody)) markupBody = null;

        return new Pair<>(regularBody, markupBody);
    }



    private static List<ReferenceElement> getReferences(List<ExtensionElement> elements) {
        List<ReferenceElement> references = new ArrayList<>();
        for (ExtensionElement element : elements) {
            if (element instanceof ReferenceElement) references.add((ReferenceElement) element);
        }
        return references;
    }

    private static String charsToString(String[] array) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (!array[i].equals(String.valueOf(Character.MIN_VALUE)))
                builder.append(array[i]);
        }
        return builder.toString();
    }

    private static String[] stringToChars(String source) {
        String[] result = new String[source.codePointCount(0, source.length())];
        int i = 0;
        for (int offset = 0; offset < source.length(); ) {
            int codepoint = source.codePointAt(offset);
            result[i] = String.valueOf(Character.toChars(codepoint));
            offset += Character.charCount(codepoint);
            i++;
        }
        return result;
    }

    private static String[] modifyBodyWithReferences(String[] chars, ReferenceElement reference) {
        int begin = reference.getBegin();
        if (begin < 0) begin = 0;
        int end = reference.getEnd();
        if (end >= chars.length) end = chars.length - 1;
        if (begin > end) return chars;

        switch (reference.getType()) {
            case media:
                chars = remove(begin, end, chars);
                break;
            case forward:
                chars = remove(begin, end, chars);
                break;
            case groupchat:
                chars = remove(begin, end, chars);
                break;
            case markup:
                chars = markup(begin, end, chars, (Markup) reference);
                break;
            case quote:
                chars = quote(begin, end, chars, (Quote) reference);
                break;
            case mention:
                chars = mention(begin, end, chars, (Mention) reference);
                break;
        }
        return chars;
    }

    private static String[] remove(int begin, int end, String[] source) {
        for (int i = begin; i <= end; i++) {
            source[i] = String.valueOf(Character.MIN_VALUE);
        }
        return source;
    }

    private static String[] quote(int begin, int end, String[] source, Quote reference) {
        int del = Utils.xmlEncode(reference.getMarker()).length();
        int removed = 0;
        for (int i = begin; i <= end; i++) {
            if (removed < del) {
                if (removed == 0) source[i] = "<font color='#9e9e9e'>\u2503</font> ";
                else source[i] = String.valueOf(Character.MIN_VALUE);
                removed++;
            }
            if (source[i].equals("\n")) removed = 0;
        }
        return source;
    }

    private static String[] markup(int begin, int end, String[] source, Markup reference) {
        StringBuilder builderOpen = new StringBuilder();
        StringBuilder builderClose = new StringBuilder();
        if (reference.isBold()) {
            builderOpen.append("<b>");
            builderClose.append(new StringBuilder("</b>").reverse());
        }
        if (reference.isItalic()) {
            builderOpen.append("<i>");
            builderClose.append(new StringBuilder("</i>").reverse());
        }
        if (reference.isUnderline()) {
            builderOpen.append("<u>");
            builderClose.append(new StringBuilder("</u>").reverse());
        }
        if (reference.isStrike()) {
            builderOpen.append("<strike>");
            builderClose.append(new StringBuilder("</strike>").reverse());
        }
        if (reference.getUri() != null && !reference.getUri().isEmpty()) {
            // Add [&zwj;] (zero-with-join) symbol before custom tag to avoid issue:
            // https://stackoverflow.com/questions/23568481/weird-taghandler-behavior-detecting-opening-and-closing-tags
            builderOpen.append("&zwj;<click uri='");
            builderOpen.append(reference.getUri());
            builderOpen.append("' type='");
            builderOpen.append(ClickSpan.TYPE_HYPERLINK);
            builderOpen.append("'>");
            builderClose.append(new StringBuilder("</click>").reverse());
        }
        source[begin] = builderOpen.append(source[begin]).toString();
        builderClose.append(new StringBuilder(source[end]).reverse());
        source[end] = builderClose.reverse().toString();
        return source;
    }

    private static String[] mention(int begin, int end, String[] source, Mention reference) {
        StringBuilder builderOpen = new StringBuilder();
        StringBuilder builderClose = new StringBuilder();
        if (reference.getUri() != null && !reference.getUri().isEmpty()) {
            // Add [&zwj;] (zero-with-join) symbol before custom tag to avoid issue:
            // https://stackoverflow.com/questions/23568481/weird-taghandler-behavior-detecting-opening-and-closing-tags
            builderOpen.append("&zwj;<click uri='");
            builderOpen.append(reference.getUri());
            builderOpen.append("' type='");
            builderOpen.append(ClickSpan.TYPE_MENTION);
            builderOpen.append("'>");
            builderClose.append(new StringBuilder("</click>").reverse());
        }
        source[begin] = builderOpen.append(source[begin]).toString();
        builderClose.append(new StringBuilder(source[end]).reverse());
        source[end] = builderClose.reverse().toString();
        return source;
    }

}
