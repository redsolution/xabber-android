package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groupchat.OnGroupchatRequestListener
import com.xabber.android.data.extension.groupchat.rights.GroupchatMemberRightsReplyIQ
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupchatMember
import com.xabber.android.data.message.chat.groupchat.GroupchatMemberManager
import com.xabber.android.ui.color.ColorManager
import org.jivesoftware.smackx.xdata.FormField

class GroupchatMemberInfoFragment(val groupchatMember: GroupchatMember, val groupchat: GroupChat)
    : Fragment(), OnGroupchatRequestListener {

    /** Restrictions **/
    var restHeaderTv: TextView? = null

    var restSendMessagesRoot: RelativeLayout? = null
    var restSendMessagesCb: AppCompatCheckBox? = null
    var restSendMessagesTimeTv: TextView? = null

    var restReadMessagesRoot: RelativeLayout? = null
    var restReadMessagesCb: AppCompatCheckBox? = null
    var restReadMessagesTimeTv: TextView? = null

    var restSendInvitationsRoot: RelativeLayout? = null
    var restSendInvitationsCb: AppCompatCheckBox? = null
    var restSendInvitationsTimeTv: TextView? = null

    var restSendAudioRoot: RelativeLayout? = null
    var restSendAudioCb: AppCompatCheckBox? = null
    var restSendAudioTimeTv: TextView? = null

    var restSendImagesRoot: RelativeLayout? = null
    var restSendImagesCb: AppCompatCheckBox? = null
    var restSendImagesTimeTv: TextView? = null

    /** Permissions */
    var permHeaderTv: TextView? = null

    var permOwnerRoot: RelativeLayout? = null
    var permOwnerCb: AppCompatCheckBox? = null
    var permOwnerTimeTv: TextView? = null

    var permRestrictMembersRoot: RelativeLayout? = null
    var permRestrictMembersCb: AppCompatCheckBox? = null
    var permRestrictMembersTimeTv: TextView? = null

    var permBlockMembersRoot: RelativeLayout? = null
    var permBlockMembersCb: AppCompatCheckBox? = null
    var permBlockMembersTimeTv: TextView? = null

    var permAdministratorRoot: RelativeLayout? = null
    var permAdministratorCb: AppCompatCheckBox? = null
    var permAdministratorTimeTv: TextView? = null

    var permChangeBadgesRoot: RelativeLayout? = null
    var permChangeBadgesCb: AppCompatCheckBox? = null
    var permChangeBadgesTimeTv: TextView? = null

    var permChangeNicknamesRoot: RelativeLayout? = null
    var permChangeNicknamesCb: AppCompatCheckBox? = null
    var permChangeNicknamesTimeTv: TextView? = null

    var permDeleteMessagesRoot: RelativeLayout? = null
    var permDeleteMessagesCb: AppCompatCheckBox? = null
    var permDeleteMessagesTimeTv: TextView? = null

    var iq: GroupchatMemberRightsReplyIQ? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.groupchat_member_edit_fragment, container, false)

        GroupchatMemberManager.getInstance().requestGroupchatMemberRightsForm(groupchat.account, groupchat.contactJid, groupchatMember)

        initRestrictionsVars(view)
        initPermissionsVars(view)

        Application.getInstance().addUIListener(OnGroupchatRequestListener::class.java, this)

        return view
    }

    override fun onPause() {
        Application.getInstance().removeUIListener(OnGroupchatRequestListener::class.java, this)
        super.onPause()
    }

    private fun initPermissionsVars(view: View){
        permHeaderTv = view.findViewById(R.id.groupchat_member_edit_permissions_header_tv)

        permOwnerRoot = view.findViewById(R.id.groupchat_member_edit_permissions_owner_rl)
        permOwnerCb = view.findViewById(R.id.groupchat_member_edit_permissions_owner_checkbox)
        permOwnerTimeTv = view.findViewById(R.id.groupchat_member_edit_permissions_owner_timer_tv)

        permRestrictMembersRoot = view.findViewById(R.id.groupchat_member_edit_permissions_to_restrict_members_rl)
        permRestrictMembersCb = view.findViewById(R.id.groupchat_member_edit_permissions_to_restrict_members_checkbox)
        permRestrictMembersTimeTv = view.findViewById(R.id.groupchat_member_edit_permissions_to_restrict_members_timer_tv)

        permBlockMembersRoot = view.findViewById(R.id.groupchat_member_edit_permissions_to_block_members_rl)
        permBlockMembersCb = view.findViewById(R.id.groupchat_member_edit_permissions_to_block_members_checkbox)
        permBlockMembersTimeTv = view.findViewById(R.id.groupchat_member_edit_permissions_to_block_members_timer_tv)

        permAdministratorRoot = view.findViewById(R.id.groupchat_member_edit_permissions_administrator_rl)
        permAdministratorCb = view.findViewById(R.id.groupchat_member_edit_permissions_administrator_checkbox)
        permAdministratorTimeTv = view.findViewById(R.id.groupchat_member_edit_permissions_administrator_timer_tv)

        permChangeBadgesRoot = view.findViewById(R.id.groupchat_member_edit_permissions_to_change_badges_rl)
        permChangeBadgesCb = view.findViewById(R.id.groupchat_member_edit_permissions_to_change_badges_checkbox)
        permChangeBadgesTimeTv = view.findViewById(R.id.groupchat_member_edit_permissions_to_change_badges_timer_tv)

        permChangeNicknamesRoot = view.findViewById(R.id.groupchat_member_edit_permissions_to_change_nicknames_rl)
        permChangeNicknamesCb = view.findViewById(R.id.groupchat_member_edit_permissions_to_change_nicknames_checkbox)
        permChangeNicknamesTimeTv = view.findViewById(R.id.groupchat_member_edit_permissions_to_change_nicknames_timer_tv)

        permDeleteMessagesRoot = view.findViewById(R.id.groupchat_member_edit_permissions_to_delete_messages_rl)
        permDeleteMessagesCb = view.findViewById(R.id.groupchat_member_edit_permissions_to_delete_messages_checkbox)
        permDeleteMessagesTimeTv = view.findViewById(R.id.groupchat_member_edit_permissions_to_delete_messages_timer_tv)
    }

    private fun initRestrictionsVars(view: View){
        restHeaderTv = view.findViewById(R.id.groupchat_member_edit_restrictions_header_tv)

        restSendMessagesRoot = view.findViewById(R.id.groupchat_member_edit_restrictions_send_messages_rl)
        restSendMessagesCb = view.findViewById(R.id.groupchat_member_edit_restrictions_send_messages_checkbox)
        restSendMessagesTimeTv = view.findViewById(R.id.groupchat_member_edit_restrictions_send_messages_timer_tv)

        restReadMessagesRoot = view.findViewById(R.id.groupchat_member_edit_restrictions_read_messages_rl)
        restReadMessagesCb = view.findViewById(R.id.groupchat_member_edit_restrictions_read_messages_checkbox)
        restReadMessagesTimeTv = view.findViewById(R.id.groupchat_member_edit_restrictions_read_messages_timer_tv)

        restSendInvitationsRoot = view.findViewById(R.id.groupchat_member_edit_restrictions_send_invitations_rl)
        restSendInvitationsCb = view.findViewById(R.id.groupchat_member_edit_restrictions_send_invitations_checkbox)
        restSendInvitationsTimeTv = view.findViewById(R.id.groupchat_member_edit_restrictions_send_invitations_timer_tv)

        restSendAudioRoot = view.findViewById(R.id.groupchat_member_edit_restrictions_send_audio_rl)
        restSendAudioCb = view.findViewById(R.id.groupchat_member_edit_restrictions_send_audio_checkbox)
        restSendAudioTimeTv = view.findViewById(R.id.groupchat_member_edit_restrictions_send_audio_timer_tv)

        restSendImagesRoot = view.findViewById(R.id.groupchat_member_edit_restrictions_send_images_rl)
        restSendImagesCb = view.findViewById(R.id.groupchat_member_edit_restrictions_send_images_checkbox)
        restSendImagesTimeTv = view.findViewById(R.id.groupchat_member_edit_restrictions_send_images_timer_tv)
    }

    private fun setupPermissionsLayout(){
        permHeaderTv?.setTextColor(ColorManager.getInstance().accountPainter
                .getAccountSendButtonColor(groupchat.account))

        var hasEditablePermissions = false;

        if (iq != null){
            for (field in iq?.dataFrom!!.fields){
                when (field.variable){

                    GroupchatMemberRightsReplyIQ.FIELD_PERMISSIONS -> {
                        permHeaderTv?.visibility = View.VISIBLE
                        hasEditablePermissions = true
                    }

                    GroupchatMemberRightsReplyIQ.FIELD_OWNER -> {
                        permOwnerRoot?.visibility = View.VISIBLE
                        permOwnerCb?.setOnClickListener { }
                        hasEditablePermissions = true
                    }

                    GroupchatMemberRightsReplyIQ.FIELD_RESTRICT_PARTICIPANTS -> {
                        permRestrictMembersRoot?.visibility = View.VISIBLE
                        permRestrictMembersCb?.setOnClickListener { }
                        hasEditablePermissions = true
                    }

                    GroupchatMemberRightsReplyIQ.FIELD_BLOCK_PARTICIPANTS -> {
                        permBlockMembersRoot?.visibility = View.VISIBLE
                        permBlockMembersCb?.setOnClickListener { }
                        hasEditablePermissions = true
                    }

                    GroupchatMemberRightsReplyIQ.FIELD_ADMINISTRATOR -> {
                        permAdministratorRoot?.visibility = View.VISIBLE
                        permAdministratorCb?.setOnClickListener { }
                        hasEditablePermissions = true
                    }

                    GroupchatMemberRightsReplyIQ.FIELD_CHANGE_BADGES -> {
                        permChangeBadgesRoot?.visibility = View.VISIBLE
                        permChangeBadgesCb?.setOnClickListener { }
                        hasEditablePermissions = true
                    }

                    GroupchatMemberRightsReplyIQ.FIELD_CHANGE_NICKNAMES -> {
                        permChangeNicknamesRoot?.visibility = View.VISIBLE
                        permChangeNicknamesCb?.setOnClickListener { }
                        hasEditablePermissions = true
                    }

                    GroupchatMemberRightsReplyIQ.FIELD_DELETE_MESSAGES -> {
                        permDeleteMessagesRoot?.visibility = View.VISIBLE
                        permDeleteMessagesCb?.setOnClickListener { }
                        hasEditablePermissions = true
                    }

                }
            }
            if (hasEditablePermissions) permHeaderTv?.visibility = View.VISIBLE
            else permHeaderTv?.visibility = View.GONE
        }
    }

    private fun setupRestrictionsLayout(){
        restHeaderTv?.setTextColor(ColorManager.getInstance().accountPainter
                .getAccountSendButtonColor(groupchat.account))

        var hasEditableRestrictions = false

        if (iq != null){
            for (field in iq?.dataFrom!!.fields){
                when (field.variable){

                    GroupchatMemberRightsReplyIQ.FIELD_RESTRICTIONS -> {
                        restHeaderTv?.visibility = View.VISIBLE
                        hasEditableRestrictions = true
                    }

                    GroupchatMemberRightsReplyIQ.FIELD_SEND_MESSAGES -> {
                        restSendMessagesRoot?.visibility = View.VISIBLE
                        restSendMessagesCb?.setOnCheckedChangeListener { buttonView, isChecked ->
                            if (isChecked)
                                OptionPickerDialog(field).show(fragmentManager!!, "SUKATAG")

                        }
                        hasEditableRestrictions = true
                    }

                    GroupchatMemberRightsReplyIQ.FIELD_READ_MESSAGES -> {
                        restReadMessagesRoot?.visibility = View.VISIBLE
                        restReadMessagesCb?.setOnClickListener { }
                        hasEditableRestrictions = true
                    }

                    GroupchatMemberRightsReplyIQ.FIELD_SEND_INVITATIONS -> {
                        restSendInvitationsRoot?.visibility = View.VISIBLE
                        restSendInvitationsCb?.setOnClickListener { }
                        hasEditableRestrictions = true
                    }

                    GroupchatMemberRightsReplyIQ.FIELD_SEND_AUDIO -> {
                        restSendAudioRoot?.visibility = View.VISIBLE
                        restSendAudioCb?.setOnClickListener { }
                        hasEditableRestrictions = true
                    }

                    GroupchatMemberRightsReplyIQ.FIELD_SEND_IMAGES -> {
                        restSendImagesRoot?.visibility = View.VISIBLE
                        restSendImagesCb?.setOnClickListener { }
                        hasEditableRestrictions = true
                    }
                }
            }
            if (hasEditableRestrictions) restHeaderTv?.visibility = View.VISIBLE
            else restHeaderTv?.visibility = View.GONE
        }
    }

    private fun isChanged(): Boolean{

        //todo check is member setting changed

        return true
    }

    override fun onGroupchatBlocklistReceived(account: AccountJid?, groupchatJid: ContactJid?) { }

    override fun onGroupchatInvitesReceived(account: AccountJid?, groupchatJid: ContactJid?) { }

    override fun onGroupchatMembersReceived(account: AccountJid?, groupchatJid: ContactJid?) { }

    override fun onMeReceived(accountJid: AccountJid?, groupchatJid: ContactJid?) { }

    override fun onGroupchatMemberUpdated(accountJid: AccountJid?, groupchatJid: ContactJid?, groupchatMemberId: String?) { }

    override fun onGroupchatMemberRightsFormReceived(accountJid: AccountJid,
                                                     groupchatJid: ContactJid,
                                                     iq: GroupchatMemberRightsReplyIQ) {
        var isThisMember = false
        for (field in iq.dataFrom!!.fields)
            if (field.variable == GroupchatMemberRightsReplyIQ.FIELD_USER_ID && groupchatMember.id == field.values[0]){
                isThisMember = true
                break
            }

        if (isThisMember){
            this.iq = iq
            Application.getInstance().runOnUiThread {
                setupPermissionsLayout()
                setupRestrictionsLayout()
            }
        }
    }

    class OptionPickerDialog(private val formField: FormField): BottomSheetDialogFragment(){

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.groupchat_rights_options_picker_dialog_layout, container, true)
            for (option in formField.options){
                val optionView = layoutInflater.inflate(android.R.layout.simple_list_item_1, container, false)
                optionView.setOnClickListener{
                    LogManager.d(javaClass.simpleName, "picked ${option.value}")
                }

            }
            return view
        }

    }

}