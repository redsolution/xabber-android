package com.xabber.xmpp.uuu;

public enum ChatState {
    /**
     * User is actively participating in the chat session.
     */
    active,
    /**
     * User is composing a message.
     */
    composing,
    /**
     * User had been composing but now has stopped.
     */
    paused,
    /**
     * User has not been actively participating in the chat session.
     */
    inactive,
    /**
     * User has effectively ended their participation in the chat session.
     */
    gone
}
