package com.xabber.xmpp.vcard

import com.xabber.android.data.message.chat.groupchat.GroupchatIndexType
import com.xabber.android.data.message.chat.groupchat.GroupchatMembershipType
import com.xabber.android.data.message.chat.groupchat.GroupchatPrivacyType
import org.jivesoftware.smack.provider.IQProvider
import org.jivesoftware.smack.util.StringUtils
import org.xmlpull.v1.XmlPullParser

open class VCardCustomProvider : IQProvider<VCard>() {

    override fun parse(parser: XmlPullParser?, initialDepth: Int): VCard {
        val vCard = VCard()
        var name = ""
        outerloop@ while (true) {
            when (parser!!.next()) {
                XmlPullParser.START_TAG -> {
                    name = parser.name
                    when (name) {
                        "N" -> parseName(parser, vCard)
                        "ORG" -> parseOrg(parser, vCard)
                        "TEL" -> parseTel(parser, vCard)
                        "ADR" -> parseAddress(parser, vCard)
                        "EMAIL" -> parseEmail(parser, vCard)
                        "NICKNAME" -> vCard.nickName = parser.nextText()
                        "JABBERID" -> vCard.jabberId = parser.nextText()
                        "PHOTO" -> parsePhoto(parser, vCard)
                        VCard.PRIVACY_ELEMENT -> parsePrivacy(parser, vCard)
                        VCard.INDEX_ELEMENT -> parseIndex(parser, vCard)
                        VCard.MEMBERSHIP_ELEMENT -> parseMembership(parser, vCard)
                        VCard.DESCRIPTION_ELEMENT -> parseDescription(parser, vCard)
                        VCard.MEMBERS_ELEMENT -> parseMembers(parser, vCard)
                        else -> {
                        }
                    }
                }
                XmlPullParser.TEXT -> if (initialDepth + 1 == parser.depth) {
                    vCard.setField(name, parser.text)
                }
                XmlPullParser.END_TAG -> if (parser.depth == initialDepth) {
                    break@outerloop
                }
                else -> {
                }
            }
        }

        return vCard
    }

    private fun parsePrivacy(parser: XmlPullParser, vCard: VCard){
        vCard.privacyType = GroupchatPrivacyType.getPrivacyTypeFromXml(parser.nextText())
    }

    private fun parseIndex(parser: XmlPullParser, vCard: VCard){
        vCard.indexType = GroupchatIndexType.getIndexTypeFromXml(parser.nextText())
    }

    private fun parseMembership(parser: XmlPullParser, vCard: VCard){
        vCard.membershipType = GroupchatMembershipType.getMembershipTypeFromXml(parser.nextText())
    }

    private fun parseDescription(parser: XmlPullParser, vCard: VCard){
        vCard.description = parser.nextText()
    }

    private fun parseMembers(parser: XmlPullParser, vCard: VCard){
        vCard.membersCount = parser.nextText().toInt()
    }

    private fun parseAddress(parser: XmlPullParser, vCard: VCard) {
        val initialDepth = parser.depth
        var isWork = true
        outerloop@ while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    if ("HOME" == name) {
                        isWork = false
                    } else {
                        for (adr in ADR) {
                            if (adr == name) {
                                if (isWork) {
                                    vCard.setAddressFieldWork(name, parser.nextText())
                                } else {
                                    vCard.setAddressFieldHome(name, parser.nextText())
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> if (parser.depth == initialDepth) {
                    break@outerloop
                }
                else -> {
                }
            }
        }
    }

    private fun parseTel(parser: XmlPullParser, vCard: VCard) {
        val initialDepth = parser.depth
        var isWork = false
        var isHome = false
        var telLabel: String? = null
        outerloop@ while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    if ("HOME" == name) {
                        isWork = false
                        isHome = true
                    } else if ("WORK" == name) {
                        isWork = true
                        isHome = false
                    } else {
                        if ("NUMBER" == name) {
                            if (StringUtils.isNullOrEmpty(telLabel)) {
                                // RFC 2426 § 3.3.1:
                                // "The default type is 'voice'"
                                telLabel = "VOICE"
                            }
                            when {
                                isWork -> {
                                    vCard.setPhoneWork(telLabel, parser.nextText())
                                    isWork = false
                                    isHome = false
                                }
                                isHome -> {
                                    vCard.setPhoneHome(telLabel, parser.nextText())
                                    isWork = false
                                    isHome = false
                                }
                                else -> {
                                    vCard.setPhoneMobile(telLabel!!, parser.nextText())
                                }
                            }
                        } else {
                            for (tel in TEL) {
                                if (tel == name) {
                                    telLabel = name
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> if (parser.depth == initialDepth) {
                    break@outerloop
                }
                else -> {
                }
            }
        }
    }

    private fun parseOrg(parser: XmlPullParser, vCard: VCard) {
        val initialDepth = parser.depth
        outerloop@ while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "ORGNAME" -> vCard.organization = parser.nextText()
                        "ORGUNIT" -> vCard.organizationUnit = parser.nextText()
                        else -> {
                        }
                    }
                }
                XmlPullParser.END_TAG -> if (parser.depth == initialDepth) {
                    break@outerloop
                }
                else -> {
                }
            }
        }
    }

    private fun parseEmail(parser: XmlPullParser, vCard: VCard) {
        val initialDepth = parser.depth
        var isWork = false
        outerloop@ while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "WORK" -> isWork = true
                        "USERID" -> if (isWork) {
                            vCard.emailWork = parser.nextText()
                        } else {
                            vCard.emailHome = parser.nextText()
                        }
                        else -> {
                        }
                    }
                }
                XmlPullParser.END_TAG -> if (parser.depth == initialDepth) {
                    break@outerloop
                }
                else -> {
                }
            }
        }
    }

    private fun parseName(parser: XmlPullParser, vCard: VCard) {
        val initialDepth = parser.depth
        outerloop@ while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "FAMILY" -> vCard.lastName = parser.nextText()
                        "GIVEN" -> vCard.firstName = parser.nextText()
                        "MIDDLE" -> vCard.middleName = parser.nextText()
                        "PREFIX" -> vCard.prefix = parser.nextText()
                        "SUFFIX" -> vCard.suffix = parser.nextText()
                        else -> {
                        }
                    }
                }
                XmlPullParser.END_TAG -> if (parser.depth == initialDepth) {
                    break@outerloop
                }
                else -> {
                }
            }
        }
    }

    private fun parsePhoto(parser: XmlPullParser, vCard: VCard) {
        val initialDepth = parser.depth
        var binval: String? = null
        var mimetype: String? = null
        outerloop@ while (true) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "BINVAL" -> binval = parser.nextText()
                        "TYPE" -> mimetype = parser.nextText()
                        else -> {
                        }
                    }
                }
                XmlPullParser.END_TAG -> if (parser.depth == initialDepth) {
                    break@outerloop
                }
                else -> {
                }
            }
        }
        if (binval == null || mimetype == null) {
            return
        }
        vCard.setAvatar(binval, mimetype)
    }

    companion object {
        // @formatter:off
        private val ADR = arrayOf(
                "POSTAL",
                "PARCEL",
                "DOM",
                "INTL",
                "PREF",
                "POBOX",
                "EXTADR",
                "STREET",
                "LOCALITY",
                "REGION",
                "PCODE",
                "CTRY",
                "FF")
        private val TEL = arrayOf(
                "VOICE",
                "FAX",
                "PAGER",
                "MSG",
                "CELL",
                "VIDEO",
                "BBS",
                "MODEM",
                "ISDN",
                "PCS",
                "PREF")
    }

}