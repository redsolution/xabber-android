package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import com.xabber.android.R
import com.xabber.android.ui.activity.ContactAddActivity
import com.xabber.android.ui.activity.CreateGroupchatActivity

class AddFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.add_fragment, container, false)

        view.findViewById<LinearLayoutCompat>(R.id.add_contact_btn).setOnClickListener {
            startActivity(ContactAddActivity.createIntent(activity))}

        view.findViewById<LinearLayoutCompat>(R.id.create_groupchat_btn).setOnClickListener {
            startActivity(CreateGroupchatActivity.createCreatePublicGroupchatIntent())}

        view.findViewById<LinearLayoutCompat>(R.id.create_incognito_groupchat_btn).setOnClickListener {
            startActivity(CreateGroupchatActivity.createCreateIncognitoGroupchatIntent())}
        return view
    }
}