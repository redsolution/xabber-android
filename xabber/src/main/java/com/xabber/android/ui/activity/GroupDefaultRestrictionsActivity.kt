package com.xabber.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
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
import com.xabber.android.ui.fragment.GroupDefaultRestrictionsFragment

class GroupDefaultRestrictionsActivity : ManagedActivity() {

    lateinit var progressbar: ProgressBar
    lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_with_toolbar_progress_and_container)

        progressbar = findViewById(R.id.toolbarProgress)
        toolbar = findViewById(R.id.toolbar_default)

        toolbar = findViewById(R.id.toolbar_default)
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp)
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)
        }
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = getString(R.string.groupchat_default_restrictions)

        BarPainter(this, toolbar).setDefaultColor()

        if (intent != null) {
            val group = ChatManager.getInstance().getChat(
                intent.getAccountJid(), intent.getContactJid()
            )
            if (group != null && group is GroupChat)
                supportFragmentManager.beginTransaction()
                    .add(
                        R.id.fragment_container,
                        GroupDefaultRestrictionsFragment(group),
                        GroupDefaultRestrictionsFragment.TAG
                    ).commit()
        } else finish()

    }

    fun showToolbarMenu(isVisible: Boolean) {
        toolbar.apply {
            menu?.clear()
            if (isVisible) {
                setNavigationIcon(R.drawable.ic_clear_grey_24dp)

                inflateMenu(R.menu.update_groupchat_member)
                setOnMenuItemClickListener {
                    (supportFragmentManager.findFragmentByTag(GroupDefaultRestrictionsFragment.TAG)
                            as GroupDefaultRestrictionsFragment).sendSaveRequest()
                    true
                }
            } else {
                setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp)
                setNavigationOnClickListener { finish() }
            }
        }
    }

    fun showProgressBar(isVisible: Boolean) =
        if (isVisible) {
            progressbar.visibility = View.VISIBLE
        } else {
            progressbar.visibility = View.GONE
        }

    companion object {
        fun createIntent(context: Context, account: AccountJid, groupchatJid: ContactJid): Intent =
            createContactIntent(
                context, GroupDefaultRestrictionsActivity::class.java, account, groupchatJid
            )
    }

}