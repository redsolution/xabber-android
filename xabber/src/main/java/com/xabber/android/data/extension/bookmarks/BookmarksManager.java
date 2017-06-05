package com.xabber.android.data.extension.bookmarks;

import android.support.annotation.NonNull;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bookmarks.BookmarkManager;
import org.jivesoftware.smackx.bookmarks.BookmarkedConference;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Manage bookmarks and there requests.
 *
 * Created by valery.miller on 02.06.17.
 */

public class BookmarksManager {

    private static BookmarksManager instance;

    public static BookmarksManager getInstance() {
        if (instance == null)
            instance = new BookmarksManager();
        return instance;
    }

    public void addConferenceToBookmarks(AccountJid accountJid, String conferenceName,
                                         EntityBareJid conferenceJid, Resourcepart userNick) {

        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);

        if (accountItem != null) {
            BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(accountItem.getConnection());
            try {
                bookmarkManager.addBookmarkedConference(
                        conferenceName,
                        conferenceJid,
                        true,
                        userNick,
                        "");
            } catch (SmackException.NoResponseException | InterruptedException |
                    SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                LogManager.exception(this, e);
            }
        }
    }

    @NonNull
    private List<BookmarkedConference> getConferencesFromBookmarks(AccountJid accountJid) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
        List<BookmarkedConference> conferences = Collections.emptyList();
        if (accountItem != null) {
            BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(accountItem.getConnection());
            try {
                conferences = bookmarkManager.getBookmarkedConferences();
            } catch (SmackException.NoResponseException | InterruptedException |
                    SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                LogManager.exception(this, e);
            }
        }
        return conferences;
    }

    public void removeConferenceFromBookmarks(AccountJid accountJid, EntityBareJid conferenceJid) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
        if (accountItem != null) {
            BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(accountItem.getConnection());
            try {
                bookmarkManager.removeBookmarkedConference(conferenceJid);
            } catch (SmackException.NoResponseException | InterruptedException |
                    SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                LogManager.exception(this, e);
            }
        }
    }

    public void onAuthorized(AccountJid account) {
        List<BookmarkedConference> conferences = getConferencesFromBookmarks(account);
        if (!conferences.isEmpty()) {
            for (BookmarkedConference conference : conferences) {
                if (!MUCManager.getInstance().hasRoom(account, conference.getJid())) {
                    createMUC(account, conference);
                    LogManager.d(this, " Conference " + conference.getName() + "was added to roster from bookmarks");
                }
            }
        }

        Collection<AbstractChat> chats = MessageManager.getInstance().getChats(account);
        if (!chats.isEmpty()) {
            for (AbstractChat chat : chats) {
                if (chat instanceof RoomChat) {
                    if (!hasConference(conferences, ((RoomChat)chat).getTo())) {
                        removeMUC(account, chat.getUser());
                        LogManager.d(this, " Conference " + chat.getTo().toString() + " was deleted from phone");
                    }
                }
            }
        }
    }

    private boolean hasConference(List<BookmarkedConference> conferences, EntityBareJid jid) {
        for (int i = 0; i < conferences.size(); i++) {
            BookmarkedConference conference = conferences.get(i);
            if (conference.getJid().toString().equals(jid.toString())) return true;
        }
        return false;
    }

    private void removeMUC(final AccountJid account, final UserJid user) {
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MUCManager.getInstance().removeRoom(account, user.getJid().asEntityBareJidIfPossible());
                MessageManager.getInstance().closeChat(account, user);
                BookmarksManager.getInstance().removeConferenceFromBookmarks(account, user.getJid().asEntityBareJidIfPossible());
                NotificationManager.getInstance().removeMessageNotification(account, user);
            }
        });
    }

    private void createMUC(AccountJid account, BookmarkedConference conference) {
        Resourcepart nickname = conference.getNickname();
        if (nickname == null)
            nickname = getNickname(account, conference.getJid());

        String password = conference.getPassword();
        if (password == null) password = "";

        MUCManager.getInstance().createRoom(
                account,
                conference.getJid(),
                nickname,
                password,
                conference.isAutoJoin()
        );
    }

    @NonNull
    private Resourcepart getNickname(AccountJid account, EntityBareJid conferenceJid) {
        // try get nickname from exist muc
        Resourcepart nickname = MUCManager.getInstance().getNickname(account, conferenceJid);
        if (nickname == null || nickname.toString().isEmpty()) {
            // try get nickname from account
            try {
                nickname = Resourcepart.from(getStringNick(account));
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }
        }
        // try get nickname from resource
        if (nickname == null || nickname.toString().isEmpty()) {
            nickname = account.getFullJid().getResourcepart();
        }
        return nickname;
    }

    private String getStringNick(AccountJid account) {
        if (account == null) {
            return "";
        }
        String nickname = AccountManager.getInstance().getNickName(account);
        String name = XmppStringUtils.parseLocalpart(nickname);
        if ("".equals(name)) {
            return nickname;
        } else {
            return name;
        }
    }
}
