package com.xabber.android.presentation.emoji

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.xabber.android.R
import com.xabber.android.util.AppConstants.TEMP_FILE_NAME
import java.io.File
import java.io.FileOutputStream

class EmojiAvatarViewModel : ViewModel() {

    fun getBitmapFromView(context: Context, view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if (bgDrawable != null)
            bgDrawable.draw(canvas)
        else
            canvas.drawColor(ContextCompat.getColor(context, R.color.blue_400))
        view.draw(canvas)
        return bitmap
    }

    fun saveBitmapToFile(bitmap: Bitmap, parentPath: File) {
        val file = File(parentPath, TEMP_FILE_NAME)
        val ostream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 10, ostream)
        ostream.close()
    }
}