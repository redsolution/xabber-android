package com.xabber.android.ui.adapter;

import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.ui.activity.ManagedActivity;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.widget.ItemTouchHelperAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by valery.miller on 29.08.17.
 */

public class AccountListReorderAdapter extends RecyclerView.Adapter implements ItemTouchHelperAdapter {

    @SuppressWarnings("WeakerAccess")
    static final String LOG_TAG = AccountListAdapter.class.getSimpleName();
    @SuppressWarnings("WeakerAccess")
    List<AccountItem> accountItems;
    @SuppressWarnings("WeakerAccess")
    AccountListAdapter.Listener listener;
    ManagedActivity activity;

    public interface Listener {
        void onAccountClick(AccountJid account);
        void onEditAccountStatus(AccountItem accountItem);
        void onEditAccount(AccountItem accountItem);
        void onDeleteAccount(AccountItem accountItem);
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public AccountListReorderAdapter(ManagedActivity activity, AccountListAdapter.Listener listener) {
        this.accountItems = new ArrayList<>();
        this.activity = activity;
        this.listener = listener;
    }

    public void setAccountItems(List<AccountItem> accountItems) {
        this.accountItems = accountItems;
        Collections.sort(accountItems);
        notifyDataSetChanged();
    }

    public List<AccountItem> getItems() {
        return accountItems;
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(accountItems, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(accountItems, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AccountListReorderAdapter.AccountViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account_for_reorder, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final AccountListReorderAdapter.AccountViewHolder accountHolder = (AccountListReorderAdapter.AccountViewHolder) holder;
        AccountItem accountItem = accountItems.get(position);

        accountHolder.avatar.setImageDrawable(
                AvatarManager.getInstance().getAccountAvatar(accountItem.getAccount()));
        accountHolder.avatar.setBorderColor(ColorManager.getInstance().getAccountPainter().
                getAccountMainColor(accountItem.getAccount()));

        accountHolder.name.setText(AccountManager.getInstance().getVerboseName(accountItem.getAccount()));
        accountHolder.status.setText(accountItem.getState().getStringId());

        accountHolder.ivAnchor.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) ==
                        MotionEvent.ACTION_DOWN) {
                    listener.onStartDrag(accountHolder);
                }
                return false;
            }
        });

    }

    @Override
    public int getItemCount() {
        return accountItems.size();
    }

    private class AccountViewHolder extends RecyclerView.ViewHolder {
        CircleImageView avatar;
        TextView name;
        TextView status;
        ImageView ivAnchor;


        AccountViewHolder(View itemView) {
            super(itemView);
            avatar = (CircleImageView) itemView.findViewById(R.id.avatar);
            name = (TextView) itemView.findViewById(R.id.item_account_name);
            status = (TextView) itemView.findViewById(R.id.item_account_status);
            ivAnchor = (ImageView) itemView.findViewById(R.id.ivAnchor);
        }
    }

}
