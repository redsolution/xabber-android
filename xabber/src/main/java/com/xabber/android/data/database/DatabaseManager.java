package com.xabber.android.data.database;

import android.os.Looper;
import android.util.Log;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnClearListener;
import com.xabber.android.data.OnCloseListener;
import com.xabber.android.data.database.realmobjects.AccountRealmObject;
import com.xabber.android.data.database.realmobjects.EmailRealmObject;
import com.xabber.android.data.database.realmobjects.PatreonGoalRealmObject;
import com.xabber.android.data.database.realmobjects.PatreonRealmObject;
import com.xabber.android.data.database.realmobjects.SocialBindingRealmObject;
import com.xabber.android.data.database.realmobjects.SyncStateRealmObject;
import com.xabber.android.data.database.realmobjects.XMPPUserRealmObject;
import com.xabber.android.data.database.realmobjects.XabberAccountRealmObject;
import com.xabber.android.data.log.LogManager;

import java.util.UUID;

import io.realm.DynamicRealm;
import io.realm.DynamicRealmObject;
import io.realm.FieldAttribute;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;
import io.realm.RealmObjectSchema;
import io.realm.RealmSchema;

public class DatabaseManager implements OnClearListener, OnCloseListener {

    private static final int CURRENT_DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "realm_db_xabber";
    private static final String LOG_TAG = DatabaseManager.class.getSimpleName();

    private static DatabaseManager instance;
    private RealmConfiguration realmConfiguration;

    private Realm realmInstanceInUI;

    private DatabaseManager() {
        Realm.init(Application.getInstance().getApplicationContext());
        realmConfiguration = createRealmConfiguration();
        Realm.setDefaultConfiguration(realmConfiguration);
    }

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Realm getDefaultRealmInstance() {
        Realm result;

        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (realmInstanceInUI == null) realmInstanceInUI = Realm.getInstance(Realm.getDefaultConfiguration());
            result = realmInstanceInUI;
        } else {
            result = Realm.getDefaultInstance();
        }

