package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.extension.groupchat.GroupchatUpdateIQ
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupchatIndexType
import com.xabber.android.data.message.chat.groupchat.GroupchatManager
import com.xabber.android.data.message.chat.groupchat.GroupchatMembershipType

class GroupchatSettingsFragment(private val groupchat: GroupChat): Fragment(),
        GroupchatUpdateIQ.UpdateGroupchatSettingsIqResultListener {

    private lateinit var groupchatNameEt: EditText
    private lateinit var membershipTypeSp: Spinner
    private lateinit var indexTypeSp: Spinner
    private lateinit var descriptionEt: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.groupchat_update_settings_fragment, container, false)

        groupchatNameEt = view.findViewById(R.id.create_groupchat_name_et)
        groupchatNameEt.setText(groupchat.name)

        membershipTypeSp = view.findViewById(R.id.create_groupchat_membership_spinner)
        when (groupchat.membershipType){
            GroupchatMembershipType.MEMBER_ONLY -> membershipTypeSp.setSelection(0)
            GroupchatMembershipType.OPEN -> membershipTypeSp.setSelection(1)
            else -> membershipTypeSp.setSelection(2)
        }

        indexTypeSp = view.findViewById(R.id.create_groupchat_index_type_spinner)
        when (groupchat.indexType){
            GroupchatIndexType.LOCAL -> indexTypeSp.setSelection(0)
            GroupchatIndexType.GLOBAL -> indexTypeSp.setSelection(1)
            else -> indexTypeSp.setSelection(2)
        }

        descriptionEt = view.findViewById(R.id.create_groupchat_description)
        descriptionEt.setText(groupchat.description)

        setupMembershipTypeSpinner()
        setupIndexTypeSpinner()

        return  view
    }

    private fun setupMembershipTypeSpinner(){
        val adapter = ArrayAdapter(Application.getInstance().applicationContext,
                android.R.layout.simple_spinner_item, GroupchatMembershipType.getLocalizedValues())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        membershipTypeSp.adapter = adapter
        membershipTypeSp.setSelection(2)
    }

    private fun setupIndexTypeSpinner(){
        val adapter = ArrayAdapter(Application.getInstance().applicationContext,
                android.R.layout.simple_spinner_item, GroupchatIndexType.getLocalizedValues())

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        indexTypeSp.adapter = adapter
        indexTypeSp.setSelection(2)
    }

    fun updateSettings(){
        val membershipType = GroupchatMembershipType
                .getMembershipByLocalizedString(membershipTypeSp.selectedItem as String)

        val indexType = GroupchatIndexType
                .getPrivacyByLocalizedString(indexTypeSp.selectedItem as String)

        GroupchatManager.getInstance().sendUpdateGroupchatSettingsRequestWithCallback(groupchat,
                groupchatNameEt.text.toString(), descriptionEt.text.toString(),
                membershipType, indexType, null, this);

    }

    override fun onSuccess() {
        Application.getInstance().runOnUiThread {
            activity?.finish() }
    }

    override fun onError() {
        Application.getInstance().runOnUiThread {
            Toast.makeText(context, getString(R.string.groupchat_failed_to_change_groupchat_settings),
                    Toast.LENGTH_SHORT).show() }
    }

}