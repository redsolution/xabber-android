Xabber - XMPP client for Android
================================

Open source Jabber (XMPP) client with multi-account support, clean and simple interface.
Being both free (as in freedom!) and ad-free, Xabber is designed to be the best Jabber client for Android.

Build instrustions
==================

Xabber uses Gradle build system. The only specific thing is git submodule for otr4j library. To make it work use following commands:

 ::
 
 git submodule init
 
 git submodule update
 
And otr4j would be cloned to your local repository. 

Supported protocols
===================

* RFC-3920: Core
* RFC-3921: Instant Messaging and Presence
* XEP-0030: Service Discovery
* XEP-0128: Service Discovery Extensions
* XEP-0115: Entity Capabilities
* XEP-0054: vcard-temp
* XEP-0153: vCard-Based Avatars
* XEP-0045: Multi-User Chat (incompletely)
* XEP-0078: Non-SASL Authentication
* XEP-0138: Stream Compression
* XEP-0203: Delayed Delivery
* XEP-0091: Legacy Delayed Delivery
* XEP-0199: XMPP Ping
* XEP-0147: XMPP URI Scheme Query Components
* XEP-0085: Chat State Notifications
* XEP-0184: Message Delivery Receipts
* XEP-0155: Stanza Session Negotiation
* XEP-0059: Result Set Management
* XEP-0136: Message Archiving
* XEP-0224: Attention
* XEP-0077: In-Band Registration
* XEP-0352: Client State Indication

Translations
============



We use crowdin.com as our translation system.
All related resources are automatically generated from files got with crowdin.com.
If you want to update any translation go to Xabber page https://crowdin.com/project/xabber and request to join our translation team
Please don't create pull requests with translation fixes as any changes will be overwritten with the next update from crowdin.com.

Wiki
====

Visit our wiki pages for additional information: https://github.com/redsolution/xabber-android/wiki
