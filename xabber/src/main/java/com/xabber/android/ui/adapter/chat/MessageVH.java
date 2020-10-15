package com.xabber.android.ui.adapter.chat;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.StyleRes;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.groupchat.GroupchatMember;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.text.ClickTagHandler;
import com.xabber.android.ui.widget.CorrectlyMeasuringTextView;
import com.xabber.android.utils.StringUtils;
import com.xabber.android.utils.Utils;

import java.util.Arrays;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MessageVH extends BasicMessageVH implements View.OnClickListener, View.OnLongClickListener {

    private static final String LOG_TAG = MessageVH.class.getSimpleName();
    private MessageClickListener listener;
    private MessageLongClickListener longClickListener;
    public boolean isUnread;

    TextView tvFirstUnread;
    TextView messageTime;
    TextView messageHeader;
    TextView messageNotDecrypted;
    View messageBalloon;
    View messageShadow;
    ImageView statusIcon;
    ImageView ivEncrypted;
    String messageId;
    Long timestamp;
    View messageInfo;
    View forwardLayout;
    View forwardLeftBorder;

    public interface MessageClickListener {
        void onMessageClick(View caller, int position);
    }

    public interface MessageLongClickListener {
        void onLongMessageClick(int position);
    }

    public MessageVH(View itemView, MessageClickListener listener,
                     MessageLongClickListener longClickListener, @StyleRes int appearance) {
        super(itemView, appearance);
        this.listener = listener;
        this.longClickListener = longClickListener;

        tvFirstUnread = itemView.findViewById(R.id.tvFirstUnread);
        messageInfo = itemView.findViewById(R.id.message_info);
        messageTime = itemView.findViewById(R.id.message_time);
        messageHeader = itemView.findViewById(R.id.message_header);
        messageNotDecrypted = itemView.findViewById(R.id.message_not_decrypted);
        messageBalloon = itemView.findViewById(R.id.message_balloon);
        messageShadow = itemView.findViewById(R.id.message_shadow);
        statusIcon = itemView.findViewById(R.id.message_status_icon);
        ivEncrypted = itemView.findViewById(R.id.message_encrypted_icon);
        forwardLayout = itemView.findViewById(R.id.forwardLayout);
        forwardLeftBorder = itemView.findViewById(R.id.forwardLeftBorder);

        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }

    public void bind(MessageRealmObject messageRealmObject, MessagesAdapter.MessageExtraData extraData) {

        messageHeader.setVisibility(View.GONE);

        // groupchat
        if (extraData.getGroupchatMember() != null) {
            GroupchatMember user = extraData.getGroupchatMember();
            messageHeader.setText(user.getNickname());
            messageHeader.setTextColor(ColorManager.changeColor(
                    ColorGenerator.MATERIAL.getColor(user.getNickname()), 0.8f));
            messageHeader.setVisibility(View.VISIBLE);
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark)
                messageText.setTextColor(itemView.getContext().getColor(R.color.grey_200));
            else messageText.setTextColor(itemView.getContext().getColor(R.color.black));

        if (messageRealmObject.isEncrypted()) {
            ivEncrypted.setVisibility(View.VISIBLE);
        } else {
            ivEncrypted.setVisibility(View.GONE);
        }

        // Added .concat("&zwj;") and .concat(String.valueOf(Character.MIN_VALUE)
        // to avoid click by empty space after ClickableSpan
        // Try to decode to avoid ugly non-english links
        if (messageRealmObject.getMarkupText() != null && !messageRealmObject.getMarkupText().isEmpty()){
            SpannableStringBuilder spannable = (SpannableStringBuilder) Html.fromHtml(
                    messageRealmObject.getMarkupText()
                            .trim().replace("\n", "<br/>").concat("&zwj;"),
                    null,
                    new ClickTagHandler(extraData.getContext(), messageRealmObject.getAccount())
            );

            int color;
            DisplayMetrics displayMetrics = itemView.getContext().getResources().getDisplayMetrics();
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                color = ColorManager.getInstance().getAccountPainter().getAccountMainColor(messageRealmObject.getAccount());
            } else {
                color = ColorManager.getInstance().getAccountPainter().getAccountSendButtonColor(messageRealmObject.getAccount());
            }

            Utils.modifySpannableWithCustomQuotes(spannable, displayMetrics, color);
            messageText.setText(spannable, TextView.BufferType.SPANNABLE);
        } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                messageText.setText(Utils.getDecodedSpannable(messageRealmObject.getText().trim().concat(String.valueOf(Character.MIN_VALUE))),
                        TextView.BufferType.SPANNABLE);
            } else {
                messageText.setText(messageRealmObject.getText().trim().concat(String.valueOf(Character.MIN_VALUE)));
            }
        }

        if (OTRManager.getInstance().isEncrypted(messageRealmObject.getText())) {
            if (extraData.isShowOriginalOTR())
                messageText.setVisibility(View.VISIBLE);
            else messageText.setVisibility(View.GONE);
            messageNotDecrypted.setVisibility(View.VISIBLE);
        } else {
            messageText.setVisibility(View.VISIBLE);
            messageNotDecrypted.setVisibility(View.GONE);
        }
        messageText.setMovementMethod(CorrectlyMeasuringTextView.LocalLinkMovementMethod.getInstance());

        //Since the original and forwarded voice messages are basically the same, we need some help with properly differentiating them to avoid cases when
        //original voice message and the forward with this voice message are showing the same progress change during playback.
        //Saving any type of data from the base message (message that "houses" the forwarded messages) will help us differentiate
        //original voice message and voice message inside forwards, as well as same forwarded messages in different replies.
        //TODO:should probably swap timestamp to the UID of the message, since it's more versatile
        timestamp = extraData.getMainMessageTimestamp();

        String time = StringUtils.getTimeText(new Date(messageRealmObject.getTimestamp()));
        Long delayTimestamp = messageRealmObject.getDelayTimestamp();
        if (delayTimestamp != null) {
            String delay = extraData.getContext().getString(messageRealmObject.isIncoming() ? R.string.chat_delay : R.string.chat_typed,
                    StringUtils.getTimeText(new Date(delayTimestamp)));
            time += " (" + delay + ")";
        }
        Long editedTimestamp = messageRealmObject.getEditedTimestamp();
        if (editedTimestamp != null) {
            time += extraData.getContext().getString(R.string.edited, StringUtils.getTimeText(new Date (editedTimestamp)));
        }

        messageTime.setText(time);

        // set unread status
        isUnread = extraData.isUnread();

        // set date
        needDate = extraData.isNeedDate();
        date = StringUtils.getDateStringForMessage(messageRealmObject.getTimestamp());

        // setup CHECKED
        if (extraData.isChecked())
            itemView.setBackgroundColor(extraData.getContext().getResources()
                    .getColor(R.color.unread_messages_background));
        else itemView.setBackgroundDrawable(null);
    }

    void setupForwarded(MessageRealmObject messageRealmObject, MessagesAdapter.MessageExtraData extraData) {
        String[] forwardedIDs = messageRealmObject.getForwardedIdsAsArray();
        if (!Arrays.asList(forwardedIDs).contains(null)) {
            Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance();
            RealmResults<MessageRealmObject> forwardedMessages = realm
                            .where(MessageRealmObject.class)
                            .in(MessageRealmObject.Fields.UNIQUE_ID, forwardedIDs)
                            .findAll()
                            .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING);

            if (forwardedMessages.size() > 0) {
                RecyclerView recyclerView = forwardLayout.findViewById(R.id.recyclerView);
                ForwardedAdapter adapter = new ForwardedAdapter(forwardedMessages, extraData);
                recyclerView.setLayoutManager(new LinearLayoutManager(extraData.getContext()));
                recyclerView.setAdapter(adapter);
                forwardLayout.setBackgroundColor(ColorManager
                        .getColorWithAlpha(R.color.forwarded_background_color, 0.2f));
                if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
                    forwardLeftBorder.setBackgroundColor(extraData.getAccountMainColor());
                    forwardLeftBorder.setAlpha(1);
                }
                else{
                    forwardLeftBorder.setBackgroundColor(ColorManager.getInstance()
                            .getAccountPainter().getAccountColorWithTint(messageRealmObject.getAccount(), 900));
                    forwardLeftBorder.setAlpha(0.6f);
                }
                forwardLayout.setVisibility(View.VISIBLE);
            }
            if (Looper.myLooper() != Looper.getMainLooper()) realm.close();
        }
    }

    @Override
    public void onClick(View v) {
        int adapterPosition = getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            LogManager.w(LOG_TAG, "onClick: no position");
            return;
        }
        listener.onMessageClick(messageBalloon, adapterPosition);
    }

    @Override
    public boolean onLongClick(View v) {
        int adapterPosition = getAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            LogManager.w(LOG_TAG, "onClick: no position");
            return false;
        }
        longClickListener.onLongMessageClick(adapterPosition);
        return true;
    }


    protected void setUpMessageBalloonBackground(View view, ColorStateList colorList) {
        final Drawable originalBackgroundDrawable = view.getBackground();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            originalBackgroundDrawable.setTintList(colorList);
        else {
            Drawable wrapDrawable = DrawableCompat.wrap(originalBackgroundDrawable);
            DrawableCompat.setTintList(wrapDrawable, colorList);

            int pL = view.getPaddingLeft();
            int pT = view.getPaddingTop();
            int pR = view.getPaddingRight();
            int pB = view.getPaddingBottom();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                view.setBackground(wrapDrawable);
            else view.setBackgroundDrawable(wrapDrawable);
            view.setPadding(pL, pT, pR, pB);
        }

    }
}
