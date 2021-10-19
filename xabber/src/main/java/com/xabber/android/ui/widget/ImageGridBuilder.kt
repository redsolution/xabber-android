package com.xabber.android.ui.widget

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.realm.RealmList
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject
import com.xabber.android.ui.widget.ImageGridBuilder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.xabber.android.ui.adapter.chat.MessageVH
import com.xabber.android.ui.helper.RoundedBorders
import com.xabber.android.data.extension.file.FileManager
import com.xabber.android.data.message.MessageManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.log.LogManager
import io.realm.Realm
import java.lang.Exception
import java.lang.StringBuilder

class ImageGridBuilder {
    fun inflateView(parent: ViewGroup, imageCount: Int): View {
        return LayoutInflater.from(
            parent.context
        ).inflate(
            getLayoutResource(imageCount),
            parent,
            false
        )
    }

    fun bindView(
        view: View, attachmentRealmObjects: RealmList<AttachmentRealmObject>,
        clickListener: View.OnClickListener?
    ) {
        if (attachmentRealmObjects.size == 1) {
            val imageView = getImageView(view, 0)
            bindOneImage(attachmentRealmObjects[0], view, imageView)
            imageView.setOnClickListener(clickListener)
        } else {
            val tvCounter = view.findViewById<TextView>(R.id.tvCounter)
            var index = 0
            loop@ for (attachmentRealmObject in attachmentRealmObjects) {
                if (index > 5) break@loop
                val imageView = getImageView(view, index)
                if (imageView != null) {
                    bindImage(attachmentRealmObject, view, imageView)
                    imageView.setOnClickListener(clickListener)
                }
                index++
            }
            if (tvCounter != null) {
                if (attachmentRealmObjects.size > MAX_IMAGE_IN_GRID) {
                    tvCounter.text =
                        StringBuilder("+").append(attachmentRealmObjects.size - MAX_IMAGE_IN_GRID)
                    tvCounter.visibility = View.VISIBLE
                } else tvCounter.visibility = View.GONE
            }
        }
    }

    private fun bindImage(
        attachmentRealmObject: AttachmentRealmObject,
        parent: View,
        imageView: ImageView
    ) {
        var uri = attachmentRealmObject.filePath
        if (uri == null || uri.isEmpty()) uri = attachmentRealmObject.fileUrl
        Glide.with(parent.context)
            .load(uri)
            .transform(
                MultiTransformation(
                    CenterCrop(),
                    RoundedCorners(MessageVH.IMAGE_ROUNDED_CORNERS),
                    RoundedBorders(
                        MessageVH.IMAGE_ROUNDED_BORDER_CORNERS,
                        MessageVH.IMAGE_ROUNDED_BORDER_WIDTH
                    )
                )
            )
            .placeholder(R.drawable.ic_recent_image_placeholder)
            .error(R.drawable.ic_recent_image_placeholder)
            .into(imageView)
    }

    private fun bindOneImage(
        attachmentRealmObject: AttachmentRealmObject?,
        parent: View,
        imageView: ImageView
    ) {
        val imagePath = attachmentRealmObject!!.filePath
        val imageUrl = attachmentRealmObject.fileUrl
        val imageWidth = attachmentRealmObject.imageWidth
        val imageHeight = attachmentRealmObject.imageHeight
        val uniqId = attachmentRealmObject.uniqueId
        if (imagePath != null) {
            val result = FileManager.loadImageFromFile(parent.context, imagePath, imageView)
            if (!result) {
                MessageManager.setAttachmentLocalPathToNull(uniqId)
            }
        } else {
            val layoutParams = imageView.layoutParams
            if (imageWidth != null && imageHeight != null) {
                FileManager.scaleImage(layoutParams, imageHeight, imageWidth)
                Glide.with(parent.context)
                    .load(imageUrl)
                    .transform(
                        MultiTransformation(
                            CenterInside(),
                            RoundedCorners(MessageVH.IMAGE_ROUNDED_CORNERS),
                            RoundedBorders(
                                MessageVH.IMAGE_ROUNDED_BORDER_CORNERS,
                                MessageVH.IMAGE_ROUNDED_BORDER_WIDTH
                            )
                        )
                    )
                    .placeholder(R.drawable.ic_recent_image_placeholder)
                    .error(R.drawable.ic_recent_image_placeholder)
                    .into(imageView)
            } else {
                Glide.with(parent.context)
                    .asBitmap()
                    .load(imageUrl)
                    .transform(
                        MultiTransformation(
                            RoundedCorners(MessageVH.IMAGE_ROUNDED_CORNERS),
                            RoundedBorders(
                                MessageVH.IMAGE_ROUNDED_BORDER_CORNERS,
                                MessageVH.IMAGE_ROUNDED_BORDER_WIDTH
                            )
                        )
                    )
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
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            val width = resource.width
                            val height = resource.height
                            if (width <= 0 || height <= 0) {
                                return
                            }
                            Application.getInstance().runInBackground {
                                var realm: Realm? = null
                                try {
                                    realm = DatabaseManager.getInstance().defaultRealmInstance
                                    realm.executeTransactionAsync(Realm.Transaction { realm1: Realm ->
                                        val first = realm1.where(
                                            AttachmentRealmObject::class.java
                                        )
                                            .equalTo(AttachmentRealmObject.Fields.UNIQUE_ID, uniqId)
                                            .findFirst()
                                        if (first != null) {
                                            first.imageWidth = width
                                            first.imageHeight = height
                                        }
                                    })
                                    FileManager.scaleImage(layoutParams, height, width)
                                    Application.getInstance()
                                        .runOnUiThread { imageView.setImageBitmap(resource) }
                                } catch (e: Exception) {
                                    LogManager.exception(ImageGridBuilder::class.java.simpleName, e)
                                } finally {
                                    realm?.close()
                                }
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }
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

    companion object {
        private const val MAX_IMAGE_IN_GRID = 6
    }
}