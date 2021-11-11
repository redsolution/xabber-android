package com.xabber.android.ui.activity

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.createContactIntent
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.getAccountJid
import com.xabber.android.data.getContactJid
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.ui.color.BarPainter
import com.xabber.android.ui.fragment.groups.GroupUpdateSettingsFragment

class GroupchatUpdateSettingsActivity : ManagedActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_with_toolbar_progress_and_container)

        toolbar = findViewById(R.id.toolbar_default)

        progressBar = findViewById(R.id.toolbarProgress)

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp)
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)
        }

        toolbar.title = getString(R.string.groupchat_group_settings)
        toolbar.setNavigationOnClickListener { finish() }


        BarPainter(this, toolbar).setDefaultColor()

        (ChatManager.getInstance().getChat(
            intent.getAccountJid(), intent.getContactJid()
        ) as? GroupChat)?.let { groupChat ->
            supportFragmentManager.beginTransaction().add(
                R.id.fragment_container,
                GroupUpdateSettingsFragment(groupChat), FRAGMENT_TAG
            ).commit()
        }
    }

    fun showProgressBar(isVisible: Boolean) {
        progressBar.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    fun showToolbarButtons(isEdited: Boolean) {
        toolbar.menu?.clear()
        if (isEdited) {
            toolbar.inflateMenu(R.menu.toolbar_groupchat_settings)

            (toolbar.findViewById<View>(R.id.action_update_groupchat_settings) as? TextView)?.apply {
                if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                    setTextColor(resources.getColor(R.color.grey_900))
                } else {
                    setTextColor(Color.WHITE)
                }
            }

            toolbar.setOnMenuItemClickListener {
                (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as GroupUpdateSettingsFragment).saveChanges()
                return@setOnMenuItemClickListener true
            }

            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                toolbar.setNavigationIcon(R.drawable.ic_clear_grey_24dp)
            } else {
                toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp)
            }

        } else {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp)
            } else {
                toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)
            }

        }

    }

    companion object {
        private const val FRAGMENT_TAG =
            "com.xabber.android.ui.fragment.groups.GroupchatSettingsFragment"

        fun createOpenGroupchatSettingsIntentForGroupchat(
            context: Context, accountJid: AccountJid, contactJid: ContactJid
        ) = createContactIntent(
            context, GroupchatUpdateSettingsActivity::class.java, accountJid, contactJid
        )
    }

}