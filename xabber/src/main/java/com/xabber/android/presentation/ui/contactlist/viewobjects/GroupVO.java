package com.xabber.android.presentation.ui.contactlist.viewobjects;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager;
import com.xabber.android.data.notification.custom_notification.Key;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.ui.adapter.contactlist.GroupConfiguration;
import com.xabber.android.ui.color.ColorManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IExpandable;
import eu.davidea.flexibleadapter.items.ISectionable;
import eu.davidea.viewholders.ExpandableViewHolder;

/**
 * Created by valery.miller on 02.02.18.
 */

public class GroupVO extends AbstractFlexibleItem<GroupVO.ViewHolder>
        implements IExpandable<GroupVO.ViewHolder, ContactVO>,
        ISectionable<GroupVO.ViewHolder, AccountVO> {

    public static final String RECENT_CHATS_TITLE = "Recent chats";

    private String id;

    private int accountColorIndicator;
    private int accountColorIndicatorBack;
    private boolean showOfflineShadow;

    private String title;
    private int offlineIndicatorLevel;
    private String groupName;
    private AccountJid accountJid;
    private boolean firstInAccount = false;
    private boolean isCustomNotification;

    private boolean mExpanded = true;
    private List<ContactVO> mSubItems;
    private AccountVO mHeader;

    protected final GroupClickListener listener;

    public interface GroupClickListener {
        void onGroupCreateContextMenu(int adapterPosition, ContextMenu menu);
    }

    public GroupVO(int accountColorIndicator, int accountColorIndicatorBack,
                   boolean showOfflineShadow, String title,
                   boolean expanded, int offlineIndicatorLevel, String groupName,
                   AccountJid accountJid, boolean firstInAccount, boolean isCustomNotification,
                   GroupClickListener listener) {

        this.id = UUID.randomUUID().toString();
        this.accountColorIndicator = accountColorIndicator;
        this.accountColorIndicatorBack = accountColorIndicatorBack;
        this.showOfflineShadow = showOfflineShadow;
        this.title = title;
        this.mExpanded = expanded;
        this.offlineIndicatorLevel = offlineIndicatorLevel;
        this.groupName = groupName;
        this.accountJid = accountJid;
        this.firstInAccount = firstInAccount;
        this.isCustomNotification = isCustomNotification;
        this.listener = listener;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GroupVO) {
            GroupVO inItem = (GroupVO) o;
            return this.id.equals(inItem.id);
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_group_in_contact_list;
    }

    @Override
    public ViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new ViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, ViewHolder viewHolder, int position, List<Object> payloads) {

        /** set up OFFLINE SHADOW */
        if (isShowOfflineShadow())
            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
        else viewHolder.offlineShadow.setVisibility(View.GONE);

        /** set up ACCOUNT COLOR indicator */
        viewHolder.accountColorIndicator.setBackgroundColor(getAccountColorIndicator());
        viewHolder.accountColorIndicatorBack.setBackgroundColor(getAccountColorIndicatorBack());

        /** set up EXPAND indicator */
        //viewHolder.indicator.setImageLevel(getExpandIndicatorLevel());
        viewHolder.indicator.setImageLevel(mExpanded ? 1 : 0);

        if (getTitle().equals(GroupVO.RECENT_CHATS_TITLE))
            viewHolder.indicator.setVisibility(View.GONE);
        else viewHolder.indicator.setVisibility(View.VISIBLE);

        /** set up OFFLINE indicator */
        viewHolder.groupOfflineIndicator.setVisibility(View.GONE);
        viewHolder.groupOfflineIndicator.setImageLevel(getOfflineIndicatorLevel());
        viewHolder.groupOfflineIndicator.setVisibility(View.VISIBLE);

        /** set up NAME */
        viewHolder.name.setText(getTitle());

        /** set up divider LINE */
        viewHolder.line.setVisibility(firstInAccount ? View.INVISIBLE : View.VISIBLE);

        /** set up CUSTOM NOTIFICATION */
        Context context = viewHolder.itemView.getContext();
        Resources resources = context.getResources();
        viewHolder.name.setCompoundDrawablesWithIntrinsicBounds(null, null,
                isCustomNotification() ? resources.getDrawable(R.drawable.ic_notif_custom)
                        : null, null);
    }

    @Override
    public AccountVO getHeader() {
        return mHeader;
    }

    @Override
    public void setHeader(AccountVO header) {
        this.mHeader = header;
    }

    @Override
    public boolean isExpanded() {
        return mExpanded;
    }

    @Override
    public void setExpanded(boolean expanded) {
        this.mExpanded = expanded;
        GroupManager.getInstance().setExpanded(accountJid, groupName, mExpanded);
    }

    @Override
    public int getExpansionLevel() {
        return 0;
    }

    @Override
    public List<ContactVO> getSubItems() {
        return mSubItems;
    }

    public void addSubItem(ContactVO subItem) {
        if (mSubItems == null)
            mSubItems = new ArrayList<ContactVO>();
        mSubItems.add(subItem);
    }

    public static GroupVO convert(GroupConfiguration configuration, boolean firstInAccount,
                                  GroupClickListener listener) {

        String name = GroupManager.getInstance().getGroupName(configuration.getAccount(),
                configuration.getGroup());
        boolean showOfflineShadow = false;
        int accountColorIndicator;
        int accountColorIndicatorBack;
        boolean expanded;
        int offlineIndicatorLevel;

        AccountJid account = configuration.getAccount();
        if (account == null || account == GroupManager.NO_ACCOUNT) {
            accountColorIndicator = ColorManager.getInstance().getAccountPainter().getDefaultMainColor();
            accountColorIndicatorBack = ColorManager.getInstance().getAccountPainter().getDefaultIndicatorBackColor();
        } else {
            accountColorIndicator = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);
            accountColorIndicatorBack = ColorManager.getInstance().getAccountPainter().getAccountIndicatorBackColor(account);
        }

        expanded = configuration.isExpanded();
        offlineIndicatorLevel = configuration.getShowOfflineMode().ordinal();

        // custom notification
        boolean isCustomNotification = CustomNotifyPrefsManager.getInstance().
                isPrefsExist(Key.createKey(account, name));

        if (!name.equals(RECENT_CHATS_TITLE))
            name = String.format("%s (%d/%d)", name, configuration.getOnline(), configuration.getTotal());

        AccountItem accountItem = AccountManager.getInstance().getAccount(configuration.getAccount());

        if (accountItem != null) {
            StatusMode statusMode = accountItem.getDisplayStatusMode();
            if (statusMode == StatusMode.unavailable || statusMode == StatusMode.connection)
                showOfflineShadow = true;
        }

        return new GroupVO(accountColorIndicator, accountColorIndicatorBack, showOfflineShadow, name, expanded,
                offlineIndicatorLevel, configuration.getGroup(), configuration.getAccount(),
                firstInAccount, isCustomNotification, listener);
    }

    public String getTitle() {
        return title;
    }

    public boolean isCustomNotification() {
        return isCustomNotification;
    }

    public int getOfflineIndicatorLevel() {
        return offlineIndicatorLevel;
    }

    public String getGroupName() {
        return groupName;
    }

    public AccountJid getAccountJid() {
        return accountJid;
    }

    public int getAccountColorIndicator() {
        return accountColorIndicator;
    }

    public int getAccountColorIndicatorBack() {
        return accountColorIndicatorBack;
    }

    public boolean isShowOfflineShadow() {
        return showOfflineShadow;
    }

    public class ViewHolder extends ExpandableViewHolder implements View.OnCreateContextMenuListener {

        final ImageView indicator;
        final TextView name;
        final ImageView groupOfflineIndicator;
        final ImageView offlineShadow;
        final View accountColorIndicator;
        final View accountColorIndicatorBack;
        final View line;

        public ViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);

            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);

            accountColorIndicator = view.findViewById(R.id.accountColorIndicator);
            accountColorIndicatorBack = view.findViewById(R.id.accountColorIndicatorBack);
            indicator = (ImageView) view.findViewById(R.id.indicator);
            name = (TextView) view.findViewById(R.id.name);
            groupOfflineIndicator = (ImageView) view.findViewById(R.id.group_offline_indicator);
            offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
            line = view.findViewById(R.id.line);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;
            listener.onGroupCreateContextMenu(adapterPosition, menu);
        }
    }
}
