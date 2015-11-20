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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.ShowOfflineMode;
import com.xabber.android.ui.activity.AccountViewer;
import com.xabber.android.ui.activity.ChatViewer;
import com.xabber.android.ui.activity.ConferenceAdd;
import com.xabber.android.ui.activity.ContactAdd;
import com.xabber.android.ui.activity.ContactEditor;
import com.xabber.android.ui.activity.GroupEditor;
import com.xabber.android.ui.activity.StatusEditor;
import com.xabber.android.ui.adapter.UpdatableAdapter;
import com.xabber.android.ui.dialog.BlockContactDialog;
import com.xabber.android.ui.dialog.ContactDeleteDialogFragment;
import com.xabber.android.ui.dialog.GroupDeleteDialogFragment;
import com.xabber.android.ui.dialog.GroupRenameDialogFragment;
import com.xabber.android.ui.dialog.MUCDeleteDialogFragment;

/**
 * Helper class for context menu creation.
 *
 * @author alexander.ivanov
 */
public class ContextMenuHelper {

    private ContextMenuHelper() {
    }

    public static void createContactContextMenu(final FragmentActivity activity,
            final UpdatableAdapter adapter, AbstractContact abstractContact, ContextMenu menu) {
        final String account = abstractContact.getAccount();
        final String user = abstractContact.getUser();
        menu.setHeaderTitle(abstractContact.getName());
        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(R.menu.contact_list_contact_context_menu, menu);

        setContactContextMenuActions(activity, adapter, menu, account, user);
        setContactContextMenuItemsVisibilty(abstractContact, menu, account, user);
    }

