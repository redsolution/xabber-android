package com.xabber.android.data.database.realmobjects;

import com.xabber.android.data.extension.devices.DeviceVO;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class DeviceRealmObject extends RealmObject {

    public static class Fields {
        public static final String ID = "id";
    }

    @PrimaryKey
    @Required
    private String id;
    private String token;
    private long expire;
    private int counter;

    public DeviceRealmObject() {
        this.id = UUID.randomUUID().toString();
    }

    public DeviceRealmObject(String id, String token, long expire, int counter) {
        this.id = id;
        this.token = token;
        this.expire = expire;
        this.counter = counter;
    }

    public String getId() {
        return id;
    }

    public String getSecret() {
        return token;
    }

    public void setSecret(String token) {
        this.token = token;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public int getCounter() { return counter; }

    public void setCounter(int counter) { this.counter = counter; }

    static public DeviceRealmObject createFromDevice(DeviceVO deviceVO) {
        return new DeviceRealmObject(
                deviceVO.getId(), deviceVO.getSecret(), deviceVO.getExpire(), deviceVO.getCounter()
        );
    }

    public DeviceVO toDeviceVO() {
        return new DeviceVO(id, token, expire, counter);
    }

}
