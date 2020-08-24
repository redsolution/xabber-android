package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.xabber.android.R
import com.xabber.android.data.message.chat.groupchat.GroupchatMember

class GroupchatMemberInfoFragment(val groupchatMember: GroupchatMember): Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_groupchat_member_info, container, false)

        view.findViewById<TextView>(R.id.member_role).text = groupchatMember.role

        view.findViewById<LinearLayout>(R.id.rights_button_layout).setOnClickListener {
            Toast.makeText(context, "ops", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<LinearLayout>(R.id.restrictionsButtonLayout).setOnClickListener {
            Toast.makeText(context, "ops", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}