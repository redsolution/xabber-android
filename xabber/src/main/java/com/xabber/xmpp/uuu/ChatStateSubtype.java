package com.xabber.xmpp.uuu;

public enum ChatStateSubtype {

    /**
     * User is recording a voice message.
     */
    //TODO:Should change back to "voice" when the bug on web client will be fixed
    audio,
    /**
     * User is recording a video message.
     */
    video,
    /**
     * User is preparing a message with files by uploading them to the server.
     */
    upload
}
