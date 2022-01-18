package com.xabber.android.presentation.emoji.type

import androidx.annotation.DrawableRes
import androidx.annotation.IntegerRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.databinding.ItemEmojiKeyBinding
import com.xabber.android.databinding.ItemEmojiTypeBinding

class EmojiTypeViewHolder(
    private val binding: ItemEmojiTypeBinding,
    private val onClick: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(@DrawableRes item: Int) {
        with(binding) {
            emojiType.setImageDrawable(
                ContextCompat.getDrawable(
                    itemView.context,
                    item
                )
            )
            emojiType.setOnClickListener { onClick(item) }
        }
    }
}