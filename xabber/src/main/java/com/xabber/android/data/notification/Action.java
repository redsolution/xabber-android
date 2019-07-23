package com.xabber.android.data.notification;

public class Action {
    private int notificationID;
    private CharSequence replyText;
    private ActionType actionType;

    public Action(int notificationID, CharSequence replyText, ActionType actionType) {
        this.notificationID = notificationID;
        this.replyText = replyText;
        this.actionType = actionType;
    }

    public Action(int notificationID, ActionType actionType) {
        this.notificationID = notificationID;
        this.actionType = actionType;
    }

    public int getNotificationID() {
        return notificationID;
    }

    public CharSequence getReplyText() {
        return replyText;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public enum ActionType {
        reply,
        read,
        snooze,
        cancel
    }
}


