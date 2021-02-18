package com.xabber.android.ui.adapter.groups.rights

import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.ui.dialog.OptionPickerDialog
import org.jivesoftware.smackx.xdata.FormField

abstract class GroupRightsVH(itemView: View) : RecyclerView.ViewHolder(itemView)

class HiddenVH(itemView: View) : GroupRightsVH(itemView)

class TitleVH(itemView: View) : GroupRightsVH(itemView) {

    private val titleTv = itemView.findViewById<TextView>(R.id.item_group_member_rights_title_tv)

    fun bind(title: String, color: Int) = with(titleTv) {
        text = title
        setTextColor(color)
    }

}

class FieldVH(itemView: View, val listener: Listener) : GroupRightsVH(itemView),
        OptionPickerDialog.OptionPickerDialogListener {

    private val fieldTitleTv = itemView.findViewById<TextView>(R.id.item_group_member_rights_field_tv)
    private val checkBox = itemView.findViewById<CheckBox>(R.id.item_group_member_rights_checkbox)
    private val timeTv = itemView.findViewById<TextView>(R.id.item_group_member_rights__timer_tv)
    private val checkboxRoot = itemView.findViewById<LinearLayout>(R.id.item_group_member_rights_checkbox_root)

    private var formField: FormField? = null

    fun bind(field: FormField, fragmentManager: FragmentManager) {
        formField = field
        fieldTitleTv.text = field.label

        if (field.values != null && field.values.size > 0){
            checkBox.isChecked = true
            if (field.values[0] != "0") {
                timeTv.text = field.values[0].toLong().getHumanReadableEstimatedTime()
                timeTv.visibility = View.VISIBLE
            }
        }

        checkBox.isClickable = false

        checkboxRoot.setOnClickListener {
            if (!checkBox.isChecked) {
                OptionPickerDialog(field, this)
                        .show(fragmentManager, OptionPickerDialog.OPTION_PICKER_TAG)
            } else {
                checkBox.isChecked = false
                listener.onPicked(formField!!, null, false)
                timeTv.visibility = View.GONE
            }
        }
    }

    override fun onCanceled() {}

    override fun onOptionPicked(formField: FormField, option: FormField.Option) {
        checkBox.isChecked = true
        timeTv.visibility = View.VISIBLE
        timeTv.text = option.label
        listener.onPicked(formField, option, true)
    }

    interface Listener {
        fun onPicked(formField: FormField, option: FormField.Option?, isChecked: Boolean)
    }

    private fun Long.getHumanReadableEstimatedTime(): String{
        require(this >= 0) { "Duration must be greater than zero!" }

        val currentTime = System.currentTimeMillis() / 1000
        var secondsLeft: Long = this - currentTime

        val MILLIS_IN_DAY: Long = 86400
        val MILLIS_IN_HOUR: Long = 3600
        val MILLIS_IN_MINUTE: Long = 60

        val days = secondsLeft / MILLIS_IN_DAY
        secondsLeft -= days * MILLIS_IN_DAY

        val hours = secondsLeft / MILLIS_IN_HOUR
        secondsLeft -= hours * MILLIS_IN_HOUR

        val minutes = (secondsLeft / MILLIS_IN_MINUTE).toInt()

        val resources = itemView.resources

        return when {
            days >= 1 -> resources.getQuantityString(R.plurals.estimated_in_days, days.toInt(), days)
            hours >= 1 -> resources.getQuantityString(R.plurals.estimated_in_hours, hours.toInt(), hours)
            minutes >= 1 -> resources.getQuantityString(R.plurals.estimated_in_minutes, minutes, minutes)
            secondsLeft >= 1 -> resources.getQuantityString(R.plurals.estimated_in_seconds, secondsLeft.toInt(), secondsLeft)
            else -> ""
        }
    }

}