package com.xabber.android.data.extension.references;

import android.text.Html;
import android.text.TextUtils;

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
            if (element instanceof ReferenceElement) {
                forwarded.addAll(((ReferenceElement) element).getForwarded());
            }
        }
        return forwarded;
    }

    public static ReferenceElement createMediaReferences(RealmList<Attachment> attachments, String legacyBody) {
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
        return new ReferenceElement(ReferenceElement.Type.data, 0, chars.length - 1, 0, null, mediaList);
    }

    public static ReferenceElement createForwardReference(RealmResults<MessageItem> items, String legacyBody) {
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
        return new ReferenceElement(ReferenceElement.Type.forward, 0, chars.length - 1, 0,
                forwardedList, null);
    }

    @Nonnull
    public static List<RefMedia> getMediaFromReferences(Stanza packet) {
        List<ExtensionElement> elements = packet.getExtensions(ReferenceElement.ELEMENT, ReferenceElement.NAMESPACE);
        if (elements == null || elements.size() == 0) return Collections.emptyList();

        List<RefMedia> media = new ArrayList<>();
        for (ExtensionElement element : elements) {
            if (element instanceof ReferenceElement) {
                media.addAll(((ReferenceElement) element).getMedia());
            }
        }
        return media;
    }

    public static String modifyBodyWithReferences(Message message, String body) {
        if (body == null || body.isEmpty() || body.trim().isEmpty()) return body;

        List<ExtensionElement> elements = message.getExtensions(ReferenceElement.ELEMENT, ReferenceElement.NAMESPACE);
        if (elements == null || elements.size() == 0) return body;

        char[] chars = TextUtils.htmlEncode(body).toCharArray();
        for (ExtensionElement element : elements) {
            if (element instanceof ReferenceElement) {
                chars = modifyBodyWithReferences(chars, (ReferenceElement) element);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != Character.MIN_VALUE)
                builder.append(chars[i]);
        }
        return Html.fromHtml(builder.toString()).toString();
    }

    private static char[] modifyBodyWithReferences(char[] chars, ReferenceElement reference) {
        int begin = reference.getBegin();
        if (begin < 0) begin = 0;
        int end = reference.getEnd();
        if (end >= chars.length) end = chars.length - 1;

        switch (reference.getType()) {
            case data:
                chars = remove(begin, end, chars);
                break;
            case forward:
                chars = remove(begin, end, chars);
                break;
            case legacy:
                chars = remove(begin, end, chars);
                break;
        }
        return chars;
    }

    private static char[] remove(int begin, int end, char[] source) {
        for (int i = begin; i <= end; i++) {
            source[i] = Character.MIN_VALUE;
        }
        return source;
    }

}
