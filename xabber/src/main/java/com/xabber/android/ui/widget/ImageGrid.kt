package com.xabber.android.ui.widget

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject
import com.xabber.android.data.extension.file.FileManager
import com.xabber.android.data.message.MessageManager
import com.xabber.android.ui.helper.RoundedBorders
import io.realm.Realm
import io.realm.RealmList
import java.io.File

class ImageGrid {

    private val centerCropTransformation: MultiTransformation<Bitmap> by lazy {
        createStandardTransformation(CenterCrop())
    }

    private val centerInsideTransformation: MultiTransformation<Bitmap> by lazy {
        createStandardTransformation(CenterInside())
    }

    private val justRoundedTransformation: MultiTransformation<Bitmap> by lazy {
        createStandardTransformation()
    }

    private fun createStandardTransformation(vararg extraTransformation: BitmapTransformation) =
        MultiTransformation(
            listOf(
                RoundedCorners(IMAGE_ROUNDED_CORNERS),
                RoundedBorders(IMAGE_ROUNDED_BORDER_CORNERS, IMAGE_ROUNDED_BORDER_WIDTH),
                *extraTransformation
            )
        )

    fun inflateView(parent: ViewGroup, imageCount: Int): View =
        LayoutInflater.from(parent.context).inflate(getLayoutResource(imageCount), parent, false)

