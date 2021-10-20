package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.QuoteSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.StyleRes;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.GroupMemberRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.extension.groups.GroupPrivacyType;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.references.mutable.voice.VoiceManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.GroupChat;
import com.xabber.android.ui.adapter.FilesAdapter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.text.ClickTagHandler;
import com.xabber.android.ui.text.CustomQuoteSpan;
import com.xabber.android.ui.text.StringUtilsKt;
import com.xabber.android.ui.widget.CorrectlyMeasuringTextView;
import com.xabber.android.ui.widget.ImageGrid;

import java.util.Arrays;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.Sort;
import rx.subscriptions.CompositeSubscription;

public class MessageVH extends BasicMessageVH implements View.OnClickListener, FilesAdapter.FileListListener, View.OnLongClickListener {

    public boolean isUnread;
    public boolean needName;

    TextView messageTime;
    TextView messageHeader;
    View messageBalloon;
    View messageShadow;
    ImageView statusIcon;
    String messageId;
    Long timestamp;
    View messageInfo;
    RecyclerView forwardedMessagesRV;

    private final CompositeSubscription subscriptions = new CompositeSubscription();

    private final MessageClickListener listener;
    private final MessageLongClickListener longClickListener;
    private final FileListener fileListener;

    private int imageCounter;
    private int imageCount;
    private int fileCounter;
    private int fileCount;

    final TextView messageFileInfo;
    final ProgressBar progressBar;
    final RecyclerView rvFileList;
    final FrameLayout imageGridContainer;
    final ProgressBar uploadProgressBar;
    final ImageButton ivCancelUpload;

    public interface FileListener {
        void onImageClick(int messagePosition, int attachmentPosition, String messageUID);
        void onFileClick(int messagePosition, int attachmentPosition, String messageUID);
        void onVoiceClick(int messagePosition, int attachmentPosition, String attachmentId, String messageUID, Long timestamp);
        void onFileLongClick(AttachmentRealmObject attachmentRealmObject, View caller);
        void onDownloadCancel();
        void onUploadCancel();
        void onDownloadError(String error);
    }

    public interface MessageClickListener {
        void onMessageClick(View caller, int position);
    }

    public interface MessageLongClickListener {
        void onLongMessageClick(int position);
    }

    public MessageVH(
            View itemView, MessageClickListener messageListener,
            MessageLongClickListener longClickListener, FileListener fileListener,
            @StyleRes int appearance
    ) {

        super(itemView, appearance);
        this.listener = messageListener;
        this.longClickListener = longClickListener;
        this.fileListener = fileListener;

        messageInfo = itemView.findViewById(R.id.message_info);
        messageTime = itemView.findViewById(R.id.message_time);
        messageHeader = itemView.findViewById(R.id.message_sender_tv);
        messageBalloon = itemView.findViewById(R.id.message_balloon);
        messageShadow = itemView.findViewById(R.id.message_shadow);
        statusIcon = itemView.findViewById(R.id.message_status_icon);

        forwardedMessagesRV = itemView.findViewById(R.id.forwardedRecyclerView);

        rvFileList = itemView.findViewById(R.id.file_list_rv);
        imageGridContainer = itemView.findViewById(R.id.image_grid_container_fl);
        uploadProgressBar = itemView.findViewById(R.id.uploadProgressBar);
        ivCancelUpload = itemView.findViewById(R.id.ivCancelUpload);
        progressBar = itemView.findViewById(R.id.message_progress_bar);
        messageFileInfo = itemView.findViewById(R.id.message_file_info);

        if (ivCancelUpload != null) {
            ivCancelUpload.setOnClickListener(this);
        }

        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }

