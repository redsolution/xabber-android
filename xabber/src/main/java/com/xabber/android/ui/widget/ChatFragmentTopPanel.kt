package com.xabber.android.ui.widget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.NetworkException
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.data.extension.groups.GroupInviteManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.roster.PresenceManager
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.color.ColorManager
import java.util.*

class ChatFragmentTopPanel : Fragment() {

    private lateinit var chat: AbstractChat

    private val accountJid: AccountJid
        get() = chat.account

    private val contactJid: ContactJid
        get() = chat.contactJid

    private lateinit var newContactLayout: LinearLayout
    private lateinit var addContact: TextView
    private lateinit var blockContact: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.layout_chat_new_contact, container, false)

        newContactLayout = view as LinearLayout
        addContact = view.findViewById(R.id.add_contact)
        blockContact = newContactLayout.findViewById(R.id.block_contact)

        inflateNewContactLayout()

        return view
    }


    private fun inflateNewContactLayout() {
        val subscriptionState =
            RosterManager.getInstance().getSubscriptionState(accountJid, contactJid)

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
            newContactLayout.setBackgroundResource(R.color.grey_950)
        }
//        val transition: Transition = Slide(Gravity.TOP)
//        transition.duration = 300
//        transition.addTarget(newContactLayout)

        val closeNewContactLayout =
            newContactLayout.findViewById<ImageButton>(R.id.close_new_contact_layout)
        when (subscriptionState.subscriptionType) {
            RosterManager.SubscriptionState.FROM, RosterManager.SubscriptionState.NONE ->
                when (subscriptionState.pendingSubscription) {
                    RosterManager.SubscriptionState.PENDING_NONE -> {
                        if (RosterManager.getInstance()
                                .getRosterContact(accountJid, contactJid) != null
                        ) {
                            newContactLayout.findViewById<TextView>(R.id.add_contact_message)
                                ?.apply {
                                    setText(R.string.chat_subscribe_request_outgoing)
                                    visibility = View.VISIBLE
                                }
                            addContact.setText(R.string.chat_subscribe)
                            blockContact.visibility = View.GONE
                        } else {
                            if (GroupInviteManager.hasActiveIncomingInvites(
                                    accountJid,
                                    contactJid
                                )
                            ) {
                                newContactLayout.findViewById<TextView>(R.id.add_contact_message)
                                    ?.visibility = View.GONE
                                addContact.setText(R.string.groupchat_join)
                                blockContact.apply {
                                    setText(R.string.groupchat_decline)
                                    visibility = View.VISIBLE
                                }
                            } else {
                                setNewContactAddLayout()
                            }
                        }
                    }

                    RosterManager.SubscriptionState.PENDING_IN -> setNewContactAddLayout()

                    RosterManager.SubscriptionState.PENDING_IN_OUT -> setNewContactAllowLayout()
                }
            RosterManager.SubscriptionState.TO -> {
                if (subscriptionState.pendingSubscription == RosterManager.SubscriptionState.PENDING_IN) {
                    setNewContactAllowLayout()
                }
            }
        }

        addContact.setTextColor(
            ColorManager.getInstance().accountPainter.getAccountMainColor(
                accountJid
            )
        )

        addContact.setOnClickListener {
            Application.getInstance().runInBackgroundNetworkUserRequest {
                if (GroupInviteManager.hasActiveIncomingInvites(accountJid, contactJid)) {
                    GroupInviteManager.acceptInvitation(accountJid, contactJid)
                    //deflateIncomingInvite()
                } else {
                    if (RosterManager.getInstance()
                            .getRosterContact(accountJid, contactJid) == null
                    ) {
                        val name =
                            RosterManager.getInstance().getBestContact(accountJid, contactJid)?.name
                                ?: contactJid.toString()
                        RosterManager.getInstance().createContact(
                            accountJid, contactJid, name, ArrayList()
                        )
                    } else {
                        if (subscriptionState.subscriptionType == RosterManager.SubscriptionState.FROM
                            || subscriptionState.subscriptionType == RosterManager.SubscriptionState.NONE
                        ) {
                            if (!subscriptionState.hasOutgoingSubscription()) {
                                PresenceManager.subscribeForPresence(accountJid, contactJid)
                            }
                        }
                    }
                    if (subscriptionState.subscriptionType == RosterManager.SubscriptionState.TO) {
                        PresenceManager.addAutoAcceptSubscription(accountJid, contactJid)
                    } else if (subscriptionState.subscriptionType == RosterManager.SubscriptionState.NONE) {
                        if (subscriptionState.hasIncomingSubscription()) {
                            PresenceManager.acceptSubscription(accountJid, contactJid, false)
                        } else {
                            PresenceManager.addAutoAcceptSubscription(accountJid, contactJid)
                        }
                    }
                }
            }
            //TransitionManager.beginDelayedTransition((rootView as ViewGroup?)!!, transition)
            newContactLayout.visibility = View.GONE
        }

        blockContact.setTextColor(
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
                resources.getColor(R.color.red_700)
            } else {
                resources.getColor(R.color.red_900)
            }
        )

        blockContact.setOnClickListener {
            try {
                if (GroupInviteManager.hasActiveIncomingInvites(accountJid, contactJid)) {
                    GroupInviteManager.declineInvitation(accountJid, contactJid)
                    activity?.finish()
                }
            } catch (e: NetworkException) {
                Application.getInstance().onError(R.string.CONNECTION_FAILED)
            }
            if (!GroupInviteManager.hasActiveIncomingInvites(accountJid, contactJid)) {
                BlockingManager.getInstance().blockContact(
                    accountJid,
                    contactJid,
                    object : BlockingManager.BlockContactListener {
                        override fun onSuccessBlock() {
                            Toast.makeText(
                                Application.getInstance(),
                                R.string.contact_blocked_successfully,
                                Toast.LENGTH_SHORT
                            ).show()
                            if (newContactLayout.visibility == View.VISIBLE) {
                                newContactLayout.visibility = View.GONE
                            }
                            activity?.finish()
                        }

                        override fun onErrorBlock() {
                            Toast.makeText(
                                Application.getInstance(),
                                R.string.error_blocking_contact,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
            //TransitionManager.beginDelayedTransition((rootView as ViewGroup?)!!, transition)
        }

        closeNewContactLayout.setOnClickListener {
            if (subscriptionState.hasIncomingSubscription()) {
                try {
                    PresenceManager.discardSubscription(accountJid, contactJid)
                } catch (e: NetworkException) {
                    LogManager.exception(javaClass.simpleName, e)
                }
            }
            chat.isAddContactSuggested = true
            //TransitionManager.beginDelayedTransition((rootView as ViewGroup?)!!, transition)
            newContactLayout.visibility = View.GONE
        }
    }

    private fun setNewContactAddLayout() {
        newContactLayout.findViewById<TextView>(R.id.add_contact_message).visibility = View.GONE
        addContact.setText(R.string.contact_add)
        blockContact.visibility = View.VISIBLE
    }

    private fun setNewContactAllowLayout() {
        newContactLayout.findViewById<TextView>(R.id.add_contact_message)?.apply {
            setText(R.string.chat_subscribe_request_incoming)
            visibility = View.VISIBLE
        }
        addContact.setText(R.string.chat_allow)
        blockContact.visibility = View.GONE
    }

    companion object {

        // Yep, it's incorrect instancing of fragment
        fun newInstance(chat: AbstractChat) = ChatFragmentTopPanel().apply {
            this.chat = chat
        }

    }

}