    fun bindView(
        view: View,
        attachmentRealmObjects: RealmList<AttachmentRealmObject>,
        clickListener: View.OnClickListener?,
        wholeGridLongTapListener: View.OnLongClickListener? = null
    ) {
        if (attachmentRealmObjects.size == 1) {
            getImageView(view, 0)
                .apply {
                    setOnLongClickListener(wholeGridLongTapListener)
                    setOnClickListener(clickListener)
                }
                .also { setupImageViewIntoFlexibleSingleImageCell(attachmentRealmObjects[0]!!, it) }
        } else {
            attachmentRealmObjects.take(5).forEachIndexed { index, attachmentRealmObject ->
                getImageView(view, index)
                    .apply {
                        setOnLongClickListener(wholeGridLongTapListener)
                        setOnClickListener(clickListener)
                    }
                    .also { setupImageViewIntoRigidGridCell(attachmentRealmObject, it) }
            }
            
            view.findViewById<TextView>(R.id.tvCounter)?.apply {
                if (attachmentRealmObjects.size > MAX_IMAGE_IN_GRID) {
                    text = StringBuilder("+").append(attachmentRealmObjects.size - MAX_IMAGE_IN_GRID)
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }
        }
    }

    private fun setupImageViewIntoRigidGridCell(attachmentRealmObject: AttachmentRealmObject, imageView: ImageView) {
        val uri = attachmentRealmObject.filePath?.takeIf { it.isNotEmpty() }
            ?: attachmentRealmObject.fileUrl

        Glide.with(imageView.context)
            .load(uri)
            .transform(centerCropTransformation)
            .placeholder(R.drawable.ic_recent_image_placeholder)
            .error(R.drawable.ic_recent_image_placeholder)
            .into(imageView)
    }

    private fun setupImageViewIntoFlexibleSingleImageCell(
        attachmentRealmObject: AttachmentRealmObject, imageView: ImageView
    ) {
        attachmentRealmObject.filePath?.let {
            val result = loadImageFromFile(it, imageView)
            if (!result) {
                MessageManager.setAttachmentLocalPathToNull(attachmentRealmObject.uniqueId)
            }
            return
        }

        val imageWidth = attachmentRealmObject.imageWidth
        val imageHeight = attachmentRealmObject.imageHeight

        if (imageWidth != null && imageHeight != null) {
            setupImageViewWithDimensions(
                imageView, attachmentRealmObject.fileUrl, imageHeight, imageWidth
            )
        } else {
            setupImageViewWithoutDimensions(
                imageView, attachmentRealmObject.fileUrl, attachmentRealmObject.uniqueId
            )
        }
    }

    private fun setupImageViewWithoutDimensions(
        imageView: ImageView, url: String, attachmentId: String
    ) {
        Glide.with(imageView.context)
            .asBitmap()
            .load(url)
            .transform(centerCropTransformation)
            .placeholder(R.drawable.ic_recent_image_placeholder)
            .error(R.drawable.ic_recent_image_placeholder)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onLoadStarted(placeholder: Drawable?) {
                    super.onLoadStarted(placeholder)
                    imageView.setImageDrawable(placeholder)
                    imageView.visibility = View.VISIBLE
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    imageView.setImageDrawable(errorDrawable)
                    imageView.visibility = View.VISIBLE
                }

                override fun onResourceReady(
                    resource: Bitmap, transition: Transition<in Bitmap?>?
                ) {
                    val width = resource.width
                    val height = resource.height
                    if (width <= 0 || height <= 0) {
                        return
                    }
                    Application.getInstance().runInBackground {
                        DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                            realm.executeTransactionAsync { realm1: Realm ->
                                realm1.where(AttachmentRealmObject::class.java)
                                    .equalTo(AttachmentRealmObject.Fields.UNIQUE_ID, attachmentId)
                                    .findFirst()
                                    ?.apply {
                                        this.imageWidth = width
                                        this.imageHeight = height
                                    }
                            }
                        }

                        Application.getInstance().runOnUiThread {
                            imageView.setImageBitmap(resource)
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun setupImageViewWithDimensions(
        imageView: ImageView, url: String, width: Int, height: Int
    ) {
        scaleImage(imageView.layoutParams, height, width)

        Glide.with(imageView.context)
            .load(url)
            .transform(centerInsideTransformation)
            .placeholder(R.drawable.ic_recent_image_placeholder)
            .error(R.drawable.ic_recent_image_placeholder)
            .into(imageView)
    }

    private fun getLayoutResource(imageCount: Int): Int {
        return when (imageCount) {
            1 -> R.layout.image_grid_1
            2 -> R.layout.image_grid_2
            3 -> R.layout.image_grid_3
            4 -> R.layout.image_grid_4
            5 -> R.layout.image_grid_5
            else -> R.layout.image_grid_6
        }
    }

    private fun getImageView(view: View, index: Int): ImageView {
        return when (index) {
            1 -> view.findViewById(R.id.ivImage1)
            2 -> view.findViewById(R.id.ivImage2)
            3 -> view.findViewById(R.id.ivImage3)
            4 -> view.findViewById(R.id.ivImage4)
            5 -> view.findViewById(R.id.ivImage5)
            else -> view.findViewById(R.id.ivImage0)
        }
    }

    private fun loadImageFromFile(path: String, imageView: ImageView): Boolean {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        // Returns null, sizes are in the options variable
        BitmapFactory.decodeFile(path, options)

        val layoutParams = imageView.layoutParams

        if (FileManager.isImageNeededDimensionsFlip(Uri.fromFile(File(path)))) {
            scaleImage(layoutParams, options.outWidth, options.outHeight)
        } else {
            scaleImage(layoutParams, options.outHeight, options.outWidth)
        }

        if (options.outHeight == 0 || options.outWidth == 0) {
            return false
        }

        imageView.layoutParams = layoutParams

        Glide.with(imageView.context)
            .load(path)
            .transform(justRoundedTransformation)
            .into(imageView)

        return true
    }

    private fun scaleImage(layoutParams: ViewGroup.LayoutParams, height: Int, width: Int) {
        var scaledWidth: Int
        var scaledHeight: Int
        if (width <= height) {
            if (height > MAX_IMAGE_HEIGHT_SIZE) {
                scaledWidth =
                    (width / (height.toDouble() / MAX_IMAGE_HEIGHT_SIZE)).toInt()
                scaledHeight = MAX_IMAGE_HEIGHT_SIZE
            } else if (width < MIN_IMAGE_SIZE) {
                scaledWidth = MIN_IMAGE_SIZE
                scaledHeight = (height / (width.toDouble() / MIN_IMAGE_SIZE)).toInt()
                if (scaledHeight > MAX_IMAGE_HEIGHT_SIZE) {
                    scaledHeight = MAX_IMAGE_HEIGHT_SIZE
                }
            } else {
                scaledWidth = width
                scaledHeight = height
            }
        } else {
            when {
                width > MAX_IMAGE_SIZE -> {
                    scaledWidth = MAX_IMAGE_SIZE
                    scaledHeight = (height / (width.toDouble() / MAX_IMAGE_SIZE)).toInt()
                }
                height < MIN_IMAGE_SIZE -> {
                    scaledWidth = (width / (height.toDouble() / MIN_IMAGE_SIZE)).toInt()
                    if (scaledWidth > MAX_IMAGE_SIZE) {
                        scaledWidth = MAX_IMAGE_SIZE
                    }
                    scaledHeight = MIN_IMAGE_SIZE
                }
                else -> {
                    scaledWidth = width
                    scaledHeight = height
                }
            }
        }
        layoutParams.width = scaledWidth
        layoutParams.height = scaledHeight
    }

    companion object {
        private val resources = Application.getInstance().resources

        private const val MAX_IMAGE_IN_GRID = 6

        private val MAX_IMAGE_SIZE = resources.getDimensionPixelSize(R.dimen.max_chat_image_size)
        private val MIN_IMAGE_SIZE = resources.getDimensionPixelSize(R.dimen.min_chat_image_size)

        private val MAX_IMAGE_HEIGHT_SIZE = resources.getDimensionPixelSize(R.dimen.max_chat_image_height_size)

        private val IMAGE_ROUNDED_CORNERS = resources.getDimensionPixelSize(R.dimen.chat_image_corner_radius)
        private val IMAGE_ROUNDED_BORDER_CORNERS = resources.getDimensionPixelSize(R.dimen.chat_image_border_radius)

        const val IMAGE_ROUNDED_BORDER_WIDTH = 0
    }

}