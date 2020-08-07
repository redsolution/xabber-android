package com.xabber.android.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.ui.color.BarPainter
import com.xabber.android.ui.fragment.CreateGroupchatFragment

class GroupchatSettingsActivity: ManagedActivity(), Toolbar.OnMenuItemClickListener {

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

        supportFragmentManager.beginTransaction().add(R.id.container,
                CreateGroupchatFragment(), FRAGMENT_TAG).commit()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        //TODO this
        return true
    }

    companion object{

        private const val OPEN_GROUPCHAT_SETTINGS = "com.xabber.android.ui.activity.GroupchatSettingsActivity.OPEN_GROUPCHAT_SETTINGS"
        private const val GROUPCHAT_EXTRA = "com.xabber.android.ui.activity.GroupchatSettingsActivity.GROUPCHAT_EXTRA"
        fun createOpenGroupchatSettingsIntentForGroupchat(groupchat: GroupChat) =
                Intent(Application.getInstance().applicationContext,
                        GroupchatSettingsActivity::class.java).apply {
                    putExtra(GROUPCHAT_EXTRA, groupchat);
                }

    }

}