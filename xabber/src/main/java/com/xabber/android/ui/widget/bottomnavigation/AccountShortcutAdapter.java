package com.xabber.android.ui.widget.bottomnavigation;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
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

        // set equals view sizes
        if (getItemCount() < 6) {
            int height = 44;
            final float scale = context.getResources().getDisplayMetrics().density;
            int pixelsHeight = (int) (height * scale + 0.5f);

            int width = parent.getMeasuredWidth() / getItemCount();

            view.setLayoutParams(new RecyclerView.LayoutParams(width, pixelsHeight));
        }

        view.setOnClickListener(listener);
        return new AccountShortcutAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AccountShortcutAdapter.ViewHolder holder, int position) {
        AccountShortcutVO account = items.get(position);
        holder.ivAvatar.setImageDrawable(account.getAvatar());
        //holder.ivAvatar.setBorderColor(account.getAccountColorIndicator());
        holder.ivStatus.setVisibility(View.GONE);
        //holder.ivStatus.setImageLevel(account.getStatusLevel());
        holder.ivAvatar.setBorderColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(account.getAccountJid()));
        if (Build.VERSION.SDK_INT > 20){
            holder.ivAvatar.setElevation(6);
            holder.ivAvatarOverlay.setElevation(6);
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
