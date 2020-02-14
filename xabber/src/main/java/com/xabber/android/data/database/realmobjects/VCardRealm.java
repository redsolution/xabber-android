package com.xabber.android.data.database.realmobjects;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class VCardRealm extends RealmObject {

    public static final class Fields{
        public static final String USER = "user";
        public static final String NICK_NAME = "nickName";
        public static final String FORMATTED_NAME = "formattedName";
        public static final String FIRST_NAME = "firstName";
        public static final String MIDDLE_NAME = "middleName";
        public static final String LAST_NAME = "lastName";
    }

    @PrimaryKey
    private String user;

    private String nickName;
    private String formattedName;
    private String firstName;
    private String middleName;
    private String lastName;

    public VCardRealm(){
        user = "user";
        nickName = "nickname";
        formattedName = "formatted name";
        firstName = "first name";
        middleName = "middle name";
        lastName = "last name";
    }

    public VCardRealm(String user, String nickName, String formattedName, String firstName,
                      String middleName, String lastName){
        this.user = user;
        this.nickName = nickName;
        this.formattedName = formattedName;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
    }

    public void setUser(String user) { this.user = user; }
    public String getUser() { return user; }

    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getNickName() { return nickName; }

    public void setFormattedName(String formattedName) { this.formattedName = formattedName; }
    public String getFormattedName() { return formattedName; }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getFirstName() { return firstName; }

    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getMiddleName() { return middleName; }

    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getLastName() { return lastName; }

}
