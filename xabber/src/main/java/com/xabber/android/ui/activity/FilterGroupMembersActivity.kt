package com.xabber.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groupchat.OnGroupchatRequestListener
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.groups.GroupMember
import com.xabber.android.data.groups.GroupMemberManager
import com.xabber.android.ui.activity.GroupchatMemberActivity.Companion.createIntentForGroupchatAndMemberId
import com.xabber.android.ui.adapter.GroupchatMembersAdapter
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.color.StatusBarPainter
import com.xabber.android.utils.StringUtils

class FilterGroupMembersActivity: ManagedActivity(), OnGroupchatRequestListener,
        GroupchatMembersAdapter.OnMemberClickListener {

    private lateinit var arrowBackIv: ImageView
    private lateinit var clearIv: ImageView
    private lateinit var editText: EditText
    private lateinit var recyclerView: RecyclerView

    private lateinit var groupchat: GroupChat

    private var filterString = ""
    private lateinit var adapter: GroupchatMembersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.filter_group_members_activity)

        try {
            val accountJid = AccountJid.from(intent.getStringExtra(ACCOUNT_TAG)!!)
            val contactJid = ContactJid.from(intent.getStringExtra(CONTACT_TAG))
            groupchat = ChatManager.getInstance().getChat(accountJid, contactJid) as GroupChat
        } catch (e: Exception){
            LogManager.exception(this.javaClass, e)
            finish()
        }

        setupToolbar()
        setupRecyclerView()
        updateRecyclerView()
    }

    private fun setupToolbar(){

        val isLightTheme = SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light

        arrowBackIv = findViewById(R.id.toolbar_search_back_button)
        clearIv = findViewById(R.id.search_toolbar_clear_button)
        editText = findViewById(R.id.search_toolbar_edittext)
        val root = findViewById<RelativeLayout>(R.id.group_member_search_toolbar_root)

        val typedValue = TypedValue()
        if (isLightTheme) {
            val accountPainter = ColorManager.getInstance().accountPainter
            root.setBackgroundColor(accountPainter.defaultRippleColor)
            StatusBarPainter.instanceUpdateWIthColor(this, accountPainter.defaultMainColor)
        } else {
            this.theme.resolveAttribute(R.attr.bars_color, typedValue, true)
            root.setBackgroundColor(typedValue.data)
            StatusBarPainter.instanceUpdateWIthColor(this, typedValue.data)
        }

        arrowBackIv.setOnClickListener { finish() }
        clearIv.setOnClickListener { editText.setText("") }
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                filterString = p0.toString()
                if (p0.isNullOrEmpty()) {
                    clearIv.visibility = View.GONE
                } else {
                    clearIv.visibility = View.VISIBLE
                }
                updateRecyclerView()
            }
        })

        editText.requestFocus()

    }

    private fun setupRecyclerView(){
        recyclerView = findViewById(R.id.filter_group_members_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter = GroupchatMembersAdapter(arrayListOf<GroupMember>(), groupchat, this)
        recyclerView.adapter = adapter
    }

    private fun updateRecyclerView(){
        if (filterString.isNotEmpty()) {
            val newList = ArrayList<GroupMember>()
            for (groupchatMember in GroupMemberManager.getInstance()
                    .getGroupMembers(groupchat.contactJid))
                if (groupchatMember.nickname!!.toLowerCase().contains(filterString)
                        || groupchatMember.nickname!!.toLowerCase().contains(StringUtils.translitirateToLatin(filterString))
                        || (groupchatMember.jid != null && groupchatMember.jid!!.toLowerCase().contains(filterString))
                        || (groupchatMember.jid != null && groupchatMember.jid!!.toLowerCase().contains(StringUtils.translitirateToLatin(filterString))))
                    newList.add(groupchatMember)
            adapter.setItems(newList)
        } else {
            val list = ArrayList(GroupMemberManager.getInstance()
                    .getGroupMembers(groupchat.contactJid))
            list.sortWith { o1: GroupMember, o2: GroupMember ->
                if (o1.isMe && !o2.isMe) return@sortWith -1
                if (o2.isMe && !o1.isMe) return@sortWith 1
                0
            }
            adapter.setItems(list)
        }

        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        Application.getInstance().addUIListener(OnGroupchatRequestListener::class.java, this)
        GroupMemberManager.getInstance().requestGroupchatMembers(groupchat.account, groupchat.contactJid)
        super.onResume()
    }

    override fun onPause() {
        Application.getInstance().removeUIListener(OnGroupchatRequestListener::class.java, this)
        super.onPause()
    }


    override fun onGroupchatMemberUpdated(accountJid: AccountJid, groupchatJid: ContactJid, groupchatMemberId: String?) {
        if (isThisChat(accountJid, groupchatJid))
            Application.getInstance().runOnUiThread { updateRecyclerView() }
    }

    override fun onGroupchatMembersReceived(account: AccountJid?, groupchatJid: ContactJid?) {
        updateRecyclerView()
    }

    override fun onMeReceived(accountJid: AccountJid, groupchatJid: ContactJid) {
        if (isThisChat(accountJid, groupchatJid))
            Application.getInstance().runOnUiThread { updateRecyclerView() }
    }

    override fun onMemberClick(groupMember: GroupMember?) = startActivity(
            createIntentForGroupchatAndMemberId(this, groupMember!!.id, groupchat))

    private fun isThisChat(account: AccountJid, contactJid: ContactJid) =
            groupchat.account == account && groupchat.contactJid == contactJid

    private fun isThisChat(groupchat: GroupChat) = this.groupchat == groupchat

    companion object{
        private const val ACCOUNT_TAG = "com.xabber.android.ui.activity.FilterGroupMembersActivity.ACCOUNT_TAG"
        private const val CONTACT_TAG = "com.xabber.android.ui.activity.FilterGroupMembersActivity.CONTACT_TAG"

        fun createIntent(context: Context, account: AccountJid, contactJid: ContactJid): Intent{
            return Intent(context, FilterGroupMembersActivity::class.java).apply {

                putExtra(ACCOUNT_TAG, account.fullJid.toString())
                putExtra(CONTACT_TAG, contactJid.bareJid.toString())
            }
        }
    }

}