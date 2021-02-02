package com.xabber.android.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.ui.color.BarPainter
import com.xabber.android.ui.fragment.groups.GroupUpdateSettingsFragment

class GroupchatUpdateSettingsActivity : ManagedActivity() {

    private val FRAGMENT_TAG = "com.xabber.android.ui.fragment.groups.GroupchatSettingsFragment"

    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_with_toolbar_progress_and_container)

        toolbar = findViewById(R.id.toolbar_default)

        progressBar = findViewById(R.id.toolbarProgress)

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp)
        else toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)

        toolbar.title = getString(R.string.groupchat_group_settings)
        toolbar.setNavigationOnClickListener { finish() }


        BarPainter(this, toolbar).setDefaultColor()

        if (intent != null && intent.getParcelableExtra<ContactJid>(GROUPCHAT_CONTACTJID) != null) {
            val groupChat = ChatManager.getInstance().getChat(intent.getParcelableExtra(GROUPCHAT_ACCOUNTJID),
                    intent.getParcelableExtra(GROUPCHAT_CONTACTJID))
            if (groupChat != null && groupChat is GroupChat)
                supportFragmentManager.beginTransaction().add(R.id.fragment_container,
                        GroupUpdateSettingsFragment(groupChat), FRAGMENT_TAG).commit()
        } else finish()
    }

    fun showProgressBar(isVisible: Boolean) =
            if (isVisible) progressBar.visibility = View.VISIBLE
            else progressBar.visibility = View.GONE

    fun showToolbarButtons(isEdited: Boolean) {
        toolbar.menu?.clear()
        if (isEdited) {
            toolbar.inflateMenu(R.menu.toolbar_groupchat_settings)

            val view = toolbar.findViewById<View>(R.id.action_update_groupchat_settings)
            if (view != null && view is TextView) {
                if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                    view.setTextColor(resources.getColor(R.color.grey_900))
                else view.setTextColor(Color.WHITE)
            }

            toolbar.setOnMenuItemClickListener {
                (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as GroupUpdateSettingsFragment).saveChanges()
                return@setOnMenuItemClickListener true
            }

            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                toolbar.setNavigationIcon(R.drawable.ic_clear_grey_24dp)
            else toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp)

        } else {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp)
            else toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)

        }

    }

    companion object {

        private const val GROUPCHAT_ACCOUNTJID = "com.xabber.android.ui.activity.GroupchatSettingsActivity.GROUPCHAT_ACCOUNTJID"
        private const val GROUPCHAT_CONTACTJID = "com.xabber.android.ui.activity.GroupchatSettingsActivity.GROUPCHAT_CONTACTJID"

        fun createOpenGroupchatSettingsIntentForGroupchat(accountJid: AccountJid,
                                                          contactJid: ContactJid) =
                Intent(Application.getInstance().applicationContext,
                        GroupchatUpdateSettingsActivity::class.java).apply {
                    putExtra(GROUPCHAT_CONTACTJID, contactJid)
                    putExtra(GROUPCHAT_ACCOUNTJID, accountJid as Parcelable)
                }

    }

}