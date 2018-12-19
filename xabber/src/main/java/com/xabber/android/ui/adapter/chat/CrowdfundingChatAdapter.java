package com.xabber.android.ui.adapter.chat;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.realm.CrowdfundingMessage;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.StringUtils;
import com.xabber.android.utils.Utils;

import java.util.Date;

import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class CrowdfundingChatAdapter extends RealmRecyclerViewAdapter<CrowdfundingMessage, CrowdfundingChatAdapter.CrowdMessageVH> {

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
    public CrowdMessageVH onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        return new CrowdMessageVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_incoming, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CrowdMessageVH holder, int i) {
        final CrowdfundingMessage message = getMessage(i);
        if (message == null) return;

        // appearance
        holder.messageText.setTextAppearance(context, appearanceStyle);

        String text = message.getMessageForCurrentLocale();
        if (text == null) text = "";

        // text or image
        if (FileManager.isImageUrl(text)) {
            holder.messageImage.setVisibility(View.VISIBLE);
            final ImageView image = holder.messageImage;
            final TextView textMessage = holder.messageText;
            Glide.with(context)
                .load(text)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        image.setVisibility(View.GONE);
                        textMessage.setVisibility(View.VISIBLE);
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(holder.messageImage);
        } else {
            holder.messageImage.setVisibility(View.GONE);
            holder.messageText.setText(Html.fromHtml(text));
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
        ImageView statusIcon;
        ImageView ivEncrypted;
        ImageView avatar;
        ImageView avatarBackground;
        ImageView messageImage;
        View messageShadow;
        View messageBalloon;

        CrowdMessageVH(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.message_text);
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
