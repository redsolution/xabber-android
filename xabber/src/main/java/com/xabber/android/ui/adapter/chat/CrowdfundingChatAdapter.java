package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.realm.CrowdfundingMessage;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.StringUtils;
import com.xabber.android.utils.Utils;

import java.util.Date;

import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class CrowdfundingChatAdapter extends RealmRecyclerViewAdapter<CrowdfundingMessage, CrowdfundingChatAdapter.CrowdMessageVH> {

    public static final int VIEW_TYPE_MESSAGE = 1;
    public static final int VIEW_TYPE_MESSAGE_NOFLEX = 2;

    private BindListener listener;
    private final int appearanceStyle = SettingsManager.chatsAppearanceStyle();

    public CrowdfundingChatAdapter(Context context, RealmResults<CrowdfundingMessage> realmResults,
                                   boolean automaticUpdate, BindListener listener) {
        super(context, realmResults, automaticUpdate);
        this.listener = listener;
    }

    public interface BindListener {
        void onBind(CrowdfundingMessage message);
    }

    @Nullable
    public CrowdfundingMessage getMessage(int position) {
        if (position == RecyclerView.NO_POSITION) return null;

        if (position < realmResults.size())
            return realmResults.get(position);
        else return null;
    }

    @NonNull
    @Override
    public CrowdMessageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_MESSAGE_NOFLEX)
            return new CrowdMessageVH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_incoming_noflex, parent, false));
        else return new CrowdMessageVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_incoming, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        final CrowdfundingMessage message = getMessage(position);
        if (message == null) return 0;

        String text = message.getMessageForCurrentLocale();
        if (FileManager.isImageUrl(text)) return VIEW_TYPE_MESSAGE_NOFLEX;
        else return VIEW_TYPE_MESSAGE;
    }

    @Override
    public void onBindViewHolder(@NonNull CrowdMessageVH holder, int i) {
        final CrowdfundingMessage message = getMessage(i);
        if (message == null) return;

        // appearance
        holder.messageText.setTextAppearance(context, appearanceStyle);

        // text
        String text = message.getMessageForCurrentLocale();
        if (text == null) text = "";
        holder.messageText.setText(Html.fromHtml(text));
        // to avoid bug - https://issuetracker.google.com/issues/36907309
        holder.messageText.setAutoLinkMask(0);
        holder.messageText.setMovementMethod(LinkMovementMethod.getInstance());

        // text or image
        if (FileManager.isImageUrl(text)) {
            holder.messageImage.setVisibility(View.VISIBLE);
            holder.messageText.setVisibility(View.GONE);
            final ImageView image = holder.messageImage;
            final TextView textMessage = holder.messageText;

            Glide.with(context)
                .load(text)
                .placeholder(R.drawable.ic_recent_image_placeholder)
                .error(R.drawable.ic_recent_image_placeholder)
                .into(new SimpleTarget<GlideDrawable>() {
                    @Override
                    public void onLoadStarted(Drawable placeholder) {
                        super.onLoadStarted(placeholder);
                        image.setImageDrawable(placeholder);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        super.onLoadFailed(e, errorDrawable);
                        image.setImageDrawable(errorDrawable);
                        textMessage.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                        image.setImageDrawable(resource);
                    }
                });
        } else {
            holder.messageImage.setVisibility(View.GONE);
            holder.messageText.setVisibility(View.VISIBLE);
        }

        // nickname
        String nick = message.getNameForCurrentLocale();
        if (nick != null) {
            holder.messageHeader.setText(nick);
            holder.messageHeader.setTextColor(ColorManager.changeColor(
                    ColorGenerator.MATERIAL.getColor(nick), 0.8f));
            holder.messageHeader.setVisibility(View.VISIBLE);
        } else holder.messageHeader.setVisibility(View.GONE);

        // time
        String time = StringUtils.getTimeText(new Date((long)message.getReceivedTimestamp()*1000));
        holder.messageTime.setText(time);

        // status
        holder.statusIcon.setVisibility(View.GONE);
        holder.ivEncrypted.setVisibility(View.GONE);

        // need tail
        boolean needTail = false;
        CrowdfundingMessage nextMessage = getMessage(i + 1);
        if (nextMessage != null)
            needTail = !message.getAuthorJid().equals(nextMessage.getAuthorJid());
        else needTail = true;

        // date
        boolean needDate;
        CrowdfundingMessage previousMessage = getMessage(i - 1);
        if (previousMessage != null) {
            needDate = !Utils.isSameDay((long) message.getReceivedTimestamp()*1000,
                    (long) previousMessage.getReceivedTimestamp()*1000);
        } else needDate = true;

        if (holder.tvDate != null) {
            if (needDate) {
                holder.tvDate.setText(StringUtils.getDateStringForMessage(
                        (long) message.getReceivedTimestamp()*1000));
                holder.tvDate.setVisibility(View.VISIBLE);
            } else holder.tvDate.setVisibility(View.GONE);
        }

        // avatar
        String avatarUrl = message.getAuthorAvatar();
        if (needTail && avatarUrl != null) {
            setupAvatar(holder.avatar, avatarUrl);
            holder.avatar.setVisibility(View.VISIBLE);
            holder.avatarBackground.setVisibility(View.VISIBLE);
        } else {
            holder.avatar.setVisibility(View.INVISIBLE);
            holder.avatarBackground.setVisibility(View.INVISIBLE);
        }

        // setup BACKGROUND
        Drawable balloonDrawable = context.getResources()
                .getDrawable(needTail ? R.drawable.msg_in : R.drawable.msg);
        Drawable shadowDrawable = context.getResources()
                .getDrawable(needTail ? R.drawable.msg_in_shadow : R.drawable.msg_shadow);
        shadowDrawable.setColorFilter(context.getResources().getColor(R.color.black), PorterDuff.Mode.MULTIPLY);
        holder.messageBalloon.setBackgroundDrawable(balloonDrawable);
        holder.messageShadow.setBackgroundDrawable(shadowDrawable);

        // setup BALLOON margins
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        layoutParams.setMargins(
                Utils.dipToPx(needTail ? 2f : 11f, context),
                Utils.dipToPx(2f, context),
                Utils.dipToPx(0f, context),
                Utils.dipToPx(2f, context));
        holder.messageShadow.setLayoutParams(layoutParams);

        // setup MESSAGE padding
        holder.messageBalloon.setPadding(
                Utils.dipToPx(needTail ? 20f : 12f, context),
                Utils.dipToPx(8f, context),
                Utils.dipToPx(12f, context),
                Utils.dipToPx(8f, context));

        holder.itemView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) { listener.onBind(message); }

            @Override
            public void onViewDetachedFromWindow(View v) { }
        });
    }

    private void setupAvatar(ImageView view, String url) {
        Glide.with(context).load(url).into(view);
    }

    public class CrowdMessageVH extends RecyclerView.ViewHolder {

        TextView messageText;
        TextView messageHeader;
        TextView messageTime;
        TextView tvDate;
        ImageView statusIcon;
        ImageView ivEncrypted;
        ImageView avatar;
        ImageView avatarBackground;
        ImageView messageImage;
        View messageShadow;
        View messageBalloon;

        CrowdMessageVH(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.message_text);
            tvDate = itemView.findViewById(R.id.tvDate);
            messageHeader = itemView.findViewById(R.id.message_header);
            statusIcon = itemView.findViewById(R.id.message_status_icon);
            ivEncrypted = itemView.findViewById(R.id.message_encrypted_icon);
            messageTime = itemView.findViewById(R.id.message_time);
            avatar = itemView.findViewById(R.id.avatar);
            avatarBackground = itemView.findViewById(R.id.avatarBackground);
            messageShadow = itemView.findViewById(R.id.message_shadow);
            messageBalloon = itemView.findViewById(R.id.message_balloon);
            messageImage = itemView.findViewById(R.id.message_image);
        }
    }

}
