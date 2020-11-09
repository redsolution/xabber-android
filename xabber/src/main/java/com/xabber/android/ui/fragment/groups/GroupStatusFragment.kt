package com.xabber.android.ui.fragment.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.extension.groupchat.status.GroupStatusResultListener
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupchatManager
import com.xabber.android.ui.activity.GroupStatusActivity
import com.xabber.android.ui.adapter.groups.GroupStatusAdapter
import org.jivesoftware.smackx.xdata.FormField
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupStatusFragment(val groupchat: GroupChat) : Fragment(), GroupStatusResultListener,
        GroupStatusAdapter.Listener {

    private lateinit var recyclerView: RecyclerView

    private lateinit var dataForm: DataForm

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.simple_nested_scroll_with_recycler_view,
                container, false)

        recyclerView = view.findViewById(R.id.recycler_view)

        return view
    }

    override fun onResume() {
        super.onResume()
        GroupchatManager.getInstance().requestGroupStatusForm(groupchat)
        (activity as GroupStatusActivity).showProgressBar(true)

        Application.getInstance().addUIListener(GroupStatusResultListener::class.java, this)
    }

    override fun onPause() {
        super.onPause()
        Application.getInstance().removeUIListener(GroupStatusResultListener::class.java, this)
    }

    override fun onStatusDataFormReceived(groupchat: GroupChat, dataForm: DataForm) {
        if (!isThisGroupChat(groupchat)) return
        (activity as GroupStatusActivity).showProgressBar(false)

        this.dataForm = dataForm
        val descriptions = mutableListOf<FormField>()

        for (field in dataForm.fields)
            if (field.variable != "status" && field.type != FormField.Type.hidden && field.type != FormField.Type.fixed)
                descriptions.add(field)

        val adapter = GroupStatusAdapter(dataForm.getField("status").options,
                descriptions, this)

        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun isThisGroupChat(groupchat: GroupChat) = this.groupchat == groupchat

    override fun onError(groupchat: GroupChat) {
        if (!isThisGroupChat(groupchat)) return
        (activity as GroupStatusActivity).showProgressBar(false)
        GroupchatManager.getInstance().requestGroupStatusForm(groupchat)
        Toast.makeText(context, R.string.groupchat_error, Toast.LENGTH_SHORT).show()
    }

    override fun onStatusSuccessfullyChanged(groupchat: GroupChat) {
        if (!isThisGroupChat(groupchat)) return
        (activity as GroupStatusActivity).showProgressBar(false)
        activity?.finish()
    }

    override fun onStatusClicked(option: FormField.Option) {
        (activity as GroupStatusActivity).showProgressBar(true)

        for (field in dataForm.fields) {
            if (field.variable == "status") {
                field.options.clear()
                field.values[0] = option.value
            }
        }

        GroupchatManager.getInstance().sendSetGroupchatStatusRequest(groupchat, dataForm)
    }

}