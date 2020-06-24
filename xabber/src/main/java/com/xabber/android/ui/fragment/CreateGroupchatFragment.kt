package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.message.chat.groupchat.GroupchatIndexType
import com.xabber.android.data.message.chat.groupchat.GroupchatMembershipType
import com.xabber.android.ui.adapter.AccountChooseAdapter
import com.xabber.android.ui.widget.NoDefaultSpinner

class CreateGroupchatFragment :  Fragment() {

    lateinit var accountSp: NoDefaultSpinner
    lateinit var groupchatNameEt: EditText
    lateinit var groupchatJidEt: EditText
    lateinit var serverEt: EditText
    lateinit var membershipTypeSp: Spinner
    lateinit var indexTypeSp: Spinner
    lateinit var descriptionEt: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.create_groupchat_fragment, container, false)
        accountSp = view.findViewById(R.id.contact_account)
        groupchatNameEt = view.findViewById(R.id.create_groupchat_name_et)
        groupchatJidEt = view.findViewById(R.id.create_groupchat_jid_et)
        serverEt = view.findViewById(R.id.create_groupchat_server_et)
        membershipTypeSp = view.findViewById(R.id.create_groupchat_membership_spinner)
        indexTypeSp = view.findViewById(R.id.create_groupchat_index_type_spinner)
        descriptionEt = view.findViewById(R.id.create_groupchat_description)

        setupAccountSpinner()
        setupMembershipTypeSpinner()
        setupIndexTypeSpinner()

        return  view
    }

    private fun setupAccountSpinner(){
        accountSp.adapter = AccountChooseAdapter(activity)

        if (AccountManager.getInstance().enabledAccounts.size <= 1){
            accountSp.setSelection(0)
            accountSp.visibility = View.GONE
        }
    }

    private fun setupMembershipTypeSpinner(){
        val adapter = ArrayAdapter(Application.getInstance().applicationContext,
                android.R.layout.simple_spinner_item, GroupchatMembershipType.values())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        membershipTypeSp.adapter = adapter
        membershipTypeSp.setSelection(2)
    }

    private fun setupIndexTypeSpinner(){
        val adapter = ArrayAdapter(Application.getInstance().applicationContext,
        android.R.layout.simple_spinner_item, GroupchatIndexType.values())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        indexTypeSp.adapter = adapter
        indexTypeSp.setSelection(2)
    }

    fun createGroupchat(isIncognito: Boolean){
        //GroupchatManager.getInstance().sendCreateGroupchatRequest()

    }

}