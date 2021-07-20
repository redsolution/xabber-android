package com.xabber.android.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.color.StatusBarPainter
import com.xabber.android.ui.fragment.MessagesFragment

class MessagesActivity : ManagedActivity() {

    private lateinit var user: ContactJid
    private lateinit var account: AccountJid
    private lateinit var messageId: String
    private lateinit var action: String

    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forwarded)

        intent?.let {
            messageId = it.getStringExtra(KEY_MESSAGE_ID) ?: throw NullPointerException("Message id mustn't be null")
            account = it.getParcelableExtra(KEY_ACCOUNT) ?: throw NullPointerException("AccountJid mustn't be null")
            user = it.getParcelableExtra(KEY_USER) ?: throw NullPointerException("ContactJid mustn't be null")
            action = it.action ?: throw NullPointerException("Action mustn't be null")
        }

        toolbar = findViewById(R.id.toolbar_default)

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.apply {
                setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp)
                setBackgroundColor(
                    ColorManager.getInstance().accountPainter.getAccountRippleColor(account)
                )
                setTitleTextColor(Color.BLACK)
            }
            StatusBarPainter(this).updateWithAccountName(account)
        } else {
            toolbar.apply {
                toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)
                toolbar.setBackgroundColor(
                    TypedValue().also {
                        theme.resolveAttribute(R.attr.bars_color, it, true)
                    }.data
                )
                toolbar.setTitleTextColor(Color.WHITE)
            }
            StatusBarPainter(this).updateWithColor(Color.BLACK)
        }

        toolbar.setNavigationOnClickListener {
            NavUtils.navigateUpFromSameTask(this@MessagesActivity)
        }
    }

    override fun onResume() {
        super.onResume()

        supportFragmentManager.beginTransaction().let { transaction ->
            transaction.replace(
                R.id.container,
                MessagesFragment.newInstance(account, user, messageId, action)
            )
            transaction.commit()
        }
    }

    fun setToolbar(forwardedCount: Int) {
        when (action) {
            ACTION_SHOW_FORWARDED -> {
                toolbar.title = resources.getQuantityString(
                    R.plurals.forwarded_messages_count, forwardedCount, forwardedCount
                )
            }
            ACTION_SHOW_PINNED -> {
                toolbar.title = getString(R.string.pinned_message)
            }
        }
    }

    companion object {
        const val ACTION_SHOW_FORWARDED = "com.xabber.android.ui.activity.ACTION_SHOW_FORWARDED"
        const val ACTION_SHOW_PINNED = "com.xabber.android.ui.activity.ACTION_SHOW_PINNED"
        private const val KEY_MESSAGE_ID = "messageId"
        private const val KEY_ACCOUNT = "account"
        private const val KEY_USER = "user"

        fun createIntentShowForwarded(
            context: Context, messageId: String, user: ContactJid, account: AccountJid
        ) = Intent(context, MessagesActivity::class.java).apply {
            putExtra(KEY_MESSAGE_ID, messageId)
            putExtra(KEY_ACCOUNT, account as Parcelable)
            putExtra(KEY_USER, user)
            action = ACTION_SHOW_FORWARDED
        }

        fun createIntentShowPinned(
            context: Context, messageId: String, user: ContactJid, account: AccountJid
        ) = Intent(context, MessagesActivity::class.java).apply {
            putExtra(KEY_MESSAGE_ID, messageId)
            putExtra(KEY_ACCOUNT, account as Parcelable)
            putExtra(KEY_USER, user)
            action = ACTION_SHOW_PINNED
        }

    }

}