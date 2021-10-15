package com.xabber.android.ui.adapter.chat

import android.view.View
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.ui.widget.CorrectlyTouchEventTextView

abstract class BasicMessageVH internal constructor(
    itemView: View, @StyleRes appearance: Int
) : RecyclerView.ViewHolder(itemView) {

    val messageText: AppCompatTextView = itemView.findViewById(R.id.message_text)
    var needDate = false
    var date: String? = null

    init {
        messageText.setTextAppearance(itemView.context, appearance)
    }

}