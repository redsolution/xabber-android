package com.xabber.android.ui.fragment.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.extension.groupchat.rights.GroupMemberRightsListener
import com.xabber.android.data.extension.groupchat.rights.GroupchatMemberRightsReplyIQ
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupMember
import com.xabber.android.data.message.chat.groupchat.GroupMemberManager
import com.xabber.android.ui.activity.GroupchatMemberActivity
import com.xabber.android.ui.adapter.groups.rights.RightsFormListAdapter
import com.xabber.android.ui.color.ColorManager
import org.jivesoftware.smackx.xdata.FormField
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupMemberRightsFragment(val groupMember: GroupMember, val groupchat: GroupChat)
    : Fragment(), GroupMemberRightsListener, RightsFormListAdapter.Listener {

    var recyclerView: RecyclerView? = null
    var adapter: RightsFormListAdapter? = null

    var oldDataForm: DataForm? = null

    private val newFields = mutableMapOf<String, FormField>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.simple_nested_scroll_with_recycler_view, container, false)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView?.layoutManager = LinearLayoutManager(context).apply {
            orientation = LinearLayoutManager.VERTICAL
        }

        GroupMemberManager.getInstance().requestGroupchatMemberRightsForm(groupchat.account,
                groupchat.contactJid, groupMember)

        return view
    }

    override fun onResume() {
        Application.getInstance().addUIListener(GroupMemberRightsListener::class.java, this)
        super.onResume()
    }

    override fun onPause() {
        Application.getInstance().removeUIListener(GroupMemberRightsListener::class.java, this)
        super.onPause()
    }

    private fun setupRecyclerViewWithDataForm(dataForm: DataForm) {
        adapter = RightsFormListAdapter(dataForm,
                ColorManager.getInstance().accountPainter.getAccountSendButtonColor(groupchat.account),
                fragmentManager!!, this)

        recyclerView?.adapter = adapter
        adapter?.notifyDataSetChanged()

    }

    override fun onSuccessfullyChanges(groupchat: GroupChat) {
        if (isTHisGroup(groupchat)){
            newFields.clear()
            GroupMemberManager.getInstance().requestGroupchatMemberRightsForm(groupchat.account,
                    groupchat.contactJid, groupMember)
            notifyActivityAboutNewFieldSizeChanged()
        }
    }

    override fun onError(groupchat: GroupChat) {
        if (isTHisGroup(groupchat))
            Toast.makeText(context, getString(R.string.groupchat_error), Toast.LENGTH_SHORT).show()
    }

    override fun onGroupchatMemberRightsFormReceived(groupchat: GroupChat, iq: GroupchatMemberRightsReplyIQ) {
        if (isTHisGroup(groupchat))
            for (field in iq.dataFrom!!.fields)
                if (field.variable == GroupchatMemberRightsReplyIQ.FIELD_USER_ID
                        && groupMember.id == field.values[0]) {
                    oldDataForm = iq.dataFrom
                    Application.getInstance().runOnUiThread {
                        setupRecyclerViewWithDataForm(iq.dataFrom!!)
                    }
                    break
                }
    }

    private fun isTHisGroup(groupchat: GroupChat) = this.groupchat == groupchat

    override fun onOptionPicked(field: FormField, option: FormField.Option?, isChecked: Boolean) {

        if (newFields.containsKey(field.variable)) newFields.remove(field.variable)

        if (checkPickIsNew(field, option, isChecked)) {

            val newFormField = FormField(field.variable)
            newFormField.type = FormField.Type.list_single
            newFormField.label = field.label
            if (option != null)
                newFormField.addValue(option.value)

            newFields[field.variable] = newFormField
        }

        notifyActivityAboutNewFieldSizeChanged()
    }

    private fun checkPickIsNew(newField: FormField, newOption: FormField.Option?, isChecked: Boolean): Boolean {
        for (oldField in oldDataForm!!.fields) {
            if (oldField.variable == newField.variable) {
                if (newOption != null) {
                    if (oldField.values != null && oldField.values.size != 0) {
                        if (oldField.values[0] as String == newOption.value) return false
                    } else return true
                } else {
                    return oldField.values != null && oldField.values.size != 0 && !isChecked
                }
            }
        }
        return true
    }

    private fun notifyActivityAboutNewFieldSizeChanged() {
        if (activity != null && activity is GroupchatMemberActivity)
            (activity as GroupchatMemberActivity).onNewMemberRightsFormFieldChanged(newFields.size)
    }

    private fun createNewDataFrom(): DataForm {
        val newDataForm = DataForm(DataForm.Type.submit).apply {
            title = oldDataForm?.title
            instructions = oldDataForm?.instructions
        }

        for (oldFormField in oldDataForm!!.fields) {

            if (oldFormField.variable == null) continue

            val formFieldToBeAdded = FormField(oldFormField.variable).apply {
                type = oldFormField.type
                label = oldFormField.label
            }

            if (newFields.containsKey(formFieldToBeAdded.variable)) {
                if (!newFields[formFieldToBeAdded.variable]!!.values.isNullOrEmpty()) {
                    formFieldToBeAdded.addValue(newFields[formFieldToBeAdded.variable]!!.values[0])
                }
            } else if (oldFormField.values != null && oldFormField.values.size > 0)
                formFieldToBeAdded.addValue(oldFormField.values[0])

            newDataForm.addField(formFieldToBeAdded)
        }

        return newDataForm
    }

    fun sendSaveRequest() = GroupMemberManager.getInstance()
            .requestGroupchatMemberRightsChange(groupchat, createNewDataFrom())

    companion object {
        const val TAG = "com.xabber.android.ui.fragment.GroupchatMemberInfoFragment"
    }

}