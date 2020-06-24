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
import com.xabber.android.ui.color.BarPainter
import com.xabber.android.ui.fragment.CreateGroupchatFragment

class CreateGroupchatActivity : ManagedActivity(), Toolbar.OnMenuItemClickListener{

    var isIncognito = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.add_activity_layout)

        isIncognito = intent != null
                && intent.action != null
                && intent.action.equals(CREATE_INCOGNITO_GROUPCHAT_INTENT)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_default)

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            toolbar.setNavigationIcon(R.drawable.ic_clear_grey_24dp)
        else toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp)

        toolbar.setNavigationOnClickListener { finish() }

        toolbar.inflateMenu(if (isIncognito) R.menu.toolbar_create_incognito_groupchat
                            else R.menu.toolbar_create_groupchat)

        toolbar.menu.findItem(R.id.action_create_groupchat)?.icon = null
        toolbar.menu.findItem(R.id.action_create_groupchat)?.isEnabled = false

        toolbar.menu.findItem(R.id.action_create_incognito_groupchat)?.icon = null
        toolbar.menu.findItem(R.id.action_create_incognito_groupchat)?.isEnabled = false

        val view = toolbar.findViewById<View>(R.id.action_create_groupchat)
        if (view != null && view is TextView) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                view.setTextColor(resources.getColor(R.color.grey_900))
            else view.setTextColor(Color.WHITE)
        }

        toolbar.setOnMenuItemClickListener(this)

        BarPainter(this, toolbar).setDefaultColor()

        supportFragmentManager.beginTransaction().add(R.id.container,
                CreateGroupchatFragment()).commit()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return if (item!!.itemId == R.id.action_add_account) { (supportFragmentManager
                .findFragmentById(R.id.fragment_container) as CreateGroupchatFragment)
                .createGroupchat(isIncognito)
            true
        } else super.onOptionsItemSelected(item)
    }

    companion object{

        private const val CREATE_INCOGNITO_GROUPCHAT_INTENT = "com.xabber.android.ui.activity.CreateGroupchatActivity.CREATE_INCOGNITO_GROUPCHAT_INTENT"
        private const val CREATE_PUBLIC_GROUPCHAT_INTENT = "com.xabber.android.ui.activity.CreateGroupchatActivity.CREATE_PUBLIC_GROUPCHAT_INTENT"

        fun createCreateIncognitoGroupchatIntent() =
                Intent(Application.getInstance().applicationContext,
                        CreateGroupchatActivity::class.java).apply {
                    action =  CREATE_INCOGNITO_GROUPCHAT_INTENT
                }

        fun createCreatePublicGroupchatIntent() =
                Intent(Application.getInstance().applicationContext,
                        CreateGroupchatActivity::class.java).apply {
                    action =  CREATE_PUBLIC_GROUPCHAT_INTENT
                }

    }

}