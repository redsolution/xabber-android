package com.xabber.xmpp.chat_state;

public enum ChatStateSubtype {

    /**
     * User is recording a voice message.
     */
    voice,
    /**
     * User is recording a video message.
     */
    video,
    /**
     * User is preparing a message with files by uploading them to the server.
     */
    upload
}
