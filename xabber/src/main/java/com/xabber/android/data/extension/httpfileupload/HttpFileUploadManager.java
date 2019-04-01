package com.xabber.android.data.extension.httpfileupload;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.webkit.MimeTypeMap;

import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.listeners.OnAccountRemovedListener;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.service.UploadService;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;

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
    private Map<BareJid, Jid> uploadServers = new ConcurrentHashMap<>();
    private PublishSubject<ProgressData> progressSubscribe = PublishSubject.create();
    private boolean isUploading;

    public static HttpFileUploadManager getInstance() {
        if (instance == null) {
            instance = new HttpFileUploadManager();
        }
        return instance;
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
        return uploadServers.containsKey(account.getFullJid().asBareJid());
    }

    public void retrySendFileMessage(final MessageItem messageItem, Context context) {
        List<String> notUploadedFilesPaths = new ArrayList<>();

        for (Attachment attachment : messageItem.getAttachments()) {
            if (attachment.getFileUrl() == null || attachment.getFileUrl().isEmpty())
                notUploadedFilesPaths.add(attachment.getFilePath());
        }

        // if all attachments have url that they was uploaded. just resend existing message
        if (notUploadedFilesPaths.size() == 0) {
            final AccountJid accountJid = messageItem.getAccount();
            final UserJid userJid = messageItem.getUser();
            final String messageId = messageItem.getUniqueId();
            Application.getInstance().runInBackgroundUserRequest(new Runnable() {
                @Override
                public void run() {
                    MessageManager.getInstance().removeErrorAndResendMessage(accountJid, userJid, messageId);
                }
            });
        }

        // else, upload files that haven't urls. Then write they in existing message and send
        else uploadFile(messageItem.getAccount(), messageItem.getUser(),
                notUploadedFilesPaths, null, messageItem.getUniqueId(), context);
    }

    public void uploadFile(final AccountJid account, final UserJid user,
                           final List<String> filePaths, Context context) {
        uploadFile(account, user, filePaths, null, null, context);
    }

    public void uploadFileViaUri(final AccountJid account, final UserJid user,
                                 final List<Uri> fileUris, Context context) {
        uploadFile(account, user, null, fileUris,null, context);
    }

    public void uploadFile(final AccountJid account, final UserJid user,
                           final List<String> filePaths, final List<Uri> fileUris,
                           String existMessageId, Context context) {

        if (isUploading) {
            progressSubscribe.onNext(new ProgressData(0, 0, "Uploading already started",
                    false, null));
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
        intent.putStringArrayListExtra(UploadService.KEY_FILE_PATHS, (ArrayList<String>) filePaths);
        intent.putParcelableArrayListExtra(UploadService.KEY_FILE_URIS, (ArrayList<Uri>) fileUris);
        intent.putExtra(UploadService.KEY_UPLOAD_SERVER_URL, (CharSequence) uploadServerUrl);
        intent.putExtra(UploadService.KEY_MESSAGE_ID, existMessageId);

        context.startService(intent);
    }

    public void cancelUpload(Context context) {
        Intent intent = new Intent(context, UploadService.class);
        context.stopService(intent);
    }

    private void discoverSupport(AccountJid account, XMPPConnection xmppConnection) throws SmackException.NotConnectedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException, InterruptedException {

        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(xmppConnection);

        List<DomainBareJid> services;
        try {
            services = discoManager.findServices(com.xabber.xmpp.httpfileupload.Request.NAMESPACE, true, true);
        } catch (ClassCastException e) {
            services = Collections.emptyList();
            LogManager.exception(this, e);
        }

        if (!services.isEmpty()) {
            final DomainBareJid uploadServerUrl = services.get(0);
            LogManager.i(this, "Http file upload server: " + uploadServerUrl);
            uploadServers.put(account.getFullJid().asBareJid(), uploadServerUrl);
            saveOrUpdateToRealm(account.getFullJid().asBareJid(), uploadServerUrl);
        }
    }

    public void onAuthorized(final ConnectionItem connectionItem) {
        try {
            discoverSupport(connectionItem.getAccount(), connectionItem.getConnection());
        } catch (SmackException.NotConnectedException | XMPPException.XMPPErrorException
                | SmackException.NoResponseException | InterruptedException e) {
            LogManager.exception(this, e);
        }
    }

    public static ImageSize getImageSizes(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(new File(filePath).getAbsolutePath(), options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        return new ImageSize(imageHeight, imageWidth);
    }

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


    public static String getMimeType(String path) {
        String extension = path.substring(path.lastIndexOf(".")).substring(1);
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (type == null || type.isEmpty()) type = "*/*";
        return type;
    }

    public static RealmList<Attachment> parseFileMessage(Stanza packet) {
        RealmList<Attachment> attachments = new RealmList<>();

        DataForm dataForm = DataForm.from(packet);
        if (dataForm != null) {

            List<FormField> fields = dataForm.getFields();
            for (FormField field : fields) {
                if (field instanceof ExtendedFormField) {
                    ExtendedFormField.Media media = ((ExtendedFormField)field).getMedia();
                    attachments.add(mediaToAttachment(media, field.getLabel()));
                }
            }
        }
        return attachments;
    }

    private static Attachment mediaToAttachment(ExtendedFormField.Media media, String title) {
        Attachment attachment = new Attachment();
        attachment.setTitle(title);

        try {
            if (media.getWidth() != null && !media.getWidth().isEmpty())
                attachment.setImageWidth(Integer.valueOf(media.getWidth()));

            if (media.getHeight() != null && !media.getHeight().isEmpty())
                attachment.setImageHeight(Integer.valueOf(media.getHeight()));

        } catch (NumberFormatException e) {
            LogManager.exception(LOG_TAG, e);
        }

        ExtendedFormField.Uri uri = media.getUri();
        if (uri != null) {
            attachment.setMimeType(uri.getType());
            attachment.setFileSize(uri.getSize());
            attachment.setDuration(uri.getDuration());
            attachment.setFileUrl(uri.getUri());
            attachment.setIsImage(FileManager.isImageUrl(uri.getUri()));
        }
        return attachment;
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

        public ProgressData(int fileCount, int progress, String error, boolean completed, String messageId) {
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

    // Realm

    private void saveOrUpdateToRealm(final BareJid account, final Jid server) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Realm realm = RealmManager.getInstance().getNewRealm();
                UploadServer item = realm.where(UploadServer.class)
                        .equalTo(UploadServer.Fields.ACCOUNT, account.toString()).findFirst();
                realm.beginTransaction();
                if (item == null) item = new UploadServer(account, server);
                else item.setServer(server);
                realm.copyToRealmOrUpdate(item);
                realm.commitTransaction();
            }
        });
    }

    private void removeFromRealm(final BareJid account) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Realm realm = RealmManager.getInstance().getNewRealm();
                UploadServer item = realm.where(UploadServer.class)
                        .equalTo(UploadServer.Fields.ACCOUNT, account.toString()).findFirst();
                realm.beginTransaction();
                if (item != null) item.deleteFromRealm();
                realm.commitTransaction();
            }
        });
    }

    private void loadAllFromRealm(Map<BareJid, Jid> uploadServers) {
        uploadServers.clear();
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<UploadServer> items = realm.where(UploadServer.class).findAll();
        for (UploadServer item : items) {
            uploadServers.put(item.getAccount(), item.getServer());
        }
        realm.close();
    }
}
