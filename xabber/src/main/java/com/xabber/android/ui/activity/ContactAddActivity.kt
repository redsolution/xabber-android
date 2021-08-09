/*
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 * <p>
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 * <p>
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.intent.EntityIntentBuilder
import com.xabber.android.ui.color.BarPainter
import com.xabber.android.ui.fragment.ContactAddFragment
import com.xabber.android.ui.fragment.ContactAddFragment.Companion.newInstance
import com.xabber.android.ui.helper.ContactAdder

class ContactAddActivity : ManagedActivity(), ContactAddFragment.Listener {

    private lateinit var barPainter: BarPainter
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_with_toolbar_progress_and_container)

        progressBar = findViewById(R.id.toolbarProgress)

        findViewById<Toolbar>(R.id.toolbar_default).apply {
            toolbar = this

            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                setNavigationIcon(R.drawable.ic_clear_grey_24dp)
            } else {
                setNavigationIcon(R.drawable.ic_clear_white_24dp)
            }

            setNavigationOnClickListener { finish() }

            inflateMenu(R.menu.toolbar_add_contact)

            setOnMenuItemClickListener {
                (supportFragmentManager.findFragmentById(R.id.fragment_container) as ContactAdder?)!!.addContact()
                return@setOnMenuItemClickListener true
            }
        }

        val view = findViewById<View>(R.id.action_add_contact)
        if (view is TextView) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                view.setTextColor(resources.getColor(R.color.grey_900))
            } else {
                view.setTextColor(resources.getColor(R.color.white))
            }
        }

        toolbarSetEnabled(false)

        barPainter = BarPainter(this, toolbar)
        barPainter.setDefaultColor()

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, newInstance(getAccount(intent), getUser(intent)))
                .commit()
        }

    }

    override fun onAccountSelected(account: AccountJid?) {
        barPainter.updateWithAccountName(account)
    }

    override fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        toolbar.menu.findItem(R.id.action_add_contact).isVisible = !show
    }

    fun toolbarSetEnabled(active: Boolean) {
        toolbar.menu.findItem(R.id.action_add_contact).isEnabled = active
        val view = findViewById<View>(R.id.action_add_contact)
        if (view is TextView) {
            view.setTextColor(view.textColors.withAlpha(if (active) 255 else 127))
        }
    }

    companion object {

        @JvmStatic
        @JvmOverloads
        fun createIntent(
            context: Context?,
            account: AccountJid? = null,
            user: ContactJid? = null
        ): Intent = EntityIntentBuilder(context, ContactAddActivity::class.java)
            .setAccount(account)
            .setUser(user)
            .build()

        private fun getAccount(intent: Intent): AccountJid? {
            return EntityIntentBuilder.getAccount(intent)
        }

        private fun getUser(intent: Intent): ContactJid? {
            return EntityIntentBuilder.getContactJid(intent)
        }

    }

}