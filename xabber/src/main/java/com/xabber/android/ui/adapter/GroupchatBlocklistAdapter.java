package com.xabber.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.groupchat.block.GroupchatBlocklistItemElement;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.fragment.groups.GroupchatInfoFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupchatBlocklistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<BlockedElementHolder> listOfItems;
    private List<GroupchatBlocklistItemElement> blockedItems;
    private Set<String> selectedJids;
    private ArrayList<BlockedElementHolder> selectedItems;

    GroupchatInfoFragment.GroupchatSelectorListItemActions listener;
    private boolean clicksAreDisabled = false;

    /**
     * Comparator that is used to sort a list of general
     * elements of the blocklist(headers and blocked items)
     */
    private static final Comparator<BlockedElementHolder> blockedElementComparator = (o1, o2) -> {
        int blockType1 = o1.getBlockedItemType();
        int blockType2 = o2.getBlockedItemType();
        int result1 = Integer.compare(blockType1, blockType2);
        if (result1 == 0) {
            int holderType1 = o1.getItemVHType();
            int holderType2 = o2.getItemVHType();
            return Integer.compare(holderType1, holderType2);
        }
        return result1;
    };

    public GroupchatBlocklistAdapter() {
        listOfItems = new ArrayList<>();
        blockedItems = new ArrayList<>();
        selectedJids = new HashSet<>();
        selectedItems = new ArrayList<>();
    }

    public void disableItemClicks(boolean disable) {
        clicksAreDisabled = disable;
    }

    public void setListener(GroupchatInfoFragment.GroupchatSelectorListItemActions listener) {
        this.listener = listener;
    }

    public void setBlockedItems(List<GroupchatBlocklistItemElement> blockedItems) {
        this.blockedItems.clear();
        this.blockedItems.addAll(blockedItems);
        updateListOfItems();
    }

    public void cancelSelection() {
        this.selectedJids.clear();
        notifyDataSetChanged();
    }

    public void removeSelectionStateFrom(List<String> successfulUnblocks) {
        for (BlockedElementHolder holder : selectedItems) {
            if (successfulUnblocks.contains(holder.blockedItem)) {
                selectedItems.remove(holder);
                selectedJids.remove(holder.blockedItem);
            }
        }
        notifyDataSetChanged();
    }

    public ArrayList<GroupchatBlocklistItemElement> getSelectedItems() {
        ArrayList<GroupchatBlocklistItemElement> selectedXmlItems = new ArrayList<>();
        for (BlockedElementHolder elementHolder : selectedItems) {
            selectedXmlItems.add(
                    new GroupchatBlocklistItemElement(
                            BlockedElementHolder.intDefToItemType(
                                    elementHolder.getBlockedItemType()
                            ),
                            elementHolder.getBlockedItem()
                    )
            );
        }
        return selectedXmlItems;
    }

    private void updateListOfItems() {
        listOfItems.clear();
        int[] blockedValuesCounter = new int[GroupchatBlocklistItemElement.ItemType.values().length];
        for (GroupchatBlocklistItemElement element : blockedItems) {
            blockedValuesCounter[element.getItemType().ordinal()]++;
            BlockedElementHolder item = new BlockedElementHolder(
                    BlockedElementHolder.blockedElement, element.getItemType());
            item.setBlockedString(element.getBlockedItem());
            listOfItems.add(item);
        }
//        for (GroupchatBlocklistItemElement.ItemType type : GroupchatBlocklistItemElement.ItemType.values()) {
//            if (blockedValuesCounter[type.ordinal()] > 0) {
//                BlockedElementHolder item = new BlockedElementHolder(
//                        BlockedElementHolder.headerElement, type);
//                listOfItems.add(item);
//            }
//        }
        Collections.sort(listOfItems, blockedElementComparator);
        notifyDataSetChanged();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        listener = null;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupchatBlockViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_groupchat_block, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        return listOfItems.get(position).getItemVHType();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BlockedElementHolder item = listOfItems.get(position);

        if (item.getItemVHType() == BlockedElementHolder.blockedElement) {
            GroupchatBlockViewHolder blocked = (GroupchatBlockViewHolder) holder;
            try {
                blocked.avatar.setImageDrawable(AvatarManager.getInstance()
                        .getUserAvatarForContactList(ContactJid.from(item.getBlockedItem()), item.getBlockedItem()));
            } catch (ContactJid.UserJidCreateException e) {
                e.printStackTrace();
            }

            blocked.blockJid.setText(item.getBlockedItem());

            blocked.blockCheckBox.setChecked(selectedJids.contains(item.blockedItem));
        }
    }

    @Override
    public int getItemCount() {
        return listOfItems.size();
    }

    class GroupchatBlockViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final String LOG_TAG = GroupchatInvitesAdapter.GroupchatInviteViewHolder.class.getSimpleName();

        private ImageView avatar;
        private TextView blockJid;
        private CheckBox blockCheckBox;

        GroupchatBlockViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            blockJid = itemView.findViewById(R.id.tv_blocked_jid);
            blockCheckBox = itemView.findViewById(R.id.chk_block);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (clicksAreDisabled) {
                return;
            }
            int adapterPosition = getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                LogManager.w(LOG_TAG, "onClick: no position");
                return;
            }

            BlockedElementHolder element = listOfItems.get(adapterPosition);
            boolean actionIsSelect = false;
            if (selectedJids.contains(element.blockedItem)) {
                selectedJids.remove(element.blockedItem);
                selectedItems.remove(element);
                blockCheckBox.setChecked(false);
            } else {
                selectedJids.add(element.blockedItem);
                selectedItems.add(element);
                blockCheckBox.setChecked(true);
                actionIsSelect = true;
            }

            if (listener != null) {
                if (actionIsSelect) {
                    listener.onListItemSelected();
                } else {
                    listener.onListItemDeselected();
                }
            }
        }
    }

    static class BlockedElementHolder {

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({headerElement, blockedElement})
        public @interface BlocklistVHType {}
        public static final int headerElement = 1;
        public static final int blockedElement = 2;

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({blockedJid, blockedDomain, blockedUserId})
        public @interface BlockedElementType {}
        public static final int blockedJid = -3;
        public static final int blockedDomain = -2;
        public static final int blockedUserId = -1;

        @BlocklistVHType
        private int itemVHType;
        @BlockedElementType
        private int blockedItemType;

        private String blockedItem;

        BlockedElementHolder(@BlocklistVHType int itemVHType, @BlockedElementType int blockedItemType) {
            this.itemVHType = itemVHType;
            this.blockedItemType = blockedItemType;
        }

        BlockedElementHolder(@BlocklistVHType int itemVHType, GroupchatBlocklistItemElement.ItemType blockedItemType) {
            this.itemVHType = itemVHType;
            this.blockedItemType = itemTypeToIntDef(blockedItemType);
        }

        public void setBlockedString(String blockedItem) {
            this.blockedItem = blockedItem;
        }

        public String getBlockedItem() { return blockedItem; }

        @BlocklistVHType
        public int getItemVHType() {
            return itemVHType;
        }

        @BlockedElementType
        public int getBlockedItemType() {
            return blockedItemType;
        }

        @BlockedElementType
        private static int itemTypeToIntDef(GroupchatBlocklistItemElement.ItemType itemType) {
            switch (itemType) {
                case id:
                    return blockedUserId;
                case jid:
                    return blockedJid;
                case domain:
                    return blockedDomain;
            }
            throw new IllegalArgumentException("Incorrect type of the BlockListItemElement enum = " + itemType);
        }

        private static GroupchatBlocklistItemElement.ItemType intDefToItemType(@BlockedElementType int blockedItemType) {
            switch (blockedItemType) {
                case blockedJid:
                    return GroupchatBlocklistItemElement.ItemType.jid;
                case blockedDomain:
                    return GroupchatBlocklistItemElement.ItemType.domain;
                case blockedUserId:
                    return GroupchatBlocklistItemElement.ItemType.id;
            }
            throw new IllegalArgumentException("Incorrect type of the BlockedElementType IntDef = " + blockedItemType);
        }
    }
}