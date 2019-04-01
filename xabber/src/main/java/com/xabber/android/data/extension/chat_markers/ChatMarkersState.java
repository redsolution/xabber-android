package com.xabber.android.data.extension.chat_markers;

public enum ChatMarkersState {
    /**
     *  Indicates that a message can be marked with a Chat Marker and is therefore
     *  a "markable message".
     */
    markable,
    /**
     * The message has been received by a client.
     */
    received,
    /**
     * The message has been displayed to a user in a active chat and not in a system notification.
     */
    displayed,
    /**
     * The message has been acknowledged by some user interaction e.g. pressing an
     * acknowledgement button.
     */
    acknowledged
}