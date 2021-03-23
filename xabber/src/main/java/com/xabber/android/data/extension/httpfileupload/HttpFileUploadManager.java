package com.xabber.android.data.extension.httpfileupload;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountRemovedListener;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AccountRealmObject;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.references.ReferencesManager;
import com.xabber.android.data.extension.references.mutable.filesharing.FileInfo;
import com.xabber.android.data.extension.references.mutable.filesharing.FileSharingExtension;
import com.xabber.android.data.extension.references.mutable.filesharing.FileSources;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.service.UploadService;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Domainpart;
import org.jxmpp.jid.parts.Localpart;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import rx.subjects.PublishSubject;

public class HttpFileUploadManager implements OnLoadListener, OnAccountRemovedListener {

    private static final String LOG_TAG = HttpFileUploadManager.class.getSimpleName();

    private static HttpFileUploadManager instance;
    private Map<BareJid, Thread> supportDiscoveryThreads = new ConcurrentHashMap<>();
    private Map<BareJid, Jid> uploadServers = new ConcurrentHashMap<>();
    private PublishSubject<ProgressData> progressSubscribe = PublishSubject.create();
    private boolean isUploading;

    public static HttpFileUploadManager getInstance() {
        if (instance == null) {
            instance = new HttpFileUploadManager();
        }
        return instance;
    }

