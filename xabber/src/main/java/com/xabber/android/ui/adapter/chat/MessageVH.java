package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.StyleRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.adapter.ChatMessageAdapter;
import com.xabber.android.ui.adapter.FilesAdapter;
import com.xabber.android.utils.StringUtils;

import java.util.Date;

public class MessageVH extends BasicMessageVH implements View.OnClickListener,
        FilesAdapter.FileListListener {

    private static final String LOG_TAG = ChatMessageAdapter.Message.class.getSimpleName();
    protected MessageClickListener onClickListener;

    TextView messageTime;
    TextView messageHeader;
    TextView messageNotDecrypted;
    View messageBalloon;

    ImageView messageImage;
    ImageView statusIcon;
    ImageView ivEncrypted;

    View fileLayout;
    RecyclerView rvFileList;
    FrameLayout imageGridContainer;

    String messageId;
    final ProgressBar uploadProgressBar;
    final ImageButton ivCancelUpload;

    public MessageVH(View itemView, MessageClickListener onClickListener, @StyleRes int appearance) {
        super(itemView, appearance);
        this.onClickListener = onClickListener;

        uploadProgressBar = itemView.findViewById(R.id.uploadProgressBar);
        ivCancelUpload = itemView.findViewById(R.id.ivCancelUpload);
        if (ivCancelUpload != null) ivCancelUpload.setOnClickListener(this);

        messageTime = (TextView) itemView.findViewById(R.id.message_time);
        messageHeader = (TextView) itemView.findViewById(R.id.message_header);
        messageNotDecrypted = (TextView) itemView.findViewById(R.id.message_not_decrypted);
        messageBalloon = itemView.findViewById(R.id.message_balloon);

        messageImage = (ImageView) itemView.findViewById(R.id.message_image);

        statusIcon = (ImageView) itemView.findViewById(R.id.message_status_icon);
        ivEncrypted = (ImageView) itemView.findViewById(R.id.message_encrypted_icon);

        fileLayout = itemView.findViewById(R.id.fileLayout);
        rvFileList = itemView.findViewById(R.id.rvFileList);

        imageGridContainer = itemView.findViewById(R.id.imageGridContainer);

        itemView.setOnClickListener(this);
        messageImage.setOnClickListener(this);
    }

    public void bind(MessageItem messageItem, boolean isMUC, boolean showOriginalOTR,
                     Context context, boolean unread) {
        if (isMUC) {
            messageHeader.setText(messageItem.getResource());
            messageHeader.setVisibility(View.VISIBLE);
        } else {
            messageHeader.setVisibility(View.GONE);
        }

        if (messageItem.isEncrypted()) {
            ivEncrypted.setVisibility(View.VISIBLE);
        } else {
            ivEncrypted.setVisibility(View.GONE);
        }

        messageText.setText(messageItem.getText());
        if (OTRManager.getInstance().isEncrypted(messageItem.getText())) {
            if (showOriginalOTR)
                messageText.setVisibility(View.VISIBLE);
            else messageText.setVisibility(View.GONE);
            messageNotDecrypted.setVisibility(View.VISIBLE);
        } else {
            messageText.setVisibility(View.VISIBLE);
            messageNotDecrypted.setVisibility(View.GONE);
        }

        String time = StringUtils.getSmartTimeText(context, new Date(messageItem.getTimestamp()));

        Long delayTimestamp = messageItem.getDelayTimestamp();
        if (delayTimestamp != null) {
            String delay = context.getString(messageItem.isIncoming() ? R.string.chat_delay : R.string.chat_typed,
                    StringUtils.getSmartTimeText(context, new Date(delayTimestamp)));
            time += " (" + delay + ")";
        }

        messageTime.setText(time);

        /** setup UNREAD */
        if (unread) itemView.setBackgroundColor(context.getResources().getColor(R.color.unread_messages_background));
        else itemView.setBackgroundDrawable(null);
    }

    @Override
    public void onFileClick(int attachmentPosition) {
        int messagePosition = getAdapterPosition();
        if (messagePosition == RecyclerView.NO_POSITION) {
            LogManager.w(LOG_TAG, "onClick: no position");
            return;
        }

        onClickListener.onFileClick(messagePosition, attachmentPosition);
    }

    @Override
    public void onFileLongClick(Attachment attachment, View caller) {
        onClickListener.onFileLongClick(attachment, caller);
    }

    @Override
    public void onDownloadCancel() {
        onClickListener.onDownloadCancel();
    }

    @Override
    public void onDownloadError(String error) {
        onClickListener.onDownloadError(error);
    }

    @Override
    public void onClick(View v) {
        int adapterPosition = getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            LogManager.w(LOG_TAG, "onClick: no position");
            return;
        }

        switch (v.getId()) {
            case R.id.ivImage0:
                onClickListener.onImageClick(adapterPosition, 0);
                break;
            case R.id.ivImage1:
                onClickListener.onImageClick(adapterPosition, 1);
                break;
            case R.id.ivImage2:
                onClickListener.onImageClick(adapterPosition, 2);
                break;
            case R.id.ivImage3:
                onClickListener.onImageClick(adapterPosition, 3);
                break;
            case R.id.ivImage4:
                onClickListener.onImageClick(adapterPosition, 4);
                break;
            case R.id.ivImage5:
                onClickListener.onImageClick(adapterPosition, 5);
                break;
            case R.id.message_image:
                onClickListener.onImageClick(adapterPosition, 0);
                break;
            case R.id.ivCancelUpload:
                onClickListener.onUploadCancel();
                break;
            default:
                onClickListener.onMessageClick(messageBalloon, adapterPosition);
                break;
        }
    }

    public interface MessageClickListener {
        void onMessageClick(View caller, int position);
        void onImageClick(int messagePosition, int attachmentPosition);
        void onFileClick(int messagePosition, int attachmentPosition);
        void onFileLongClick(Attachment attachment, View caller);
        void onDownloadCancel();
        void onUploadCancel();
        void onDownloadError(String error);
    }
}
