package com.xabber.android.ui.fragment.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.extension.groups.GroupsManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.ui.OnGroupStatusResultListener
import com.xabber.android.ui.activity.GroupStatusActivity
import com.xabber.android.ui.adapter.groups.GroupStatusAdapter
import org.jivesoftware.smackx.xdata.FormField
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupStatusFragment(val groupchat: GroupChat) : Fragment(), OnGroupStatusResultListener,
    GroupStatusAdapter.Listener {

    private lateinit var recyclerView: RecyclerView

    private lateinit var dataForm: DataForm

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(
            R.layout.simple_nested_scroll_with_recycler_view,
            container, false
        )

        recyclerView = view.findViewById(R.id.recycler_view)

        val llm = LinearLayoutManager(context).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        recyclerView.layoutManager = llm

        val dividerItemDecoration = DividerItemDecoration(recyclerView.context, llm.orientation)
        recyclerView.addItemDecoration(dividerItemDecoration)

        return view
    }

    override fun onResume() {
        super.onResume()
        GroupsManager.requestGroupStatusForm(groupchat)
        (activity as GroupStatusActivity).showProgressBar(true)

        Application.getInstance().addUIListener(OnGroupStatusResultListener::class.java, this)
    }

    override fun onPause() {
        super.onPause()
        Application.getInstance().removeUIListener(OnGroupStatusResultListener::class.java, this)
    }

    override fun onStatusDataFormReceived(groupchat: GroupChat, dataForm: DataForm) {
        if (!isThisGroupChat(groupchat)) return
        Application.getInstance().runOnUiThread {
            (activity as GroupStatusActivity).showProgressBar(false)

            this.dataForm = dataForm
            val descriptions = mutableListOf<FormField>()

            for (field in dataForm.fields)
                if (field.variable != "status"
                    && field.type != FormField.Type.hidden
                    && !field.description.isNullOrEmpty()
                ) {
                    descriptions.add(field)
                }

            val adapter = GroupStatusAdapter(
                dataForm.getField("status").options,
                groupchat, descriptions, this
            )

            recyclerView.adapter = adapter
            adapter.notifyDataSetChanged()
        }
    }

    private fun isThisGroupChat(groupchat: GroupChat) = this.groupchat == groupchat

    override fun onError(groupchat: GroupChat) {
        if (!isThisGroupChat(groupchat)) return
        Application.getInstance().runOnUiThread {
            (activity as GroupStatusActivity).showProgressBar(false)
            GroupsManager.requestGroupStatusForm(groupchat)
            Toast.makeText(context, R.string.groupchat_error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStatusSuccessfullyChanged(groupchat: GroupChat) {
        if (!isThisGroupChat(groupchat)) return
        Application.getInstance().runOnUiThread {
            (activity as GroupStatusActivity).showProgressBar(false)
            activity?.finish()
        }
    }

    private fun createNewDataForm(option: FormField.Option): DataForm {
        val newDataForm = DataForm(DataForm.Type.submit).apply {
            title = dataForm.title
            instructions = dataForm.instructions
        }

        for (oldFormField in dataForm.fields) {

            if (oldFormField.variable == null) continue

            val formFieldToBeAdded = FormField(oldFormField.variable).apply {
                type = oldFormField.type
                label = oldFormField.label
                description = oldFormField.description
            }

            if (oldFormField.variable == "status")
                formFieldToBeAdded.addValue(option.value)
            else formFieldToBeAdded.addValue(oldFormField.values[0])

            newDataForm.addField(formFieldToBeAdded)
        }

        return newDataForm

    }

    override fun onStatusClicked(option: FormField.Option) {
        (activity as GroupStatusActivity).showProgressBar(true)

        GroupsManager.sendSetGroupchatStatusRequest(groupchat, createNewDataForm(option))
    }

}