package com.xabber.android.ui.widget.bottomnavigation;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.ui.color.ColorManager;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by valery.miller on 06.10.17.
 */

public class AccountShortcutAdapter extends RecyclerView.Adapter<AccountShortcutAdapter.ViewHolder> {

    private ArrayList<AccountShortcutVO> items;
    private Context context;
    private View.OnClickListener listener;

    public AccountShortcutAdapter(ArrayList<AccountShortcutVO> items, Context context,
                                  View.OnClickListener listener) {
        this.items = items;
        this.context = context;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public CircleImageView ivAvatar;
        public ImageView ivStatus;
        public CircleImageView ivAvatarOverlay;

        public ViewHolder(View v) {
            super(v);
            ivAvatar = (CircleImageView) v.findViewById(R.id.ivAvatar);
            ivStatus = (ImageView) v.findViewById(R.id.ivStatus);
            ivAvatarOverlay = (CircleImageView) v.findViewById(R.id.ivAvatarOverlay);
        }
    }


    @Override
    public AccountShortcutAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.avatar_view_small, parent, false);
        view.setOnClickListener(listener);
        return new AccountShortcutAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AccountShortcutAdapter.ViewHolder holder, int position) {
        AccountShortcutVO account = items.get(position);
        holder.ivAvatar.setImageDrawable(account.getAvatar());
        holder.ivAvatar.setCircleBackgroundColor(account.getAccountColorIndicator());
        holder.ivAvatar.setBorderColor(ColorManager.getInstance().getUnreadMessageBackground(account.getAccountJid()));
        holder.ivAvatar.setBorderWidth(1);
        holder.ivStatus.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT > 20){
            holder.ivAvatar.setElevation(4);
            holder.ivAvatarOverlay.setElevation(4);
            holder.ivAvatar.setPadding(0,0,0,6);
            holder.ivAvatarOverlay.setPadding(0,0,0, 6);
            holder.itemView.setPaddingRelative(0,15,0,12);
        }
        if (AccountManager.getInstance().getAccount(account.getAccountJid()).getDisplayStatusMode() != null){
            switch (AccountManager.getInstance().getAccount(account.getAccountJid()).getDisplayStatusMode()){
                case unavailable:
                    holder.ivAvatarOverlay.setVisibility(View.VISIBLE);
                    holder.ivAvatarOverlay.clearAnimation();
                    break;
                case connection:
                    holder.ivAvatarOverlay.setVisibility(View.VISIBLE);
                    holder.ivAvatarOverlay.startAnimation(AnimationUtils.loadAnimation(context, R.anim.connection));
                    break;
                default:
                    holder.ivAvatarOverlay.setVisibility(View.GONE);
                    holder.ivAvatarOverlay.clearAnimation();
            }
        }

    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
