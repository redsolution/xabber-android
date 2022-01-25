package com.xabber.android.presentation.emoji

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xabber.android.R
import com.xabber.android.databinding.FragmentEmojiAvatarBinding
import com.xabber.android.presentation.base.FragmentTag
import com.xabber.android.presentation.signup.SignupFragment
import com.xabber.android.presentation.util.setFragmentResultListener
import com.xabber.android.util.AppConstants.EMOJI_KEY_REQUEST_KEY
import com.xabber.android.util.AppConstants.EMOJI_KEY_RESPONSE_KEY

class EmojiAvatarBottomSheet : BottomSheetDialogFragment() {

    private val binding by viewBinding(FragmentEmojiAvatarBinding::bind)
    private val viewModel = EmojiAvatarViewModel()

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

        val palette = mapOf(
            binding.greenTint to R.color.green_100,
            binding.orangeTint to R.color.orange_100,
            binding.redTint to R.color.red_100,
            binding.blueTint to R.color.blue_100,
            binding.indigoTint to R.color.indigo_100,
            binding.purpleTint to R.color.purple_100,
            binding.limeTint to R.color.lime_100,
            binding.pinkTint to R.color.pink_100,
            binding.amberTint to R.color.amber_100,
        )

        val toggles = mapOf(
            binding.greenTint to binding.greenTintToggle,
            binding.orangeTint to binding.orangeTintToggle,
            binding.redTint to binding.redTintToggle,
            binding.blueTint to binding.blueTintToggle,
            binding.indigoTint to binding.indigoTintToggle,
            binding.purpleTint to binding.purpleTintToggle,
            binding.limeTint to binding.limeTintToggle,
            binding.pinkTint to binding.pinkTintToggle,
            binding.amberTint to binding.amberTintToggle,
        )

        palette.forEach { mapElem ->
            mapElem.key.setOnClickListener {
                binding.avatarBackground.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), mapElem.value)
                )
                for (t in toggles) {
                    t.value.isVisible = false
                }
                toggles[mapElem.key]!!.isVisible = true
            }
        }

        with(binding) {
            toggles[blueTint]!!.isVisible = true
            avatarBackground.setOnClickListener {
                EmojiKeyboardBottomSheet().show(requireFragmentManager(), null)
            }
            editBackground.setOnClickListener {
                EmojiKeyboardBottomSheet().show(requireFragmentManager(), null)
            }
            saveButton.setOnClickListener {
                val avatar = avatarBackground
                avatar.radius = 0F
                val bitmap = viewModel.getBitmapFromView(requireContext(), avatar)
                val fragment =
                    requireFragmentManager().findFragmentByTag(FragmentTag.Signup4.toString()) as SignupFragment
                fragment.setAvatar(bitmap)
                viewModel.saveBitmapToFile(bitmap, requireContext().cacheDir)
                dismiss()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(EMOJI_KEY_REQUEST_KEY) { _, result ->
            result.getString(EMOJI_KEY_RESPONSE_KEY)?.let { emoji ->
                binding.emojiText.text = emoji
            }
        }
    }
}