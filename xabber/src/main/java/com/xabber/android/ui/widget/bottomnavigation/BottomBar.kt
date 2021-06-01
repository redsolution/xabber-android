package com.xabber.android.ui.widget.bottomnavigation

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.SettingsManager.InterfaceTheme
import com.xabber.android.ui.activity.MainActivity
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.fragment.chatListFragment.ChatListFragment

class BottomBar : Fragment(), View.OnClickListener {

    private var listener: OnClickListener? = null
    private var chatsButton: RelativeLayout? = null
    private var contactsButton: ImageButton? = null
    private var discoverButton: ImageButton? = null
    private var callsButton: ImageButton? = null
    private var settingsButton: ImageButton? = null
    private var unreadCoutTextView: TextView? = null
    private var chatsImage: ImageView? = null

    interface OnClickListener {
        fun onChatsClick()
        fun onContactsClick()
        fun onDiscoverClick()
        fun onCallsClick()
        fun onSettingsClick()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            listener = context as OnClickListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(context!!.toString() + " must implement OnClickListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.view_bottom_bar, container, false)
        chatsButton = view.findViewById(R.id.show_chats_button)
        contactsButton = view.findViewById(R.id.show_contacts_button)
        callsButton = view.findViewById(R.id.show_calls_button)
        discoverButton = view.findViewById(R.id.show_discover_button)
        settingsButton = view.findViewById(R.id.show_settings_button)
        chatsImage = view.findViewById(R.id.show_chats_image)
        chatsButton!!.setOnClickListener(this)
        contactsButton!!.setOnClickListener(this)
        discoverButton!!.setOnClickListener(this)
        callsButton!!.setOnClickListener(this)
        if (SettingsManager.interfaceTheme().equals(InterfaceTheme.dark))
            callsButton!!.alpha = 0.5f
        else callsButton!!.alpha = 0.4f
        settingsButton!!.setOnClickListener(this)
        unreadCoutTextView = view.findViewById(R.id.unread_count_textview)
        return view
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.show_chats_button -> listener!!.onChatsClick()
            R.id.show_calls_button -> listener!!.onCallsClick()
            R.id.show_contacts_button -> listener!!.onContactsClick()
            R.id.show_discover_button -> listener!!.onDiscoverClick()
            R.id.show_settings_button -> listener!!.onSettingsClick()
        }
    }

    fun setUnreadMessages(count: Int) {
        if (count > 0) {
            if (count > 99) {
                unreadCoutTextView?.text = "99"
                unreadCoutTextView?.visibility = View.VISIBLE
            }
            unreadCoutTextView?.text = count.toString()
            unreadCoutTextView?.visibility = View.VISIBLE
        } else {
            unreadCoutTextView?.visibility = View.GONE
        }

    }

    companion object {
        fun newInstance(): BottomBar = BottomBar()
    }

    fun setChatStateIcon(currentChatState: ChatListFragment.ChatListState) {
        when (currentChatState) {
            ChatListFragment.ChatListState.recent -> {
                chatsImage?.setImageDrawable(resources.getDrawable(R.drawable.ic_chats_list_new))
            }
            ChatListFragment.ChatListState.unread -> {
                chatsImage?.setImageDrawable(resources.getDrawable(R.drawable.ic_chats_list_unread_new))
            }
            ChatListFragment.ChatListState.archived -> {
                chatsImage?.setImageDrawable(resources.getDrawable(R.drawable.ic_chats_list_new))
            }
        }
    }

    fun setColoredButton(activeFragmentType: MainActivity.ActiveFragmentType) {
        val colorBasic = Color.parseColor("#757575")
        val colorActive = ColorManager.getInstance().accountPainter.defaultMainColor
        callsButton?.setColorFilter(colorBasic)
        chatsImage?.setColorFilter(colorBasic)
        contactsButton?.setColorFilter(colorBasic)
        discoverButton?.setColorFilter(colorBasic)
        settingsButton?.setColorFilter(colorBasic)
        when (activeFragmentType) {
            MainActivity.ActiveFragmentType.CHATS -> chatsImage?.setColorFilter(colorActive)
            MainActivity.ActiveFragmentType.CALLS -> callsButton?.setColorFilter(colorActive)
            MainActivity.ActiveFragmentType.CONTACTS -> contactsButton?.setColorFilter(colorActive)
            MainActivity.ActiveFragmentType.DISCOVER -> discoverButton?.setColorFilter(colorActive)
            MainActivity.ActiveFragmentType.SETTINGS -> settingsButton?.setColorFilter(colorActive)
        }
    }
}
