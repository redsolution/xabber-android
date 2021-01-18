package com.xabber.android.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.ui.color.BarPainter
import com.xabber.android.ui.fragment.groups.CreateGroupchatFragment

class CreateGroupchatActivity : ManagedActivity(), CreateGroupchatFragment.Listener, Toolbar.OnMenuItemClickListener {

    private var isIncognito = false
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: Toolbar
    private lateinit var barPainter: BarPainter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_with_toolbar_progress_and_container)

        isIncognito = intent != null
                && intent.action != null
                && intent.action.equals(CREATE_INCOGNITO_GROUPCHAT_INTENT)

        toolbar = findViewById(R.id.toolbar_default)

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            toolbar.setNavigationIcon(R.drawable.ic_clear_grey_24dp)
        else toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp)

        toolbar.setNavigationOnClickListener { finish() }

        toolbar.inflateMenu(if (isIncognito) R.menu.toolbar_create_incognito_groupchat
        else R.menu.toolbar_create_groupchat)

        val view = toolbar.findViewById<View>(R.id.action_create_groupchat)
        if (view != null && view is TextView) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                view.setTextColor(resources.getColor(R.color.grey_900))
            else view.setTextColor(Color.WHITE)
        }

        toolbar.setOnMenuItemClickListener(this)

        barPainter = BarPainter(this, toolbar)
        barPainter.setDefaultColor()

        supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, CreateGroupchatFragment(), FRAGMENT_TAG)
                .commit()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as CreateGroupchatFragment)
                .createGroupchat(isIncognito)
        return true
    }

    override fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        //toolbar.menu.findItem(R.id.toolbar_create_groupchat).isVisible = !show
    }

    override fun onAccountSelected(account: AccountJid?) {
        barPainter.updateWithAccountName(account)
    }

    private fun toolbarSetEnabled(enabled: Boolean){
        //toolbar.menu.findItem(R.id.toolbar_create_groupchat)
    }

    companion object {

        private const val FRAGMENT_TAG = "com.xabber.android.ui.fragment.groups.CreateGroupchatFragment"
        private const val CREATE_INCOGNITO_GROUPCHAT_INTENT = "com.xabber.android.ui.activity.CreateGroupchatActivity.CREATE_INCOGNITO_GROUPCHAT_INTENT"
        private const val CREATE_PUBLIC_GROUPCHAT_INTENT = "com.xabber.android.ui.activity.CreateGroupchatActivity.CREATE_PUBLIC_GROUPCHAT_INTENT"

        fun createCreateIncognitoGroupchatIntent() =
                Intent(Application.getInstance().applicationContext,
                        CreateGroupchatActivity::class.java).apply {
                    action = CREATE_INCOGNITO_GROUPCHAT_INTENT
                }

        fun createCreatePublicGroupchatIntent() =
                Intent(Application.getInstance().applicationContext,
                        CreateGroupchatActivity::class.java).apply {
                    action = CREATE_PUBLIC_GROUPCHAT_INTENT
                }

    }

}