package com.xabber.android.ui.adapter.groups.settings

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import org.jivesoftware.smackx.xdata.FormField

abstract class GroupSettingsVH(itemView: View) : RecyclerView.ViewHolder(itemView)

class HiddenVH(itemView: View) : GroupSettingsVH(itemView)

class GroupSettingsFixedFieldVH(val itemView: View) : GroupSettingsVH(itemView) {

    fun bind(text: String) {
        itemView.findViewById<TextView>(R.id.group_settings_fixed_tv).text = text
    }

}

class GroupSettingsSingleListFieldVH(val itemView: View) : GroupSettingsVH(itemView) {

    private val context = itemView.context

    fun bind(field: FormField, color: Int, listener: Listener) {

        itemView.findViewById<TextView>(R.id.group_settings_fixed_tv).apply {
            text = field.label
            setTextColor(color)
        }

        val radioGroup = itemView.findViewById<RadioGroup>(R.id.group_settings_radio_group)

        for ((index, option) in field.options.withIndex()) {
            val radioButton = RadioButton(context).apply {
                setPadding(24, 0, 0, 0)
                text = option.label
                setOnClickListener { listener.onOptionSelected(option) }

                if(Build.VERSION.SDK_INT >= 21)
                    buttonTintList = ColorStateList(
                            arrayOf(intArrayOf(-android.R.attr.state_checked),
                                    intArrayOf(android.R.attr.state_checked)),
                            intArrayOf( Color.BLACK /** disabled */ , color /** enabled */))

            }

            radioGroup.addView(radioButton)

            if (index != field.options.size - 1) {
                val spaceView = View(context)
                spaceView.minimumHeight = 12
                radioGroup.addView(spaceView)
            }

            if (option.value == field.values[0]) radioGroup.check(radioButton.id)
        }

    }

    interface Listener {
        fun onOptionSelected(option: FormField.Option)
    }

}

class GroupSettingsTextSingleFieldVH(val itemView: View) : GroupSettingsVH(itemView) {

    fun bind(field: FormField, color: Int, listener: Listener, groupchatJid: String? = null) {
        itemView.findViewById<TextView>(R.id.group_settings_label_tv).apply {
            text = field.label
        }
        itemView.findViewById<EditText>(R.id.group_settings_et).apply {
            setText(field.values[0])
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun afterTextChanged(s: Editable?) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    listener.onTextChanged(s.toString())
                }
            })
        }

        if (field.variable == "name" && groupchatJid != null){
            itemView.findViewById<TextView>(R.id.group_settings_single_text_bottom_tv).apply {
                visibility = View.VISIBLE
                text = groupchatJid
            }
        }

    }

    interface Listener {
        fun onTextChanged(text: String)
    }

}
