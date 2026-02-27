package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.ui.activity.MessagesActivity
import com.xabber.android.ui.adapter.chat.MessagesAdapter
import com.xabber.android.ui.color.ColorManager

class MessagesFragment : FileInteractionFragment() {

    private lateinit var messageId: String
    private var action: String? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var backgroundView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

            accountJid = it.getParcelable(ARGUMENT_ACCOUNT)
                ?: throw NullPointerException("Non-null accountJid id is required!")

            contactJid = it.getParcelable(ARGUMENT_USER)
                ?: throw NullPointerException("Non-null contactJid id is required!")
            messageId = it.getString(KEY_MESSAGE_ID)

                ?: throw NullPointerException("Non-null message id is required!")
            action = it.getString(ACTION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_forwarded, container, false).also {
            recyclerView = it.findViewById(R.id.recyclerView)
            backgroundView = it.findViewById(R.id.backgroundView)
        }

    override fun onResume() {
        super.onResume()

        // background
        if (SettingsManager.chatsShowBackground()) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
                backgroundView.setBackgroundResource(R.color.black)
            } else {
                backgroundView.setBackgroundResource(R.drawable.chat_background_repeat)
            }
        } else {
            backgroundView.setBackgroundColor(ColorManager.getInstance().chatBackgroundColor)
        }

        val realm = DatabaseManager.getInstance().defaultRealmInstance

        realm.where(MessageRealmObject::class.java)
            .equalTo(MessageRealmObject.Fields.PRIMARY_KEY, messageId)
            .findFirst()
            ?.let { messageRealmObject ->
                val messages =
                    if (action == MessagesActivity.ACTION_SHOW_FORWARDED) {
                        realm.where(MessageRealmObject::class.java)
                            .`in`(
                                MessageRealmObject.Fields.PRIMARY_KEY,
                                messageRealmObject.forwardedIdsAsArray
                            )
                            .findAll()
                            .also {
                                (activity as? MessagesActivity)?.setToolbar(it.size)
                            }
                    } else {
                        realm.where(MessageRealmObject::class.java)
                            .equalTo(
                                MessageRealmObject.Fields.PRIMARY_KEY,
                                messageRealmObject.primaryKey
                            )
                            .findAll()
                            .also {
                                (activity as? MessagesActivity)?.setToolbar(0)
                            }
                    }

                recyclerView.layoutManager = LinearLayoutManager(activity)
                recyclerView.adapter = MessagesAdapter(
                    requireContext(),
                    messages,
                    ChatManager.getInstance().getChat(accountJid, contactJid)!!
                )
            }
    }

    companion object {
        private const val ARGUMENT_ACCOUNT = "ARGUMENT_ACCOUNT"
        private const val ARGUMENT_USER = "ARGUMENT_USER"
        private const val KEY_MESSAGE_ID = "messageId"
        private const val ACTION = "action"

        fun newInstance(account: AccountJid, user: ContactJid, messageId: String, action: String?) =
            MessagesFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGUMENT_ACCOUNT, account)
                    putParcelable(ARGUMENT_USER, user)
                    putString(KEY_MESSAGE_ID, messageId)
                    action?.let { putString(ACTION, it) }
                }
            }

    }

}