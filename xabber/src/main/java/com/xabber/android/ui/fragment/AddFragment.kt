package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.ui.OnAccountChangedListener
import com.xabber.android.ui.activity.ContactAddActivity
import com.xabber.android.ui.activity.CreateGroupActivity

class AddFragment : Fragment(), OnAccountChangedListener {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.add_fragment, container, false)

        view.findViewById<LinearLayoutCompat>(R.id.add_contact_btn).setOnClickListener {
            startActivity(ContactAddActivity.createIntent(activity))
        }

        view.findViewById<LinearLayoutCompat>(R.id.create_groupchat_btn).setOnClickListener {
            startActivity(CreateGroupActivity.createCreatePublicGroupchatIntent())
        }

        view.findViewById<LinearLayoutCompat>(R.id.create_incognito_groupchat_btn)
            .setOnClickListener {
                startActivity(CreateGroupActivity.createCreateIncognitoGroupchatIntent())
            }

        if (AccountManager.getInstance().enabledAccounts.isEmpty()) showPlaceholder(true)

        return view
    }

    override fun onResume() {
        Application.getInstance().addUIListener(OnAccountChangedListener::class.java, this)
        super.onResume()
    }

    override fun onPause() {
        Application.getInstance().removeUIListener(OnAccountChangedListener::class.java, this)
        super.onPause()
    }

    override fun onAccountsChanged(accounts: Collection<AccountJid>) {
        if (AccountManager.getInstance().enabledAccounts.isEmpty()) {
            Application.getInstance().runOnUiThread { showPlaceholder(true) }
        }
    }

    private fun showPlaceholder(show: Boolean) {
        if (show) {
            view?.findViewById<LinearLayout>(R.id.placeholder)?.visibility = View.VISIBLE
            view?.findViewById<LinearLayout>(R.id.buttons)?.visibility = View.GONE
        } else {
            view?.findViewById<LinearLayout>(R.id.placeholder)?.visibility = View.GONE
            view?.findViewById<LinearLayout>(R.id.buttons)?.visibility = View.VISIBLE
        }

    }

}