        return result;
    }

    @Override
    public void onClear() {
        deleteRealmDatabase();
    }

    @Override
    public void onClose() {
        Realm.compactRealm(Realm.getDefaultConfiguration());
    }

    private RealmConfiguration createRealmConfiguration() {
        return new RealmConfiguration.Builder()
                .name(DATABASE_NAME)
                .schemaVersion(CURRENT_DATABASE_VERSION + 1) // Set to 2
                .migration(new RealmMigration() {
                    @Override
                    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
                        RealmSchema schema = realm.getSchema();

                        // Handle migrations from any version (0, 1, or older versions) to version 2
                        if (oldVersion <= 1) {
                            // Step 1: Rename existing classes to match new schema
                            if (schema.contains("AccountRealm")) schema.rename("AccountRealm", "AccountRealmObject");
                            if (schema.contains("EmailRealm")) schema.rename("EmailRealm", "EmailRealmObject");
                            if (schema.contains("PatreonGoalRealm")) schema.rename("PatreonGoalRealm", "PatreonGoalRealmObject");
                            if (schema.contains("PatreonRealm")) schema.rename("PatreonRealm", "PatreonRealmObject");
                            if (schema.contains("SyncStateRealm")) schema.rename("SyncStateRealm", "SyncStateRealmObject");
                            if (schema.contains("XMPPUserRealm")) schema.rename("XMPPUserRealm", "XMPPUserRealmObject");
                            if (schema.contains("XabberAccountRealm")) schema.rename("XabberAccountRealm", "XabberAccountRealmObject");
                            if (schema.contains("DiscoveryInfoCache")) schema.rename("DiscoveryInfoCache", "DiscoveryInfoRealmObject");
                            if (schema.contains("NotifChatRealm")) schema.rename("NotifChatRealm", "NotificationChatRealmObject");
                            if (schema.contains("NotifMessageRealm")) schema.rename("NotifMessageRealm", "NotificationMessageRealmObject");

                            // Step 2: Remove obsolete classes if they exist
                            String[] obsoleteClasses = {
                                    "UploadServer",
                                    "ChatDataRealm",
                                    "NotificationStateRealm",
                                    "GroupchatUserRealm",
                                    "PushLogRecord",
                                    "XTokenRealm",
                                    "CrowdfundingMessage"
                            };
                            for (String className : obsoleteClasses) {
                                if (schema.contains(className)) {
                                    schema.remove(className);
                                }
                            }

                            // Step 3: Create or update classes to match the new schema
                            // AccountRealmObject
                            if (!schema.contains("AccountRealmObject")) {
                                schema.create("AccountRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("enabled", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("serverName", String.class)
                                        .addField("uploadServer", String.class)
                                        .addRealmListField("groupServers", String.class)
                                        .addRealmListField("customGroupServers", String.class)
                                        .addField("userName", String.class)
                                        .addField("resource", String.class)
                                        .addField("custom", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("host", String.class)
                                        .addField("port", int.class, FieldAttribute.REQUIRED)
                                        .addField("storePassword", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("password", String.class)
                                        .addField("token", String.class)
                                        .addRealmObjectField("device", schema.get("DeviceRealmObject"))
                                        .addField("colorIndex", int.class, FieldAttribute.REQUIRED)
                                        .addField("timestamp", int.class, FieldAttribute.REQUIRED)
                                        .addField("order", int.class, FieldAttribute.REQUIRED)
                                        .addField("syncNotAllowed", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("xabberAutoLoginEnabled", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("priority", int.class, FieldAttribute.REQUIRED)
                                        .addField("statusMode", String.class)
                                        .addField("statusText", String.class)
                                        .addField("saslEnabled", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("tlsMode", String.class)
                                        .addField("compression", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("proxyType", String.class)
                                        .addField("proxyHost", String.class)
                                        .addField("proxyPort", int.class, FieldAttribute.REQUIRED)
                                        .addField("proxyUser", String.class)
                                        .addField("proxyPassword", String.class)
                                        .addField("syncable", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("publicKeyBytes", byte[].class)
                                        .addField("privateKeyBytes", byte[].class)
                                        .addField("archiveMode", String.class)
                                        .addField("clearHistoryOnExit", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("mamDefaultBehavior", String.class)
                                        .addField("loadHistorySettings", String.class)
                                        .addField("retractVersion", String.class)
                                        .addField("startHistoryTimestamp", long.class, FieldAttribute.REQUIRED)
                                        .addField("successfulConnectionHappened", boolean.class, FieldAttribute.REQUIRED);
                            } else {
                                RealmObjectSchema accountSchema = schema.get("AccountRealmObject");
                                if (!accountSchema.hasField("uploadServer")) accountSchema.addField("uploadServer", String.class);
                                if (!accountSchema.hasField("groupServers")) accountSchema.addRealmListField("groupServers", String.class);
                                if (!accountSchema.hasField("customGroupServers")) accountSchema.addRealmListField("customGroupServers", String.class);
                                if (!accountSchema.hasField("device") && schema.contains("DeviceRealmObject")) accountSchema.addRealmObjectField("device", schema.get("DeviceRealmObject"));
                                if (!accountSchema.hasField("retractVersion")) accountSchema.addField("retractVersion", String.class);
                                if (!accountSchema.hasField("startHistoryTimestamp")) accountSchema.addField("startHistoryTimestamp", long.class, FieldAttribute.REQUIRED);
                                String[] fieldsToRemove = {"xToken", "lastSync", "pushNode", "pushServiceJid", "pushEnabled", "pushWasEnabled"};
                                for (String field : fieldsToRemove) {
                                    if (accountSchema.hasField(field)) accountSchema.removeField(field);
                                }
                                // Ensure required constraints only if not already set
                                if (!accountSchema.isRequired("enabled")) accountSchema.setRequired("enabled", true);
                                if (!accountSchema.isRequired("custom")) accountSchema.setRequired("custom", true);
                                if (!accountSchema.isRequired("port")) accountSchema.setRequired("port", true);
                                if (!accountSchema.isRequired("storePassword")) accountSchema.setRequired("storePassword", true);
                                if (!accountSchema.isRequired("colorIndex")) accountSchema.setRequired("colorIndex", true);
                                if (!accountSchema.isRequired("timestamp")) accountSchema.setRequired("timestamp", true);
                                if (!accountSchema.isRequired("order")) accountSchema.setRequired("order", true);
                                if (!accountSchema.isRequired("syncNotAllowed")) accountSchema.setRequired("syncNotAllowed", true);
                                if (!accountSchema.isRequired("xabberAutoLoginEnabled")) accountSchema.setRequired("xabberAutoLoginEnabled", true);
                                if (!accountSchema.isRequired("priority")) accountSchema.setRequired("priority", true);
                                if (!accountSchema.isRequired("saslEnabled")) accountSchema.setRequired("saslEnabled", true);
                                if (!accountSchema.isRequired("compression")) accountSchema.setRequired("compression", true);
                                if (!accountSchema.isRequired("proxyPort")) accountSchema.setRequired("proxyPort", true);
                                if (!accountSchema.isRequired("syncable")) accountSchema.setRequired("syncable", true);
                                if (!accountSchema.isRequired("clearHistoryOnExit")) accountSchema.setRequired("clearHistoryOnExit", true);
                                if (!accountSchema.isRequired("successfulConnectionHappened")) accountSchema.setRequired("successfulConnectionHappened", true);
                                if (!accountSchema.isRequired("startHistoryTimestamp") && accountSchema.hasField("startHistoryTimestamp")) accountSchema.setRequired("startHistoryTimestamp", true);
                            }

                            // AvatarRealmObject
                            if (!schema.contains("AvatarRealmObject")) {
                                schema.create("AvatarRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY)
                                        .addField("accountJid", String.class)
                                        .addField("contactJid", String.class)
                                        .addField("vCardHash", String.class)
                                        .addField("pepHash", String.class);
                            }

                            // CircleRealmObject
                            if (!schema.contains("CircleRealmObject")) {
                                schema.create("CircleRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("circleName", String.class)
                                        .addRealmListField("contacts", String.class)
                                        .addField("expanded", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("offline", int.class, FieldAttribute.REQUIRED);
                            } else {
                                RealmObjectSchema circleSchema = schema.get("CircleRealmObject");
                                if (!circleSchema.isRequired("expanded")) circleSchema.setRequired("expanded", true);
                                if (!circleSchema.isRequired("offline")) circleSchema.setRequired("offline", true);
                            }

                            // ContactRealmObject
                            if (!schema.contains("ContactRealmObject")) {
                                schema.create("ContactRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY)
                                        .addField("accountJid", String.class)
                                        .addField("contactJid", String.class)
                                        .addField("bestName", String.class)
                                        .addRealmListField("chats", schema.get("RegularChatRealmObject"))
                                        .addRealmListField("avatars", schema.get("AvatarRealmObject"))
                                        .addRealmListField("resources", schema.get("ResourceRealmObject"))
                                        .addRealmListField("circles", schema.get("CircleRealmObject"));
                            }

                            // DeviceRealmObject
                            if (!schema.contains("DeviceRealmObject")) {
                                schema.create("DeviceRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("token", String.class)
                                        .addField("expire", long.class, FieldAttribute.REQUIRED)
                                        .addField("counter", int.class, FieldAttribute.REQUIRED);
                            } else {
                                RealmObjectSchema deviceSchema = schema.get("DeviceRealmObject");
                                if (!deviceSchema.isRequired("expire")) deviceSchema.setRequired("expire", true);
                                if (!deviceSchema.isRequired("counter")) deviceSchema.setRequired("counter", true);
                            }

                            // ForwardIdRealmObject
                            if (!schema.contains("ForwardIdRealmObject")) {
                                schema.create("ForwardIdRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("forwardMessageId", String.class);
                            }

                            // GroupInviteRealmObject
                            if (!schema.contains("GroupInviteRealmObject")) {
                                schema.create("GroupInviteRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY)
                                        .addField("accountJid", String.class)
                                        .addField("groupJid", String.class)
                                        .addField("senderJid", String.class)
                                        .addField("date", long.class, FieldAttribute.REQUIRED)
                                        .addField("reason", String.class)
                                        .addField("isIncoming", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("isAccepted", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("isDeclined", boolean.class, FieldAttribute.REQUIRED);
                            } else {
                                RealmObjectSchema groupInviteSchema = schema.get("GroupInviteRealmObject");
                                if (!groupInviteSchema.isRequired("date")) groupInviteSchema.setRequired("date", true);
                                if (!groupInviteSchema.isRequired("isIncoming")) groupInviteSchema.setRequired("isIncoming", true);
                                if (!groupInviteSchema.isRequired("isAccepted")) groupInviteSchema.setRequired("isAccepted", true);
                                if (!groupInviteSchema.isRequired("isDeclined")) groupInviteSchema.setRequired("isDeclined", true);
                            }

                            // GroupMemberRealmObject
                            if (!schema.contains("GroupMemberRealmObject")) {
                                schema.create("GroupMemberRealmObject")
                                        .addField("primaryKey", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("memberId", String.class)
                                        .addField("accountJid", String.class)
                                        .addField("groupJid", String.class)
                                        .addField("jid", String.class)
                                        .addField("nickname", String.class)
                                        .addField("role", String.class)
                                        .addField("badge", String.class)
                                        .addField("avatarHash", String.class)
                                        .addField("avatarUrl", String.class)
                                        .addField("lastSeen", String.class)
                                        .addField("subscriptionState", String.class)
                                        .addField("isMe", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("isBlocked", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("isKicked", boolean.class, FieldAttribute.REQUIRED);
                            } else {
                                RealmObjectSchema groupMemberSchema = schema.get("GroupMemberRealmObject");
                                if (!groupMemberSchema.isRequired("isMe")) groupMemberSchema.setRequired("isMe", true);
                                if (!groupMemberSchema.isRequired("isBlocked")) groupMemberSchema.setRequired("isBlocked", true);
                                if (!groupMemberSchema.isRequired("isKicked")) groupMemberSchema.setRequired("isKicked", true);
                            }

                            // GroupchatRealmObject
                            if (!schema.contains("GroupchatRealmObject")) {
                                schema.create("GroupchatRealmObject")
                                        .addField("primary", String.class, FieldAttribute.PRIMARY_KEY)
                                        .addField("groupchatJid", String.class)
                                        .addField("accountJid", String.class)
                                        .addField("owner", String.class)
                                        .addField("name", String.class)
                                        .addField("privacy", String.class)
                                        .addField("index", String.class)
                                        .addField("membership", String.class)
                                        .addField("description", String.class)
                                        .addField("pinnedMessageId", String.class)
                                        .addField("membersListVersion", String.class)
                                        .addField("membersCount", int.class, FieldAttribute.REQUIRED)
                                        .addField("present", int.class, FieldAttribute.REQUIRED)
                                        .addField("collect", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("peerToPeer", boolean.class, FieldAttribute.REQUIRED)
                                        .addRealmListField("domains", String.class)
                                        .addRealmListField("invited", String.class)
                                        .addField("status", String.class)
                                        .addField("resource", String.class)
                                        .addField("notificationMode", String.class)
                                        .addField("notificationTimestamp", long.class, FieldAttribute.REQUIRED)
                                        .addField("retractVersion", String.class)
                                        .addField("lastPosition", int.class, FieldAttribute.REQUIRED);
                            } else {
                                RealmObjectSchema groupchatSchema = schema.get("GroupchatRealmObject");
                                if (!groupchatSchema.isRequired("membersCount")) groupchatSchema.setRequired("membersCount", true);
                                if (!groupchatSchema.isRequired("present")) groupchatSchema.setRequired("present", true);
                                if (!groupchatSchema.isRequired("collect")) groupchatSchema.setRequired("collect", true);
                                if (!groupchatSchema.isRequired("peerToPeer")) groupchatSchema.setRequired("peerToPeer", true);
                                if (!groupchatSchema.isRequired("notificationTimestamp")) groupchatSchema.setRequired("notificationTimestamp", true);
                                if (!groupchatSchema.isRequired("lastPosition")) groupchatSchema.setRequired("lastPosition", true);
                            }

                            // MessageRealmObject
                            if (!schema.contains("MessageRealmObject")) {
                                schema.create("MessageRealmObject")
                                        .addField("primaryKey", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("account", String.class)
                                        .addField("user", String.class)
                                        .addField("resource", String.class)
                                        .addField("text", String.class)
                                        .addField("markupText", String.class)
                                        .addField("action", String.class)
                                        .addField("incoming", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("timestamp", long.class)
                                        .addField("editedTimestamp", long.class)
                                        .addField("delayTimestamp", long.class)
                                        .addField("messageStatus", String.class)
                                        .addField("read", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("errorDescription", String.class)
                                        .addField("stanzaId", String.class)
                                        .addField("originId", String.class)
                                        .addField("forwarded", boolean.class, FieldAttribute.REQUIRED)
                                        .addRealmListField("referenceRealmObjects", schema.get("ReferenceRealmObject"))
                                        .addField("originalStanza", String.class)
                                        .addField("originalFrom", String.class)
                                        .addField("parentMessageId", String.class)
                                        .addField("groupchatUserId", String.class)
                                        .addField("isGroupchatSystem", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("isRegularReceived", boolean.class, FieldAttribute.REQUIRED)
                                        .addRealmListField("forwardedIds", schema.get("ForwardIdRealmObject"));
                            } else {
                                RealmObjectSchema messageSchema = schema.get("MessageRealmObject");
                                if (!messageSchema.isRequired("incoming")) messageSchema.setRequired("incoming", true);
                                if (!messageSchema.isRequired("read")) messageSchema.setRequired("read", true);
                                if (!messageSchema.isRequired("forwarded")) messageSchema.setRequired("forwarded", true);
                                if (!messageSchema.isRequired("isGroupchatSystem")) messageSchema.setRequired("isGroupchatSystem", true);
                                if (!messageSchema.isRequired("isRegularReceived")) messageSchema.setRequired("isRegularReceived", true);
                            }

                            // MessageWithoutReceiptRealmObject
                            if (!schema.contains("MessageWithoutReceiptRealmObject")) {
                                schema.create("MessageWithoutReceiptRealmObject")
                                        .addField("messageOriginId", String.class);
                            }

                            // NotificationChatRealmObject
                            if (!schema.contains("NotificationChatRealmObject")) {
                                schema.create("NotificationChatRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("account", String.class)
                                        .addField("user", String.class)
                                        .addField("notificationID", int.class, FieldAttribute.REQUIRED)
                                        .addField("chatTitle", String.class)
                                        .addField("isGroupChat", boolean.class, FieldAttribute.REQUIRED)
                                        .addRealmListField("messages", schema.get("NotificationMessageRealmObject"))
                                        .addField("privacyType", String.class);
                            } else {
                                RealmObjectSchema notificationChatSchema = schema.get("NotificationChatRealmObject");
                                if (!notificationChatSchema.hasField("privacyType")) {
                                    notificationChatSchema.addField("privacyType", String.class);
                                }
                                if (!notificationChatSchema.isRequired("notificationID")) notificationChatSchema.setRequired("notificationID", true);
                                if (!notificationChatSchema.isRequired("isGroupChat")) notificationChatSchema.setRequired("isGroupChat", true);
                            }

                            // NotificationMessageRealmObject
                            if (!schema.contains("NotificationMessageRealmObject")) {
                                schema.create("NotificationMessageRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("author", String.class)
                                        .addField("text", String.class)
                                        .addField("timestamp", long.class, FieldAttribute.REQUIRED)
                                        .addField("memberId", String.class);
                            } else {
                                RealmObjectSchema notificationMessageSchema = schema.get("NotificationMessageRealmObject");
                                if (!notificationMessageSchema.hasField("memberId")) {
                                    notificationMessageSchema.addField("memberId", String.class);
                                }
                                if (!notificationMessageSchema.isRequired("timestamp")) notificationMessageSchema.setRequired("timestamp", true);
                            }

                            // PhraseNotificationRealmObject
                            if (!schema.contains("PhraseNotificationRealmObject")) {
                                schema.create("PhraseNotificationRealmObject")
                                        .addField("id", int.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("value", String.class)
                                        .addField("user", String.class)
                                        .addField("group", String.class)
                                        .addField("regexp", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("sound", String.class);
                            } else {
                                RealmObjectSchema phraseNotificationSchema = schema.get("PhraseNotificationRealmObject");
                                if (!phraseNotificationSchema.isRequired("regexp")) phraseNotificationSchema.setRequired("regexp", true);
                            }

                            // RecentSearchRealmObject
                            if (!schema.contains("RecentSearchRealmObject")) {
                                schema.create("RecentSearchRealmObject")
                                        .addField("uuid", String.class, FieldAttribute.PRIMARY_KEY)
                                        .addField("accountJid", String.class)
                                        .addField("contactJid", String.class)
                                        .addField("timestamp", long.class);
                            }

                            // ReferenceRealmObject
                            if (!schema.contains("ReferenceRealmObject")) {
                                schema.create("ReferenceRealmObject")
                                        .addField("uniqueId", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("title", String.class)
                                        .addField("fileUrl", String.class)
                                        .addField("filePath", String.class)
                                        .addField("isImage", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("isVoice", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("isGeo", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("imageWidth", int.class)
                                        .addField("imageHeight", int.class)
                                        .addField("fileSize", int.class)
                                        .addField("mimeType", String.class)
                                        .addField("duration", int.class)
                                        .addField("longitude", double.class, FieldAttribute.REQUIRED)
                                        .addField("latitude", double.class, FieldAttribute.REQUIRED);
                            } else {
                                RealmObjectSchema referenceSchema = schema.get("ReferenceRealmObject");
                                if (!referenceSchema.isRequired("isImage")) referenceSchema.setRequired("isImage", true);
                                if (!referenceSchema.isRequired("isVoice")) referenceSchema.setRequired("isVoice", true);
                                if (!referenceSchema.isRequired("isGeo")) referenceSchema.setRequired("isGeo", true);
                                if (!referenceSchema.isRequired("longitude")) referenceSchema.setRequired("longitude", true);
                                if (!referenceSchema.isRequired("latitude")) referenceSchema.setRequired("latitude", true);
                            }

                            // RegularChatRealmObject
                            if (!schema.contains("RegularChatRealmObject")) {
                                schema.create("RegularChatRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY)
                                        .addField("accountJid", String.class)
                                        .addField("contactJid", String.class)
                                        .addRealmObjectField("lastMessage", schema.get("MessageRealmObject"))
                                        .addField("lastMessageTimestamp", long.class)
                                        .addField("isArchived", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("isBlocked", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("unreadMessagesCount", int.class, FieldAttribute.REQUIRED)
                                        .addField("lastPosition", int.class, FieldAttribute.REQUIRED)
                                        .addField("notificationMode", String.class)
                                        .addField("notificationTimestamp", long.class, FieldAttribute.REQUIRED);
                            } else {
                                RealmObjectSchema regularChatSchema = schema.get("RegularChatRealmObject");
                                if (!regularChatSchema.isRequired("isArchived")) regularChatSchema.setRequired("isArchived", true);
                                if (!regularChatSchema.isRequired("isBlocked")) regularChatSchema.setRequired("isBlocked", true);
                                if (!regularChatSchema.isRequired("unreadMessagesCount")) regularChatSchema.setRequired("unreadMessagesCount", true);
                                if (!regularChatSchema.isRequired("lastPosition")) regularChatSchema.setRequired("lastPosition", true);
                                if (!regularChatSchema.isRequired("notificationTimestamp")) regularChatSchema.setRequired("notificationTimestamp", true);
                            }

                            // ResourceRealmObject
                            if (!schema.contains("ResourceRealmObject")) {
                                schema.create("ResourceRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY);
                            }

                            // StatusRealmObject
                            if (!schema.contains("StatusRealmObject")) {
                                schema.create("StatusRealmObject")
                                        .addField("statusMode", String.class)
                                        .addField("statusText", String.class);
                            }

                            // VCardRealmObject
                            if (!schema.contains("VCardRealmObject")) {
                                schema.create("VCardRealmObject")
                                        .addField("contactJid", String.class)
                                        .addField("vCardString", String.class)
                                        .addField("nickName", String.class)
                                        .addField("formattedName", String.class)
                                        .addField("firstName", String.class)
                                        .addField("lastName", String.class)
                                        .addField("middleName", String.class);
                            }

                            // NotifyPrefsRealm
                            if (!schema.contains("NotifyPrefsRealm")) {
                                schema.create("NotifyPrefsRealm")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("channelID", String.class)
                                        .addField("type", String.class)
                                        .addField("account", String.class)
                                        .addField("user", String.class)
                                        .addField("group", String.class)
                                        .addField("phraseID", long.class)
                                        .addField("vibro", String.class)
                                        .addField("showPreview", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("sound", String.class);
                            } else {
                                RealmObjectSchema notifyPrefsSchema = schema.get("NotifyPrefsRealm");
                                if (!notifyPrefsSchema.isRequired("showPreview")) notifyPrefsSchema.setRequired("showPreview", true);
                            }

                            // EmailRealmObject
                            if (!schema.contains("EmailRealmObject")) {
                                schema.create("EmailRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("email", String.class)
                                        .addField("verified", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("primary", boolean.class, FieldAttribute.REQUIRED);
                            } else {
                                RealmObjectSchema emailSchema = schema.get("EmailRealmObject");
                                if (!emailSchema.isRequired("verified")) emailSchema.setRequired("verified", true);
                                if (!emailSchema.isRequired("primary")) emailSchema.setRequired("primary", true);
                            }

                            // PatreonGoalRealmObject
                            if (!schema.contains("PatreonGoalRealmObject")) {
                                schema.create("PatreonGoalRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("title", String.class)
                                        .addField("goal", int.class, FieldAttribute.REQUIRED);
                            } else {
                                RealmObjectSchema patreonGoalSchema = schema.get("PatreonGoalRealmObject");
                                if (!patreonGoalSchema.isRequired("goal")) patreonGoalSchema.setRequired("goal", true);
                            }

                            // PatreonRealmObject
                            if (!schema.contains("PatreonRealmObject")) {
                                schema.create("PatreonRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("string", String.class)
                                        .addField("pledged", int.class, FieldAttribute.REQUIRED)
                                        .addRealmListField("goals", schema.get("PatreonGoalRealmObject"));
                            } else {
                                RealmObjectSchema patreonSchema = schema.get("PatreonRealmObject");
                                if (!patreonSchema.isRequired("pledged")) patreonSchema.setRequired("pledged", true);
                            }

                            // SyncStateRealmObject
                            if (!schema.contains("SyncStateRealmObject")) {
                                schema.create("SyncStateRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("jid", String.class)
                                        .addField("sync", boolean.class, FieldAttribute.REQUIRED);
                            } else {
                                RealmObjectSchema syncStateSchema = schema.get("SyncStateRealmObject");
                                if (!syncStateSchema.isRequired("sync")) syncStateSchema.setRequired("sync", true);
                            }

                            // XMPPUserRealmObject
                            if (!schema.contains("XMPPUserRealmObject")) {
                                schema.create("XMPPUserRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("username", String.class)
                                        .addField("host", String.class)
                                        .addField("registration_date", String.class);
                            }

                            // XabberAccountRealmObject
                            if (!schema.contains("XabberAccountRealmObject")) {
                                schema.create("XabberAccountRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("accountStatus", String.class)
                                        .addField("token", String.class)
                                        .addField("username", String.class)
                                        .addField("domain", String.class)
                                        .addField("firstName", String.class)
                                        .addField("lastName", String.class)
                                        .addField("registerDate", String.class)
                                        .addField("language", String.class)
                                        .addField("phone", String.class)
                                        .addField("needToVerifyPhone", boolean.class, FieldAttribute.REQUIRED)
                                        .addField("hasPassword", boolean.class, FieldAttribute.REQUIRED)
                                        .addRealmListField("xmppUsers", schema.get("XMPPUserRealmObject"))
                                        .addRealmListField("emails", schema.get("EmailRealmObject"))
                                        .addRealmListField("socialBindings", schema.get("SocialBindingRealmObject"));
                            } else {
                                RealmObjectSchema xabberAccountSchema = schema.get("XabberAccountRealmObject");
                                if (!xabberAccountSchema.isRequired("needToVerifyPhone")) xabberAccountSchema.setRequired("needToVerifyPhone", true);
                                if (!xabberAccountSchema.isRequired("hasPassword")) xabberAccountSchema.setRequired("hasPassword", true);
                            }

                            // DiscoveryInfoRealmObject
                            if (!schema.contains("DiscoveryInfoRealmObject")) {
                                schema.create("DiscoveryInfoRealmObject")
                                        .addField("nodeVer", String.class)
                                        .addField("discoveryInfoXml", String.class, FieldAttribute.REQUIRED);
                            } else {
                                RealmObjectSchema discoveryInfoSchema = schema.get("DiscoveryInfoRealmObject");
                                if (!discoveryInfoSchema.isRequired("discoveryInfoXml")) discoveryInfoSchema.setRequired("discoveryInfoXml", true);
                            }

                            // SocialBindingRealmObject
                            if (!schema.contains("SocialBindingRealmObject")) {
                                schema.create("SocialBindingRealmObject")
                                        .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                        .addField("provider", String.class)
                                        .addField("uid", String.class)
                                        .addField("firstName", String.class)
                                        .addField("lastName", String.class);
                            }

                            // Step 4: Data migration
                            // Migrate XTokenRealm to DeviceRealmObject
                            if (schema.contains("XTokenRealm")) {
                                for (DynamicRealmObject xToken : realm.where("XTokenRealm").findAll()) {
                                    if (!schema.contains("DeviceRealmObject")) {
                                        schema.create("DeviceRealmObject")
                                                .addField("id", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                                .addField("token", String.class)
                                                .addField("expire", long.class, FieldAttribute.REQUIRED)
                                                .addField("counter", int.class, FieldAttribute.REQUIRED);
                                    }
                                    DynamicRealmObject device = realm.createObject("DeviceRealmObject", xToken.getString("id"));
                                    device.setString("token", xToken.getString("token"));
                                    device.setLong("expire", xToken.getLong("expire"));
                                    device.setInt("counter", 0); // Default value
                                }
                                schema.remove("XTokenRealm");
                            }

                            // Migrate GroupchatUserRealm to GroupMemberRealmObject
                            if (schema.contains("GroupchatUserRealm")) {
                                for (DynamicRealmObject user : realm.where("GroupchatUserRealm").findAll()) {
                                    if (!schema.contains("GroupMemberRealmObject")) {
                                        schema.create("GroupMemberRealmObject")
                                                .addField("primaryKey", String.class, FieldAttribute.PRIMARY_KEY, FieldAttribute.REQUIRED)
                                                .addField("memberId", String.class)
                                                .addField("accountJid", String.class)
                                                .addField("groupJid", String.class)
                                                .addField("jid", String.class)
                                                .addField("nickname", String.class)
                                                .addField("role", String.class)
                                                .addField("badge", String.class)
                                                .addField("avatarHash", String.class)
                                                .addField("avatarUrl", String.class)
                                                .addField("lastSeen", String.class)
                                                .addField("subscriptionState", String.class)
                                                .addField("isMe", boolean.class, FieldAttribute.REQUIRED)
                                                .addField("isBlocked", boolean.class, FieldAttribute.REQUIRED)
                                                .addField("isKicked", boolean.class, FieldAttribute.REQUIRED);
                                    }
                                    DynamicRealmObject member = realm.createObject("GroupMemberRealmObject", user.getString("uniqueId"));
                                    member.setString("jid", user.getString("jid"));
                                    member.setString("nickname", user.getString("nickname"));
                                    member.setString("role", user.getString("role"));
                                    member.setString("badge", user.getString("badge"));
                                    member.setString("avatarUrl", user.getString("avatar"));
                                    member.setBoolean("isMe", false);
                                    member.setBoolean("isBlocked", false);
                                    member.setBoolean("isKicked", false);
                                }
                                schema.remove("GroupchatUserRealm");
                            }

                            // Migrate ChatDataRealm to RegularChatRealmObject and GroupchatRealmObject
                            if (schema.contains("ChatDataRealm")) {
                                for (DynamicRealmObject chatData : realm.where("ChatDataRealm").findAll()) {
                                    boolean isGroupChat = chatData.getString("userJid").contains("/"); // Heuristic
                                    if (isGroupChat) {
                                        if (!schema.contains("GroupchatRealmObject")) {
                                            schema.create("GroupchatRealmObject")
                                                    .addField("primary", String.class, FieldAttribute.PRIMARY_KEY)
                                                    .addField("groupchatJid", String.class)
                                                    .addField("accountJid", String.class)
                                                    .addField("owner", String.class)
                                                    .addField("name", String.class)
                                                    .addField("privacy", String.class)
                                                    .addField("index", String.class)
                                                    .addField("membership", String.class)
                                                    .addField("description", String.class)
                                                    .addField("pinnedMessageId", String.class)
                                                    .addField("membersListVersion", String.class)
                                                    .addField("membersCount", int.class, FieldAttribute.REQUIRED)
                                                    .addField("present", int.class, FieldAttribute.REQUIRED)
                                                    .addField("collect", boolean.class, FieldAttribute.REQUIRED)
                                                    .addField("peerToPeer", boolean.class, FieldAttribute.REQUIRED)
                                                    .addRealmListField("domains", String.class)
                                                    .addRealmListField("invited", String.class)
                                                    .addField("status", String.class)
                                                    .addField("resource", String.class)
                                                    .addField("notificationMode", String.class)
                                                    .addField("notificationTimestamp", long.class, FieldAttribute.REQUIRED)
                                                    .addField("retractVersion", String.class)
                                                    .addField("lastPosition", int.class, FieldAttribute.REQUIRED);
                                        }
                                        DynamicRealmObject groupchat = realm.createObject("GroupchatRealmObject", UUID.randomUUID().toString());
                                        groupchat.setString("groupchatJid", chatData.getString("userJid"));
                                        groupchat.setString("accountJid", chatData.getString("accountJid"));
                                        groupchat.setInt("membersCount", 0);
                                        groupchat.setInt("present", 0);
                                        groupchat.setBoolean("collect", false);
                                        groupchat.setBoolean("peerToPeer", false);
                                        groupchat.setLong("notificationTimestamp", 0L);
                                        groupchat.setInt("lastPosition", chatData.getInt("lastPosition"));
                                    } else {
                                        if (!schema.contains("RegularChatRealmObject")) {
                                            schema.create("RegularChatRealmObject")
                                                    .addField("id", String.class, FieldAttribute.PRIMARY_KEY)
                                                    .addField("accountJid", String.class)
                                                    .addField("contactJid", String.class)
                                                    .addRealmObjectField("lastMessage", schema.get("MessageRealmObject"))
                                                    .addField("lastMessageTimestamp", long.class)
                                                    .addField("isArchived", boolean.class, FieldAttribute.REQUIRED)
                                                    .addField("isBlocked", boolean.class, FieldAttribute.REQUIRED)
                                                    .addField("unreadMessagesCount", int.class, FieldAttribute.REQUIRED)
                                                    .addField("lastPosition", int.class, FieldAttribute.REQUIRED)
                                                    .addField("notificationMode", String.class)
                                                    .addField("notificationTimestamp", long.class, FieldAttribute.REQUIRED);
                                        }
                                        DynamicRealmObject regularChat = realm.createObject("RegularChatRealmObject", chatData.getString("id"));
                                        regularChat.setString("accountJid", chatData.getString("accountJid"));
                                        regularChat.setString("contactJid", chatData.getString("userJid"));
                                        regularChat.setBoolean("isArchived", chatData.getBoolean("archived"));
                                        regularChat.setBoolean("isBlocked", false);
                                        regularChat.setInt("unreadMessagesCount", chatData.getInt("unreadCount"));
                                        regularChat.setInt("lastPosition", chatData.getInt("lastPosition"));
                                        regularChat.setLong("notificationTimestamp", 0L);
                                    }
                                }
                                schema.remove("ChatDataRealm");
                            }

                            oldVersion = 2; // Set to the new schema version
                        }
                    }
                })
                .build();
    }

    private void deleteRealmDatabase() {
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                Realm.deleteRealm(realm.getConfiguration());
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

    /**
     * Logs the schema details of the Realm database, including all classes and their fields.
     */
    public void logSchemaDetails() {
        Log.d(LOG_TAG, "zero zero zero zero");

        Application.getInstance().runInBackground(() -> {
            Log.d(LOG_TAG, "one one one one one");
            DynamicRealm dynamicRealm = null;
            Log.d(LOG_TAG, "two two two two");

            try {
                dynamicRealm = DynamicRealm.getInstance(realmConfiguration);
                Log.d(LOG_TAG, "three three three");
                RealmSchema schema = dynamicRealm.getSchema();
                Log.i(LOG_TAG, "Database Schema (Version: " + dynamicRealm.getVersion() + ")");
                for (RealmObjectSchema objectSchema : schema.getAll()) {
                    Log.i(LOG_TAG, "Class: " + objectSchema.getClassName());
                    for (String fieldName : objectSchema.getFieldNames()) {
                        String fieldType = objectSchema.getFieldType(fieldName).toString();
                        boolean isPrimaryKey = objectSchema.isPrimaryKey(fieldName);
                        boolean isRequired = objectSchema.isRequired(fieldName);
                        boolean isNullable = objectSchema.isNullable(fieldName);
                        Log.i(LOG_TAG, "Class: " + objectSchema.getClassName() +
                                " Field: " + fieldName +
                                " Type: " + fieldType +
                                " Primary Key: " + isPrimaryKey +
                                " Required: " + isRequired +
                                " Nullable: " + isNullable);
                    }
                }
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (dynamicRealm != null) dynamicRealm.close();
            }
        });
    }
}