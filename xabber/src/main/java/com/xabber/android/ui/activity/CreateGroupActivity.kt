package com.xabber.android.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.ui.color.BarPainter
import com.xabber.android.ui.fragment.groups.CreateGroupFragment

class CreateGroupActivity : ManagedActivity(), CreateGroupFragment.Listener {

    private var isIncognito = false

    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: Toolbar
    private lateinit var barPainter: BarPainter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_with_toolbar_progress_and_container)

        isIncognito = intent != null && intent.action != null && intent.action == CREATE_INCOGNITO_GROUPCHAT_INTENT

        progressBar = findViewById(R.id.toolbarProgress)

        findViewById<Toolbar>(R.id.toolbar_default).apply {
            toolbar = this

            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
                setNavigationIcon(R.drawable.ic_clear_grey_24dp)
            } else setNavigationIcon(R.drawable.ic_clear_white_24dp)

            setNavigationOnClickListener { finish() }

            inflateMenu(if (isIncognito) R.menu.toolbar_create_incognito_groupchat else R.menu.toolbar_create_groupchat)

            setOnMenuItemClickListener {
                (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as CreateGroupFragment).createGroupchat()
                return@setOnMenuItemClickListener true
            }
        }

        val view = toolbar.findViewById<View>(R.id.action_create_groupchat) ?: toolbar.findViewById(R.id.action_create_incognito_groupchat)
        if (view != null && view is TextView) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                view.setTextColor(resources.getColor(R.color.grey_900))
            } else view.setTextColor(Color.WHITE)
        }

        toolbarSetEnabled(false)

        barPainter = BarPainter(this, toolbar)
        barPainter.setDefaultColor()

        if (savedInstanceState == null){
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, CreateGroupFragment.newInstance(isIncognito), FRAGMENT_TAG)
                    .commit()
        }
    }

    override fun onAccountSelected(account: AccountJid?) {
        barPainter.updateWithAccountName(account)
    }

    override fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        toolbar.menu.findItem(R.id.action_create_groupchat)?.isVisible = !show
        toolbar.menu.findItem(R.id.action_create_incognito_groupchat)?.isVisible = !show
    }

    override fun toolbarSetEnabled(enabled: Boolean){
        toolbar.menu.findItem(R.id.action_create_groupchat)?.isEnabled = enabled
        toolbar.menu.findItem(R.id.action_create_incognito_groupchat)?.isEnabled = enabled
        val view = findViewById<View>(R.id.action_create_groupchat) ?: findViewById(R.id.action_create_incognito_groupchat)
        if (view is TextView) view.setTextColor(view.textColors.withAlpha(if (enabled) 255 else 127))
    }

    companion object {

        private const val FRAGMENT_TAG = "com.xabber.android.ui.fragment.groups.CreateGroupchatFragment"
        private const val CREATE_INCOGNITO_GROUPCHAT_INTENT = "com.xabber.android.ui.activity.CreateGroupchatActivity.CREATE_INCOGNITO_GROUPCHAT_INTENT"
        private const val CREATE_PUBLIC_GROUPCHAT_INTENT = "com.xabber.android.ui.activity.CreateGroupchatActivity.CREATE_PUBLIC_GROUPCHAT_INTENT"

        fun createCreateIncognitoGroupchatIntent() =
                Intent(Application.getInstance().applicationContext, CreateGroupActivity::class.java).apply {
                    action = CREATE_INCOGNITO_GROUPCHAT_INTENT
                }

        fun createCreatePublicGroupchatIntent() =
                Intent(Application.getInstance().applicationContext, CreateGroupActivity::class.java).apply {
                    action = CREATE_PUBLIC_GROUPCHAT_INTENT
                }

    }

}