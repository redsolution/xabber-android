/*
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.entity.AccountJid
import de.hdodenhof.circleimageview.CircleImageView

class AccountSpinner : LinearLayout {

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private lateinit var hint: TextView
    private lateinit var promtLayout: LinearLayout
    private lateinit var promtTextTv: TextView
    private lateinit var selectedLayout: LinearLayout
    private lateinit var selectedJid: TextView
    private lateinit var recyclerView: RecyclerView

    private var isExpanded = false

    private var listener: Listener? = null

    var selected: AccountJid? = null
        private set

    private fun init(context: Context) {
        inflate(context, R.layout.account_spinner_view, this)
        orientation = HORIZONTAL

        hint = findViewById(R.id.account_spinner_hint_text)

        promtLayout = findViewById(R.id.account_spinner_promt_layout)
        promtTextTv = findViewById(R.id.promt_tv)

        selectedLayout = findViewById(R.id.account_spinner_selected_layout)
        selectedJid = findViewById(R.id.selected_jid)

        recyclerView = findViewById(R.id.account_spinner_recycler_view)

        findViewById<LinearLayoutCompat>(R.id.primary_linear_layout).setOnClickListener {
            toggleRecyclerExpanded()
        }
    }

    fun setup(hintText: String? = null, promtText: String? = null, accountsList: List<AccountJid>,
              avatarsList: List<Drawable?>, names: List<String?>, listener: Listener? = null) {

        this.listener = listener

        hint.text = hintText
        if (promtText != null) {
            promtTextTv.text = promtText
        }

        val llm = LinearLayoutManager(this.context)
        llm.orientation = RecyclerView.VERTICAL

        recyclerView.layoutManager = llm
        recyclerView.adapter = AccountsAdapter(accountsList, avatarsList, names, object : AccountsAdapter.Listener {
            override fun onClick(accountJid: AccountJid, avatar: Drawable?) {
                onAccountSelected(accountJid, avatar)
                toggleRecyclerExpanded()
            }
        })

        onAccountSelected(accountsList.first(), avatarsList.first())

        recyclerView.addItemDecoration(DividerItemDecoration(this.context, llm.orientation))
    }

    private fun toggleRecyclerExpanded() {

        fun toggleChevron(textView: TextView){
            if (isExpanded){
                val chevron = ContextCompat.getDrawable(context, R.drawable.ic_chevron_down)
                if (Build.VERSION.SDK_INT >= 17){
                    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, chevron, null)
                } else {
                    textView.setCompoundDrawablesWithIntrinsicBounds(null, null ,chevron, null)
                }
            } else {
                val chevron = ContextCompat.getDrawable(context, R.drawable.ic_chevron_up)
                if (Build.VERSION.SDK_INT  >= 17){
                    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, chevron, null)
                } else {
                    textView.setCompoundDrawablesWithIntrinsicBounds(null, null, chevron, null)
                }
            }
        }

        toggleChevron(promtTextTv)
        toggleChevron(selectedJid)

        if (isExpanded) {
            val animation = AnimationUtils.loadAnimation(context, R.anim.slide_out_top)
            animation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    recyclerView.visibility = View.GONE
                    if (Build.VERSION.SDK_INT >= 21){
                        elevation = 0f
                    }
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
            recyclerView.startAnimation(animation)
        } else {
            recyclerView.visibility = View.VISIBLE
            recyclerView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_top))
            if (Build.VERSION.SDK_INT >= 21){
                elevation = 16f
            }
        }
        isExpanded = !isExpanded
    }

    private fun onAccountSelected(accountJid: AccountJid, avatar: Drawable?) {
        promtLayout.visibility = GONE
        selectedLayout.visibility = VISIBLE

        selectedJid.text = accountJid.bareJid.toString()
        if (avatar != null) {
            findViewById<CircleImageView>(R.id.selected_avatar).setImageDrawable(avatar)
        }

        selected = accountJid
        listener?.onSelected(accountJid)
    }

    interface Listener {
        fun onSelected(accountJid: AccountJid)
    }

    private class AccountViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarIv: CircleImageView = view.findViewById(R.id.avatar)
        val nickTv: TextView = view.findViewById(R.id.nickname_tv)
        val jidTv: TextView = view.findViewById(R.id.jid_tv)
    }

    private class AccountsAdapter(private val jids: List<AccountJid>, private val avatars: List<Drawable?>,
                                  private val names: List<String?>, private val listener: Listener
    ) : RecyclerView.Adapter<AccountViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = AccountViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.account_spinner_list_item, parent, false))

        override fun getItemCount() = jids.size

        override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
            holder.jidTv.text = jids[position].bareJid.toString()

            if (!names[position].isNullOrEmpty()) {
                holder.nickTv.text = names[position]
            } else {
                holder.nickTv.visibility = View.GONE
            }

            if (avatars[position] != null) {
                holder.avatarIv.setImageDrawable(avatars[position])
            } else {
                holder.avatarIv.visibility = View.INVISIBLE
            }

            holder.itemView.setOnClickListener {
                listener.onClick(jids[position], avatars[position])
            }
        }

        interface Listener {
            fun onClick(accountJid: AccountJid, avatar: Drawable? = null)
        }

    }

}