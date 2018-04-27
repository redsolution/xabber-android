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
import org.jivesoftware.smackx.bookmarks.BookmarkedURL;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Manage bookmarks and there requests.
 *
 * Created by valery.miller on 02.06.17.
 */

public class BookmarksManager {

    public static final String XABBER_NAME = "Xabber bookmark";
    public static final String XABBER_URL = "Required to correctly sync bookmarks";

    private static BookmarksManager instance;

    public static BookmarksManager getInstance() {
        if (instance == null)
            instance = new BookmarksManager();
        return instance;
    }

    public boolean isSupported(AccountJid accountJid) throws XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
        if (accountItem != null) {
            BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(accountItem.getConnection());
            return bookmarkManager.isSupported();
        } else return false;
    }

    public List<BookmarkedURL> getUrlFromBookmarks(AccountJid accountJid) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
        List<BookmarkedURL> urls = Collections.emptyList();
        if (accountItem != null) {
            BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(accountItem.getConnection());
            try {
                urls = bookmarkManager.getBookmarkedURLs();
            } catch (SmackException.NoResponseException | InterruptedException |
                    SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                LogManager.exception(this, e);
            }
        }
        return urls;
    }

    public void addUrlToBookmarks(AccountJid accountJid, String url, String name, boolean isRSS) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);

        if (accountItem != null) {
            BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(accountItem.getConnection());
            try {
                bookmarkManager.addBookmarkedURL(url, name, isRSS);
            } catch (SmackException.NoResponseException | InterruptedException |
                    SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                LogManager.exception(this, e);
            }
        }
    }

    public void removeUrlFromBookmarks(AccountJid accountJid, String url) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
        if (accountItem != null) {
            BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(accountItem.getConnection());
            try {
                bookmarkManager.removeBookmarkedURL(url);
            } catch (SmackException.NoResponseException | InterruptedException |
                    SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                LogManager.exception(this, e);
            }
        }
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
    public List<BookmarkedConference> getConferencesFromBookmarks(AccountJid accountJid)
            throws SmackException.NoResponseException, SmackException.NotConnectedException,
            InterruptedException, XMPPException.XMPPErrorException {

        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
        List<BookmarkedConference> conferences = Collections.emptyList();
        if (accountItem != null) {
            BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(accountItem.getConnection());
            conferences = bookmarkManager.getBookmarkedConferences();
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

    public void removeBookmarks(AccountJid accountJid, ArrayList<BookmarkVO> bookmarks) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
        if (accountItem != null) {
            BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(accountItem.getConnection());
            try {
                for (BookmarkVO bookmark : bookmarks) {
                    // remove conferences
                    if (bookmark.getType() == BookmarkVO.TYPE_CONFERENCE) {
                        bookmarkManager.removeBookmarkedConference(
                                JidCreate.from(bookmark.getJid()).asEntityBareJidIfPossible());
                    }
                    // remove url
                    if (bookmark.getType() == BookmarkVO.TYPE_URL)
                        bookmarkManager.removeBookmarkedURL(bookmark.getUrl());
                }
            } catch (SmackException.NoResponseException | InterruptedException |
                    SmackException.NotConnectedException | XMPPException.XMPPErrorException
                    | XmppStringprepException e) {
                LogManager.exception(this, e);
            }
        }
    }

    public void cleanCache(AccountJid accountJid) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
        if (accountItem != null) {
            BookmarkManager bookmarkManager = BookmarkManager.getBookmarkManager(accountItem.getConnection());
            bookmarkManager.cleanCache();
        }
    }

    public void onAuthorized(AccountJid account) {
        cleanCache(account);

        List<BookmarkedConference> conferences;

        try {
            conferences = getConferencesFromBookmarks(account);
        } catch (SmackException.NoResponseException | InterruptedException |
                SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
            LogManager.exception(this, e);
            return;
        }

        if (!conferences.isEmpty()) {
            for (BookmarkedConference conference : conferences) {
                if (!MUCManager.getInstance().hasRoom(account, conference.getJid())) {
                    createMUC(account, conference);
                    LogManager.d(this, " Conference " + conference.getJid() + "was added to roster from bookmarks");
                }
            }
        }

        // Check bookmarks on first run new Xabber. Adding all conferences to bookmarks.
        if (!isBookmarkCheckedByXabber(account)) {
            // add conferences from phone to bookmarks
            Collection<AbstractChat> chats = MessageManager.getInstance().getChats(account);
            if (!chats.isEmpty()) {
                for (AbstractChat chat : chats) {
                    if (chat instanceof RoomChat) {
                        RoomChat roomChat = (RoomChat) chat;
                        if (!hasConference(conferences, roomChat.getTo())) {
                            addConferenceToBookmarks(account, roomChat.getTo().toString(), roomChat.getTo(), roomChat.getNickname());
                        }
                    }
                }
            }
            // add url about check to bookmarks
            addUrlToBookmarks(account, XABBER_URL, XABBER_NAME, false);
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

    private boolean isBookmarkCheckedByXabber(AccountJid account) {
        List<BookmarkedURL> urls = getUrlFromBookmarks(account);
        if (!urls.isEmpty()) {
            for (BookmarkedURL url : urls) {
                if (url.getURL().equals(XABBER_URL)) return true;
            }
        }
        return false;
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
                NotificationManager.getInstance().removeMessageNotification(account, user);
            }
        });
        BookmarksManager.getInstance().removeConferenceFromBookmarks(account, user.getJid().asEntityBareJidIfPossible());
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
