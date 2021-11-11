package com.xabber.android.data.message

enum class MessageStatus {

    // for incoming messages
    // icon: none
    NONE,

    // only for http upload, while file is uploading to server
    // icon: none or round animated progress bar
    UPLOADING,

    // outgoing message that was only created, but not sent yet
    // icon: none
    NOT_SENT,

    // when message was sent
    // icon: blue clock
    SENT,

    // if received error
    // icon: red cross mark
    ERROR,

    // if received a confirmation (according to XEP Stream Management) or receipt (according to XEP-DELIVERY)
    // from server
    // icon: gray check mark
    DELIVERED,

    // if received a "received" chat marker from contact
    // icon: single green check mark
    RECEIVED,

    // if received a "displayed" chat marker from contact
    // icon: double green check mark
    DISPLAYED,

}