package com.xabber.android.data.extension.references.mutable.geo.thumbnails

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Looper
import com.xabber.android.data.Application
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.realmobjects.ReferenceRealmObject
import com.xabber.android.data.log.LogManager
import com.xabber.android.ui.color.ColorManager
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

    fun modifyMessageWithThumbnailIfNeed(
        message: MessageRealmObject
    ) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            LogManager.w(this, "Writing to database in UI thread!")
        }
        val pointerColor = ColorManager.getInstance().accountPainter.getAccountColorWithTint(
            message.account, 500
        )

        fun modifyReferenceAndSave(
            referencePrimaryKey: String, bitmapWidth: Int, bitmapHeight: Int, bitmapFilePath: String
        ) {
            Application.getInstance().runInBackground {
                DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                    realm.executeTransaction { transaction ->
                        transaction.where(ReferenceRealmObject::class.java)
                            .equalTo(ReferenceRealmObject.Fields.UNIQUE_ID, referencePrimaryKey)
                            .findFirst()
                            ?.apply {
                                imageWidth = bitmapWidth
                                imageHeight = bitmapHeight
                                filePath = bitmapFilePath
                                setIsImage(true)
                            }?.also {
                                transaction.copyToRealmOrUpdate(it)
                            }
                    }
                }
            }
        }

        message.referencesRealmObjects
            .forEach { reference ->
                if (reference.isGeo && reference.filePath.isNullOrEmpty()) {
                    val lon = reference.longitude
                    val lat = reference.latitude

                    val fileName = createName(lon, lat, pointerColor)
                    val file = File(dir, fileName)
                    if (!file.exists()) {
                        thumbCreator.requestThumbnail(lon, lat, pointerColor) { bitmap: Bitmap ->
                            writeToDisk(bitmap, fileName)
                            modifyReferenceAndSave(reference.uniqueId, bitmap.width, bitmap.height, file.path)
                        }
                    } else {
                        val bitmap = BitmapFactory.decodeFile(file.path)
                        modifyReferenceAndSave(reference.uniqueId, bitmap.width, bitmap.height, file.path)
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
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, it)
                it.flush()
            }
        }
    }

    private fun createName(lon: Double, lat: Double, pointerColor: Int) =
        "${lon}_${lat}_$pointerColor.JPEG"

    private companion object {
        private const val DIRECTORY_PATH = "geolocation_thumbnails"
    }

}