package com.xabber.android.ui.adapter.groups.members

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import org.jivesoftware.smackx.xdata.FormField
import org.jivesoftware.smackx.xdata.packet.DataForm

class GroupMemberRightsFormListAdapter(private val dataForm: DataForm, val color: Int,
                                       private val supportFragmentManager: FragmentManager,
                                       val listener: Listener) : FieldVH.Listener,
        RecyclerView.Adapter<GroupMemberRightsVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            when (viewType) {
                TITLE_VIEW_TYPE -> TitleVH(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_group_member_rights_title, parent, false))

                FIELD_VIEW_TYPE -> FieldVH(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_group_member_rights_field, parent, false), this)

                else -> HiddenVH(View(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT)
                    visibility = View.GONE
                })
            }


    override fun onBindViewHolder(holder: GroupMemberRightsVH, position: Int) {
        when (holder.itemViewType) {
            TITLE_VIEW_TYPE -> (holder as TitleVH).bind(dataForm.fields[position].values[0], color)
            FIELD_VIEW_TYPE -> (holder as FieldVH).bind(dataForm.fields[position], supportFragmentManager)
        }
    }

    override fun getItemViewType(position: Int) =
            when (dataForm.fields[position].type) {
                FormField.Type.list_single -> FIELD_VIEW_TYPE
                FormField.Type.fixed -> TITLE_VIEW_TYPE
                else -> HIDDEN_VIEW_TYPE
            }

    override fun getItemCount(): Int = dataForm.fields.size

    override fun onPicked(formField: FormField, option: FormField.Option?, isChecked: Boolean)
            = listener.onOptionPicked(formField, option, isChecked)

    companion object {
        private const val TITLE_VIEW_TYPE = 0
        private const val FIELD_VIEW_TYPE = 1
        private const val HIDDEN_VIEW_TYPE = 3
    }

    interface Listener {
        fun onOptionPicked(field: FormField, option: FormField.Option?, isChecked: Boolean)
    }

}