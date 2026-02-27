package com.xabber.android.presentation.emoji

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xabber.android.R
import com.xabber.android.data.dto.EmojiTypeDto
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.util.toMap
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringWriter

class EmojiKeyboardViewModel : ViewModel() {

    fun getEmojiMap(resources: Resources): Map<Int, List<String>> {
        val ins: InputStream = resources.openRawResource(R.raw.emojis)
        val writer = StringWriter()
        val buffer = CharArray(1024)
        runCatching {
            val reader = BufferedReader(InputStreamReader(ins, "UTF-8"))
            var n: Int = reader.read(buffer)
            while (n != -1) {
                writer.write(buffer, 0, n)
                n = reader.read(buffer)
            }
        }.also {
            ins.close()
        }.onFailure {
            LogManager.e(this::class.java.simpleName, it.stackTraceToString())
        }

        val jsonString = writer.toString()
        val collectionType = object : TypeToken<List<EmojiTypeDto>>() {}.type
        val dataset: List<EmojiTypeDto> =
            Gson().fromJson(jsonString, collectionType)

        val resultMap: Map<Int, List<String>> =
            dataset.toMap()
                .map {
                    val key = EmojiKeyboardBottomSheet.emojiTypes[it.key]!!
                    val value = it.value.map { list -> list[0] }
                    key to value
                }.toMap()

        return resultMap
    }
}