    private static void setContactContextMenuActions(final FragmentActivity activity,
                                                     final UpdatableAdapter adapter,
                                                     ContextMenu menu,
                                                     final String account, final String user) {
        menu.findItem(R.id.action_chat).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        MessageManager.getInstance().openChat(account, user);
                        activity.startActivity(ChatViewer.createSpecificChatIntent(
                                activity, account, user));
                        return true;
                    }
                });

        menu.findItem(R.id.action_edit_conference).setIntent(
                ConferenceAdd.createIntent(activity, account, user));

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
                        MUCManager.getInstance().joinRoom(account,
                                user, true);
                        return true;
                    }
                });

        menu.findItem(R.id.action_leave_conference).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        MUCManager.getInstance().leaveRoom(account, user);
                        MessageManager.getInstance().closeChat(account, user);
                        NotificationManager.getInstance().removeMessageNotification(account, user);
                        adapter.onChange();
                        return true;
                    }

                });

        menu.findItem(R.id.action_contact_info).setIntent(
                ContactEditor.createIntent(activity, account, user));
        menu.findItem(R.id.action_edit_contact_groups).setIntent(
                GroupEditor.createIntent(activity, account, user));

        menu.findItem(R.id.action_delete_contact).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        ContactDeleteDialogFragment.newInstance(account,
                                user).show(activity.getFragmentManager(), "CONTACT_DELETE");
                        return true;
                    }

                });

        menu.findItem(R.id.action_block_contact).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        BlockContactDialog.newInstance(account, user).show(activity.getFragmentManager(), BlockContactDialog.class.getName());
                        return true;
            }
        });

        menu.findItem(R.id.action_close_chat).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        MessageManager.getInstance().closeChat(account,
                                user);
                        NotificationManager.getInstance()
                                .removeMessageNotification(account,
                                        user);
                        adapter.onChange();
                        return true;
                    }

                });

        menu.findItem(R.id.action_request_subscription)
                .setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                try {
                                    PresenceManager.getInstance()
                                            .requestSubscription(
                                                    account, user);
                                } catch (NetworkException e) {
                                    Application.getInstance()
                                            .onError(e);
                                }
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
                        activity.startActivity(GroupEditor.createIntent(activity, account, user));
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
    }

    private static void setContactContextMenuItemsVisibilty(AbstractContact abstractContact,
                                                            ContextMenu menu,
                                                            String account, String user) {
        // all menu items are visible by default
        // it allows to hide items in xml file without touching code

        if (!MUCManager.getInstance().hasRoom(account, user)) {
            // is not conference

            menu.findItem(R.id.action_edit_conference).setVisible(false);
            menu.findItem(R.id.action_delete_conference).setVisible(false);
            menu.findItem(R.id.action_leave_conference).setVisible(false);
            menu.findItem(R.id.action_join_conference).setVisible(false);

            if (!MessageManager.getInstance().hasActiveChat(account, user)) {
                menu.findItem(R.id.action_close_chat).setVisible(false);
            }

            if (!BlockingManager.getInstance().isSupported(account)
                    && !MUCManager.getInstance().isMucPrivateChat(account, user)) {
                menu.findItem(R.id.action_block_contact).setVisible(false);
            }
            if (abstractContact.getStatusMode() != StatusMode.unsubscribed) {
                menu.findItem(R.id.action_request_subscription).setVisible(false);
            }
        } else { // is conference

            menu.findItem(R.id.action_contact_info).setVisible(false);
            menu.findItem(R.id.action_edit_contact_groups).setVisible(false);
            menu.findItem(R.id.action_delete_contact).setVisible(false);
            menu.findItem(R.id.action_block_contact).setVisible(false);
            menu.findItem(R.id.action_close_chat).setVisible(false);
            menu.findItem(R.id.action_request_subscription).setVisible(false);

            if (MUCManager.getInstance().inUse(account, user)) {
                menu.findItem(R.id.action_edit_conference).setVisible(false);
            }

            if (MUCManager.getInstance().isDisabled(account, user)) {
                menu.findItem(R.id.action_leave_conference).setVisible(false);
            } else {
                menu.findItem(R.id.action_join_conference).setVisible(false);
            }

        }

        if (!PresenceManager.getInstance().hasSubscriptionRequest(account, user)) {
            menu.findItem(R.id.action_accept_subscription).setVisible(false);
            menu.findItem(R.id.action_discard_subscription).setVisible(false);
        }
    }

    public static void createGroupContextMenu(final FragmentActivity activity,
              final UpdatableAdapter adapter, final String account, final String group, ContextMenu menu) {
        menu.setHeaderTitle(GroupManager.getInstance().getGroupName(account, group));
        if (!group.equals(GroupManager.ACTIVE_CHATS) && !group.equals(GroupManager.IS_ROOM)) {
            menu.add(R.string.group_rename).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            GroupRenameDialogFragment.newInstance(
                                    account.equals(GroupManager.NO_ACCOUNT) ? null : account,
                                    group.equals(GroupManager.NO_GROUP) ? null
                                            : group).show(activity.getFragmentManager(),
                                    "GROUP_RENAME");
                            return true;
                        }
                    });
            if (!group.equals(GroupManager.NO_GROUP)) {
                menu.add(R.string.group_remove).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                GroupDeleteDialogFragment.newInstance(
                                        account.equals(GroupManager.NO_ACCOUNT) ? null : account, group)
                                        .show(activity.getFragmentManager(), "GROUP_DELETE");
                                return true;
                            }
                        });
            }
        }
        if (!group.equals(GroupManager.ACTIVE_CHATS)) {
                menu.add(R.string.show_offline_settings).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        createOfflineContactsDialog(activity, adapter, account, group).show();
                        return true;
                    }
                });
        }
    }

    public static void createAccountContextMenu( final FragmentActivity activity, final UpdatableAdapter adapter,
                                                 final String account, ContextMenu menu) {
        activity.getMenuInflater().inflate(R.menu.account, menu);
        menu.setHeaderTitle(AccountManager.getInstance().getVerboseName(account));

        setUpAccountMenu(activity, adapter, account, menu);
    }

    public static void setUpAccountMenu(final FragmentActivity activity, final UpdatableAdapter adapter, final String account, Menu menu) {
        final AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        ConnectionState state = accountItem.getState();

        if (state == ConnectionState.waiting) {
            menu.findItem(R.id.action_reconnect_account).setVisible(true).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (accountItem.updateConnection(true))
                                AccountManager.getInstance().onAccountChanged(account);
                            return true;
                        }

                    });
        }

        menu.findItem(R.id.action_edit_account_status).setIntent(StatusEditor.createIntent(activity, account));
        menu.findItem(R.id.action_edit_account).setIntent(AccountViewer.createAccountPreferencesIntent(activity, account));

        if (state.isConnected()) {
            menu.findItem(R.id.action_contact_info).setVisible(true).setOnMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            activity.startActivity(AccountViewer.createAccountInfoIntent(activity, account));
                            return true;
                        }
                    });
            menu.findItem(R.id.action_add_contact).setVisible(true).setIntent(ContactAdd.createIntent(activity, account));
        }

        if (SettingsManager.contactsShowAccounts()) {
            menu.findItem(R.id.action_set_up_offline_contacts).setVisible(true)
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            ContextMenuHelper.createOfflineContactsDialog(activity, adapter,
                                    account, GroupManager.IS_ACCOUNT).show();
                            return true;
                        }
                    });
        }
    }

    public static AlertDialog createOfflineContactsDialog(Context context, final UpdatableAdapter adapter,
                                                          final String account, final String group) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.show_offline_settings)
                .setSingleChoiceItems(
                        R.array.offline_contacts_show_option,
                        GroupManager.getInstance().getShowOfflineMode(account, group).ordinal(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                GroupManager.getInstance().setShowOfflineMode(account,
                                        group, ShowOfflineMode.values()[which]);
                                adapter.onChange();
                                dialog.dismiss();
                            }
                        }).create();
    }
}
