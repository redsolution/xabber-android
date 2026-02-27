package com.xabber.android.presentation.avatar

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xabber.android.R
import com.xabber.android.databinding.FragmentAvatarBinding
import com.xabber.android.presentation.emoji.EmojiAvatarBottomSheet
import com.xabber.android.presentation.main.MainActivity
import com.xabber.android.presentation.util.setFragmentResultListener
import com.xabber.android.util.AppConstants

class AvatarBottomSheet : BottomSheetDialogFragment() {

    private val binding by viewBinding(FragmentAvatarBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_avatar, container, false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { setupBottomSheet(it) }
        return dialog
    }

    private fun setupBottomSheet(dialogInterface: DialogInterface) {
        val bottomSheetDialog = dialogInterface as BottomSheetDialog
        val bottomSheet = bottomSheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )
            ?: return
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            btnEmoji.setOnClickListener {
                EmojiAvatarBottomSheet().show(requireFragmentManager(), null)
                dismiss()
            }
            btnSelfie.setOnClickListener {
                (activity as MainActivity).onTakePhoto()
                dismiss()
            }
            btnChoseImage.setOnClickListener {
                (activity as MainActivity).onChooseFromGallery()
                dismiss()
            }
        }
    }
}