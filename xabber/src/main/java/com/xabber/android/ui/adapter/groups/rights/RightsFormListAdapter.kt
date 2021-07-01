package com.xabber.android.ui.adapter.groups.rights

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import org.jivesoftware.smackx.xdata.FormField
import org.jivesoftware.smackx.xdata.packet.DataForm

class RightsFormListAdapter(
    private val dataForm: DataForm,
    val color: Int,
    private val supportFragmentManager: FragmentManager,
    val listener: Listener,
) : ListSingleFieldVH.Listener, RecyclerView.Adapter<GroupRightsVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (viewType) {
            TITLE_VIEW_TYPE -> TitleVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_group_member_rights_title, parent, false)
            )

            LIST_SINGLE_FIELD_VIEW_TYPE -> ListSingleFieldVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_group_member_rights_field, parent, false), this
            )

            NOT_A_TITLE_FIXED_FIELD_TYPE -> CheckboxField(
                LayoutInflater.from(parent.context).inflate(R.layout.item_group_member_rights_field, parent, false)
            )

            else -> HiddenVH(View(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                visibility = View.GONE
            })
        }


    override fun onBindViewHolder(holder: GroupRightsVH, position: Int) {
        when (holder.itemViewType) {
            TITLE_VIEW_TYPE -> (holder as TitleVH).bind(dataForm.fields[position].values[0], color)
            LIST_SINGLE_FIELD_VIEW_TYPE ->
                (holder as ListSingleFieldVH).bind(dataForm.fields[position], supportFragmentManager)
            NOT_A_TITLE_FIXED_FIELD_TYPE ->
                (holder as CheckboxField).bind(dataForm.fields[position])
        }
    }

    override fun getItemViewType(position: Int): Int {
        val field = dataForm.fields[position]
        val isTitleFixed = field.type == FormField.Type.fixed
                && field.variable == RESTRICTIONS_FIELD_VAR || field.variable == PERMISSIONS_FIELD_VAR
        return when {
            field.type == FormField.Type.list_single -> LIST_SINGLE_FIELD_VIEW_TYPE
            isTitleFixed -> TITLE_VIEW_TYPE
            field.type == FormField.Type.fixed && !isTitleFixed -> NOT_A_TITLE_FIXED_FIELD_TYPE
            else -> HIDDEN_VIEW_TYPE
        }
    }


    override fun getItemCount(): Int = dataForm.fields.size

    override fun onPicked(formField: FormField, option: FormField.Option?, isChecked: Boolean) =
        listener.onOptionPicked(formField, option, isChecked)

    companion object {
        private const val TITLE_VIEW_TYPE = 0
        private const val LIST_SINGLE_FIELD_VIEW_TYPE = 1
        private const val NOT_A_TITLE_FIXED_FIELD_TYPE = 2
        private const val HIDDEN_VIEW_TYPE = 3

        private const val PERMISSIONS_FIELD_VAR = "permission"
        private const val RESTRICTIONS_FIELD_VAR = "restriction"

    }

    interface Listener {
        fun onOptionPicked(field: FormField, option: FormField.Option?, isChecked: Boolean)
    }

}