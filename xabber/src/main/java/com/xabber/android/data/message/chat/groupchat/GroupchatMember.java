package com.xabber.android.data.message.chat.groupchat;

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
    private boolean isMe;
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

    public GroupchatMember(String id, String jid, String groupchatJid, String nickname, String role,
                           String badge, String avatarHash, String avatarUrl, String lastPresent,
                           boolean isMe, boolean isCanRestrictMembers, boolean isCanBlockMembers,
                           boolean isCanChangeBadge, boolean isCanChangeNickname,
                           boolean isCanDeleteMessages, boolean isRestrictedToSendMessages,
                           boolean isRestrictedToReadMessages, boolean isRestrictedToSendInvitations,
                           boolean isRestrictedToSendAudio, boolean isRestrictedToSendImages) {
        this.id = id;
        this.jid = jid;
        this.groupchatJid = groupchatJid;
        this.nickname = nickname;
        this.role = role;
        this.badge = badge;
        this.avatarHash = avatarHash;
        this.avatarUrl = avatarUrl;
        this.lastPresent = lastPresent;
        this.isMe = isMe;
        this.isCanRestrictMembers = isCanRestrictMembers;
        this.isCanBlockMembers = isCanBlockMembers;
        this.isCanChangeBadge = isCanChangeBadge;
        this.isCanChangeNickname = isCanChangeNickname;
        this.isCanDeleteMessages = isCanDeleteMessages;
        this.isRestrictedToSendMessages = isRestrictedToSendMessages;
        this.isRestrictedToReadMessages = isRestrictedToReadMessages;
        this.isRestrictedToSendInvitations = isRestrictedToSendInvitations;
        this.isRestrictedToSendAudio = isRestrictedToSendAudio;
        this.isRestrictedToSendImages = isRestrictedToSendImages;
    }

    public boolean isMe() { return isMe; }
    public void setMe(boolean me) { isMe = me; }

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
    public void setAvatarHash(String avatarHash) { this.avatarHash = avatarHash; }

    public String getAvatarUrl() {
        return avatarUrl;
    }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

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

}
