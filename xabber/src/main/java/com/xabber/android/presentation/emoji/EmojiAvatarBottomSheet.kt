package com.xabber.android.presentation.emoji

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xabber.android.R
import com.xabber.android.databinding.FragmentEmojiAvatarBinding
import com.xabber.android.presentation.util.setFragmentResultListener

class EmojiAvatarBottomSheet : BottomSheetDialogFragment() {

    private val binding by viewBinding(FragmentEmojiAvatarBinding::bind)
    private lateinit var palette: Map<ImageView, Int>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_emoji_avatar, container, false)

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

        palette = mapOf(
            binding.greenTint to R.color.green_400,
            binding.orangeTint to R.color.orange_400,
            binding.redTint to R.color.red_400,
            binding.blueTint to R.color.blue_400,
            binding.indigoTint to R.color.indigo_400,
            binding.purpleTint to R.color.purple_400,
            binding.limeTint to R.color.lime_400,
            binding.pinkTint to R.color.pink_400,
            binding.amberTint to R.color.amber_400,
        )

        palette.forEach { mapElem ->
            mapElem.key.setOnClickListener {
                binding.avatarBackground.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), mapElem.value)
                )
            }
        }

        with(binding) {
            avatarBackground.setOnClickListener {
                EmojiKeyboardBottomSheet().show(requireFragmentManager(), null)
            }
            editBackground.setOnClickListener {
                EmojiKeyboardBottomSheet().show(requireFragmentManager(), null)
            }
            saveButton.setOnClickListener {

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(
            "EMOJI"
        ) { _, result ->
            result.getString("qwe")?.let { note ->
                binding.emojiText.text = note
            }
        }
    }
}