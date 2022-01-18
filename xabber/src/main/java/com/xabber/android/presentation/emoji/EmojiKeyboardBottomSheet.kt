package com.xabber.android.presentation.emoji

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.xabber.android.R
import com.xabber.android.data.log.LogManager
import com.xabber.android.databinding.FragmentEmojiKeyboardBinding
import com.xabber.android.presentation.emoji.key.EmojiKeyAdapter
import com.xabber.android.presentation.emoji.type.EmojiTypeAdapter
import com.xabber.android.presentation.util.setFragmentResult
import java.io.*

class EmojiKeyboardBottomSheet : BottomSheetDialogFragment() {

    private val binding by viewBinding(FragmentEmojiKeyboardBinding::bind)
    private var keysAdapter: EmojiKeyAdapter? = null
    private var typesAdapter: EmojiTypeAdapter? = null
    private lateinit var dataset: Map<Int, List<String>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_emoji_keyboard, container, false)

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

        dataset = getDataset()
        with(binding) {
            with(recyclerViewKeys) {
                adapter = EmojiKeyAdapter {
                    onEmojiClick(it)
                }.also { keysAdapter = it }
            }
            keysAdapter!!.submitList(dataset[R.drawable.smileysandpeople])
            with(recyclerViewKeysTypes) {
                adapter = EmojiTypeAdapter {
                    onEmojiTypeClick(it)
                }.also { typesAdapter = it }
            }
            typesAdapter!!.submitList(dataset.keys.toMutableList())
        }
    }

    override fun onDestroy() {
        keysAdapter = null
        typesAdapter = null
        super.onDestroy()
    }

    private fun onEmojiClick(emoji: String) {
        setFragmentResult(
            "EMOJI", bundleOf(
                "qwe" to emoji
            )
        )
        dismiss()
    }

    private fun onEmojiTypeClick(emojiType: Int) {
        keysAdapter!!.submitList(dataset[emojiType])
    }

    private fun getDataset(): Map<Int, List<String>> {
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
        val collectionType = object : TypeToken<List<EmojiType>>() {}.type
        val dataset: List<EmojiType> =
            Gson().fromJson(jsonString, collectionType)

        val resultMap: Map<Int, List<String>> =
            dataset.toMap()
                .map {
                    val key = emojiTypes[it.key]!!
                    val value = it.value.map { list -> list[0] }
                    key to value
                }.toMap()

        return resultMap
    }

    companion object {

        val emojiTypes = mapOf(
            "smileysAndPeople" to R.drawable.smileysandpeople,
            "animalsAndNature" to R.drawable.animalsandnature,
            "foodAndDrink" to R.drawable.foodanddrink,
            "activity" to R.drawable.activity,
            "travelAndPlaces" to R.drawable.travelandplaces,
            "objects" to R.drawable.objects,
            "symbols" to R.drawable.symbols,
            "flags" to R.drawable.flags,
        )
    }
}

data class EmojiType(
    @SerializedName("emojis")
    val list: List<List<String>>,
    @SerializedName("type")
    val name: String
)

fun List<EmojiType>.toMap(): Map<String, List<List<String>>> {
    val map = this.map {
        it.name to it.list
    }.toMap()

    return map
}