package com.xabber.android.presentation.ui.contactlist.viewobjects;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.presentation.ui.contactlist.ChatListFragment;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;

import java.util.List;
import java.util.UUID;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractHeaderItem;
import eu.davidea.flexibleadapter.items.IHeader;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by valery.miller on 05.02.18.
 */

public class ToolbarVO extends AbstractHeaderItem<ToolbarVO.ViewHolder> implements IHeader<ToolbarVO.ViewHolder> {

    private String id;
    private Drawable avatar;
    private ChatListFragment.ChatListState currentChatsState;
    private int statusLevel;

    private Context context;
    protected OnClickListener listener;

    public interface OnClickListener {
        void onStateSelected(ChatListFragment.ChatListState state);
        void onAddContactClick();
        void onJoinConferenceClick();
        void onSetStatusClick();
    }

    public ToolbarVO(Context context, OnClickListener listener,
                     ChatListFragment.ChatListState currentChatsState, Drawable avatar, int statusLevel) {
        this.id = UUID.randomUUID().toString();
        this.context = context;
        this.listener = listener;
        this.currentChatsState = currentChatsState;
        this.avatar = avatar;
        this.statusLevel = statusLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ToolbarVO) {
            ToolbarVO inItem = (ToolbarVO) o;
            return this.id.equals(inItem.id);
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_main_title_in_contact_list;
    }

    @Override
    public ToolbarVO.ViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new ToolbarVO.ViewHolder(view, adapter, context, listener);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, ViewHolder holder, int position, List<Object> payloads) {
        Context context = holder.itemView.getContext();

        /** bind AVATAR */
        if (SettingsManager.contactsShowAvatars()){
            holder.ivAvatar.setVisibility(View.VISIBLE);
            holder.ivStatus.setVisibility(View.VISIBLE);
            holder.ivAvatar.setImageDrawable(avatar);
        } else {
            holder.ivAvatar.setVisibility(View.GONE);
            holder.ivStatus.setVisibility(View.GONE);
        }

        /** bind STATUS IMAGE */
        holder.ivStatus.setImageLevel(statusLevel);

        /** set up ACCOUNT COLOR indicator */
        holder.accountColorIndicator.setBackgroundColor(
                ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        holder.accountColorIndicatorBack.setBackgroundColor(
                ColorManager.getInstance().getAccountPainter().getDefaultIndicatorBackColor());

        /** set up BACKGROUND COLOR */
        final int[] accountGroupColors = context.getResources().getIntArray(
                getThemeResource(context, R.attr.contact_list_account_group_background));
        final int level = AccountManager.getInstance().getColorLevel(AccountPainter.getFirstAccount());
        holder.itemView.setBackgroundColor(accountGroupColors[level]);

        /** set up STATE TITLE */
        switch (currentChatsState) {
            case unread:
                holder.tvTitle.setText(R.string.unread_chats);
                break;
            case archived:
                holder.tvTitle.setText(R.string.archived_chats);
                break;
            case all:
                holder.tvTitle.setText(R.string.all_chats);
                break;
            default:
                holder.tvTitle.setText("Xabber");
                break;
        }
    }

    private int getThemeResource(Context context, int themeResourceId) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[] {themeResourceId});
        final int accountGroupColorsResourceId = a.getResourceId(0, 0);
        a.recycle();
        return accountGroupColorsResourceId;
    }

    public class ViewHolder extends FlexibleViewHolder
            implements android.widget.PopupMenu.OnMenuItemClickListener {

        final View accountColorIndicator;
        final View accountColorIndicatorBack;
        final ImageView ivAdd;
        final TextView tvTitle;
        final ImageView ivAvatar;
        final ImageView ivStatus;

        private final Context context;
        private OnClickListener listener;

        public ViewHolder(View view, FlexibleAdapter adapter, Context context, OnClickListener listener) {
            super(view, adapter, true);

            this.context = context;
            this.listener = listener;

            accountColorIndicator = view.findViewById(R.id.accountColorIndicator);
            accountColorIndicatorBack = view.findViewById(R.id.accountColorIndicatorBack);
            ivAdd = (ImageView) view.findViewById(R.id.ivAdd);
            ivAdd.setOnClickListener(this);
            tvTitle = (TextView) view.findViewById(R.id.tvTitle);
            ivAvatar = (ImageView) view.findViewById(R.id.ivAvatar);
            ivStatus = (ImageView) view.findViewById(R.id.ivStatus);
            ivAvatar.setOnClickListener(this);
            tvTitle.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.ivAdd:
                    showToolbarPopup(ivAdd);
                    break;
                case R.id.ivAvatar:
                    listener.onSetStatusClick();
                    break;
                case R.id.tvTitle:
                    showTitlePopup(tvTitle);
                    break;
                default:
                    super.onClick(v);
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_add_contact:
                    listener.onAddContactClick();
                    return true;
                case R.id.action_join_conference:
                    listener.onJoinConferenceClick();
                    return true;
                case R.id.action_recent_chats:
                    listener.onStateSelected(ChatListFragment.ChatListState.recent);
                    return true;
                case R.id.action_unread_chats:
                    listener.onStateSelected(ChatListFragment.ChatListState.unread);
                    return true;
                case R.id.action_archived_chats:
                    listener.onStateSelected(ChatListFragment.ChatListState.archived);
                    return true;
                default:
                    return false;
            }
        }

        private void showToolbarPopup(View v) {
            PopupMenu popupMenu = new PopupMenu(context, v);
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.inflate(R.menu.menu_add_in_contact_list);
            popupMenu.show();
        }

        private void showTitlePopup(View v) {
            PopupMenu popupMenu = new PopupMenu(context, v);
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.inflate(R.menu.menu_chat_list);
            popupMenu.show();
        }
    }
}
