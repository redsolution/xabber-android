package com.xabber.android.presentation.avatar

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.soundcloud.android.crop.Crop
import com.theartofdev.edmodo.cropper.CropImage
import com.xabber.android.R
import com.xabber.android.data.extension.file.FileManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.databinding.FragmentAvatarBinding
import com.xabber.android.presentation.main.MainActivity
import com.xabber.android.ui.activity.AccountActivity
import com.xabber.android.ui.activity.ManagedActivity
import com.xabber.android.ui.fragment.AccountInfoEditFragment
import com.xabber.android.ui.helper.PermissionsRequester
import com.xabber.android.ui.helper.PermissionsRequester.*
import java.io.IOException
import java.net.SocketPermission

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
            emojiViewGroup.setOnClickListener {
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