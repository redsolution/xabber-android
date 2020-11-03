package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupchatManager

class GroupchatSettingsFragment(private val groupchat: GroupChat): Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.groupchat_update_settings_fragment, container, false)

        GroupchatManager.getInstance().requestGroupSettingsForm(groupchat)



        return  view
    }

    fun updateSettings(){

//        GroupchatManager.getInstance().sendUpdateGroupchatSettingsRequestWithCallback(groupchat,
//                groupchatNameEt.text.toString(), descriptionEt.text.toString(),
//                membershipType, indexType, null, this)

    }

    fun onSuccess() {
        Application.getInstance().runOnUiThread {
            activity?.finish() }
    }

    fun onError() {
        Application.getInstance().runOnUiThread {
            Toast.makeText(context, getString(R.string.groupchat_failed_to_change_groupchat_settings),
                    Toast.LENGTH_SHORT).show() }
    }

}