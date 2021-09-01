package com.xabber.android.ui.text

import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.URLSpan
import android.text.util.Linkify
import androidx.annotation.RequiresApi
import com.xabber.android.R
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject
import com.xabber.android.data.log.LogManager
import com.xabber.xmpp.groups.GroupPresenceExtensionElement
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.CharacterIterator
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.*

fun String.wrapWithItalicTag() = "<i>$this</i>"

fun String.wrapWithColorTag(color: Int) =
    "<font color='${String.format("#%06X", 0xFFFFFF and color)}'>$this</font>"

fun String.escapeXml() = this
    //$NON-NLS-1$
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("&", "&amp;")
    .replace("\"", "&quot;")
    // In this implementation we use &apos; instead of &#39; because we encode XML, not HTML.
    .replace("\\", "&apos;")

/**
 * Returns the text to be displayed in the status area of the groupchat
 * with the amount of members and online members.
 * Not to be mistaken with the stanza status value.
 */
fun GroupPresenceExtensionElement.getDisplayStatusForGroupchat(context: Context): String? {
    val members = this.allMembers
    val online = this.presentMembers
    if (members != 0) {
        val sb = StringBuilder(
            context.resources.getQuantityString(
                R.plurals.contact_groupchat_status_member, members, members
            )
        )
        if (online > 0) {
            sb.append(context.getString(R.string.contact_groupchat_status_online, online))
        }
        return sb.toString()
    }
    return null
}

fun getHumanReadableFileSize(bytesCount: Long): String {
    var bytes = bytesCount
    if (-1000 < bytes && bytes < 1000) {
        return "$bytes B"
    }
    val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
    while (bytes <= -999950 || bytes >= 999950) {
        bytes /= 1024
        ci.next()
    }
    return String.format(
        Locale.getDefault(),
        "%.2f %sB",
        bytes / 1024.0,
        ci.current().toString() + "i"
    )
}

@JvmOverloads
fun getDateStringForMessage(timestamp: Long, locale: Locale = Locale.getDefault()): String {
    val date = Date(timestamp)
    val strPattern = if (!date.isCurrentYear()) "d MMMM yyyy" else "d MMMM"
    return SimpleDateFormat(strPattern, locale).format(date)
}

fun getColoredAttachmentDisplayName(
    context: Context, attachments: List<AttachmentRealmObject>?, accountColorIndicator: Int
): String? {
    return if (attachments != null) {
        val attachmentName: String
        val attachmentBuilder = java.lang.StringBuilder()
        if (attachments.size == 1) {
            val singleAttachment = attachments[0]
            if (singleAttachment.isVoice) {
                attachmentBuilder.append(context.resources.getString(R.string.voice_message))
                if (singleAttachment.duration != null && singleAttachment.duration != 0L) {
                    attachmentBuilder.append(
                        String.format(
                            Locale.getDefault(),
                            ", %s",
                            getDurationStringForVoiceMessage(
                                null,
                                singleAttachment.duration
                            )
                        )
                    )
                }
            } else {
                if (singleAttachment.isImage) {
                    attachmentBuilder.append(
                        context.resources.getQuantityString(
                            R.plurals.recent_chat__last_message__images,
                            1
                        )
                    )
                } else {
                    attachmentBuilder.append(
                        context.resources.getQuantityString(
                            R.plurals.recent_chat__last_message__files,
                            1
                        )
                    )
                }
                if (singleAttachment.fileSize != null && singleAttachment.fileSize != 0L) {
                    attachmentBuilder
                        .append(", ")
                        .append(
                            getHumanReadableFileSize(
                                singleAttachment.fileSize
                            )
                        )
                }
            }
        } else {
            var sizeOfAllAttachments: Long = 0
            for (attachmentRealmObject in attachments) {
                sizeOfAllAttachments += attachmentRealmObject.fileSize
            }
            var isAllAttachmentsOfOneType = true
            for (i in 1 until attachments.size) {
                val currentAttachment = attachments[i]
                val previousAttachment = attachments[i - 1]
                if (!(currentAttachment.isVoice && previousAttachment.isVoice)
                    || !(currentAttachment.isImage && previousAttachment.isVoice)
                ) {
                    isAllAttachmentsOfOneType = false
                    break
                }
            }
            if (isAllAttachmentsOfOneType) {
                if (attachments[0].isImage) {
                    attachmentBuilder.append(
                        context.resources.getQuantityString(
                            R.plurals.recent_chat__last_message__images,
                            attachments.size,
                            attachments.size
                        )
                    )
                } else {
                    attachmentBuilder.append(
                        context.resources.getQuantityString(
                            R.plurals.recent_chat__last_message__files,
                            attachments.size,
                            attachments.size
                        )
                    )
                }
            } else {
                attachmentBuilder.append(
                    context.resources.getString(
                        R.string.recent_chat__last_message__attachments, attachments.size
                    )
                )
            }
            attachmentBuilder
                .append(", ")
                .append(
                    getHumanReadableFileSize(sizeOfAllAttachments)
                )
        }
        attachmentName = attachmentBuilder.toString()
        if (accountColorIndicator != -1) {
            attachmentName.wrapWithColorTag(accountColorIndicator)
        } else {
            attachmentName
        }
    } else {
        null
    }
}

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
fun getDecodedSpannable(text: String?): Spannable {
    val factory = Editable.Factory.getInstance()
    val originalSpannable = factory.newEditable(text) as SpannableStringBuilder
    if (Linkify.addLinks(originalSpannable, Linkify.WEB_URLS)) {
        // get all url spans if addLinks() returned true, meaning that it found web urls
        val originalURLSpans = originalSpannable.getSpans(
            0, originalSpannable.length,
            URLSpan::class.java
        )
        val urlSpanContainers = ArrayList<URLSpanContainer>(originalURLSpans.size)
        for (originalUrl in originalURLSpans) {
            // save the original url span data
            urlSpanContainers.add(
                URLSpanContainer(
                    originalUrl!!,
                    originalSpannable.getSpanStart(originalUrl),
                    originalSpannable.getSpanEnd(originalUrl)
                )
            )
            // remove original url span from spannable
            originalSpannable.removeSpan(originalUrl)
        }
        // iterate over each available url span from last to first, to properly
        // manage start positions in cases when the size of the text changes on decoding
        for (i in urlSpanContainers.indices.reversed()) {
            val spanContainer = urlSpanContainers[i]
            try {
                val originalURL = spanContainer.span.url
                val decodedURL = URLDecoder.decode(originalURL, StandardCharsets.UTF_8.name())
                val decodedSpan = URLSpan(decodedURL)
                if (decodedURL.length < originalURL.length) {
                    // replace the text with the decoded url
                    originalSpannable.replace(
                        spanContainer.start, spanContainer.end,
                        decodedURL,
                        0,
                        decodedURL.length
                    )
                    // set a new span range
                    originalSpannable.setSpan(
                        decodedSpan,
                        spanContainer.start,
                        decodedSpan.url.length + spanContainer.start,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    // restore the old span range
                    originalSpannable.setSpan(
                        spanContainer.span,
                        spanContainer.start,
                        spanContainer.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } catch (e: UnsupportedEncodingException) {
                originalSpannable.setSpan(
                    spanContainer.span,
                    spanContainer.start,
                    spanContainer.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                LogManager.exception("StringUtils", e)
            }
        }
    }
    return originalSpannable
}
