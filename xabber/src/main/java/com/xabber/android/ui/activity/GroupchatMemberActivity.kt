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
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.PopupMenu
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
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.extension.groupchat.OnGroupchatRequestListener
import com.xabber.android.data.extension.groupchat.rights.GroupchatMemberRightsReplyIQ
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupchatMember
import com.xabber.android.data.message.chat.groupchat.GroupchatMemberManager
import com.xabber.android.data.message.chat.groupchat.GroupchatPrivacyType
import com.xabber.android.ui.color.AccountPainter
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.fragment.GroupchatMemberInfoFragment
import com.xabber.android.ui.helper.BlurTransformation
import com.xabber.android.ui.widget.ContactBarAutoSizingLayout
import java.util.*

class GroupchatMemberActivity: ManagedActivity(), View.OnClickListener,
        PopupMenu.OnMenuItemClickListener, OnGroupchatRequestListener {

    private val LOG_TAG = this.javaClass.simpleName

    private var accountJid: AccountJid? = null
    private var groupchatJid: ContactJid? = null
    private var groupchatMember: GroupchatMember? = null
    private var groupchat: GroupChat? = null

    private var toolbar: Toolbar? = null
    private var contactTitleView: View? = null
    private var collapsingToolbar: CollapsingToolbarLayout? = null
    private var appBarLayout: AppBarLayout? = null

    private var accountMainColor = 0
    private var coloredBlockText = false

    var orientation = 0
    private val blocked = false

    companion object{

        const val GROUPCHAT_MEMBER_ID = "com.xabber.android.ui.activity.GroupchatMemberActivity.GROUPCHAT_MEMBER_ID"
        const val GROUPCHAT_JID = "com.xabber.android.ui.activity.GroupchatMemberActivity.GROUPCHAT_JID"
        const val ACCOUNT_JID = "com.xabber.android.ui.activity.GroupchatMemberActivity.ACCOUNT_JID"

        fun createIntentForGroupchatAndMemberId(context: Context, groupchatMemberId: String,
                                                groupchat: GroupChat) : Intent{
            val intent = Intent(context, GroupchatMemberActivity::class.java)
            intent.putExtra(GROUPCHAT_MEMBER_ID, groupchatMemberId)
            intent.putExtra(GROUPCHAT_JID, groupchat.contactJid.toString())
            intent.putExtra(ACCOUNT_JID, groupchat.account.toString())
            return intent
        }

    }

    override fun onGroupchatBlocklistReceived(account: AccountJid?, groupchatJid: ContactJid?) {}

    override fun onGroupchatInvitesReceived(account: AccountJid?, groupchatJid: ContactJid?) {}

    override fun onGroupchatMemberRightsFormReceived(accountJid: AccountJid, groupchatJid: ContactJid,
                                                     iq: GroupchatMemberRightsReplyIQ) {}

    override fun onGroupchatMembersReceived(account: AccountJid?, groupchatJid: ContactJid?) {
        if (account == groupchat?.account && groupchatJid == groupchat?.contactJid)
            setupNameBlock()
    }

    override fun onMeReceived(accountJid: AccountJid?, groupchatJid: ContactJid?) {}

    override fun onGroupchatMemberUpdated(accountJid: AccountJid, groupchatJid: ContactJid,
                                          groupchatMemberId: String) {
        if (accountJid == this.accountJid && groupchatJid == this.groupchatJid
                && groupchatMemberId == this.groupchatMember?.id){
            Application.getInstance().runOnUiThread {
                setupAvatar()
                setupNameBlock()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupchatMember = GroupchatMemberManager.getInstance()
                .getGroupchatMemberById(intent.getStringExtra(GROUPCHAT_MEMBER_ID))
        accountJid = AccountJid.from(intent.getStringExtra(ACCOUNT_JID)!!)
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
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val win = window
            val winParams = win.attributes
            winParams.flags = winParams.flags and WindowManager.LayoutParams
                    .FLAG_TRANSLUCENT_STATUS.inv()
            win.attributes = winParams
            win.statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_groupchat_member_info)

        //TODO: saved state restoring

        toolbar = findViewById(R.id.toolbar_default)
        toolbar?.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)
        toolbar?.setNavigationOnClickListener { finish() }

        val colorLevel = AccountPainter.getAccountColorLevel(accountJid)
        accountMainColor = ColorManager.getInstance().accountPainter.getAccountMainColor(accountJid)
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

        setupContactBar(accountMainColor, orientation)
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            orientationPortrait()
        } else {
            orientationLandscape()
        }

        setupNameBlock()
        setupAvatar()

        supportFragmentManager.beginTransaction()
                .add(R.id.scrollable_container,
                        GroupchatMemberInfoFragment(groupchatMember!!, groupchat!!),
                        GroupchatMemberInfoFragment.TAG)
                .commit()

    }

    private fun setupNameBlock(){
        val nameTv = findViewById<TextView>(R.id.name)
        nameTv.text = (groupchatMember?.bestName + " " + groupchatMember?.badge)
        nameTv.setOnClickListener {
            val adb = AlertDialog.Builder(this)
            adb.setTitle(getString(R.string.groupchat_member_nickname))

            val et = AppCompatEditText(this)
            et.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            et.hint = groupchatMember?.nickname
            adb.setView(et, 56, 0,56,0)

            adb.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            adb.setPositiveButton(R.string.groupchat_set_member_nickname) { _, _ -> GroupchatMemberManager.getInstance()
                    .sendSetMemberNicknameIqRequest(groupchat, groupchatMember, et.text.toString())}
            adb.show()
        }
        if (groupchat!!.privacyType!! != GroupchatPrivacyType.INCOGNITO)
            findViewById<TextView>(R.id.address_text).text = (groupchatMember!!.jid)

        findViewById<TextView>(R.id.groupchat_member_title).text = getString(R.string.groupchat_member_of_group_name,
                groupchatMember?.role?.capitalize(Locale.getDefault()),
                groupchat?.privacyType?.getLocalizedString()?.decapitalize(Locale.getDefault()),
                groupchat?.name)
    }

    private fun setupAvatar(){
        var backgroundSource = AvatarManager.getInstance()
                .getGroupchatMemberAvatar(groupchatMember, accountJid)
        if (backgroundSource == null) backgroundSource = resources.getDrawable(R.drawable.about_backdrop)
        Glide.with(this)
                .load(backgroundSource)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(MultiTransformation(CenterCrop(), BlurTransformation(25, 8,  /*this,*/accountMainColor)))
                .into(findViewById(R.id.backgroundView))

        val avatarIv = findViewById<ImageView>(R.id.ivAvatar)
        avatarIv.setImageDrawable(backgroundSource)
        avatarIv.setOnClickListener {
            val popupMenu = PopupMenu(this, avatarIv)
            popupMenu.menuInflater.inflate(R.menu.groupchat_member_edit_change_avatar, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener(this)
            popupMenu.show()
        }

    }

    override fun onResume() {
        super.onResume()
        //ContactTitleInflater.updateTitle(contactTitleView, this, bestContact, true)
        Application.getInstance().addUIListener(OnGroupchatRequestListener::class.java,  this)
        GroupchatMemberManager.getInstance().requestGroupchatMemberInfo(groupchat, groupchatMember?.id)
        appBarResize()
    }

    override fun onPause() {
        super.onPause()
        Application.getInstance().removeUIListener(OnGroupchatRequestListener::class.java, this)
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
        if (toolbar!!.overflowIcon != null) toolbar!!.overflowIcon!!.setColorFilter(Color.BLACK,
                PorterDuff.Mode.SRC_ATOP)
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

    private fun setupDirectChatButtonLayout(color: Int, orientation: Int){
        val imageButton = findViewById<ImageButton>(R.id.first_button)
        val textView = findViewById<TextView>(R.id.first_button_text)

        imageButton.setOnClickListener {
            //todo: direct chat opening
        }

        imageButton.setColorFilter(if (blocked) resources.getColor(R.color.grey_500) else color)

        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            textView.visibility = View.GONE
        else textView.visibility = View.VISIBLE
    }

    private fun setupMessagesButtonLayout(color: Int, orientation: Int){
        val imageButton = findViewById<ImageButton>(R.id.second_button)
        val textView = findViewById<TextView>(R.id.second_button_text)

        imageButton.isEnabled = !blocked

        imageButton.setColorFilter(color)

        imageButton.setOnClickListener{
            //todo this
        }

        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            textView.visibility = View.GONE
        else textView.visibility = View.VISIBLE
    }

    private fun setupSetBadgeLayout(color: Int, orientation: Int){
        val imageButton = findViewById<ImageButton>(R.id.third_button)
        val textView = findViewById<TextView>(R.id.third_button_text)

        imageButton?.setOnClickListener{
            val adb = AlertDialog.Builder(this)
            adb.setTitle(groupchatMember?.nickname + " " + getString(R.string.groupchat_member_badge).decapitalize())

            val et = AppCompatEditText(this)
            et.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            et.isSingleLine = true
            et.hint = groupchatMember?.badge
            adb.setView(et, 64, 0, 64, 0)

            adb.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            adb.setPositiveButton(R.string.groupchat_set_member_badge) { _, _ -> GroupchatMemberManager.getInstance()
                    .sendSetMemberBadgeIqRequest(groupchat, groupchatMember, et.text.toString())}
            adb.show()
        }

        imageButton!!.setColorFilter(if (blocked) resources.getColor(R.color.grey_500) else color)

        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            textView!!.visibility = View.GONE
        else textView!!.visibility = View.VISIBLE
    }

    private fun setupKickBlockButtonLayout(color: Int, orientation: Int){
        val imageButton = findViewById<ImageButton>(R.id.fourth_button)
        val textView = findViewById<TextView>(R.id.fourth_button_text)

        imageButton.setOnClickListener{
            //todo this
        }

        textView.setText(if (blocked) R.string.contact_bar_unblock else R.string.contact_bar_block)
        textView.setTextColor(resources.getColor(
                if (blocked || coloredBlockText) R.color.red_900
                else if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                    R.color.grey_600
                else R.color.grey_400))

        imageButton.setColorFilter(resources.getColor(R.color.red_900))
        imageButton.isEnabled = !blocked

        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            textView.visibility = View.GONE
        else textView.visibility = View.VISIBLE
    }

    private fun setupContactBar(color: Int, orientation: Int) {
        setupDirectChatButtonLayout(color, orientation)
        setupMessagesButtonLayout(color, orientation)
        setupSetBadgeLayout(color, orientation)
        setupKickBlockButtonLayout(color, orientation)

        val contactBarLayout =
                findViewById<ContactBarAutoSizingLayout>(R.id.contact_bar_layout)

        contactBarLayout?.setForGroupchatMember()

        if (orientation == Configuration.ORIENTATION_PORTRAIT) contactBarLayout!!.redrawText()
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
        val styledAttributes =
                theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))

        val actionBarSize = styledAttributes.getDimension(0, 0f).toInt()
        styledAttributes.recycle()
        return actionBarSize
    }

    override fun onClick(v: View?) {
        TODO("Not yet implemented")
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        TODO("Not yet implemented")
    }

    private fun showToolbarMenu(isVisible: Boolean){
        toolbar?.menu?.clear()
        if (isVisible){
            toolbar?.setNavigationIcon(R.drawable.ic_clear_white_24dp)

            toolbar?.inflateMenu(R.menu.update_groupchat_member)
            toolbar?.setOnMenuItemClickListener {
                (supportFragmentManager.findFragmentByTag(GroupchatMemberInfoFragment.TAG)
                        as GroupchatMemberInfoFragment).sendSaveRequest()
                true
            }
        } else {
            toolbar?.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)
            toolbar?.setNavigationOnClickListener { finish() }
        }
    }

    fun onNewMemberRightsFormFieldChanged(count: Int) = showToolbarMenu(count > 0)

}