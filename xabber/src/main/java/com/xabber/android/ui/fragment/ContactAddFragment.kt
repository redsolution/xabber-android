package com.xabber.android.ui.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.zxing.integration.android.IntentIntegrator
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.NetworkException
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.entity.ContactJid.ContactJidCreateException
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.roster.PresenceManager
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.activity.ContactAddActivity
import com.xabber.android.ui.activity.QRCodeScannerActivity
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.helper.ContactAdder
import com.xabber.android.ui.widget.AccountSpinner
import org.jivesoftware.smack.SmackException.*
import org.jivesoftware.smack.XMPPException.XMPPErrorException
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.stringprep.XmppStringprepException
import java.util.*

class ContactAddFragment : CircleEditorFragment(), ContactAdder, View.OnClickListener, AccountSpinner.Listener {

    private var listenerActivity: Listener? = null
    private var accountSpinner: AccountSpinner? = null

    private var userViewTv: TextView? = null
    private var userViewEt: EditText? = null
    private var userViewVw: View? = null

    private var nameViewTv: TextView? = null
    private var nameViewEt: EditText? = null
    private var nameViewVw: View? = null

    private var errorView: TextView? = null
    private var error: String? = null
    private var oldError = false

    private var qrScan: ImageView? = null
    private var clearText: ImageView? = null

