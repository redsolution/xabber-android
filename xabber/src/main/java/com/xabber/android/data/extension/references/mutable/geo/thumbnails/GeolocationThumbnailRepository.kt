package com.xabber.android.data.extension.references.mutable.geo.thumbnails

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.xabber.android.data.Application
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

class GeolocationThumbnailRepository(private val context: Context) {

    private val thumbCreator: GeolocationThumbnailCreator by lazy {
        GeolocationThumbnailCreator(context)
    }

    private val dir = File(context.filesDir.path, DIRECTORY_PATH).apply {
        if (!exists()) {
            this.mkdirs()
        }
    }

    fun getOrCreateThumbnail(
        lon: Double, lat: Double, pointerColor: Int, bitmapReadyCallback: OnBitmapReadyCallback
    ) {
        val fileName = createName(lon, lat, pointerColor)
        val file = File(dir, fileName)

        return if (file.exists()) {
            bitmapReadyCallback.onBitmapReady(BitmapFactory.decodeFile(file.path))

        } else {
            thumbCreator.createThumbnailBitmap(lon, lat, pointerColor
            ) {
                Bitmap.createBitmap(it.bitmap).also { bitmap ->
                    writeToDisk(bitmap, fileName)
                    bitmapReadyCallback.onBitmapReady(bitmap)
                }
            }
        }
    }

    fun removeIfExists(lon: Double, lat: Double, pointerColor: Int) {
        try {
            Application.getInstance().runInBackground {
                File(dir, createName(lon, lat, pointerColor)).delete()
            }
        } catch (ex: Exception) {
            //ignore
        }
    }

    private fun writeToDisk(bitmap: Bitmap, name: String) {
        Application.getInstance().runInBackground {
            BufferedOutputStream(FileOutputStream(File(dir, name))).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, it)
                it.flush()
            }
        }
    }

    private fun createName(lon: Double, lat: Double, pointerColor: Int) =
        "${lon}_${lat}_$pointerColor.JPEG"

    //        val pointerColor = ColorManager.getInstance().accountPainter.getAccountColorWithTint(
//            messageRealmObject.account, 500
//        )

    companion object {
        private const val DIRECTORY_PATH = "geolocation_thumbnails"
    }

    interface OnBitmapReadyCallback {
        fun onBitmapReady(bitmap: Bitmap)
    }

}