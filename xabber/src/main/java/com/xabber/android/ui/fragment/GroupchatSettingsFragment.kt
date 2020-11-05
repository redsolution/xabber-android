package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.extension.groupchat.settings.GroupSettingsResultsListener
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupchatManager
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.activity.GroupchatUpdateSettingsActivity
import com.xabber.android.ui.adapter.groups.settings.GroupSettingsFormListAdapter
import org.jivesoftware.smackx.xdata.FormField
import org.jivesoftware.smackx.xdata.packet.DataForm
import java.util.*

class GroupchatSettingsFragment(private val groupchat: GroupChat): GroupEditorFragment(),
        GroupSettingsResultsListener, GroupSettingsFormListAdapter.Listener {

    private lateinit var recyclerView: RecyclerView
    private var dataForm: DataForm? = null
    private val newFields = mutableMapOf<String, FormField>()
    private var contactGroups = ArrayList<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.groupchat_update_settings_fragment, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context).apply {
            orientation = LinearLayoutManager.VERTICAL
        }

        return  view
    }

    override fun onResume() {
        super.onResume()
        Application.getInstance().addUIListener(GroupSettingsResultsListener::class.java, this)
        sendRequestGroupSettingsDataForm()

        contactGroups = ArrayList(RosterManager.getInstance().getGroups(groupchat.account, groupchat.contactJid))
    }

    override fun onPause() {
        Application.getInstance().removeUIListener(GroupSettingsResultsListener::class.java, this)
        super.onPause()
    }

    private fun sendRequestGroupSettingsDataForm(){
        GroupchatManager.getInstance().requestGroupSettingsForm(groupchat)
        //todo show progressbar
    }

    fun sendSetNewSettingsRequest(){
        GroupchatManager.getInstance().sendSetGroupSettingsRequest(groupchat, createNewDataForm())
        //todo show progressbar
    }

    private fun createNewDataForm(): DataForm{
        val newDataForm = DataForm(DataForm.Type.submit).apply {
            title = dataForm?.title
            instructions = dataForm?.instructions
        }

        for (oldFormField in dataForm!!.fields){

            if (oldFormField.variable == null) continue

            val formFieldToBeAdded = FormField(oldFormField.variable).apply {
                type = oldFormField.type
                label = oldFormField.label
            }

            if (newFields.containsKey(formFieldToBeAdded.variable)){
                if (!newFields[formFieldToBeAdded.variable]!!.values.isNullOrEmpty()){
                    formFieldToBeAdded.addValue(newFields[formFieldToBeAdded.variable]!!.values[0])
                }
            } else if (oldFormField.values != null && oldFormField.values.size > 0)
                formFieldToBeAdded.addValue(oldFormField.values[0])

            newDataForm.addField(formFieldToBeAdded)
        }

        return newDataForm
    }

    private fun updateViewWithDataForm(dataForm: DataForm){
        val adapter = GroupSettingsFormListAdapter(dataForm, this)
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    override fun onSingleOptionClicked(field: FormField, option: FormField.Option) {
        newFields.remove(field.variable)
        if (checkIsPickedOptionNew(field, option)){
            newFields[field.variable] = field
            newFields[field.variable]?.addValue(option.value)
        }
        (activity as GroupchatUpdateSettingsActivity).showToolbarButtons(checkIsPickedOptionNew(field, option))
    }

    override fun onSingleTextTextChanged(field: FormField, text: String) {
        newFields.remove(field.variable)
        if (checkIsTextNew(field, text)){
            newFields[field.variable] = field
            newFields[field.variable]!!.addValue(text)
        }
        (activity as GroupchatUpdateSettingsActivity).showToolbarButtons(checkIsTextNew(field, text))
    }


    private fun checkIsPickedOptionNew(newField: FormField, newOption: FormField.Option?): Boolean{
        for (oldField in dataForm!!.fields){
            if (oldField.variable == newField.variable){
                return oldField.values[0] != newOption!!.value
            }
        }
        return true
    }

    private fun checkIsTextNew(newField: FormField, newText: String) : Boolean{
        for (oldField in dataForm!!.fields){
            if (oldField.variable == newField.variable){
                return oldField.values[0] != newText
            }
        }
        return true
    }

    private fun isThisGroup(groupchat: GroupChat) = groupchat == this.groupchat

    override fun onDataFormReceived(groupchat: GroupChat, dataForm: DataForm) {
        if (!isThisGroup(groupchat)) return
        Application.getInstance().runOnUiThread { updateViewWithDataForm(dataForm) }
        this.dataForm = dataForm
        newFields.clear()
        //todo hideprogressbar
    }

    override fun onGroupSettingsSuccessfullyChanged(groupchat: GroupChat) {
        if (!isThisGroup(groupchat)) return
        GroupchatManager.getInstance().requestGroupSettingsForm(groupchat)
        Application.getInstance().runOnUiThread {
            Toast.makeText(context, R.string.groupchat_permissions_successfully_changed,
                    Toast.LENGTH_SHORT).show()
            //todo hideprogressbar
            newFields.clear()
            (activity as GroupchatUpdateSettingsActivity).showToolbarButtons(false)
        }
    }

    override fun onErrorAtDataFormRequesting(groupchat: GroupChat) {
        if (!isThisGroup(groupchat)) return
        Application.getInstance().runOnUiThread {
            Toast.makeText(context, getString(R.string.groupchat_failed_to_retrieve_settings_data_form),
                    Toast.LENGTH_SHORT).show()
            //todo hideprogressbar
        }
    }

    override fun onErrorAtSettingsSetting(groupchat: GroupChat) {
        if (!isThisGroup(groupchat)) return
        Application.getInstance().runOnUiThread {
            Toast.makeText(context, getString(R.string.groupchat_failed_to_change_groupchat_settings),
                    Toast.LENGTH_SHORT).show()
            //todo hideprogressbar
            (activity as GroupchatUpdateSettingsActivity).showToolbarButtons(false)
        }
    }

}