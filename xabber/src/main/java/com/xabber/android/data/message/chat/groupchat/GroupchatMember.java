package com.xabber.android.data.message.chat.groupchat;

import com.xabber.android.data.account.StatusMode;

public class GroupchatMember {

    private String id;
    private String jid;
    private String groupchatJid;
    private String nickname;
    private String role;
    private String badge;
    private String avatarHash;
    private String avatarUrl;
    private String lastPresent;
    private StatusMode statusMode;
    private long timestamp;

    private boolean isCanRestrictMembers;
    private boolean isCanBlockMembers;
    private boolean isCanChangeBadge;
    private boolean isCanChangeNickname;
    private boolean isCanDeleteMessages;

    private boolean isRestrictedToSendMessages;
    private boolean isRestrictedToReadMessages;
    private boolean isRestrictedToSendInvitations;
    private boolean isRestrictedToSendAudio;
    private boolean isRestrictedToSendImages;

    public GroupchatMember(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getJid() {
        return jid;
    }
    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getGroupchatJid() {
        return groupchatJid;
    }
    public void setGroupchatJid(String groupchatJid) {
        this.groupchatJid = groupchatJid;
    }

    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public String getBadge() {
        return badge;
    }
    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getAvatarHash() {
        return avatarHash;
    }
    public void setAvatarHash(String avatarHash) {
        this.avatarHash = avatarHash;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLastPresent() {
        return lastPresent;
    }
    public void setLastPresent(String lastPresent) {
        this.lastPresent = lastPresent;
    }

    public String getBestName() {
        if (nickname != null && !nickname.isEmpty()) {
            return nickname;
        } else if (jid != null && !jid.isEmpty()) {
            return jid;
        } else {
            return id;
        }
    }

    public boolean isCanRestrictMembers() { return isCanRestrictMembers; }
    public void setCanRestrictMembers(boolean canRestrictMembers) {
        isCanRestrictMembers = canRestrictMembers;
    }

    public boolean isCanBlockMembers() { return isCanBlockMembers; }
    public void setCanBlockMembers(boolean canBlockMembers) { isCanBlockMembers = canBlockMembers; }

    public boolean isCanChangeBadge() { return isCanChangeBadge; }
    public void setCanChangeBadge(boolean canChangeBadge) { isCanChangeBadge = canChangeBadge; }

    public boolean isCanChangeNickname() { return isCanChangeNickname; }
    public void setCanChangeNickname(boolean canChangeNickname) {
        isCanChangeNickname = canChangeNickname;
    }

    public boolean isCanDeleteMessages() { return isCanDeleteMessages; }
    public void setCanDeleteMessages(boolean canDeleteMessages) {
        isCanDeleteMessages = canDeleteMessages;
    }

    public boolean isRestrictedToSendMessages() { return isRestrictedToSendMessages; }
    public void setRestrictedToSendMessages(boolean restrictedToSendMessages) {
        isRestrictedToSendMessages = restrictedToSendMessages;
    }

    public boolean isRestrictedToReadMessages() { return isRestrictedToReadMessages; }
    public void setRestrictedToReadMessages(boolean restrictedToReadMessages) {
        isRestrictedToReadMessages = restrictedToReadMessages;
    }

    public boolean isRestrictedToSendInvitations() { return isRestrictedToSendInvitations; }
    public void setRestrictedToSendInvitations(boolean restrictedToSendInvitations) {
        isRestrictedToSendInvitations = restrictedToSendInvitations;
    }

    public boolean isRestrictedToSendAudio() { return isRestrictedToSendAudio; }
    public void setRestrictedToSendAudio(boolean restrictedToSendAudio) {
        isRestrictedToSendAudio = restrictedToSendAudio;
    }

    public boolean isRestrictedToSendImages() { return isRestrictedToSendImages; }
    public void setRestrictedToSendImages(boolean restrictedToSendImages) {
        isRestrictedToSendImages = restrictedToSendImages;
    }


    public StatusMode getStatusMode() { return statusMode; }

    public void setStatusMode(StatusMode statusMode) { this.statusMode = statusMode; }
}
