package com.xabber.android.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.BaseIqResultUiListener
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groups.GroupInviteManager
import com.xabber.android.data.intent.AccountIntentBuilder
import com.xabber.android.data.intent.EntityIntentBuilder
import com.xabber.android.ui.color.BarPainter
import com.xabber.android.ui.fragment.groups.GroupchatInviteContactFragment
import com.xabber.android.ui.fragment.groups.GroupchatInviteContactFragment.OnNumberOfSelectedInvitesChanged
import org.jivesoftware.smack.packet.XMPPError

class GroupInviteContactActivity : ManagedActivity(), Toolbar.OnMenuItemClickListener,
        OnNumberOfSelectedInvitesChanged, BaseIqResultUiListener {

    private var account: AccountJid? = null
    private var groupContactJid: ContactJid? = null
    private var toolbar: Toolbar? = null
    private var barPainter: BarPainter? = null
    private var jidsToInvite: MutableList<ContactJid>? = null
    private var progressBar: ProgressBar? = null
    private var selectionCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        account = getAccount(intent)
        groupContactJid = getGroupchatContact(intent)
        setContentView(R.layout.activity_with_toolbar_progress_and_container)
        val lightTheme = SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light
        toolbar = findViewById(R.id.toolbar_default)
        toolbar?.setNavigationIcon(if (lightTheme) R.drawable.ic_arrow_left_grey_24dp else
            R.drawable.ic_arrow_left_white_24dp)
        if (toolbar?.overflowIcon != null) {
            toolbar?.overflowIcon?.setColorFilter(if (lightTheme) resources.getColor(R.color.grey_900) else
                resources.getColor(R.color.white), PorterDuff.Mode.SRC_IN)
        }
        toolbar?.inflateMenu(R.menu.toolbar_groupchat_list_selector)
        toolbar?.setOnMenuItemClickListener(this)
        barPainter = BarPainter(this, toolbar)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().add(R.id.fragment_container,
                    GroupchatInviteContactFragment.newInstance(account, groupContactJid),
                    GroupchatInviteContactFragment.LOG_TAG).commit()
        }

        progressBar = findViewById(R.id.progressBar)
    }

    override fun onResume() {
        super.onResume()
        updateToolbar()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return onOptionsItemSelected(item)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_perform_on_selected) {
            onInviteClick()
        } else return super.onOptionsItemSelected(item)
        return true
    }

    private fun updateMenu() {
        onPrepareOptionsMenu(toolbar!!.menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_perform_on_selected).setVisible(selectionCounter > 0).title = getString(R.string.groupchat_invite)
        return true
    }

    override fun onInviteCountChange(newCount: Int) {
        selectionCounter = newCount
        updateToolbar()
    }

    private fun updateToolbar() {
        if (selectionCounter == 0) {

            toolbar?.title = getString(R.string.groupchat_invite_members)
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                toolbar?.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp)
            else toolbar?.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)

            barPainter?.updateWithAccountName(account)
            toolbar?.setNavigationOnClickListener { finish() }

        } else {
            toolbar?.title = selectionCounter.toString()

            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                toolbar?.setNavigationIcon(R.drawable.ic_clear_grey_24dp)
            else toolbar?.setNavigationIcon(R.drawable.ic_clear_white_24dp)

            toolbar?.setNavigationOnClickListener {
                inviteFragment?.cancelSelection()
                selectionCounter = 0
                updateToolbar()
            }
        }
        updateMenu()
    }

    private val inviteFragment: GroupchatInviteContactFragment?
        get() {
            val fragment = supportFragmentManager.findFragmentByTag(GroupchatInviteContactFragment.LOG_TAG)
            return if (fragment is GroupchatInviteContactFragment) {
                fragment
            } else null
        }

    private fun onInviteClick(){
        val fragment = inviteFragment
        if (fragment != null) {
            jidsToInvite = fragment.selectedContacts
            if (account != null && groupContactJid != null && jidsToInvite != null){
                GroupInviteManager.sendGroupInvitations(account!!, groupContactJid!!, jidsToInvite!!, null, this)
            }
        }
    }

    override fun onResult() {
        Application.getInstance().runOnUiThread {
            progressBar?.visibility = View.INVISIBLE
            Toast.makeText(
                    this,
                    resources.getQuantityString(R.plurals.groupchat__toast__invitations_sent, selectionCounter),
                    Toast.LENGTH_SHORT)
                    .show()
            finish()
        }
    }

    override fun onSend() {
        Application.getInstance().runOnUiThread { progressBar?.visibility = View.VISIBLE }
    }

    override fun onIqError(error: XMPPError) {
        progressBar?.visibility = View.INVISIBLE
        val text = resources.getQuantityText(R.plurals.groupchat__toast_failed_to_sent_invitations, selectionCounter)
                .toString() + " " + error.descriptiveText
        Application.getInstance().runOnUiThread {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onIqErrors(errors: List<XMPPError>) {
        progressBar?.visibility = View.INVISIBLE
        val text = StringBuilder()
                .append(resources.getQuantityText(R.plurals.groupchat__toast_failed_to_sent_invitations, selectionCounter))
                .append(" ")
        errors.forEach { xmppError ->
            run {
                text.append(xmppError.descriptiveText ?: return)
                text.append(" ")
            }
        }

        Application.getInstance().runOnUiThread { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
    }

    override fun onOtherErrors(exceptions: List<java.lang.Exception>) = onOtherError(exceptions[0])

    override fun onOtherError(exception: Exception?) {
        progressBar?.visibility = View.INVISIBLE
        Application.getInstance().runOnUiThread {
            Toast.makeText(
                    this,
                    resources.getQuantityString(R.plurals.groupchat__toast_failed_to_sent_invitations, selectionCounter),
                    Toast.LENGTH_SHORT)
                    .show()
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context?, account: AccountJid?, groupchatJid: ContactJid?): Intent {
            return EntityIntentBuilder(context, GroupInviteContactActivity::class.java)
                    .setAccount(account)
                    .setUser(groupchatJid)
                    .build()
        }

        private fun getAccount(intent: Intent): AccountJid? {
            return AccountIntentBuilder.getAccount(intent)
        }

        private fun getGroupchatContact(intent: Intent): ContactJid? {
            return EntityIntentBuilder.getContactJid(intent)
        }
    }

}