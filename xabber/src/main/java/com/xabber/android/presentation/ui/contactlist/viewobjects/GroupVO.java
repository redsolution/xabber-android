package com.xabber.android.presentation.ui.contactlist.viewobjects;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.entity.AccountJid;
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
    private String title;
    private int expandIndicatorLevel;
    private int offlineIndicatorLevel;
    private String groupName;
    private AccountJid accountJid;

    private boolean mExpanded = true;
    private List<ContactVO> mSubItems;
    private AccountVO mHeader;

    public GroupVO(String title,
                   int expandIndicatorLevel, int offlineIndicatorLevel, String groupName,
                   AccountJid accountJid) {

        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.expandIndicatorLevel = expandIndicatorLevel;
        this.offlineIndicatorLevel = offlineIndicatorLevel;
        this.groupName = groupName;
        this.accountJid = accountJid;
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
        //viewHolder.accountColorIndicator.setBackgroundColor(getAccountColorIndicator());

        viewHolder.indicator.setImageLevel(getExpandIndicatorLevel());

        if (getTitle().equals(com.xabber.android.ui.adapter.contactlist.viewobjects.GroupVO.RECENT_CHATS_TITLE))
            viewHolder.indicator.setVisibility(View.GONE);
        else viewHolder.indicator.setVisibility(View.VISIBLE);

        viewHolder.groupOfflineIndicator.setImageLevel(getOfflineIndicatorLevel());

        viewHolder.groupOfflineIndicator.setVisibility(View.GONE);
        viewHolder.offlineShadow.setVisibility(View.GONE);

        viewHolder.name.setText(getTitle());

        viewHolder.groupOfflineIndicator.setVisibility(View.VISIBLE);

//        if (viewObject.isShowOfflineShadow())
//            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
//        else viewHolder.offlineShadow.setVisibility(View.GONE);
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

    public static GroupVO convert(GroupConfiguration configuration) {

        String name = GroupManager.getInstance().getGroupName(configuration.getAccount(), configuration.getGroup());
        boolean showOfflineShadow = false;
        int accountColorIndicator;
        int expandIndicatorLevel;
        int offlineIndicatorLevel;

        AccountJid account = configuration.getAccount();
        if (account == null || account == GroupManager.NO_ACCOUNT)
            accountColorIndicator = ColorManager.getInstance().getAccountPainter().getDefaultMainColor();
        else accountColorIndicator = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);

        expandIndicatorLevel = configuration.isExpanded() ? 1 : 0;
        offlineIndicatorLevel = configuration.getShowOfflineMode().ordinal();

        if (!name.equals(RECENT_CHATS_TITLE))
            name = String.format("%s (%d/%d)", name, configuration.getOnline(), configuration.getTotal());

        AccountItem accountItem = AccountManager.getInstance().getAccount(configuration.getAccount());

        if (accountItem != null) {
            StatusMode statusMode = accountItem.getDisplayStatusMode();
            if (statusMode == StatusMode.unavailable || statusMode == StatusMode.connection)
                showOfflineShadow = true;
        }

        return new GroupVO(name, expandIndicatorLevel,
                offlineIndicatorLevel, configuration.getGroup(), configuration.getAccount());
    }

    public String getTitle() {
        return title;
    }

    public int getExpandIndicatorLevel() {
        return expandIndicatorLevel;
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

    public class ViewHolder extends ExpandableViewHolder {

        final ImageView indicator;
        final TextView name;
        final ImageView groupOfflineIndicator;
        final ImageView offlineShadow;
        final View accountColorIndicator;

        public ViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);

            itemView.setOnClickListener(this);

            accountColorIndicator = view.findViewById(R.id.accountColorIndicator);
            indicator = (ImageView) view.findViewById(R.id.indicator);
            name = (TextView) view.findViewById(R.id.name);
            groupOfflineIndicator = (ImageView) view.findViewById(R.id.group_offline_indicator);
            offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
        }
    }
}
