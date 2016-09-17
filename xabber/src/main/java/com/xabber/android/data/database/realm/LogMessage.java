package com.xabber.android.data.database.realm;

import android.util.Log;

import com.xabber.android.utils.StringUtils;

import java.io.StringWriter;
import java.util.Date;

import io.realm.RealmObject;


public class LogMessage extends RealmObject {

    public static class Fields {
        public static final String LEVEL = "level";
        public static final String TAG = "tag";
        public static final String MESSAGE = "message";
        public static final String DATETIME = "datetime";
    }

    private int level;
    private String tag;
    private String message;
    private Date datetime;

    public LogMessage() {
    }

    public LogMessage(int level, String tag, String message) {
        this.level = level;
        this.tag = tag;
        this.message = message;
        this.datetime = new Date();
    }

    public int getLevel() {
        return level;
    }

    public String getLevelString() {
        switch (level) {
            case Log.VERBOSE:
                return "V";
            case Log.DEBUG:
                return "D";
            case Log.INFO:
                return "I";
            case Log.WARN:
                return "W";
            case Log.ERROR:
                return "E";
            default:
                return "?";

        }
    }

    public String getTag() {
        return tag;
    }

    public String getMessage() {
        return message;
    }

    public Date getDatetime() {
        return datetime;
    }

    @Override
    public String toString() {
        StringWriter writer = new StringWriter();
        writer.append(StringUtils.getLogDateTimeFormat().format(getDatetime()))
                .append(": ").append(getLevelString())
                .append("/").append(getTag())
                .append(" ").append(getMessage());
        return writer.toString();
    }
}
