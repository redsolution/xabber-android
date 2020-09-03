package com.xabber.android.ui.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.listeners.OnAccountChangedListener
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupchatMember
import com.xabber.android.data.message.chat.groupchat.GroupchatMemberManager
import com.xabber.android.data.message.chat.groupchat.GroupchatPrivacyType
import com.xabber.android.ui.color.AccountPainter
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.dialog.SnoozeDialog
import com.xabber.android.ui.fragment.GroupchatMemberInfoFragment
import com.xabber.android.ui.helper.BlurTransformation
import com.xabber.android.ui.widget.ContactBarAutoSizingLayout

class GroupchatMemberActivity: ManagedActivity(), OnAccountChangedListener, View.OnClickListener,
        SnoozeDialog.OnSnoozeListener {

    private val LOG_TAG = this.javaClass.simpleName

    private var accountJid: AccountJid? = null
    private var groupchatJid: ContactJid? = null
    private var groupchatMember: GroupchatMember? = null
    private var groupchat: GroupChat? = null

    private var toolbar: Toolbar? = null
    private var contactTitleView: View? = null
    private var collapsingToolbar: CollapsingToolbarLayout? = null
    private var appBarLayout: AppBarLayout? = null
    private var firstButton: ImageButton? = null
    private var secondButton: ImageButton? = null
    private var thirdButton: ImageButton? = null
    private var fourthButton: ImageButton? = null
    private var accountMainColor = 0
    private var coloredBlockText = false
    private var firstButtonText: TextView? = null
    private var secondButtonText: TextView? = null
    private var thirdButtonText: TextView? = null
    private var fourthButtonText: TextView? = null
    private var contactBarLayout: ContactBarAutoSizingLayout? = null

    var orientation = 0
    private val blocked = false
    private var isGroupchat = false

    companion object{
        val GROUPCHAT_MEMBER_ID = "com.xabber.android.ui.activity.GroupchatMemberActivity.GROUPCHAT_MEMBER_ID"
        val GROUPCHAT_JID = "com.xabber.android.ui.activity.GroupchatMemberActivity.GROUPCHAT_JID"
        val ACCOUNT_JID = "com.xabber.android.ui.activity.GroupchatMemberActivity.ACCOUNT_JID"
        fun createIntentForGroupchatAndMemberId(context: Context, groupchatMemberId: String, groupchat: GroupChat) : Intent{
            val intent = Intent(context, GroupchatMemberActivity::class.java)
            intent.putExtra(GROUPCHAT_MEMBER_ID, groupchatMemberId)
            intent.putExtra(GROUPCHAT_JID, groupchat.user.toString())
            intent.putExtra(ACCOUNT_JID, groupchat.account.toString())
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupchatMember = GroupchatMemberManager.getInstance().getGroupchatUser(intent.getStringExtra(GROUPCHAT_MEMBER_ID))
        accountJid = AccountJid.from(intent.getStringExtra(ACCOUNT_JID))
        groupchatJid = ContactJid.from(intent.getStringExtra(GROUPCHAT_JID))
        groupchat = ChatManager.getInstance().getChat(accountJid, groupchatJid) as GroupChat

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val win = window
            val winParams = win.attributes
            winParams.flags = winParams.flags or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            win.attributes = winParams
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val win = window
            val winParams = win.attributes
            winParams.flags = winParams.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS.inv()
            win.attributes = winParams
            win.statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_groupchat_member_info)

        //TODO: saved state restoring

        toolbar = findViewById(R.id.toolbar_default)
        toolbar?.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)
        toolbar?.setNavigationOnClickListener { finish() }

        contactBarLayout = findViewById(R.id.contact_bar_layout)
        contactBarLayout?.setForGroupchatMember()

        firstButton = findViewById(R.id.first_button)
        firstButtonText = findViewById(R.id.first_button_text)
        firstButton?.setOnClickListener(this)

        secondButton = findViewById(R.id.second_button)
        secondButtonText = findViewById(R.id.second_button_text)
        secondButton?.setOnClickListener(this)

        thirdButton = findViewById(R.id.fourth_button)
        thirdButtonText = findViewById(R.id.fourth_button_text)
        thirdButton?.setOnClickListener(this)

        fourthButton = findViewById(R.id.third_button)
        fourthButtonText = findViewById(R.id.third_button_text)
        fourthButton?.setOnClickListener(this)

        val colorLevel = AccountPainter.getAccountColorLevel(accountJid)
        accountMainColor = ColorManager.getInstance().accountPainter.getAccountMainColor(accountJid)
        val accountDarkColor = ColorManager.getInstance().accountPainter.getAccountDarkColor(accountJid)
        coloredBlockText = colorLevel == 0 || colorLevel == 1 || colorLevel == 3

        //TODO is it necessary?
        contactTitleView = findViewById(R.id.contact_title_expanded)

        //TODO check for block status was here

        orientation = resources.configuration.orientation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (this.isInMultiWindowMode) {
                orientation = Configuration.ORIENTATION_PORTRAIT
            }
        }

        setContactBar(accountMainColor, orientation)
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            orientationPortrait()
        } else {
            orientationLandscape()
        }

        setupNameBlock()
        setupAvatar()

        supportFragmentManager.beginTransaction().add(R.id.scrollable_container, GroupchatMemberInfoFragment(groupchatMember!!)).commit()

    }

    private fun setupNameBlock(){
        findViewById<TextView>(R.id.name).text = (groupchatMember?.bestName + " " + groupchatMember?.badge)
        if (groupchat!!.privacyType!! != GroupchatPrivacyType.INCOGNITO)
            findViewById<TextView>(R.id.address_text).text = (groupchatMember!!.jid)
        if (groupchatMember!!.statusMode != null)
            findViewById<ImageView>(R.id.ivStatusGroupchat).setImageLevel(groupchatMember!!.statusMode.statusLevel)
    }

    private fun setupAvatar(){
        var backgroundSource = AvatarManager.getInstance().getGroupchatMemberAvatar(groupchatMember, accountJid)
        if (backgroundSource == null) backgroundSource = resources.getDrawable(R.drawable.about_backdrop)
        Glide.with(this)
                .load(backgroundSource)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(MultiTransformation(CenterCrop(), BlurTransformation(25, 8,  /*this,*/accountMainColor)))
                .into(findViewById(R.id.backgroundView))

        findViewById<ImageView>(R.id.ivAvatar).setImageDrawable(backgroundSource)
    }

    override fun onResume() {
        super.onResume()
        Application.getInstance().addUIListener(OnAccountChangedListener::class.java, this)
        //ContactTitleInflater.updateTitle(contactTitleView, this, bestContact, true)
        appBarResize()
    }

    override fun onPause() {
        super.onPause()
        Application.getInstance().removeUIListener(OnAccountChangedListener::class.java, this)
    }

    private fun appBarResize() {
        val avatar = findViewById<ImageView>(R.id.ivAvatar)
        val avatarQR = findViewById<ImageView>(R.id.ivAvatarQR)
        if (avatar.drawable == null) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                avatar.visibility = View.GONE
                avatarQR.visibility = View.GONE
            } else {
                avatarQR.visibility = View.VISIBLE
            }
        }
    }

    private fun orientationPortrait() {
        collapsingToolbar = findViewById<View>(R.id.collapsing_toolbar) as CollapsingToolbarLayout
        appBarLayout = findViewById<View>(R.id.appbar) as AppBarLayout
        appBarLayout?.addOnOffsetChangedListener(object : OnOffsetChangedListener {
            var isShow = true
            var scrollRange = -1
            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.totalScrollRange
                }
                if (scrollRange + verticalOffset == 0) {
                    //todo collapsingToolbar?.setTitle(bestContact!!.name)
                    contactTitleView!!.visibility = View.INVISIBLE
                    isShow = true
                } else if (isShow) {
                    collapsingToolbar?.setTitle(" ")
                    contactTitleView!!.visibility = View.VISIBLE
                    isShow = false
                }
            }
        })
        collapsingToolbar?.setContentScrimColor(accountMainColor)
    }

    private fun orientationLandscape() {
        val nameHolderView = findViewById<View>(R.id.name_holder) as LinearLayout
        toolbar!!.title = ""
        toolbar!!.setBackgroundColor(Color.TRANSPARENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val win = window
            win.statusBarColor = accountMainColor
        }
        if (toolbar!!.overflowIcon != null) toolbar!!.overflowIcon!!.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP)
        val scrollView = findViewById<NestedScrollView>(R.id.scroll_v_card)
        val ll = findViewById<LinearLayout>(R.id.scroll_v_card_child)
        nameHolderView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    nameHolderView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                } else nameHolderView.viewTreeObserver.removeGlobalOnLayoutListener(this)
                val topPadding = nameHolderView.height
                ll.setPadding(0, topPadding, 0, 0)
            }
        })
    }

    private fun setContactBar(color: Int, orientation: Int) {
        var notify = true

        //todo this?
//        if (groupchat != null) {
//            groupchat?.enableNotificationsIfNeed()
//
//            if (groupchat?.notifyAboutMessage() && !blocked) fourthButton!!.setImageDrawable(resources.getDrawable(R.drawable.ic_bell)) else {
//                notify = false
//                val notificationState: NotificationState = groupchat?.getNotificationState()
//                when (notificationState.mode) {
//                    NotificationMode.disabled -> fourthButton!!.setImageDrawable(resources.getDrawable(R.drawable.ic_snooze_forever))
//                    NotificationMode.snooze1d, NotificationMode.snooze2h, NotificationMode.snooze1h, NotificationMode.snooze15m -> if (blocked) fourthButton!!.setImageDrawable(resources.getDrawable(R.drawable.ic_snooze_forever)) else fourthButton!!.setImageDrawable(resources.getDrawable(R.drawable.ic_snooze))
//                    else -> if (blocked) fourthButton!!.setImageDrawable(resources.getDrawable(R.drawable.ic_snooze_forever)) else fourthButton!!.setImageDrawable(resources.getDrawable(R.drawable.ic_snooze))
//                }
//            }
//        }
        firstButton!!.setColorFilter(if (blocked) resources.getColor(R.color.grey_500) else color)
        secondButton!!.setColorFilter(if (blocked) resources.getColor(R.color.grey_500) else color)
        fourthButton!!.setColorFilter(if (blocked || !notify) resources.getColor(R.color.grey_500) else color)
        thirdButton!!.setColorFilter(resources.getColor(R.color.red_900))
        secondButton!!.isEnabled = !blocked
        fourthButton!!.isEnabled = !blocked
        if (isGroupchat) {
        } else {
            thirdButtonText!!.setText(if (blocked) R.string.contact_bar_unblock else R.string.contact_bar_block)
            thirdButtonText!!.setTextColor(resources.getColor(if (blocked || coloredBlockText) R.color.red_900 else if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) R.color.grey_600 else R.color.grey_400))
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            firstButtonText!!.visibility = View.GONE
            secondButtonText!!.visibility = View.GONE
            thirdButtonText!!.visibility = View.GONE
            fourthButtonText!!.visibility = View.GONE
        } else {
            firstButtonText!!.visibility = View.VISIBLE
            secondButtonText!!.visibility = View.VISIBLE
            thirdButtonText!!.visibility = View.VISIBLE
            fourthButtonText!!.visibility = View.VISIBLE
            contactBarLayout!!.redrawText()
        }
    }

    fun manageAvailableUsernameSpace() {
        if (this.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val view = findViewById<TextView>(R.id.action_add_contact)
            if (view != null) {
                val width: Int
                val bounds = Rect()
                val textPaint: Paint = view.paint
                textPaint.getTextBounds(view.text.toString(), 0, view.text.length, bounds)
                width = bounds.width()
                val nameL = findViewById<LinearLayout>(R.id.name_layout)
                (nameL.layoutParams as LinearLayout.LayoutParams)
                        .setMargins(0, 0, width + 100, 0)
            }
        }
    }

    fun getActionBarSize(): Int {
        val styledAttributes = theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val actionBarSize = styledAttributes.getDimension(0, 0f).toInt()
        styledAttributes.recycle()
        return actionBarSize
    }

    override fun onAccountsChanged(accounts: MutableCollection<AccountJid>?) {
        TODO("Not yet implemented")
    }

    override fun onClick(v: View?) {
        TODO("Not yet implemented")
    }

    override fun onSnoozed() {
        TODO("Not yet implemented")
    }

}