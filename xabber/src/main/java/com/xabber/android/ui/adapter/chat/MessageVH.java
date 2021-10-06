package com.xabber.android.ui.adapter.chat;

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
import android.widget.ImageView;
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
import com.xabber.android.data.database.realmobjects.GroupMemberRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.extension.groups.GroupPrivacyType;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.GroupChat;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.text.ClickTagHandler;
import com.xabber.android.ui.text.CustomQuoteSpan;
import com.xabber.android.ui.text.StringUtilsKt;
import com.xabber.android.ui.widget.CorrectlyMeasuringTextView;

import java.util.Arrays;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MessageVH extends BasicMessageVH implements View.OnClickListener, View.OnLongClickListener {

    private static final String LOG_TAG = MessageVH.class.getSimpleName();
    private final MessageClickListener listener;
    private final MessageLongClickListener longClickListener;
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
    View forwardLayout;
    View forwardLeftBorder;

    public interface MessageClickListener {
        void onMessageClick(View caller, int position);
    }

    public interface MessageLongClickListener {
        void onLongMessageClick(int position);
    }

    public MessageVH(
            View itemView, MessageClickListener listener,
            MessageLongClickListener longClickListener, @StyleRes int appearance
    ) {

        super(itemView, appearance);
        this.listener = listener;
        this.longClickListener = longClickListener;

        messageInfo = itemView.findViewById(R.id.message_info);
        messageTime = itemView.findViewById(R.id.message_time);
        messageHeader = itemView.findViewById(R.id.message_header);
        messageBalloon = itemView.findViewById(R.id.message_balloon);
        messageShadow = itemView.findViewById(R.id.message_shadow);
        statusIcon = itemView.findViewById(R.id.message_status_icon);
        forwardLayout = itemView.findViewById(R.id.forwardLayout);
        forwardLeftBorder = itemView.findViewById(R.id.forwardLeftBorder);

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
                messageText.setTextColor(itemView.getContext().getColor(R.color.grey_200));
            } else {
                messageText.setTextColor(itemView.getContext().getColor(R.color.black));
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
            messageText.setText(spannable, TextView.BufferType.SPANNABLE);
        } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                messageText.setText(
                        StringUtilsKt.getDecodedSpannable(
                                messageRealmObject.getText().trim().concat(
                                        String.valueOf(Character.MIN_VALUE)
                                )
                        ),
                        TextView.BufferType.SPANNABLE
                );
            } else {
                messageText.setText(
                        messageRealmObject.getText().trim().concat(
                                String.valueOf(Character.MIN_VALUE)
                        )
                );
            }

        }

        messageText.setMovementMethod(
                CorrectlyMeasuringTextView.LocalLinkMovementMethod.getInstance()
        );

        // set unread status
        isUnread = extraData.isUnread();

        // set date
        needDate = extraData.isNeedDate();
        date = StringUtilsKt.getDateStringForMessage(messageRealmObject.getTimestamp());

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
                RecyclerView recyclerView = forwardLayout.findViewById(R.id.recyclerView);
                ForwardedAdapter adapter = new ForwardedAdapter(forwardedMessages, extraData);
                recyclerView.setLayoutManager(new LinearLayoutManager(extraData.getContext()));
                recyclerView.setAdapter(adapter);
                forwardLayout.setBackgroundColor(
                        ColorManager.getColorWithAlpha(R.color.forwarded_background_color, 0.2f)
                );
                if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
                    forwardLeftBorder.setBackgroundColor(extraData.getAccountMainColor());
                    forwardLeftBorder.setAlpha(1);
                }
                else{
                    forwardLeftBorder.setBackgroundColor(
                            ColorManager.getInstance().getAccountPainter().getAccountColorWithTint(
                                    messageRealmObject.getAccount(), 900
                            )
                    );
                    forwardLeftBorder.setAlpha(0.6f);
                }
                forwardLayout.setVisibility(View.VISIBLE);
            }
            if (Looper.myLooper() != Looper.getMainLooper()) {
                realm.close();
            }
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