    public void bind(MessageRealmObject messageRealmObject, MessageExtraData extraData) {

        messageHeader.setVisibility(View.GONE);
        AbstractChat chat = ChatManager.getInstance().getChat(
                messageRealmObject.getAccount(), messageRealmObject.getUser()
        );
        // groupchat
        if (extraData.getGroupMember() != null) {
            if (!extraData.getGroupMember().isMe()) {
                GroupMemberRealmObject user = extraData.getGroupMember();
                messageHeader.setText(user.getNickname());
                messageHeader.setTextColor(
                        ColorManager.changeColor(
                                ColorGenerator.MATERIAL.getColor(user.getNickname()),
                                0.8f
                        )
                );
                messageHeader.setVisibility(View.VISIBLE);
            } else if (chat instanceof GroupChat && ((GroupChat) chat).getPrivacyType() == GroupPrivacyType.INCOGNITO){
                GroupMemberRealmObject user = extraData.getGroupMember();
                messageHeader.setText(user.getNickname());
                messageHeader.setTextColor(
                        ColorManager.changeColor(
                                ColorGenerator.MATERIAL.getColor(user.getNickname()),
                                0.8f
                        )
                );
                messageHeader.setVisibility(View.VISIBLE);
            }
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {

                getMessageText().setTextColor(itemView.getContext().getColor(R.color.grey_200));
            } else {
                getMessageText().setTextColor(itemView.getContext().getColor(R.color.black));
            }

        // Added .concat("&zwj;") and .concat(String.valueOf(Character.MIN_VALUE)
        // to avoid click by empty space after ClickableSpan
        // Try to decode to avoid ugly non-english links
        if (messageRealmObject.getMarkupText() != null && !messageRealmObject.getMarkupText().isEmpty()){
            SpannableStringBuilder spannable = (SpannableStringBuilder)
                    Html.fromHtml(
                            messageRealmObject.getMarkupText()
                                    .trim()
                                    .replace("\n", "<br/>")
                                    .concat("&zwj;"),
                            null,
                            new ClickTagHandler(
                                    extraData.getContext(), messageRealmObject.getAccount()
                            )
                    );

            int color;
            DisplayMetrics displayMetrics = itemView.getContext().getResources().getDisplayMetrics();
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                color = ColorManager.getInstance().getAccountPainter().getAccountMainColor(
                        messageRealmObject.getAccount()
                );
            } else {
                color = ColorManager.getInstance().getAccountPainter().getAccountSendButtonColor(
                        messageRealmObject.getAccount()
                );
            }

            modifySpannableWithCustomQuotes(spannable, displayMetrics, color);
            getMessageText().setText(spannable, TextView.BufferType.SPANNABLE);
        } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                getMessageText().setText(
                        StringUtilsKt.getDecodedSpannable(
                                messageRealmObject.getText().trim().concat(
                                        String.valueOf(Character.MIN_VALUE)
                                )
                        ),
                        TextView.BufferType.SPANNABLE
                );
            } else {
                getMessageText().setText(
                        messageRealmObject.getText().trim().concat(
                                String.valueOf(Character.MIN_VALUE)
                        )
                );
            }

        }

        getMessageText().setMovementMethod(CorrectlyMeasuringTextView.LocalLinkMovementMethod.INSTANCE);

        // set unread status
        isUnread = extraData.isUnread();

        // set date
        setNeedDate(extraData.isNeedDate());
        setDate(StringUtilsKt.getDateStringForMessage(messageRealmObject.getTimestamp()));

        needName = extraData.isNeedName();
        if (!needName) {
            messageHeader.setVisibility(View.GONE);
        }

        // setup CHECKED
        if (extraData.isChecked()){
            itemView.setBackgroundColor(
                    extraData.getContext().getResources().getColor(
                            R.color.unread_messages_background
                    )
            );
        } else {
            itemView.setBackground(null);
        }

        setupTime(extraData, messageRealmObject);
        setupImageOrFile(messageRealmObject, extraData.getContext());
    }

    protected void setupTime(MessageExtraData extraData, MessageRealmObject messageRealmObject) {
        //Since the original and forwarded voice messages are basically the same, we need some help with properly differentiating them to avoid cases when
        //original voice message and the forward with this voice message are showing the same progress change during playback.
        //Saving any type of data from the base message (message that "houses" the forwarded messages) will help us differentiate
        //original voice message and voice message inside forwards, as well as same forwarded messages in different replies.
        //TODO:should probably swap timestamp to the UID of the message, since it's more versatile
        timestamp = extraData.getMainMessageTimestamp();

        String time = getTimeText(new Date(messageRealmObject.getTimestamp()));
        Long delayTimestamp = messageRealmObject.getDelayTimestamp();
        if (delayTimestamp != null) {
            String delay = extraData.getContext().getString(messageRealmObject.isIncoming() ? R.string.chat_delay : R.string.chat_typed,
                    getTimeText(new Date(delayTimestamp)));
            time += " (" + delay + ")";
        }
        Long editedTimestamp = messageRealmObject.getEditedTimestamp();
        if (editedTimestamp != null) {
            time += extraData.getContext().getString(
                    R.string.edited,
                    getTimeText(new Date (editedTimestamp))
            );
        }

        messageTime.setText(time);
    }

    protected void setupImageOrFile(MessageRealmObject messageRealmObject, Context context) {
          rvFileList.setVisibility(View.GONE);
        if (imageGridContainer != null) {
            imageGridContainer.removeAllViews();
            imageGridContainer.setVisibility(View.GONE);
        }
        if (messageRealmObject.haveAttachments()) {
            setUpImage(messageRealmObject.getAttachmentRealmObjects());
            //setUpVoice(messageItem.getAttachments(), context);
            setUpFile(messageRealmObject.getAttachmentRealmObjects(), context);
        }
    }

    private void setUpImage(RealmList<AttachmentRealmObject> attachmentRealmObjects) {
        final ImageGrid gridBuilder = new ImageGrid();

        if (!SettingsManager.connectionLoadImages()) return;

        RealmList<AttachmentRealmObject> imageAttachmentRealmObjects = new RealmList<>();
        for (AttachmentRealmObject attachmentRealmObject : attachmentRealmObjects) {
            if (attachmentRealmObject.isImage()) {
                imageAttachmentRealmObjects.add(attachmentRealmObject);
                imageCounter++;
            }
        }
        imageCount = imageCounter;
        imageCounter = 0;
        if (imageAttachmentRealmObjects.size() > 0) {
            View imageGridView = gridBuilder.inflateView(imageGridContainer, imageAttachmentRealmObjects.size());
            gridBuilder.bindView(imageGridView, imageAttachmentRealmObjects, this, v -> {
                onLongClick(v);
                return true;
            });

            imageGridContainer.addView(imageGridView);
            imageGridContainer.setVisibility(View.VISIBLE);
        }
    }

    private void setUpFile(RealmList<AttachmentRealmObject> attachmentRealmObjects, Context context) {
        RealmList<AttachmentRealmObject> fileAttachmentRealmObjects = new RealmList<>();
        for (AttachmentRealmObject attachmentRealmObject : attachmentRealmObjects) {
            if (!attachmentRealmObject.isImage()) {
                fileAttachmentRealmObjects.add(attachmentRealmObject);
                fileCounter++;
            }
        }
        fileCount = fileCounter;
        fileCounter = 0;
        if (fileAttachmentRealmObjects.size() > 0) {
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
            rvFileList.setLayoutManager(layoutManager);
            FilesAdapter adapter = new FilesAdapter(fileAttachmentRealmObjects, timestamp, this);
            rvFileList.setAdapter(adapter);
            rvFileList.setVisibility(View.VISIBLE);
        }
    }

    /** File list Listener */

    @Override
    public void onFileClick(int attachmentPosition) {
        int messagePosition = getAdapterPosition();
        if (messagePosition == RecyclerView.NO_POSITION) {
            LogManager.w(this, "onClick: no position");
            return;
        }
        fileListener.onFileClick(messagePosition, attachmentPosition, messageId);
    }

    @Override
    public void onVoiceClick(int attachmentPosition, String attachmentId, boolean saved, Long mainMessageTimestamp) {
        int messagePosition = getAdapterPosition();
        if (messagePosition == RecyclerView.NO_POSITION) {
            LogManager.w(this, "onClick: no position");
            return;
        }
        if (!saved) {
            fileListener.onVoiceClick(messagePosition, attachmentPosition, attachmentId, messageId, mainMessageTimestamp);
        } else VoiceManager.getInstance().voiceClicked(messageId, attachmentPosition, mainMessageTimestamp);
    }

    @Override
    public void onVoiceProgressClick(int attachmentPosition, String attachmentId, Long timestamp, int current, int max) {
        int messagePosition = getAdapterPosition();
        if (messagePosition == RecyclerView.NO_POSITION) {
            LogManager.w(this, "onClick: no position");
            return;
        }
        VoiceManager.getInstance().seekAudioPlaybackTo(attachmentId, timestamp, current, max);
    }

    @Override
    public void onFileLongClick(AttachmentRealmObject attachmentRealmObject, View caller) {
        fileListener.onFileLongClick(attachmentRealmObject, caller);
    }

    @Override
    public void onDownloadCancel() {
        fileListener.onDownloadCancel();
    }

    @Override
    public void onDownloadError(String error) {
        fileListener.onDownloadError(error);
    }

    @Override
    public void onClick(View v) {
        int adapterPosition = getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            LogManager.w(this, "onClick: no position");
            return;
        }

        switch (v.getId()) {
            case R.id.ivImage0:
            case R.id.ivImage1:
                fileListener.onImageClick(adapterPosition, 1, messageId);
                break;
            case R.id.ivImage2:
                fileListener.onImageClick(adapterPosition, 2, messageId);
                break;
            case R.id.ivImage3:
                fileListener.onImageClick(adapterPosition, 3, messageId);
                break;
            case R.id.ivImage4:
                fileListener.onImageClick(adapterPosition, 4, messageId);
                break;
            case R.id.ivImage5:
                fileListener.onImageClick(adapterPosition, 5, messageId);
                break;
            case R.id.ivCancelUpload:
                fileListener.onUploadCancel();
                break;
            default:
                listener.onMessageClick(messageBalloon, adapterPosition);
        }
    }

    /** Upload progress subscription */

    protected void subscribeForUploadProgress() {
        subscriptions.add(
                HttpFileUploadManager.getInstance()
                        .subscribeForProgress()
                        .doOnNext(this::setUpProgress)
                        .subscribe()
        );
    }

    protected void unsubscribeAll() {
        subscriptions.clear();
    }

    private void setUpProgress(HttpFileUploadManager.ProgressData progressData) {
        if (progressData != null && (messageId.equals(progressData.getMessageId()))) {
            if (progressData.isCompleted()) {
                showProgress(false);
                showFileProgressModified(rvFileList, fileCount, fileCount);
                showProgressModified(false, 0,imageCount);
            } else if (progressData.getError() != null) {
                showProgress(false);
                showFileProgressModified(rvFileList, fileCount, fileCount);
                showProgressModified(false, 0,imageCount);
                fileListener.onDownloadError(progressData.getError());
            } else {
                showProgress(true);
                if (messageFileInfo != null) messageFileInfo.setText(R.string.message_status_uploading);
                if (progressData.getProgress()<=imageCount && imageGridContainer != null){
                        showProgressModified(true, progressData.getProgress() - 1, imageCount);
                }
                if (progressData.getProgress() - imageCount <= fileCount) {
                    showFileProgressModified(rvFileList,
                            progressData.getProgress() - imageCount,
                            progressData.getFileCount() - imageCount);
                }
            }
        } else {
            showProgress(false);
            showFileProgressModified(rvFileList, fileCount, fileCount);
            showProgressModified(false, 0,imageCount);
        }
    }

    private void showProgress(boolean show) {
        if (messageFileInfo != null) {
            messageFileInfo.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (messageTime != null) {
            messageTime.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showFileProgressModified(RecyclerView view, int startAt, int endAt) {
        for (int i = 0;i<startAt;i++) {
            showFileUploadProgress(view.getChildAt(i), false);
        }
        for (int j = Math.max(startAt, 0); j<endAt; j++) {
            showFileUploadProgress(view.getChildAt(j), true);
        }
    }

    private void showFileUploadProgress(View view, boolean show) {
        ProgressBar upload = view.findViewById(R.id.uploadProgressBar);
        if (upload != null) {
            upload.setVisibility(show? View.VISIBLE : View.GONE);
        }
    }

    private void showProgressModified(boolean show, int current, int last) {
        if(show) {
            for (int i = 0; i < current; i++) {
                ProgressBar progressBar = getProgressView(imageGridContainer, i);
                ImageView imageShadow = getImageShadow(imageGridContainer, i);

                if (progressBar!=null) {
                    progressBar.setVisibility(View.GONE);
                }
                if (imageShadow != null) {
                    imageShadow.setVisibility(View.GONE);
                }
            }
            for (int j = current; j < last; j++) {
                ProgressBar progressBar = getProgressView(imageGridContainer, j);
                ImageView imageShadow = getImageShadow(imageGridContainer, j);

                if (progressBar!=null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                if (imageShadow != null) {
                    imageShadow.setVisibility(View.VISIBLE);
                }
            }
        } else {
            for (int i=0;i<last;i++) {
                ProgressBar progressBar = getProgressView(imageGridContainer, i);
                ImageView imageShadow = getImageShadow(imageGridContainer, i);

                if (progressBar!=null) {
                    progressBar.setVisibility(View.GONE);
                }
                if (imageShadow != null) {
                    imageShadow.setVisibility(View.GONE);
                }
            }
        }
    }

    private ProgressBar getProgressView(View view, int index) {
        switch (index) {
            case 1:
                return view.findViewById(R.id.uploadProgressBar1);
            case 2:
                return view.findViewById(R.id.uploadProgressBar2);
            case 3:
                return view.findViewById(R.id.uploadProgressBar3);
            case 4:
                return view.findViewById(R.id.uploadProgressBar4);
            case 5:
                return view.findViewById(R.id.uploadProgressBar5);
            default:
                return view.findViewById(R.id.uploadProgressBar0);
        }
    }

    private ImageView getImageShadow(View view, int index) {
        switch (index) {
            case 1:
                return view.findViewById(R.id.ivImage1Shadow);
            case 2:
                return view.findViewById(R.id.ivImage2Shadow);
            case 3:
                return view.findViewById(R.id.ivImage3Shadow);
            case 4:
                return view.findViewById(R.id.ivImage4Shadow);
            case 5:
                return view.findViewById(R.id.ivImage5Shadow);
            default:
                return view.findViewById(R.id.ivImage0Shadow);
        }
    }

    void setupForwarded(MessageRealmObject messageRealmObject, MessageExtraData extraData) {
        String[] forwardedIDs = messageRealmObject.getForwardedIdsAsArray();
        if (!Arrays.asList(forwardedIDs).contains(null)) {
            Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            RealmResults<MessageRealmObject> forwardedMessages = realm
                            .where(MessageRealmObject.class)
                            .in(MessageRealmObject.Fields.PRIMARY_KEY, forwardedIDs)
                            .findAll()
                            .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING);

            if (forwardedMessages.size() > 0) {
                ForwardedAdapter adapter = new ForwardedAdapter(forwardedMessages, extraData);
                forwardedMessagesRV.setLayoutManager(new LinearLayoutManager(extraData.getContext()));
                forwardedMessagesRV.setAdapter(adapter);
                forwardedMessagesRV.setBackgroundColor(
                        ColorManager.getColorWithAlpha(R.color.forwarded_background_color, 0.2f)
                );
                forwardedMessagesRV.setVisibility(View.VISIBLE);
            }
            if (Looper.myLooper() != Looper.getMainLooper()) {
                realm.close();
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        int adapterPosition = getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            LogManager.w(this, "onClick: no position");
            return false;
        } else {
            longClickListener.onLongMessageClick(adapterPosition);
            return true;
        }
    }

    protected void setUpMessageBalloonBackground(View view, ColorStateList colorList) {
        final Drawable originalBackgroundDrawable = view.getBackground();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            originalBackgroundDrawable.setTintList(colorList);
        } else {
            Drawable wrapDrawable = DrawableCompat.wrap(originalBackgroundDrawable);
            DrawableCompat.setTintList(wrapDrawable, colorList);

            int pL = view.getPaddingLeft();
            int pT = view.getPaddingTop();
            int pR = view.getPaddingRight();
            int pB = view.getPaddingBottom();

            view.setBackground(wrapDrawable);

            view.setPadding(pL, pT, pR, pB);
        }
    }

    private void modifySpannableWithCustomQuotes(
            SpannableStringBuilder spannable, DisplayMetrics displayMetrics, int color
    ) {
        QuoteSpan[] quoteSpans = spannable.getSpans(0, spannable.length(), QuoteSpan.class);

        if (quoteSpans.length > 0) {
            for (int i = quoteSpans.length - 1; i >= 0; i--) {
                QuoteSpan span = quoteSpans[i];
                int spanEnd = spannable.getSpanEnd(span);
                int spanStart = spannable.getSpanStart(span);
                spannable.removeSpan(span);
                if (spanEnd < 0 || spanStart < 0) break;

                int newlineCount = 0;
                if ('\n' == spannable.charAt(spanEnd)) {
                    newlineCount++;
                    if (spanEnd + 1 < spannable.length() && '\n' == spannable.charAt(spanEnd + 1)) {
                        newlineCount++;
                    }
                    if ('\n' == spannable.charAt(spanEnd - 1)) {
                        newlineCount++;
                    }
                }
                switch (newlineCount) {
                    case 3:
                        spannable.delete(spanEnd - 1, spanEnd + 1);
                        spanEnd = spanEnd - 2;
                        break;
                    case 2:
                        spannable.delete(spanEnd, spanEnd + 1);
                        spanEnd--;
                }

                if (spanStart > 1 && '\n' == spannable.charAt(spanStart - 1)) {
                    if ('\n' == spannable.charAt(spanStart - 2)) {
                        spannable.delete(spanStart - 2, spanStart - 1);
                        spanStart--;
                    }
                }

                spannable.setSpan(
                        new CustomQuoteSpan(color, displayMetrics),
                        spanStart,
                        spanEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                char current;
                boolean waitForNewLine = false;
                for (int j = spanStart; j < spanEnd; j++) {
                    if (j >= spannable.length()) {
                        break;
                    }
                    current = spannable.charAt(j);

                    if (waitForNewLine && current != '\n') {
                        continue;
                    } else {
                        waitForNewLine = false;
                    }

                    if (current == '>') {
                        spannable.delete(j, j + 1);
                        j--;
                        waitForNewLine = true;
                    }
                }
            }
        }
    }

    private String getTimeText(Date timeStamp) {
        return android.text.format.DateFormat
                .getTimeFormat(Application.getInstance())
                .format(timeStamp);
    }

}
