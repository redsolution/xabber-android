package com.xabber.android.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.ui.color.BarPainter
import com.xabber.android.ui.fragment.GroupchatSettingsFragment

class GroupchatUpdateSettingsActivity: ManagedActivity(), Toolbar.OnMenuItemClickListener {

    private val FRAGMENT_TAG = "com.xabber.android.ui.fragment.GroupchatSettingsFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_with_toolbar_progress_and_container)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_default)

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            toolbar.setNavigationIcon(R.drawable.ic_clear_grey_24dp)
        else toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp)

        toolbar.setNavigationOnClickListener { finish() }

        toolbar.inflateMenu(R.menu.toolbar_groupchat_settings)

        val view = toolbar.findViewById<View>(R.id.action_update_groupchat_settings)
        if (view != null && view is TextView) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                view.setTextColor(resources.getColor(R.color.grey_900))
            else view.setTextColor(Color.WHITE)
        }

        toolbar.setOnMenuItemClickListener(this)

        BarPainter(this, toolbar).setDefaultColor()

        if (intent != null && intent.getSerializableExtra(GROUPCHAT_CONTACTJID) != null){
            val groupChat = ChatManager.getInstance().getChat(intent.getParcelableExtra(GROUPCHAT_ACCOUNTJID),
                    intent.getParcelableExtra(GROUPCHAT_CONTACTJID))
            if (groupChat != null && groupChat is GroupChat)
                supportFragmentManager.beginTransaction().add(R.id.container,
                        GroupchatSettingsFragment(groupChat), FRAGMENT_TAG).commit()
        } else finish()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as GroupchatSettingsFragment).updateSettings()
        return true
    }

    companion object{

        private const val GROUPCHAT_ACCOUNTJID = "com.xabber.android.ui.activity.GroupchatSettingsActivity.GROUPCHAT_ACCOUNTJID"
        private const val GROUPCHAT_CONTACTJID = "com.xabber.android.ui.activity.GroupchatSettingsActivity.GROUPCHAT_CONTACTJID"
        fun createOpenGroupchatSettingsIntentForGroupchat(accountJid: AccountJid, contactJid: ContactJid): Intent {
            val intent = Intent(Application.getInstance().applicationContext, GroupchatSettingsActivity::class.java)
            intent.putExtra(GROUPCHAT_CONTACTJID, contactJid)
            intent.putExtra(GROUPCHAT_ACCOUNTJID, accountJid as Parcelable)
            return intent
        }

    }

}