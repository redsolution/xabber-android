package com.xabber.android.ui.adapter.groups.settings

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import org.jivesoftware.smackx.xdata.FormField

abstract class GroupSettingsVH(itemView: View) : RecyclerView.ViewHolder(itemView)

class HiddenVH(itemView: View): GroupSettingsVH(itemView)

class GroupSettingsFixedFieldVH(val itemView: View): GroupSettingsVH(itemView){

    fun bind(text: String){
        itemView.findViewById<TextView>(R.id.group_settings_fixed_tv).text = text
    }

}

class GroupSettingsSingleListFieldVH(val itemView: View): GroupSettingsVH(itemView){

    private val context = itemView.context

    fun bind(field: FormField, listener: Listener){

        val textView = TextView(context).apply {
            text = field.label
        }

        val radioGroup = RadioGroup(context)
        radioGroup.orientation = RadioGroup.VERTICAL

        for (option in field.options){
            val radioButton = RadioButton(context).apply {
                text = option.label
                setOnClickListener { listener.onOptionSelected(option) }
            }
            radioGroup.addView(radioButton)
            if (option.value == field.values[0]) radioGroup.check(radioButton.id)
        }

        if (itemView is LinearLayout){
            itemView.addView(textView)
            itemView.addView(radioGroup)
        }

    }

    interface Listener{
        fun onOptionSelected(option: FormField.Option)
    }

}

abstract class GroupSettingsTextSingleFieldVH(val itemView: View): GroupSettingsVH(itemView){

    abstract fun setupEditText(editText: EditText)

    fun bind(field: FormField, listener: Listener){
        itemView.findViewById<TextView>(R.id.group_settings_label_tv).text = field.label
        val editText = itemView.findViewById<EditText>(R.id.group_settings_et)
        editText.setText(field.values[0])
        editText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                listener.onTextChanged(s.toString())
            }
        })
        setupEditText(editText)
    }

    interface Listener{
        fun onTextChanged(text: String)
    }

}

class GroupSettingsTextSingleBigFieldVH(itemView: View): GroupSettingsTextSingleFieldVH(itemView){

    override fun setupEditText(editText: EditText) {
        editText.apply {
            isSingleLine = false
            maxLines = 5
            minLines = 5
        }
    }

}

class GroupSettingsTextSingleSingleLineFieldVH(itemView: View)
    : GroupSettingsTextSingleFieldVH(itemView){

    override fun setupEditText(editText: EditText) {
        editText.isSingleLine = true
    }

}