    public static long getVoiceLength(String filePath) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(filePath);
        String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long duration = 0;
        if (dur != null) {
            duration = Math.round(Long.valueOf(dur) / 1000);
        }
        return duration;
    }

    public static ImageSize getImageSizes(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(new File(filePath).getAbsolutePath(), options);
        int imageHeight;
        int imageWidth;
        if (FileManager.isImageNeededDimensionsFlip(Uri.fromFile(new File(filePath)))) {
            //image is sent as-is, but the BitmapFactory gets dimension sizes without respecting exif orientation,
            //resulting in flipped dimension data between photos themselves and data in the stanza
            imageHeight = options.outWidth;
            imageWidth = options.outHeight;
        } else {
            imageHeight = options.outHeight;
            imageWidth = options.outWidth;
        }
        return new ImageSize(imageHeight, imageWidth);
    }

    public static String getMimeType(String path) {
        String extension = path.substring(path.lastIndexOf(".")).substring(1);
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (type == null || type.isEmpty()) type = "*/*";
        return type;
    }

    public static RealmList<AttachmentRealmObject> parseFileMessage(Stanza packet) {
        RealmList<AttachmentRealmObject> attachmentRealmObjects = new RealmList<>();

        // parsing file references
        List<FileSharingExtension> refMediaList = ReferencesManager.getMediaFromReferences(packet);
        List<FileSharingExtension> refVoiceList = ReferencesManager.getVoiceFromReferences(packet);
        if (!refMediaList.isEmpty()) {
            for (FileSharingExtension media : refMediaList) {
                attachmentRealmObjects.add(refMediaToAttachment(media, false));
            }
        }
        if (!refVoiceList.isEmpty()) {
            for (FileSharingExtension voice : refVoiceList) {
                attachmentRealmObjects.add(refMediaToAttachment(voice, true));
            }
        }

        // parsing data forms
        DataForm dataForm = DataForm.from(packet);
        if (dataForm != null) {

            List<FormField> fields = dataForm.getFields();
            for (FormField field : fields) {
                if (field instanceof ExtendedFormField) {
                    ExtendedFormField.Media media = ((ExtendedFormField) field).getMedia();
                    if (media != null)
                        attachmentRealmObjects.add(mediaToAttachment(media, field.getLabel()));
                }
            }
        }

        if (attachmentRealmObjects.size() == 0) {
            AttachmentRealmObject attachment = messageBodyToAttachment(packet);
            if (attachment != null) attachmentRealmObjects.add(attachment);
        }

        return attachmentRealmObjects;
    }

    private static AttachmentRealmObject refMediaToAttachment(FileSharingExtension sharedFile,
                                                              boolean isVoice) {
        AttachmentRealmObject attachmentRealmObject = new AttachmentRealmObject();
        FileSources fileSources = sharedFile.getFileSources();

        String url = fileSources.getUris().get(0);
        attachmentRealmObject.setFileUrl(url);
        attachmentRealmObject.setIsImage(FileManager.isImageUrl(url));
        attachmentRealmObject.setIsVoice(isVoice);
        //attachmentRealmObject.setRefType(referenceType);

        FileInfo fileInfo = sharedFile.getFileInfo();
        if (fileInfo != null) {
            attachmentRealmObject.setTitle(fileInfo.getName());
            attachmentRealmObject.setMimeType(fileInfo.getMediaType());
            attachmentRealmObject.setDuration(fileInfo.getDuration());
            attachmentRealmObject.setFileSize(fileInfo.getSize());
            if (fileInfo.getHeight() > 0)
                attachmentRealmObject.setImageHeight(fileInfo.getHeight());
            if (fileInfo.getWidth() > 0) attachmentRealmObject.setImageWidth(fileInfo.getWidth());
        }
        return attachmentRealmObject;
    }

    private static AttachmentRealmObject mediaToAttachment(ExtendedFormField.Media media,
                                                           String title) {
        AttachmentRealmObject attachmentRealmObject = new AttachmentRealmObject();
        attachmentRealmObject.setTitle(title);

        try {
            if (media.getWidth() != null && !media.getWidth().isEmpty())
                attachmentRealmObject.setImageWidth(Integer.valueOf(media.getWidth()));

            if (media.getHeight() != null && !media.getHeight().isEmpty())
                attachmentRealmObject.setImageHeight(Integer.valueOf(media.getHeight()));

        } catch (NumberFormatException e) {
            LogManager.exception(LOG_TAG, e);
        }

        ExtendedFormField.Uri uri = media.getUri();
        if (uri != null) {
            attachmentRealmObject.setMimeType(uri.getType());
            attachmentRealmObject.setFileSize(uri.getSize());
            attachmentRealmObject.setDuration(uri.getDuration());
            attachmentRealmObject.setFileUrl(uri.getUri());
            attachmentRealmObject.setIsImage(FileManager.isImageUrl(uri.getUri()));
        }
        return attachmentRealmObject;
    }

    private static AttachmentRealmObject messageBodyToAttachment(Stanza packet) {
        Message message = (Message) packet;
        if (FileManager.isImageUrl(message.getBody())) {
            AttachmentRealmObject bodyAttachment = new AttachmentRealmObject();
            bodyAttachment.setTitle(FileManager.extractFileName(message.getBody()));
            bodyAttachment.setFileUrl(message.getBody());
            bodyAttachment.setIsImage(true);
            bodyAttachment.setMimeType(getMimeType(message.getBody()));
            return bodyAttachment;
        } else return null;
    }

    public PublishSubject<ProgressData> subscribeForProgress() {
        return progressSubscribe;
    }

    @Override
    public void onLoad() {
        loadAllFromRealm(uploadServers);
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        removeFromRealm(accountItem.getAccount().getFullJid().asBareJid());
    }

    public boolean isFileUploadSupported(AccountJid account) {
        if (AccountManager.checkIfSuccessfulConnectionHappened(account)) {
            try {
                return uploadServers.containsKey(account.getFullJid().asBareJid());
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }
        }
        return false;
    }

    public boolean isFileUploadDiscoveryInProgress(AccountJid account) {
        if (AccountManager.checkIfSuccessfulConnectionHappened(account)) {
            Thread discoThread = supportDiscoveryThreads.get(account.getBareJid());
            return discoThread != null && discoThread.getState() != Thread.State.TERMINATED;
        }
        return false;
    }

    public void retrySendFileMessage(final MessageRealmObject messageRealmObject, Context context) {
        List<String> notUploadedFilesPaths = new ArrayList<>();

        for (AttachmentRealmObject attachmentRealmObject : messageRealmObject
                .getAttachmentRealmObjects()) {
            if (attachmentRealmObject.getFileUrl() == null
                    || attachmentRealmObject.getFileUrl().isEmpty())
                notUploadedFilesPaths.add(attachmentRealmObject.getFilePath());
        }

        // if all attachments have url that they was uploaded. just resend existing message
        if (notUploadedFilesPaths.size() == 0) {
            final AccountJid accountJid = messageRealmObject.getAccount();
            final ContactJid contactJid = messageRealmObject.getUser();
            final String messageId = messageRealmObject.getPrimaryKey();
            Application.getInstance().runInBackgroundUserRequest(() -> MessageManager.getInstance()
                    .removeErrorAndResendMessage(accountJid, contactJid, messageId));
        }

        // else, upload files that haven't urls. Then write they in existing message and send
        else uploadFile(messageRealmObject.getAccount(), messageRealmObject.getUser(),
                notUploadedFilesPaths, null, messageRealmObject.getPrimaryKey(), context);
    }

    public void uploadFile(final AccountJid account, final ContactJid user,
                           final List<String> filePaths, Context context) {
        uploadFile(account, user, filePaths, null, null, context);
    }

    public void uploadFileViaUri(final AccountJid account, final ContactJid user,
                                 final List<Uri> fileUris, Context context) {
        uploadFile(account, user, null, fileUris, null, context);
    }

    public void uploadFile(final AccountJid account, final ContactJid user,
                           final List<String> filePaths, final List<Uri> fileUris,
                           String existMessageId, Context context) {
        uploadFile(account, user, filePaths, fileUris, existMessageId, null, context);
    }

    public void uploadFile(final AccountJid account, final ContactJid user,
                           final List<String> filePaths, final List<Uri> fileUris,
                           String existMessageId, String element, Context context) {

        uploadFile(account, user, filePaths, fileUris, null, existMessageId,
                null, context);
    }

    public void uploadFile(final AccountJid account, final ContactJid user,
                           final List<String> filePaths, final List<Uri> fileUris,
                           List<String> forwardIds,
                           String existMessageId, String messageAttachmentType, Context context) {
        if (isUploading) {
            progressSubscribe.onNext(new ProgressData(0, 0,
                    "Uploading already started", false, null));
            return;
        }

        isUploading = true;

        final Jid uploadServerUrl = uploadServers.get(account.getFullJid().asBareJid());
        if (uploadServerUrl == null) {
            progressSubscribe.onNext(new ProgressData(0, 0,
                    "Upload server not found", false, null));
            isUploading = false;
            return;
        }

        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(UploadService.KEY_RECEIVER, new UploadReceiver(new Handler()));
        intent.putExtra(UploadService.KEY_ACCOUNT_JID, (Parcelable) account);
        intent.putExtra(UploadService.KEY_USER_JID, user);
        intent.putExtra(UploadService.KEY_ATTACHMENT_TYPE, messageAttachmentType);
        intent.putStringArrayListExtra(UploadService.KEY_FILE_PATHS, (ArrayList<String>) filePaths);
        intent.putParcelableArrayListExtra(UploadService.KEY_FILE_URIS, (ArrayList<Uri>) fileUris);
        intent.putExtra(UploadService.KEY_UPLOAD_SERVER_URL, (CharSequence) uploadServerUrl);
        intent.putExtra(UploadService.KEY_MESSAGE_ID, existMessageId);
        intent.putStringArrayListExtra(UploadService.KEY_FORWARD_IDS, (ArrayList<String>) forwardIds);
        context.startService(intent);
    }

    public void cancelUpload(Context context) {
        Intent intent = new Intent(context, UploadService.class);
        context.stopService(intent);
    }

    private void discoverSupport(AccountJid account, XMPPConnection xmppConnection)
            throws SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException, InterruptedException {

        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(xmppConnection);

        List<DomainBareJid> services;
        try {
            services = discoManager.findServices(com.xabber.xmpp.httpfileupload.Request.NAMESPACE,
                    true, true);
        } catch (ClassCastException e) {
            services = Collections.emptyList();
            LogManager.exception(this, e);
        }

        if (!services.isEmpty()) {
            final DomainBareJid uploadServerUrl = services.get(0);
            LogManager.i(LOG_TAG, "Http file upload server: " + uploadServerUrl);
            uploadServers.put(account.getFullJid().asBareJid(), uploadServerUrl);
            saveOrUpdateToRealm(account.getFullJid().asBareJid(), uploadServerUrl);
        }
    }

    public void onAuthorized(final ConnectionItem connectionItem) {
        Thread httpSupportThread;
        if (supportDiscoveryThreads.get(connectionItem.getAccount().getBareJid()) != null) {
            httpSupportThread = supportDiscoveryThreads.remove(connectionItem.getAccount()
                    .getBareJid());
            if (httpSupportThread.getState() != Thread.State.TERMINATED) {
                return;
            } else {
                httpSupportThread = createDiscoveryThread(connectionItem);
            }
        } else {
            httpSupportThread = createDiscoveryThread(connectionItem);
        }
        supportDiscoveryThreads.put(connectionItem.getAccount().getBareJid(), httpSupportThread);
        httpSupportThread.start();
    }

    private Thread createDiscoveryThread(ConnectionItem connectionItem) {
        Thread thread = new Thread(() -> startDiscoverProcess(connectionItem));
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        return thread;
    }

    private void startDiscoverProcess(ConnectionItem connectionItem) {
        try {
            connectionItem.getConnection().setReplyTimeout(120000);
            discoverSupport(connectionItem.getAccount(), connectionItem.getConnection());
        } catch (SmackException.NotConnectedException | XMPPException.XMPPErrorException
                | SmackException.NoResponseException | InterruptedException e) {
            LogManager.exception(this, e);
        }
        connectionItem.getConnection().setReplyTimeout(ConnectionItem.defaultReplyTimeout);
    }

    private void saveOrUpdateToRealm(final BareJid account, final Jid server) {
        LogManager.d(LOG_TAG, "Started to save or update in realm with: accountJid - "
                + account.toString() + "; uploadServer - " + server.toString());
        if (server == null || server.toString().isEmpty()) {
            LogManager.d(LOG_TAG, "But incoming upload server was null");
            return;
        }
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    Localpart username = account.getLocalpartOrNull();
                    if (username == null) {
                        LogManager.d(LOG_TAG, "But username is null");
                        return;
                    }
                    Domainpart serverName = account.getDomain();
                    if (serverName == null) {
                        LogManager.d(LOG_TAG, "But serverName is null");
                        return;
                    }
                    AccountRealmObject item = realm1.where(AccountRealmObject.class)
                            .equalTo(AccountRealmObject.Fields.USERNAME, username.toString())
                            .equalTo(AccountRealmObject.Fields.SERVERNAME, serverName.toString())
                            .findFirst();
                    if (item == null) {
                        LogManager.d(LOG_TAG, "But no account realm object was found");
                        return;
                    }
                    item.setUploadServer(server);
                    realm1.copyToRealmOrUpdate(item);
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

    private void removeFromRealm(final BareJid account) {
        LogManager.d(LOG_TAG, "Started to remove upload server in realm for "
                + account.toString());
        Application.getInstance().runInBackground(() -> {
            Realm realm = null;
            try {
                realm = DatabaseManager.getInstance().getDefaultRealmInstance();
                realm.executeTransaction(realm1 -> {
                    Localpart username = account.getLocalpartOrNull();
                    if (username == null) {
                        LogManager.d(LOG_TAG, "But username is null");
                        return;
                    }
                    Domainpart serverName = account.getDomain();
                    if (serverName == null) {
                        LogManager.d(LOG_TAG, "But serverName is null");
                        return;
                    }
                    AccountRealmObject item = realm1.where(AccountRealmObject.class)
                            .equalTo(AccountRealmObject.Fields.USERNAME, username.toString())
                            .equalTo(AccountRealmObject.Fields.SERVERNAME, serverName.toString())
                            .findFirst();
                    if (item != null) {
                        item.setUploadServer("");
                        realm1.copyToRealmOrUpdate(item);
                    } else LogManager.d(LOG_TAG, "But no account realm object was found");
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            } finally {
                if (realm != null) realm.close();
            }
        });
    }

    private void loadAllFromRealm(Map<BareJid, Jid> uploadServers) {
        LogManager.d(LOG_TAG, "Started to load uploadServers from realm");
        uploadServers.clear();
        Realm realm = null;
        try {
            realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            RealmResults<AccountRealmObject> items = realm
                    .where(AccountRealmObject.class)
                    .findAll();
            LogManager.d(LOG_TAG, "Size of all realm accounts list: " + items.size());
            for (AccountRealmObject item : items) {
                LogManager.d(LOG_TAG, "In realm found: barejid - "
                        + item.getAccountJid().getBareJid().toString()
                        + " and it contains saved uploadserver - "
                        + (item.getUploadServer() != null && !item.getUploadServer().toString().isEmpty()));
                if (item.getUploadServer() != null) {
                    uploadServers.put(item.getAccountJid().getBareJid(), item.getUploadServer());
                    LogManager.d(LOG_TAG, "Loaded from realm and successfully putted into uploadServers "
                            + item.getUploadServer().toString());
                }
            }
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        } finally {
            if (realm != null && Looper.myLooper() != Looper.getMainLooper())
                realm.close();
        }
    }

    // Realm

    public static class ImageSize {
        private int height;
        private int width;

        public ImageSize(int height, int width) {
            this.height = height;
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }
    }

    public class UploadReceiver extends ResultReceiver {

        public UploadReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            int currentProgress = resultData.getInt(UploadService.KEY_PROGRESS);
            int fileCount = resultData.getInt(UploadService.KEY_FILE_COUNT);
            String messageId = resultData.getString(UploadService.KEY_MESSAGE_ID);
            String error = resultData.getString(UploadService.KEY_ERROR);

            switch (resultCode) {
                case UploadService.UPDATE_PROGRESS_CODE:
                    progressSubscribe.onNext(new ProgressData(fileCount, currentProgress, null, false, messageId));
                    break;
                case UploadService.ERROR_CODE:
                    progressSubscribe.onNext(new ProgressData(fileCount, 0, error, false, messageId));
                    isUploading = false;
                    break;
                case UploadService.COMPLETE_CODE:
                    progressSubscribe.onNext(new ProgressData(fileCount, 100, null, true, messageId));
                    isUploading = false;
                    break;
            }
        }
    }

    public class ProgressData {
        final int fileCount;
        final int progress;
        final String error;
        final boolean completed;
        final String messageId;

        public ProgressData(int fileCount, int progress, String error, boolean completed,
                            String messageId) {
            this.fileCount = fileCount;
            this.progress = progress;
            this.error = error;
            this.completed = completed;
            this.messageId = messageId;
        }

        public int getProgress() {
            return progress;
        }

        @Nullable
        public String getError() {
            return error;
        }

        public boolean isCompleted() {
            return completed;
        }

        public String getMessageId() {
            return messageId;
        }

        public int getFileCount() {
            return fileCount;
        }
    }
}
