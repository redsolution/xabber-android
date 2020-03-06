/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.CircleManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.roster.ShowOfflineMode;
import com.xabber.android.ui.activity.AccountActivity;
import com.xabber.android.ui.activity.ConferenceAddActivity;
import com.xabber.android.ui.activity.ContactAddActivity;
import com.xabber.android.ui.activity.ContactEditActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.activity.ContactViewerActivity;
import com.xabber.android.ui.activity.StatusEditActivity;
import com.xabber.android.ui.dialog.BlockContactDialog;
import com.xabber.android.ui.dialog.ChatDeleteDialog;
import com.xabber.android.ui.dialog.ContactDeleteDialog;
import com.xabber.android.ui.dialog.GroupDeleteDialogFragment;
import com.xabber.android.ui.dialog.GroupRenameDialogFragment;
import com.xabber.android.ui.dialog.MUCDeleteDialogFragment;
import com.xabber.android.ui.dialog.SnoozeDialog;
import com.xabber.android.ui.preferences.CustomNotifySettings;

/**
 * Helper class for context menu creation.
 *
 * @author alexander.ivanov
 */
public class ContextMenuHelper {

    public interface ListPresenter{
        void updateContactList();
    }

    private ContextMenuHelper() {
    }

    public static void createContactContextMenu(final FragmentActivity activity, ListPresenter presenter,
                                                AbstractContact abstractContact, ContextMenu menu) {
        final AccountJid account = abstractContact.getAccount();
        final UserJid user = abstractContact.getUser();
        //menu.setHeaderTitle(abstractContact.getName());
        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(R.menu.item_contact, menu);

        setContactContextMenuActions(activity, presenter, menu, account, user);
        if (activity instanceof ContactListActivity) {
            setContactContextMenuItemsVisibilty(abstractContact, ((ContactListActivity) activity).currentActiveFragment, menu, account, user);
        } else {
            setContactContextMenuItemsVisibilty(abstractContact, ContactListActivity.ActiveFragment.CHATS, menu, account, user);
        }
    }

