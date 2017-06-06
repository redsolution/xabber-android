package com.xabber.android.data.extension.bookmarks;

/**
 * Created by valery.miller on 06.06.17.
 */

public class BookmarkVO {

    private String name = "";
    private String url = "";
    private String jid = "";
    private String nickname = "";
    private String pass = "";
    private int type;

    public final static int TYPE_URL = 0;
    public final static int TYPE_CONFERENCE = 1;

    public BookmarkVO(String name, String url) {
        this.name = name;
        this.url = url;
        this.type = TYPE_URL;
    }

    public BookmarkVO(String name, String jid, String nickname, String pass) {
        this.name = name;
        this.jid = jid;
        this.nickname = nickname;
        this.pass = pass;
        this.type = TYPE_CONFERENCE;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getJid() {
        return jid;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPass() {
        return pass;
    }

    public int getType() {
        return type;
    }
}
