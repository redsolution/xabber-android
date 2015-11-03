package com.xabber.android.data.extension.blocking;

import com.xabber.android.data.Application;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.OnContactChangedListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local MUC private chat blocking
 */
public class PrivateMucChatBlockingManager {

    private final static PrivateMucChatBlockingManager instance;

    private Map<String, List<String>> blockListsForAccounts;

    static {
        instance = new PrivateMucChatBlockingManager();
        Application.getInstance().addManager(instance);
    }

    public static PrivateMucChatBlockingManager getInstance() {
        return instance;
    }

    public PrivateMucChatBlockingManager() {
        blockListsForAccounts = new ConcurrentHashMap<>();
    }

    public Collection<String> getBlockedContacts(String account) {
        return Collections.unmodifiableCollection(getBlockedListForAccount(account));
    }

    private List<String> getBlockedListForAccount(String account) {
        if (!blockListsForAccounts.containsKey(account)) {
            blockListsForAccounts.put(account, new ArrayList<String>());
        }
        return blockListsForAccounts.get(account);
    }

    public void blockContact(String account, String user) {
        if (!blockListsForAccounts.containsKey(account)) {
            blockListsForAccounts.put(account, new ArrayList<String>());
        }

        blockListsForAccounts.get(account).add(user);

        MessageManager.getInstance().closeChat(account, user);
        NotificationManager.getInstance().removeMessageNotification(account, user);

        notifyListeners(account);
    }

    private void notifyListeners(String account) {
        for (OnBlockedListChangedListener onBlockedListChangedListener
                : Application.getInstance().getUIListeners(OnBlockedListChangedListener.class)) {
            onBlockedListChangedListener.onBlockedListChanged(account);
        }

        for (OnContactChangedListener onContactChangedListener
                : Application.getInstance().getUIListeners(OnContactChangedListener.class)) {
            onContactChangedListener.onContactsChanged(new ArrayList<BaseEntity>());
        }
    }

    public Map<String, List<String>> getBlockedContacts() {
        return Collections.unmodifiableMap(blockListsForAccounts);
    }

    public void unblockContacts(String account, final List<String> contacts) {
        if (blockListsForAccounts.containsKey(account)) {
            blockListsForAccounts.get(account).removeAll(contacts);
        }

        notifyListeners(account);
    }

    public void unblockAll(String account) {
        if (blockListsForAccounts.containsKey(account)) {
            blockListsForAccounts.get(account).clear();
        }

        notifyListeners(account);
    }

}
