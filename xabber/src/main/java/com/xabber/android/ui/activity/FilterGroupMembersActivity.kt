package com.xabber.android.ui.activity

import android.content.Context
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
import com.xabber.android.data.*
import com.xabber.android.data.database.realmobjects.GroupMemberRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.ui.OnGroupchatRequestListener
import com.xabber.android.ui.activity.GroupchatMemberActivity.Companion.createIntentForGroupchatAndMemberId
import com.xabber.android.ui.adapter.GroupchatMembersAdapter
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.color.StatusBarPainter
import net.gcardone.junidecode.Junidecode.unidecode

class FilterGroupMembersActivity : ManagedActivity(), OnGroupchatRequestListener,
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

        groupchat = ChatManager.getInstance().getChat(
            intent.getAccountJid(), intent.getContactJid()
        ) as GroupChat

        setupToolbar()
        setupRecyclerView()
        updateRecyclerView()
    }

    private fun setupToolbar() {

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

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.filter_group_members_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter = GroupchatMembersAdapter(arrayListOf<GroupMemberRealmObject>(), groupchat, this)
        recyclerView.adapter = adapter
    }

    private fun updateRecyclerView() {
        if (filterString.isNotEmpty()) {
            val newList = ArrayList<GroupMemberRealmObject>()
            for (groupchatMember in GroupMemberManager.getCurrentGroupMembers(groupchat))
                if (groupchatMember?.nickname?.toLowerCase()?.contains(filterString) == true
                    || groupchatMember?.nickname?.toLowerCase()
                        ?.contains(unidecode(filterString)) == true
                    || (groupchatMember?.jid != null && groupchatMember.jid?.toLowerCase()
                        ?.contains(filterString) == true)
                    || (groupchatMember?.jid != null && groupchatMember.jid?.toLowerCase()
                        ?.contains(unidecode(filterString)) == true)
                ) {
                    newList.add(groupchatMember)
                }
            adapter.setItems(newList)
        } else {
            val list = ArrayList(
                GroupMemberManager.getCurrentGroupMembers(groupchat)
            )
            list.sortWith { o1, o2 ->
                if (o1?.isMe == true && o2?.isMe != true) return@sortWith -1
                if (o2?.isMe == true && o1?.isMe != true) return@sortWith 1
                0
            }
            adapter.setItems(list)
        }

        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        Application.getInstance().addUIListener(OnGroupchatRequestListener::class.java, this)
        GroupMemberManager.requestGroupchatMembers(groupchat.account, groupchat.contactJid)
        super.onResume()
    }

    override fun onPause() {
        Application.getInstance().removeUIListener(OnGroupchatRequestListener::class.java, this)
        super.onPause()
    }


    override fun onGroupchatMemberUpdated(
        accountJid: AccountJid,
        groupchatJid: ContactJid,
        groupchatMemberId: String
    ) {
        if (isThisChat(accountJid, groupchatJid)) {
            Application.getInstance().runOnUiThread(::updateRecyclerView)
        }
    }

    override fun onGroupchatMembersReceived(account: AccountJid, groupchatJid: ContactJid) {
        Application.getInstance().runOnUiThread(::updateRecyclerView)
    }

    override fun onMeReceived(accountJid: AccountJid, groupchatJid: ContactJid) {
        if (isThisChat(accountJid, groupchatJid)) {
            Application.getInstance().runOnUiThread(::updateRecyclerView)
        }
    }

    override fun onMemberClick(groupMember: GroupMemberRealmObject) = startActivity(
        createIntentForGroupchatAndMemberId(this, groupMember.memberId, groupchat)
    )

    private fun isThisChat(account: AccountJid, contactJid: ContactJid) =
        groupchat.account == account && groupchat.contactJid == contactJid

    companion object {
        fun createIntent(context: Context, account: AccountJid, contactJid: ContactJid) =
            createContactIntent(
                context, FilterGroupMembersActivity::class.java, account, contactJid
            )
    }

}