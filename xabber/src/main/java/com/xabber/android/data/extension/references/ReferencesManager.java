package com.xabber.android.data.extension.references;

import android.text.Html;
import android.text.TextUtils;
import android.util.Pair;

import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;

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

import javax.annotation.Nonnull;

import io.realm.RealmList;
import io.realm.RealmResults;

public class ReferencesManager {

    @Nonnull
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

    public static Data createMediaReferences(RealmList<Attachment> attachments, String legacyBody) {
        List<RefMedia> mediaList = new ArrayList<>();
        for (Attachment attachment : attachments) {
            RefFile.Builder builder = RefFile.newBuilder();
            builder.setName(attachment.getTitle());
            builder.setMediaType(attachment.getMimeType());
            builder.setVoice(false);
            builder.setDuration(attachment.getDuration());
            builder.setSize(attachment.getFileSize());
            if (attachment.getImageHeight() != null)
                builder.setHeight(attachment.getImageHeight());
            if (attachment.getImageWidth() != null)
                builder.setWidth(attachment.getImageWidth());
            RefMedia media = new RefMedia(builder.build(), attachment.getFileUrl());
            mediaList.add(media);
        }

        char[] chars = TextUtils.htmlEncode(legacyBody).toCharArray();
        return new Data(0, chars.length - 1, mediaList);
    }

    public static Forward createForwardReference(RealmResults<MessageItem> items, String legacyBody) {
        List<Forwarded> forwardedList = new ArrayList<>();
        for (MessageItem item : items) {
            try {
                Message forwarded = PacketParserUtils.parseStanza(item.getOriginalStanza());
                forwardedList.add(new Forwarded(new DelayInformation(new Date(item.getTimestamp())), forwarded));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        char[] chars = TextUtils.htmlEncode(legacyBody).toCharArray();
        return new Forward(0, chars.length - 1, forwardedList);
    }

    @Nonnull
    public static List<RefMedia> getMediaFromReferences(Stanza packet) {
        List<ExtensionElement> elements = packet.getExtensions(ReferenceElement.ELEMENT, ReferenceElement.NAMESPACE);
        if (elements == null || elements.size() == 0) return Collections.emptyList();

        List<RefMedia> media = new ArrayList<>();
        for (ExtensionElement element : elements) {
            if (element instanceof Data) {
                media.addAll(((Data) element).getMedia());
            }
        }
        return media;
    }

    public static Pair<String, String> modifyBodyWithReferences(Message message, String body) {
        if (body == null || body.isEmpty() || body.trim().isEmpty()) return new Pair<>(body, null);

        List<ExtensionElement> elements = message.getExtensions(ReferenceElement.ELEMENT, ReferenceElement.NAMESPACE);
        if (elements == null || elements.size() == 0) return new Pair<>(body, null);

        List<ReferenceElement> references = getReferences(elements);
        if (references.isEmpty()) return new Pair<>(body, null);

        // encode HTML and split into chars
        String[] chars = stringToChars(TextUtils.htmlEncode(body));

        // modify chars with references except markup
        for (ReferenceElement reference : references) {
            if (!(reference instanceof Markup))
                chars = modifyBodyWithReferences(chars, reference);
        }

        // chars to string and decode from html
        String regularBody = Html.fromHtml(charsToString(chars).replace("\n", "<br/>")).toString();
        String markupBody = null;

        // modify chars with markup references
        for (ReferenceElement reference : references) {
            if (reference instanceof Markup)
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
            case data:
                chars = remove(begin, end, chars);
                break;
            case forward:
                chars = remove(begin, end, chars);
                break;
            case markup:
                chars = markup(begin, end, chars, (Markup) reference);
                break;
            case quote:
                chars = removeInLine(begin, end, chars, (Quote) reference);
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

    private static String[] removeInLine(int begin, int end, String[] source, Quote reference) {
        int del = reference.getDel();
        int removed = 0;
        for (int i = begin; i <= end; i++) {
            if (removed < del) {
                source[i] = String.valueOf(Character.MIN_VALUE);
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
        source[begin] = builderOpen.append(source[begin]).toString();
        builderClose.append(new StringBuilder(source[end]).reverse());
        source[end] = builderClose.reverse().toString();
        return source;
    }

}
