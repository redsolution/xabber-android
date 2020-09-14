package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groupchat.CreateGroupchatIQ
import com.xabber.android.data.message.chat.groupchat.GroupchatIndexType
import com.xabber.android.data.message.chat.groupchat.GroupchatManager
import com.xabber.android.data.message.chat.groupchat.GroupchatMembershipType
import com.xabber.android.data.message.chat.groupchat.GroupchatPrivacyType
import com.xabber.android.ui.adapter.AccountChooseAdapter
import com.xabber.android.ui.widget.NoDefaultSpinner


class CreateGroupchatFragment :  Fragment(), CreateGroupchatIQ.CreateGroupchatIqResultListener,
        AdapterView.OnItemSelectedListener {

    private lateinit var accountSp: NoDefaultSpinner
    private lateinit var groupchatNameEt: EditText
    private lateinit var groupchatJidEt: EditText
    private lateinit var serverTv: AutoCompleteTextView
    private lateinit var membershipTypeSp: Spinner
    private lateinit var indexTypeSp: Spinner
    private lateinit var descriptionEt: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.create_groupchat_fragment, container, false)
        accountSp = view.findViewById(R.id.contact_account)
        groupchatNameEt = view.findViewById(R.id.create_groupchat_name_et)
        groupchatJidEt = view.findViewById(R.id.create_groupchat_jid_et)
        serverTv = view.findViewById(R.id.create_groupchat_server_et)
        membershipTypeSp = view.findViewById(R.id.create_groupchat_membership_spinner)
        indexTypeSp = view.findViewById(R.id.create_groupchat_index_type_spinner)
        descriptionEt = view.findViewById(R.id.create_groupchat_description)
        progressBar = view.findViewById(R.id.create_groupchat_progress_bar)

        setupAccountSpinner()
        setupMembershipTypeSpinner()
        setupIndexTypeSpinner()
        setupServerEt()

        return  view
    }

    private fun setupAccountSpinner(){
        accountSp.adapter = AccountChooseAdapter(activity)

        if (AccountManager.getInstance().enabledAccounts.size <= 1){
            accountSp.setSelection(0)
            accountSp.visibility = View.GONE
        }

        accountSp.onItemSelectedListener = this
    }

    private fun setupMembershipTypeSpinner(){
        val adapter = ArrayAdapter(Application.getInstance().applicationContext,
                android.R.layout.simple_spinner_item, GroupchatMembershipType.getLocalizedValues())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        membershipTypeSp.adapter = adapter
        membershipTypeSp.setSelection(2)
    }

    private fun setupIndexTypeSpinner(){
        val adapter = ArrayAdapter(Application.getInstance().applicationContext,
        android.R.layout.simple_spinner_item, GroupchatIndexType.getLocalizedValues())

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        indexTypeSp.adapter = adapter
        indexTypeSp.setSelection(2)
    }

    private fun setupServerEt(){
        serverTv.setOnClickListener {
            serverTv.showDropDown()
            serverTv.hint = getString(R.string.groupchat_custom_server)
        }

        if (AccountManager.getInstance().enabledAccounts.size <= 1){
            val accounts = arrayListOf<AccountJid>()
            accounts.addAll(AccountManager.getInstance().enabledAccounts)
            val list = arrayListOf<String>()
            for (jid in GroupchatManager.getInstance().getAvailableGroupchatServersForAccountJid(accounts[0]))
                list.add(jid.toString())
            val adapter = ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, list)
            serverTv.setAdapter(adapter)
        } else if (accountSp.selectedItem != null){
            val list = arrayListOf<String>()
            for (jid in GroupchatManager.getInstance().getAvailableGroupchatServersForAccountJid(accountSp.selectedItem as AccountJid))
                list.add(jid.toString())
            val adapter = ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, list)
            serverTv.setAdapter(adapter)
        }
    }

    fun createGroupchat(isIncognito: Boolean){
        val membershipType = GroupchatMembershipType
                .getMembershipByLocalizedString(membershipTypeSp.selectedItem as String)

        val indexType = GroupchatIndexType
                .getPrivacyByLocalizedString(indexTypeSp.selectedItem as String)

        val privacyType: GroupchatPrivacyType = if (isIncognito) GroupchatPrivacyType.INCOGNITO
                                                else GroupchatPrivacyType.PUBLIC

        val accountJid = accountSp.selectedItem as AccountJid
        GroupchatManager.getInstance().sendCreateGroupchatRequest(accountJid,
                serverTv.text.toString(), groupchatNameEt.text.toString(),
                descriptionEt.text.toString(), groupchatJidEt.text.toString(), membershipType,
                indexType, privacyType, this)

        progressBar.visibility = View.VISIBLE
    }

    override fun onSuccessfullyCreated(accountJid: AccountJid?, contactJid: ContactJid?) {
        //todo someshit

        progressBar.visibility = View.GONE
    }

    override fun onJidConflict() {
        Toast.makeText(context,
                getString(R.string.groupchat_failed_to_create_groupchat_jid_already_exists),
                Toast.LENGTH_LONG).show()
        progressBar.visibility = View.GONE
    }

    override fun onOtherError() {
        Toast.makeText(context,
                getString(R.string.groupchat_failed_to_create_groupchat_with_unknown_reason),
                Toast.LENGTH_LONG).show()
        progressBar.visibility = View.GONE
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        setupServerEt()
    }

}