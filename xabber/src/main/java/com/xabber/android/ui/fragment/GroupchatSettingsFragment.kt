package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.xabber.android.ui.color.ColorManager
import org.jivesoftware.smackx.xdata.FormField
import org.jivesoftware.smackx.xdata.packet.DataForm
import java.util.*

class GroupchatSettingsFragment(private val groupchat: GroupChat) : CircleEditorFragment(),
        GroupSettingsResultsListener, GroupSettingsFormListAdapter.Listener {

    init {
        account = groupchat.account
        contactJid = groupchat.contactJid
    }

    private lateinit var recyclerView: RecyclerView
    private var dataForm: DataForm? = null
    private val newFields = mutableMapOf<String, FormField>()
    private var contactCircles = ArrayList<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.groupchat_update_settings_fragment, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context).apply {
            orientation = LinearLayoutManager.VERTICAL
        }

        view.findViewById<TextView>(R.id.tvCircles).setTextColor(ColorManager.getInstance().accountPainter.getAccountSendButtonColor(account))

        return view
    }

    override fun onResume() {
        super.onResume()
        Application.getInstance().addUIListener(GroupSettingsResultsListener::class.java, this)
        sendRequestGroupSettingsDataForm()
        contactCircles = ArrayList(RosterManager.getInstance().getCircles(groupchat.account, groupchat.contactJid))
        if (circles != null) updateCircles()
        contactCircles = ArrayList(RosterManager.getInstance().getCircles(account, contactJid))
    }

    override fun onPause() {
        Application.getInstance().removeUIListener(GroupSettingsResultsListener::class.java, this)
        super.onPause()
    }

    override fun getAccount() = groupchat.account
    override fun getContactJid() = groupchat.contactJid

    private fun sendRequestGroupSettingsDataForm() {
        GroupchatManager.getInstance().requestGroupSettingsForm(groupchat)
        (activity as GroupchatUpdateSettingsActivity).showProgressBar(true)
    }

    fun saveChanges() {
        if (checkHasChangesInSettings()) sendSetNewSettingsRequest()
        if (checkIsCirclesChanged()) saveCircles()
    }

    private fun sendSetNewSettingsRequest() {
        GroupchatManager.getInstance().sendSetGroupSettingsRequest(groupchat, createNewDataForm())
        (activity as GroupchatUpdateSettingsActivity).showProgressBar(true)
    }

    private fun createNewDataForm(): DataForm {
        val newDataForm = DataForm(DataForm.Type.submit).apply {
            title = dataForm?.title
            instructions = dataForm?.instructions
        }

        for (oldFormField in dataForm!!.fields) {

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

    private fun updateViewWithDataForm(dataForm: DataForm) {
        val adapter = GroupSettingsFormListAdapter(dataForm,
                ColorManager.getInstance().accountPainter.getAccountSendButtonColor(account), this)
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    override fun onSingleOptionClicked(field: FormField, option: FormField.Option) {
        if (newFields.containsKey(field.variable)) newFields.remove(field.variable)

        if (checkIsPickedOptionNew(field, option)) {

            val formFieldToBeAdded = FormField(field.variable)

            formFieldToBeAdded.type = field.type
            formFieldToBeAdded.label = field.label
            formFieldToBeAdded.addValue(option.value)

            newFields[field.variable] = formFieldToBeAdded
        }

        (activity as GroupchatUpdateSettingsActivity).showToolbarButtons(checkHasChangesInSettings() || checkIsCirclesChanged())
    }

    override fun onSingleTextTextChanged(field: FormField, text: String) {
        if (newFields.containsKey(field.variable)) newFields.remove(field.variable)
        if (checkIsTextNew(field, text)) {
            val formFieldToBeAdded = FormField(field.variable)

            formFieldToBeAdded.type = field.type
            formFieldToBeAdded.label = field.label
            formFieldToBeAdded.addValue(text)

            newFields[field.variable] = formFieldToBeAdded
        }
        (activity as GroupchatUpdateSettingsActivity).showToolbarButtons(checkHasChangesInSettings() || checkIsCirclesChanged())
    }


    private fun checkIsPickedOptionNew(newField: FormField, newOption: FormField.Option?): Boolean {
        for (oldField in dataForm!!.fields) {
            if (oldField.variable == newField.variable) {
                return oldField.values[0] != newOption!!.value
            }
        }
        return true
    }

    private fun checkIsTextNew(newField: FormField, newText: String): Boolean {
        for (oldField in dataForm!!.fields) {
            if (oldField.variable == newField.variable) {
                return oldField.values[0] != newText
            }
        }
        return true
    }

    private fun checkIsCirclesChanged(): Boolean {
        val selectedCircles = selected
        contactCircles.sort()
        selectedCircles.sort()

        return contactCircles.size != selectedCircles.size
    }

    private fun checkHasChangesInSettings(): Boolean {
        for (field in dataForm!!.fields) {
            if (newFields.containsKey(field.variable)) return true
        }
        return false
    }

    private fun isThisGroup(groupchat: GroupChat) = groupchat == this.groupchat

    override fun onDataFormReceived(groupchat: GroupChat, dataForm: DataForm) {
        if (!isThisGroup(groupchat)) return
        this.dataForm = dataForm
        newFields.clear()
        Application.getInstance().runOnUiThread {
            updateViewWithDataForm(dataForm)
            (activity as GroupchatUpdateSettingsActivity).showProgressBar(false)
        }
    }

    override fun onGroupSettingsSuccessfullyChanged(groupchat: GroupChat) {
        if (!isThisGroup(groupchat)) return
        GroupchatManager.getInstance().requestGroupSettingsForm(groupchat)
        Application.getInstance().runOnUiThread {
            Toast.makeText(context, R.string.groupchat_permissions_successfully_changed,
                    Toast.LENGTH_SHORT).show()
            newFields.clear()
            (activity as GroupchatUpdateSettingsActivity).showProgressBar(false)
            (activity as GroupchatUpdateSettingsActivity).showToolbarButtons(false)
        }
    }

    override fun onErrorAtDataFormRequesting(groupchat: GroupChat) {
        if (!isThisGroup(groupchat)) return
        Application.getInstance().runOnUiThread {
            Toast.makeText(context, getString(R.string.groupchat_failed_to_retrieve_settings_data_form),
                    Toast.LENGTH_SHORT).show()
            (activity as GroupchatUpdateSettingsActivity).showToolbarButtons(false)
        }
    }

    override fun onErrorAtSettingsSetting(groupchat: GroupChat) {
        if (!isThisGroup(groupchat)) return
        Application.getInstance().runOnUiThread {
            Toast.makeText(context, getString(R.string.groupchat_failed_to_change_groupchat_settings),
                    Toast.LENGTH_SHORT).show()
            (activity as GroupchatUpdateSettingsActivity).showToolbarButtons(false)
        }
    }

    override fun onCircleAdded() {
        super.onCircleAdded()
        (activity as GroupchatUpdateSettingsActivity).showToolbarButtons(checkHasChangesInSettings() || checkIsCirclesChanged())
    }

    override fun onCircleToggled() {
        super.onCircleToggled()
        (activity as GroupchatUpdateSettingsActivity).showToolbarButtons(checkHasChangesInSettings() || checkIsCirclesChanged())
    }

}