    private static void setContactContextMenuActions(final FragmentActivity activity,
                                                     final ListPresenter presenter, ContextMenu menu,
                                                     final AccountJid account, final UserJid user) {

        menu.findItem(R.id.action_edit_conference).setIntent(
                ConferenceAddActivity.createIntent(activity, account, user.getBareUserJid()));

        menu.findItem(R.id.action_contact_info).setIntent(ContactViewerActivity.createIntent(activity, account, user));

        menu.findItem(R.id.action_delete_conference).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        MUCDeleteDialogFragment.newInstance(account, user)
                                .show(activity.getFragmentManager(), "MUC_DELETE");
                        return true;
                    }
                });

        menu.findItem(R.id.action_join_conference).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        MUCManager.getInstance().joinRoom(account, user.getJid().asEntityBareJidIfPossible(), true);
                        return true;
                    }
                });

        menu.findItem(R.id.action_leave_conference).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        MUCManager.getInstance().leaveRoom(account, user.getJid().asEntityBareJidIfPossible());
                        MessageManager.getInstance().closeChat(account, user);
                        NotificationManager.getInstance().removeMessageNotification(account, user);
                        presenter.updateContactList();
                        return true;
                    }

                });

        menu.findItem(R.id.action_edit_contact).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        activity.startActivity(ContactEditActivity.createIntent(activity, account, user));
                        return true;
                    }
                });

        menu.findItem(R.id.action_delete_chat).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        ChatDeleteDialog.newInstance(account, user)
                                .show(activity.getSupportFragmentManager(), ChatDeleteDialog.class.getName());
                        return true;
                    }
                });

        menu.findItem(R.id.action_delete_contact).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        ContactDeleteDialog.newInstance(account, user)
                                .show(activity.getSupportFragmentManager(), ContactDeleteDialog.class.getName());
                        return true;
                    }

                });

        menu.findItem(R.id.action_block_contact).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        BlockContactDialog.newInstance(account, user)
                                .show(activity.getSupportFragmentManager(), BlockContactDialog.class.getName());
                        return true;
            }
        });

        menu.findItem(R.id.action_accept_subscription).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        try {
                            PresenceManager.getInstance().acceptSubscription(account, user);
                        } catch (NetworkException e) {
                            Application.getInstance().onError(e);
                        }
                        activity.startActivity(ContactEditActivity.createIntent(activity, account, user));
                        return true;
                    }
                });
        menu.findItem(R.id.action_discard_subscription).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        try {
                            PresenceManager.getInstance()
                                    .discardSubscription(account, user);
                        } catch (NetworkException e) {
                            Application.getInstance().onError(e);
                        }
                        return true;
                    }
                });

        menu.findItem(R.id.action_mute_chat).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AbstractChat chat = MessageManager.getInstance().getChat(account, user);
                        showSnoozeDialog((AppCompatActivity) activity, chat, presenter);
                        return true;
                    }
                });

        menu.findItem(R.id.action_unmute_chat).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AbstractChat chat = MessageManager.getInstance().getChat(account, user);
                        if (chat != null) chat.setNotificationStateOrDefault(
                                new NotificationState(NotificationState.NotificationMode.enabled,
                                        0), true);
                        presenter.updateContactList();
                        return true;
                    }
                });


        menu.findItem(R.id.action_configure_notifications).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        activity.startActivity(CustomNotifySettings.createIntent(activity, account, user));
                        return true;
                    }
                });
    }

    private static void setContactContextMenuItemsVisibilty(AbstractContact abstractContact,
                                                            ContactListActivity.ActiveFragment fragment,
                                                            ContextMenu menu,
                                                            AccountJid account, UserJid user) {
        // all menu items are visible by default
        // it allows to hide items in xml file without touching code

        if (!MUCManager.getInstance().hasRoom(account, user)) {
            // is not conference

            if (menu.findItem(R.id.action_more).getSubMenu() != null) {
                menu.findItem(R.id.action_more).getSubMenu().setGroupVisible(R.id.group_conference_actions, false);
            }

            if (fragment == ContactListActivity.ActiveFragment.CHATS) {

                menu.findItem(R.id.action_delete_contact).setVisible(false);
                if (MessageManager.getInstance().getChat(account, user) == null) {
                    menu.findItem(R.id.action_delete_chat).setVisible(false);
                }
            } else {
                menu.findItem(R.id.action_delete_chat).setVisible(false);
                if (RosterManager.getInstance().getRosterContact(account, user) == null) {
                    menu.findItem(R.id.action_delete_contact).setVisible(false);
                }
            }

            //menu.findItem(R.id.action_configure_notifications).setVisible(false);

            Boolean supported = BlockingManager.getInstance().isSupported(account);

            if ((supported == null || !supported)
                    && !MUCManager.getInstance().isMucPrivateChat(account, user)) {
                menu.findItem(R.id.action_block_contact).setVisible(false);
            }
        } else { // is conference

            menu.findItem(R.id.action_delete_chat).setVisible(false);
            menu.findItem(R.id.action_edit_contact).setVisible(false);
            menu.findItem(R.id.action_delete_contact).setVisible(false);
            menu.findItem(R.id.action_block_contact).setVisible(false);
//            menu.findItem(R.id.action_request_subscription).setVisible(false);

            if (MUCManager.getInstance().inUse(account, user.getJid().asEntityBareJidIfPossible())) {
                menu.findItem(R.id.action_edit_conference).setVisible(false);
            }

            if (MUCManager.getInstance().isDisabled(account, user.getJid().asEntityBareJidIfPossible())) {
                menu.findItem(R.id.action_leave_conference).setVisible(false);
            } else {
                menu.findItem(R.id.action_join_conference).setVisible(false);
            }

        }

        if (!PresenceManager.getInstance().hasSubscriptionRequest(account, user)) {
            menu.findItem(R.id.action_accept_subscription).setVisible(false);
            menu.findItem(R.id.action_discard_subscription).setVisible(false);
        }

        // archive/unarchive chat
        AbstractChat chat = MessageManager.getInstance().getChat(account, user);

        // mute chat
        menu.findItem(R.id.action_mute_chat).setVisible(chat != null && chat.notifyAboutMessage());
        menu.findItem(R.id.action_unmute_chat).setVisible(chat != null && !chat.notifyAboutMessage());
    }

    public static void createGroupContextMenu(final Activity activity,
              final ListPresenter presenter, final AccountJid account, final String group, ContextMenu menu) {
        menu.setHeaderTitle(CircleManager.getInstance().getGroupName(account, group));
        if (!group.equals(CircleManager.ACTIVE_CHATS) && !group.equals(CircleManager.IS_ROOM)) {
            menu.add(R.string.circle_rename).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            GroupRenameDialogFragment.newInstance(
                                    account.equals(CircleManager.NO_ACCOUNT) ? null : account,
                                    group.equals(CircleManager.NO_GROUP) ? null
                                            : group).show(activity.getFragmentManager(),
                                    "GROUP_RENAME");
                            return true;
                        }
                    });
            if (!group.equals(CircleManager.NO_GROUP)) {
                menu.add(R.string.circle_remove).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                GroupDeleteDialogFragment.newInstance(
                                        account.equals(CircleManager.NO_ACCOUNT) ? null : account, group)
                                        .show(activity.getFragmentManager(), "GROUP_DELETE");
                                return true;
                            }
                        });
            }
        }
        if (!group.equals(CircleManager.ACTIVE_CHATS)) {
                menu.add(R.string.show_offline_settings).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        createOfflineContactsDialog(activity, presenter, account, group).show();
                        return true;
                    }
                });
        }

        if (!group.equals(CircleManager.NO_GROUP)) {
            menu.add(R.string.configure_notifications).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        activity.startActivity(CustomNotifySettings.createIntent(activity, account, group));
                        return true;
                    }
                });
        }
    }

    public static void createAccountContextMenu( final Activity activity, final ListPresenter presenter,
                                                 final AccountJid account, ContextMenu menu) {
        activity.getMenuInflater().inflate(R.menu.item_account_group, menu);
        menu.setHeaderTitle(AccountManager.getInstance().getVerboseName(account));

        setUpAccountMenu(activity, presenter, account, menu);
    }

    public static void setUpAccountMenu(final Activity activity, final ListPresenter presenter, final AccountJid account, Menu menu) {
        final AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            return;
        }
        
        ConnectionState state = accountItem.getState();

        if (state == ConnectionState.waiting) {
            menu.findItem(R.id.action_reconnect_account).setVisible(true).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            accountItem.disconnect();
                            AccountManager.getInstance().onAccountChanged(account);
                            return true;
                        }

                    });
        }

        menu.findItem(R.id.action_edit_account_status).setIntent(StatusEditActivity.createIntent(activity, account));
        menu.findItem(R.id.action_edit_account).setIntent(AccountActivity.createIntent(activity, account));
        menu.add(R.string.configure_notifications).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        activity.startActivity(CustomNotifySettings.createIntent(activity, account));
                        return true;
                    }
                });

        if (state.isConnected()) {
            menu.findItem(R.id.action_add_contact).setVisible(true).setIntent(ContactAddActivity.createIntent(activity, account));
        }

        if (SettingsManager.contactsShowAccounts()) {
            menu.findItem(R.id.action_set_up_offline_contacts).setVisible(true)
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            ContextMenuHelper.createOfflineContactsDialog(activity, presenter
                                    ,
                                    account, CircleManager.IS_ACCOUNT).show();
                            return true;
                        }
                    });
        }
    }

    public static AlertDialog createOfflineContactsDialog(Context context, final ListPresenter presenter,
                                                          final AccountJid account, final String group) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.show_offline_settings)
                .setSingleChoiceItems(
                        R.array.offline_contacts_show_option,
                        CircleManager.getInstance().getShowOfflineMode(account, group).ordinal(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                CircleManager.getInstance().setShowOfflineMode(account,
                                        group, ShowOfflineMode.values()[which]);
                                presenter.updateContactList();
                                dialog.dismiss();
                            }
                        }).create();
    }

    private static void showSnoozeDialog(AppCompatActivity activity, AbstractChat chat, final ListPresenter presenter) {
        SnoozeDialog dialog = SnoozeDialog.newInstance(chat, new SnoozeDialog.OnSnoozeListener() {
            @Override
            public void onSnoozed() {
                presenter.updateContactList();
            }
        });
        dialog.show(activity.getSupportFragmentManager(), "snooze_fragment");
    }
}