    private var isAccountSelected = AccountManager.getInstance().enabledAccounts.size == 1

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listenerActivity = context as Listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_contact_add, container, false)
        var name: String?
        if (savedInstanceState != null) {
            name = savedInstanceState.getString(SAVED_NAME)
            error = savedInstanceState.getString(SAVED_ERROR)
            setAccount(savedInstanceState.getParcelable(SAVED_ACCOUNT))
            setContactJid(savedInstanceState.getParcelable(SAVED_USER))
        } else {
            if (getAccount() == null || getContactJid() == null) {
                name = null
            } else {
                name = RosterManager.getInstance().getName(getAccount(), getContactJid())
                if (getContactJid().jid.asBareJid().toString() == name) {
                    name = null
                }
            }
        }
        if (getAccount() == null) {
            val accounts = AccountManager.getInstance().enabledAccounts
            if (accounts.size == 1) {
                setAccount(accounts.iterator().next())
            }
        }
        setUpAccountView(view)
        clearText = view.findViewById(R.id.imgCross)
        clearText?.setOnClickListener { userViewEt!!.text.clear() }
        errorView = view.findViewById(R.id.error_view)
        if (error != null && "" != error) {
            oldError = true
            setError(error!!)
        }
        userViewEt = view.findViewById(R.id.contact_user)
        userViewTv = view.findViewById(R.id.contact_user_tv)
        userViewVw = view.findViewById(R.id.contact_username_line_view)
        userViewEt?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (oldError) {
                    oldError = false
                } else {
                    setError("")
                }
                if (editable.toString() == "") {
                    (activity as ContactAddActivity?)!!.toolbarSetEnabled(false)
                    clearText?.visibility = View.GONE
                    qrScan!!.visibility = View.VISIBLE
                } else {
                    (activity as ContactAddActivity?)!!.toolbarSetEnabled(isAccountSelected)
                    clearText?.visibility = View.VISIBLE
                    qrScan!!.visibility = View.GONE
                }
            }
        })
        nameViewEt = view.findViewById(R.id.contact_name)
        nameViewTv = view.findViewById(R.id.contact_name_tv)
        nameViewVw = view.findViewById(R.id.contact_name_line_view)
        qrScan = view.findViewById(R.id.imgQRcode)
        qrScan?.setOnClickListener(this)
        if (getContactJid() != null) {
            userViewEt?.setText(getContactJid().bareJid.toString())
        }
        if (name != null) {
            nameViewEt?.setText(name)
        }
        setColor(AccountManager.getInstance().firstAccount)
        return view
    }

    private fun setColor(accountJid: AccountJid){
        val color = ColorManager.getInstance().accountPainter.getAccountSendButtonColor(accountJid)
        val defaultLabelTextColor =
                if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark)
                    ColorManager.getColorWithAlpha(Color.GRAY, 0.1f)
                else ColorManager.getColorWithAlpha(Color.GRAY, 0.9f)
        val defaultLineBackgroundColor =
                if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark)
                    ColorManager.getColorWithAlpha(Color.GRAY, 0.1f)
                else ColorManager.getColorWithAlpha(Color.GRAY, 0.9f)

        userViewEt?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus){
                userViewTv?.setTextColor(color)
                userViewVw?.setBackgroundColor(color)
            } else {
                userViewTv?.setTextColor(defaultLabelTextColor)
                userViewVw?.setBackgroundColor(defaultLineBackgroundColor)
            }
        }

        if (userViewEt!!.isFocused){
            userViewTv?.setTextColor(color)
            userViewVw?.setBackgroundColor(color)
        }

        nameViewEt?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus){
                nameViewTv?.setTextColor(color)
                nameViewVw?.setBackgroundColor(color)
            } else {
                nameViewTv?.setTextColor(defaultLabelTextColor)
                nameViewVw?.setBackgroundColor(defaultLineBackgroundColor)
            }
        }

        if (nameViewEt!!.isFocused){
            nameViewTv?.setTextColor(color)
            nameViewVw?.setBackgroundColor(color)
        }
    }

    private fun setUpAccountView(view: View) {
        accountSpinner = view.findViewById(R.id.contact_account)

        if (AccountManager.getInstance().enabledAccounts.size <= 1) {
            accountSpinner?.visibility = View.GONE
            val exceptSpinnerLinearLayout = view.findViewById<LinearLayout>(R.id.except_spinner)
            val llm = exceptSpinnerLinearLayout.layoutParams as ViewGroup.MarginLayoutParams
            llm.topMargin = 0
            exceptSpinnerLinearLayout.requestLayout()
        } else {
            val jids = AccountManager.getInstance().enabledAccounts.toList().sortedWith { o1, o2 -> o1.compareTo(o2) }

            val avatars = mutableListOf<Drawable>()
            for (jid in jids){
                avatars.add(AvatarManager.getInstance().getAccountAvatar(jid))
            }

            val nicknames = mutableListOf<String?>()
            for (jid in jids){
                val name = RosterManager.getInstance().getBestContact(jid, ContactJid.from(jid.fullJid.asBareJid())).name
                if (!name.isNullOrEmpty()){
                    nicknames.add(name)
                } else {
                    nicknames.add(null)
                }
            }

            accountSpinner?.setup(getString(R.string.add_to), getString(R.string.choose_account), jids, avatars,
                    nicknames, this)
        }
    }

    override fun onClick(view: View) {
        if (view.id == R.id.imgQRcode) {
            val integrator = IntentIntegrator.forSupportFragment(this)
            integrator.setOrientationLocked(false)
                    .setBeepEnabled(false)
                    .setCameraId(0)
                    .setPrompt("")
                    .addExtra("caller", "ContactAddFragment")
                    .setCaptureActivity(QRCodeScannerActivity::class.java)
                    .initiateScan(Collections.unmodifiableList(listOf(IntentIntegrator.QR_CODE)))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(activity, "no-go", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(activity, "Scanned = " + result.contents,
                        Toast.LENGTH_LONG).show()
                if (result.contents.length > 5) {
                    val s = result.contents.split(":").toTypedArray()
                    if ((s[0] == "xmpp" || s[0] == "xabber") && s.size >= 2) {
                        userViewEt!!.setText(s[1])
                        if (validationIsNotSuccess()) {
                            (activity as ContactAddActivity?)!!.toolbarSetEnabled(false)
                            userViewEt!!.requestFocus()
                        } else nameViewEt!!.requestFocus()
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(SAVED_ACCOUNT, getAccount())
        outState.putString(SAVED_USER, userViewEt!!.text.toString())
        outState.putString(SAVED_NAME, nameViewEt!!.text.toString())
        outState.putString(SAVED_ERROR, errorView!!.text.toString())
    }

    override fun onDetach() {
        super.onDetach()
        listenerActivity = null
    }

    override fun onSelected(accountJid: AccountJid) {
        if (listenerActivity != null){
            listenerActivity?.onAccountSelected(accountJid)
        }

        if (userViewEt!!.text != null && userViewEt!!.text.toString().isNotEmpty())
            (activity as ContactAddActivity?)!!.toolbarSetEnabled(true)

        isAccountSelected = true

        userViewEt?.isFocusableInTouchMode = true
        userViewEt?.requestFocusFromTouch()

        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(userViewEt, InputMethodManager.SHOW_IMPLICIT)

        setColor(accountJid)

        if (accountJid != getAccount()) {
            setAccount(accountJid)
            setAccountCircles()
            updateCircles()
        }

        if (listView.visibility == View.GONE) {
            listView.visibility = View.VISIBLE
        }
    }

    override fun addContact() {
        val account = accountSpinner!!.selected
        if (account == null || getAccount() == null) {
            Toast.makeText(activity, getString(R.string.EMPTY_ACCOUNT), Toast.LENGTH_LONG).show()
            return
        }
        var contactString = userViewEt!!.text.toString()
        contactString = contactString.trim { it <= ' ' }
        if (validationIsNotSuccess()) return
        val user: ContactJid
        user = try {
            val jid = JidCreate.bareFrom(contactString)
            ContactJid.from(jid)
        } catch (e: XmppStringprepException) {
            LogManager.exception(LOG_TAG, e)
            setError(getString(R.string.INCORRECT_USER_NAME))
            return
        } catch (e: ContactJidCreateException) {
            LogManager.exception(LOG_TAG, e)
            setError(getString(R.string.INCORRECT_USER_NAME))
            return
        }
        if (listenerActivity != null) listenerActivity!!.showProgress(true)
        val name = nameViewEt!!.text.toString()
        val groups = selected
        Application.getInstance().runInBackgroundNetworkUserRequest(object : Runnable {
            override fun run() {
                try {
                    RosterManager.getInstance().createContact(account, user, name, groups)
                    PresenceManager.getInstance().requestSubscription(account, user)
                } catch (e: NotLoggedInException) {
                    Application.getInstance().onError(R.string.NOT_CONNECTED)
                    stopAddContactProcess(false)
                } catch (e: NotConnectedException) {
                    Application.getInstance().onError(R.string.NOT_CONNECTED)
                    stopAddContactProcess(false)
                } catch (e: XMPPErrorException) {
                    Application.getInstance().onError(R.string.XMPP_EXCEPTION)
                    stopAddContactProcess(false)
                } catch (e: NoResponseException) {
                    Application.getInstance().onError(R.string.CONNECTION_FAILED)
                    stopAddContactProcess(false)
                } catch (e: NetworkException) {
                    Application.getInstance().onError(e)
                    stopAddContactProcess(false)
                } catch (e: InterruptedException) {
                    LogManager.exception(this, e)
                    stopAddContactProcess(false)
                }
                stopAddContactProcess(true)
            }
        })
    }

    private fun validationIsNotSuccess(): Boolean {
        var contactString = userViewEt!!.text.toString()
        contactString = contactString.trim { it <= ' ' }
        if (contactString.contains(" ")) {
            setError(getString(R.string.INCORRECT_USER_NAME))
            return true
        }
        if (TextUtils.isEmpty(contactString)) {
            setError(getString(R.string.EMPTY_USER_NAME))
            return true
        }
        val atChar = contactString.indexOf('@')
        val slashIndex = contactString.indexOf('/')
        val domainName: String
        val localName: String
        val resourceName: String
        if (slashIndex > 0) {
            resourceName = contactString.substring(slashIndex + 1)
            if (atChar in 1 until slashIndex) {
                localName = contactString.substring(0, atChar)
                domainName = contactString.substring(atChar + 1, slashIndex)
            } else {
                localName = ""
                domainName = contactString.substring(0, slashIndex)
            }
        } else {
            resourceName = ""
            if (atChar > 0) {
                localName = contactString.substring(0, atChar)
                domainName = contactString.substring(atChar + 1)
            } else {
                localName = ""
                domainName = contactString
            }
        }

        //
        if (resourceName != "") {
            setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_RESOURCE))
            return true
        }
        //Invalid when domain is empty
        if (domainName == "") {
            setError(getString(R.string.INCORRECT_USER_NAME))
            return true
        }

        //Invalid when "@" is present but localPart is empty
        if (atChar == 0) {
            setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_LOCAL_AT))
            return true
        }

        //Invalid when "@" is present in a domainPart
        if (atChar > 0) {
            if (domainName.contains("@")) {
                setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_AT))
                return true
            }
        }

        //Invalid when domain has "." at the start/end
        if (domainName[domainName.length - 1] == '.' || domainName[0] == '.') {
            setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_DOMAIN))
            return true
        }
        //Invalid when domain does not have a "." in the middle, when paired with the last check
        if (!domainName.contains(".")) {
            setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_DOMAIN))
            return true
        }
        //Invalid when domain has multiple dots in a row
        if (domainName.contains("..")) {
            setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_DOMAIN))
            return true
        }
        if (localName != "") {
            //Invalid when localPart is NOT empty, and HAS "." at the start/end
            if (localName[localName.length - 1] == '.' || localName[0] == '.') {
                setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_LOCAL))
                return true
            }
            //Invalid when localPart is NOT empty, and contains ":" or "/" symbol. Other restricted localPart symbols get checked during the creation of the jid/userJid.
            if (localName.contains(":")) {
                setError(getString(R.string.INCORRECT_USER_NAME) + String.format(getString(R.string.INCORRECT_USER_NAME_ADDENDUM_LOCAL_SYMBOL), ":"))
                return true
            }

            //Invalid when localPart is NOT empty, and has multiple dots in a row
            if (localName.contains("..")) {
                setError(getString(R.string.INCORRECT_USER_NAME) + getString(R.string.INCORRECT_USER_NAME_ADDENDUM_LOCAL))
                return true
            }
        }
        return false
    }

    private fun setError(error: String) {
        errorView!!.text = error
        errorView!!.visibility = if ("" == error) View.INVISIBLE else View.VISIBLE
    }

    private fun stopAddContactProcess(success: Boolean) {
        Application.getInstance().runOnUiThread {
            if (listenerActivity != null) listenerActivity!!.showProgress(false)
            if (success) activity!!.finish()
        }
    }

    interface Listener {
        fun onAccountSelected(account: AccountJid?)
        fun showProgress(show: Boolean)
    }

    companion object {
        private const val LOG_TAG = "ContactAddFragment"
        private const val SAVED_NAME = "com.xabber.android.ui.fragment..ContactAddFragment.SAVED_NAME"
        private const val SAVED_ACCOUNT = "com.xabber.android.ui.fragment..ContactAddFragment.SAVED_ACCOUNT"
        private const val SAVED_USER = "com.xabber.android.ui.fragment..ContactAddFragment.SAVED_USER"
        private const val SAVED_ERROR = "com.xabber.android.ui.fragment..ContactAddFragment.SAVED_ERROR"
        @JvmStatic
        fun newInstance(account: AccountJid?, user: ContactJid?): ContactAddFragment {
            val fragment = ContactAddFragment()
            val args = Bundle()
            if (account != null) args.putParcelable(ARG_ACCOUNT, account)
            if (user != null) args.putParcelable(ARG_USER, user)
            fragment.arguments = args
            return fragment
        }
    }

}