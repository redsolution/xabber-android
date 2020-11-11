package com.xabber.android.ui.adapter.groups.settings

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
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
import com.xabber.android.data.SettingsManager
import com.xabber.android.ui.color.ColorManager
import de.hdodenhof.circleimageview.CircleImageView
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

class GroupSettingsTextSingleFieldVH(val view: View) : GroupSettingsTextMultiFieldVH(view){

    fun bind(field: FormField, listener: Listener, color: Int, groupchatJid: String? = null,
             avatar: Drawable? = null) {

        bind(field, listener, color)

        val isGroupNameField = field.variable == GROUP_NAME_VARIABLE

        val avatarIv = view.findViewById<CircleImageView>(R.id.avatarView)
        val helperTv = view.findViewById<TextView>(R.id.group_settings_single_text_bottom_tv)

        avatarIv.apply {
            if (isGroupNameField && avatar != null){
                visibility = View.VISIBLE
                setImageDrawable(avatar)
                //todo avatar change menu opening
            } else {
                visibility = View.GONE
            }
        }

        helperTv.apply {
            if (isGroupNameField && groupchatJid != null){
                visibility = View.VISIBLE
                text = groupchatJid
            } else{
                visibility = View.GONE
            }
        }

    }

    private companion object{
        const val GROUP_NAME_VARIABLE = "name"
    }

}

open class GroupSettingsTextMultiFieldVH(val itemView: View): GroupSettingsVH(itemView){

    fun bind(field: FormField, listener: Listener, color: Int){

        val labelTv = itemView.findViewById<TextView>(R.id.group_settings_label_tv)
        val editText = itemView.findViewById<EditText>(R.id.group_settings_et)
        val lineView = itemView.findViewById<View>(R.id.group_settings_line_view)

        val defaultLabelTextColor = labelTv.currentTextColor
        val defaultLineBackgroundColor =
                if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark)
                    ColorManager.getColorWithAlpha(Color.GRAY, 0.1f)
                else ColorManager.getColorWithAlpha(Color.GRAY, 0.9f)

        labelTv.apply {
            hint = field.label
        }

        editText.apply {
            setText(field.values[0])

            setOnFocusChangeListener { _, b ->
                if (b){
                    labelTv.setTextColor(color)
                    lineView.setBackgroundColor(color)
                }
                else {
                    labelTv.setTextColor(defaultLabelTextColor)
                    lineView.setBackgroundColor(defaultLineBackgroundColor)
                }
            }

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun afterTextChanged(s: Editable?) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    listener.onTextChanged(s.toString())
                }
            })

        }

    }

    interface Listener {
        fun onTextChanged(text: String)
    }

}
