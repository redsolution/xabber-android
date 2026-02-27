package com.xabber.android.ui.dialog

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.xabber.android.R
import org.jivesoftware.smackx.xdata.FormField

class OptionPickerDialog(private val formField: FormField, val listener: OptionPickerDialogListener)
    : DialogFragment() {

    var checkedItem = getOptionsList().size-1

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog = AlertDialog.Builder(context)
            .setTitle(getString(R.string.groupchat_set_duration))
            //.setView(createOptionsLinearLayout())
            .setSingleChoiceItems(getOptionsList(), getOptionsList().size-1) { _, i -> checkedItem = i }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->  }
            .setPositiveButton(getString(R.string.set)) { _, _ -> listener.onOptionPicked(formField, formField.options[checkedItem])}
            .create()

    private fun getOptionsList(): Array<String>{
        val list = mutableListOf<String>()
        for (option in formField.options)
            list.add(option.label)
        return list.toTypedArray()
    }

//    private fun createOptionsLinearLayout() = LinearLayoutCompat(context!!).apply {
//        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT)
//        orientation = LinearLayoutCompat.VERTICAL
//        setPadding(76, 12, 12, 12)
//
//        for (option in formField.options)
//            addView(createOptionTv(option))
//    }
//
//    private fun createOptionTv(option: FormField.Option) = TextView(context).apply {
//        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT)
//        textSize = 18f
//        text = option.label
//        setOnClickListener {
//            listener.onOptionPicked(formField, option)
//            dismiss()
//        }
//    }

    interface OptionPickerDialogListener {
        fun onOptionPicked(formField: FormField, option: FormField.Option)
        fun onCanceled()
    }

    companion object {
        const val OPTION_PICKER_TAG = "com.xabber.android.ui.dialog.OptionPickerDialog"
    }

}