package com.xabber.android.ui.fragment

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
import com.xabber.android.data.extension.groupchat.restrictions.GroupDefaultRestrictionsListener
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupchatManager
import com.xabber.android.ui.activity.GroupDefaultRestrictionsActivity
import com.xabber.android.ui.adapter.groups.rights.RightsFormListAdapter
import com.xabber.android.ui.color.ColorManager
import org.jivesoftware.smackx.xdata.FormField
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupDefaultRestrictionsFragment(private val groupchat: GroupChat): Fragment(),
        RightsFormListAdapter.Listener, GroupDefaultRestrictionsListener {

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

        GroupchatManager.getInstance().requestGroupDefaultRestrictionsDataForm(groupchat)
        if (activity != null && activity is GroupDefaultRestrictionsActivity)
            (activity as GroupDefaultRestrictionsActivity).showProgressBar(true)

        return view
    }

    override fun onResume() {
        Application.getInstance().addUIListener(GroupDefaultRestrictionsListener::class.java, this)
        super.onResume()
    }

    override fun onPause() {
        Application.getInstance().removeUIListener(GroupDefaultRestrictionsListener::class.java, this)
        super.onPause()
    }

    private fun setupRecyclerViewWithDataForm(dataForm: DataForm) {
        adapter = RightsFormListAdapter(dataForm,
                ColorManager.getInstance().accountPainter.getAccountSendButtonColor(groupchat.account),
                fragmentManager!!, this)

        recyclerView?.adapter = adapter
        adapter?.notifyDataSetChanged()

    }

    override fun onSuccessful(groupchat: GroupChat) {
        newFields.clear()
        GroupchatManager.getInstance().requestGroupDefaultRestrictionsDataForm(groupchat)
        notifyActivityAboutNewFieldSizeChanged()
        if (activity != null && activity is GroupDefaultRestrictionsActivity)
            (activity as GroupDefaultRestrictionsActivity).showProgressBar(false)
    }

    override fun onError(groupchat: GroupChat) {
        Toast.makeText(context, getString(R.string.groupchat_error), Toast.LENGTH_SHORT).show()
        if (activity != null && activity is GroupDefaultRestrictionsActivity)
            (activity as GroupDefaultRestrictionsActivity).showProgressBar(false)
    }

    override fun onDataFormReceived(groupchat: GroupChat, dataForm: DataForm) {
        Application.getInstance().runOnUiThread {

            if (groupchat == this.groupchat) {
                oldDataForm = dataForm
                setupRecyclerViewWithDataForm(dataForm)
            }

            if (activity != null && activity is GroupDefaultRestrictionsActivity)
                (activity as GroupDefaultRestrictionsActivity).showProgressBar(false)
        }
    }

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
        Application.getInstance().runOnUiThread {
            if (activity != null && activity is GroupDefaultRestrictionsActivity)
                (activity as GroupDefaultRestrictionsActivity).showToolbarMenu(newFields.isNotEmpty())
        }
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

    fun sendSaveRequest() = GroupchatManager.getInstance()
            .requestSetGroupDefaultRestrictions(groupchat, createNewDataFrom())

    companion object {
        const val TAG = "com.xabber.android.ui.fragment.GroupDefaultRestrictionsFragment"
    }

}