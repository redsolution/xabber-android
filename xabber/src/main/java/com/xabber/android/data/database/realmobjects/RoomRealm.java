package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RoomRealm extends RealmObject {

    public static final class Fields {
        public static final String ID = "id";
        public static final String ACCOUNT = "account";
        public static final String ROOM = "room";
        public static final String NICKNAME = "nickname";
        public static final String PASSWORD = "password";
        public static final String NEED_JOIN = "needJoin";
    }

    @PrimaryKey
    private String id;

    private String account;
    private String room;
    private String nickname;
    private String password;
    private boolean needJoin;

    public RoomRealm(String id, String account, String room, String nickname, String password,
                     boolean needJoin){
        this.id = id;
        this.account = account;
        this.room = room;
        this.nickname = nickname;
        this.password = password;
        this.needJoin = needJoin;
    }

    public RoomRealm(String account, String room, String nickname, String password, boolean needJoin){
        this.id = UUID.randomUUID().toString();
        this.account = account;
        this.room = room;
        this.nickname = nickname;
        this.password = password;
        this.needJoin = needJoin;
    }

    public RoomRealm(){ this.id = UUID.randomUUID().toString(); }

    public void setId(String id) { this.id = id; }
    public String getId() { return id; }

    public void setAccount(String account) { this.account = account; }
    public String getAccount() { return account; }

    public void setRoom(String room) { this.room = room; }
    public String getRoom() { return room; }

    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getNickname() { return nickname; }

    public void setPassword(String password) { this.password = password; }
    public String getPassword() { return password; }

    public void setNeedJoin(boolean needJoin) { this.needJoin = needJoin; }
    public boolean isNeedJoin() { return needJoin; }
}
