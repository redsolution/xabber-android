package com.xabber.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.intent.AccountIntentBuilder
import com.xabber.android.data.intent.EntityIntentBuilder
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.ui.color.BarPainter
import com.xabber.android.ui.fragment.groups.GroupStatusFragment

class GroupStatusActivity : ManagedActivity() {

    lateinit var progressbar: ProgressBar
    lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_with_toolbar_progress_and_container)

        progressbar = findViewById(R.id.toolbarProgress)
        toolbar = findViewById(R.id.toolbar_default)

        toolbar = findViewById(R.id.toolbar_default)
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp)
        else toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)
        toolbar.setNavigationOnClickListener { finish() }

        BarPainter(this, toolbar).setDefaultColor()

        if (intent != null) {
            val group = ChatManager.getInstance()
                    .getChat(getAccount(intent), getGroupchatContact(intent))
            if (group != null && group is GroupChat)
                supportFragmentManager.beginTransaction()
                        .add(R.id.fragment_container, GroupStatusFragment(group))
                        .commit()
        } else finish()

    }

    fun showProgressBar(isVisible: Boolean) = if (isVisible) progressbar.visibility = View.VISIBLE
    else progressbar.visibility = View.GONE

    companion object {
        fun createIntent(context: Context, account: AccountJid, groupchatJid: ContactJid): Intent =
                EntityIntentBuilder(context, GroupStatusActivity::class.java)
                        .setAccount(account)
                        .setUser(groupchatJid)
                        .build()

        private fun getAccount(intent: Intent) = AccountIntentBuilder.getAccount(intent)

        private fun getGroupchatContact(intent: Intent) = EntityIntentBuilder.getUser(intent)

    }

}