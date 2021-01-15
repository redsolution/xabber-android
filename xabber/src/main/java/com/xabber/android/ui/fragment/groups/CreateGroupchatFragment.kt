package com.xabber.android.ui.fragment.groups

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.extension.groupchat.create.CreateGroupchatIqResultListener
import com.xabber.android.data.message.chat.groupchat.GroupchatIndexType
import com.xabber.android.data.message.chat.groupchat.GroupchatManager
import com.xabber.android.data.message.chat.groupchat.GroupchatMembershipType
import com.xabber.android.data.message.chat.groupchat.GroupchatPrivacyType
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.activity.ChatActivity
import com.xabber.android.ui.activity.CreateGroupchatActivity
import com.xabber.android.ui.widget.AccountSpinner
import com.xabber.android.utils.StringUtils


class CreateGroupchatFragment : Fragment(), CreateGroupchatIqResultListener {

    private lateinit var accountSp: AccountSpinner
    private lateinit var groupchatNameEt: EditText
    private lateinit var groupchatJidEt: EditText
    private lateinit var serversSp: Spinner
    private lateinit var membershipTypeSp: Spinner
    private lateinit var indexTypeSp: Spinner
    private lateinit var descriptionEt: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var settingsRootLl: LinearLayoutCompat

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.create_groupchat_fragment, container, false)
        accountSp = view.findViewById(R.id.contact_account)
        groupchatNameEt = view.findViewById(R.id.create_groupchat_name_et)
        groupchatJidEt = view.findViewById(R.id.create_groupchat_jid_et)
        serversSp = view.findViewById(R.id.create_groupchat_server_sp)
        membershipTypeSp = view.findViewById(R.id.create_groupchat_membership_spinner)
        indexTypeSp = view.findViewById(R.id.create_groupchat_index_type_spinner)
        descriptionEt = view.findViewById(R.id.create_groupchat_description)
        progressBar = view.findViewById(R.id.create_groupchat_progress_bar)
        settingsRootLl = view.findViewById(R.id.create_groupchat_group_setting_root)

        if (AccountManager.getInstance().enabledAccounts.size > 1) {
            settingsRootLl.visibility = View.GONE
        }

        groupchatNameEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                groupchatJidEt.setText(StringUtils.getLocalpartHintByString(s.toString()))
            }
        })

        setupAccountSpinner()
        setupMembershipTypeSpinner()
        setupIndexTypeSpinner()
        setupServerSp()

        return view
    }

    private fun setupAccountSpinner() {
        if (AccountManager.getInstance().enabledAccounts.size <= 1) {
            accountSp.visibility = View.GONE
            settingsRootLl.visibility = View.VISIBLE
            setupServerSp()
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

            accountSp.setup(getString(R.string.create_to), getString(R.string.choose_account), jids, avatars, nicknames,
                    object : AccountSpinner.Listener{
                override fun onSelected(accountJid: AccountJid) {
                    setupServerSp()
                    settingsRootLl.visibility = View.VISIBLE
                }
            })
        }
    }

    private fun setupMembershipTypeSpinner() {
        val adapter = ArrayAdapter(Application.getInstance().applicationContext,
                android.R.layout.simple_spinner_item, GroupchatMembershipType.getLocalizedValues())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        membershipTypeSp.adapter = adapter
        membershipTypeSp.setSelection(2)
    }

    private fun setupIndexTypeSpinner() {
        val adapter = ArrayAdapter(Application.getInstance().applicationContext,
                android.R.layout.simple_spinner_item, GroupchatIndexType.getLocalizedValues())

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        indexTypeSp.adapter = adapter
        indexTypeSp.setSelection(2)
    }

    private fun createListOfServers(customServer: String = ""): List<String> {
        val list = arrayListOf<String>()

        if (AccountManager.getInstance().enabledAccounts.size <= 1) {

            val accounts = arrayListOf<AccountJid>()
            accounts.addAll(AccountManager.getInstance().enabledAccounts)

            val serversList = GroupchatManager.getInstance()
                    .getAvailableGroupchatServersForAccountJid(accounts[0])

            if (serversList != null && serversList.isNotEmpty())
                for (jid in serversList)
                    list.add(jid.toString())

        } else if (accountSp.selected != null) {
            for (jid in GroupchatManager.getInstance().getAvailableGroupchatServersForAccountJid(accountSp.selected))
                list.add(jid.toString())
        }

        if (customServer.isNotEmpty()) list.add(customServer)

        list.add(getString(R.string.groupchat_custom_server))

        return list
    }

    private fun setupServerSp() {
        serversSp.adapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item,
                createListOfServers())

        serversSp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int,
                                        id: Long) {
                if (serversSp.adapter.count > 1 && position == serversSp.adapter.count - 1
                        || serversSp.adapter.count == 1 && position == 0) {
                    val dialog = AlertDialog.Builder(context!!).apply {
                        setTitle(getString(R.string.groupchat_server))
                        val editText = EditText(activity?.baseContext)
                        setView(editText)
                        setPositiveButton(getString(R.string.groupchat_add_custom_server)) { _, _ ->
                            serversSp.adapter = ArrayAdapter(context,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    createListOfServers(editText.text.toString()))

                            serversSp.setSelection(serversSp.adapter.count - 2)
                        }
                        setNegativeButton(R.string.cancel) { _, _ -> serversSp.setSelection(0) }
                    }
                    dialog.create().show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun createGroupchat(isIncognito: Boolean) {
        val membershipType = GroupchatMembershipType
                .getMembershipByLocalizedString(membershipTypeSp.selectedItem as String)

        val indexType = GroupchatIndexType
                .getIndexTypeByLocalizedString(indexTypeSp.selectedItem as String)

        val privacyType: GroupchatPrivacyType = if (isIncognito) GroupchatPrivacyType.INCOGNITO
        else GroupchatPrivacyType.PUBLIC

        GroupchatManager.getInstance().sendCreateGroupchatRequest(accountSp.selected, serversSp.selectedItem.toString(),
                groupchatNameEt.text.toString(), descriptionEt.text.toString(), groupchatJidEt.text.toString(),
                membershipType, indexType, privacyType, this)

        progressBar.visibility = View.VISIBLE
    }

    override fun onSuccessfullyCreated(accountJid: AccountJid?, contactJid: ContactJid?) {
        Application.getInstance().runOnUiThread {
            progressBar.visibility = View.GONE
            if (activity is CreateGroupchatActivity)
                activity?.startActivity(ChatActivity.createSendIntent(context, accountJid, contactJid, null))
        }
    }

    override fun onJidConflict() {
        Application.getInstance().runOnUiThread {
            Toast.makeText(context,
                    getString(R.string.groupchat_failed_to_create_groupchat_jid_already_exists),
                    Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
        }
    }

    override fun onOtherError() {
        Application.getInstance().runOnUiThread {
            Toast.makeText(context,
                    getString(R.string.groupchat_failed_to_create_groupchat_with_unknown_reason),
                    Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
        }
    }

}