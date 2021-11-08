package com.xabber.android.ui.adapter.chat

import android.content.res.ColorStateList
import android.os.Build
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateFormat
import android.text.style.QuoteSpan
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.StyleRes
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.AttachmentRealmObject
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.extension.groups.GroupPrivacyType
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager
import com.xabber.android.data.extension.references.mutable.voice.VoiceManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.ui.adapter.FilesAdapter
import com.xabber.android.ui.adapter.FilesAdapter.FileListListener
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.text.ClickTagHandler
import com.xabber.android.ui.text.CustomQuoteSpan
import com.xabber.android.ui.text.getDateStringForMessage
import com.xabber.android.ui.text.getDecodedSpannable
import com.xabber.android.ui.widget.CorrectlyTouchEventTextView
import com.xabber.android.ui.widget.CustomFlexboxLayout
import com.xabber.android.ui.widget.ImageGrid
import io.realm.RealmList
import io.realm.Sort
import rx.subscriptions.CompositeSubscription
import java.util.*

open class MessageVH(
    itemView: View,
    private val listener: MessageClickListener,
    private val longClickListener: MessageLongClickListener,
    private val fileListener: FileListener?,
    @StyleRes appearance: Int
) : BasicMessageVH(itemView, appearance), View.OnClickListener, FileListListener,
    View.OnLongClickListener {

    var isUnread = false
    var messageId: String? = null

    private val subscriptions = CompositeSubscription()

    protected val messageTime: TextView = itemView.findViewById(R.id.message_time)
    protected val messageHeader: TextView = itemView.findViewById(R.id.message_sender_tv)
    protected val messageBalloon: View = itemView.findViewById(R.id.message_balloon)
    protected val messageShadow: View = itemView.findViewById(R.id.message_shadow)
    protected val statusIcon: ImageView = itemView.findViewById(R.id.message_status_icon)
    protected val messageInfo: View = itemView.findViewById(R.id.message_info)
    private val flexboxLayout: CustomFlexboxLayout = itemView.findViewById(R.id.message_flex_layout)
    protected val forwardedMessagesRV: RecyclerView = itemView.findViewById(R.id.forwardedRecyclerView)
    protected val messageFileInfo: TextView = itemView.findViewById(R.id.message_file_info)
    protected val progressBar: ProgressBar = itemView.findViewById(R.id.message_progress_bar)
    private val rvFileList: RecyclerView = itemView.findViewById(R.id.file_list_rv)
    private val imageGridContainer: FrameLayout = itemView.findViewById(R.id.image_grid_container_fl)

    //todo there are duplicated views! (or else triplicated!)
    private val messageStatusLayout: LinearLayoutCompat = itemView.findViewById(R.id.message_bottom_status)
    protected val bottomMessageTime: TextView = messageStatusLayout.findViewById(R.id.message_time)
    protected var bottomStatusIcon: ImageView = messageStatusLayout.findViewById(R.id.message_status_icon)

    private val uploadProgressBar: ProgressBar? = itemView.findViewById(R.id.uploadProgressBar)
    private val ivCancelUpload: ImageButton? = itemView.findViewById(R.id.ivCancelUpload)

    private var imageCount = 0
    private var fileCount = 0

    interface FileListener {
        fun onImageClick(messagePosition: Int, attachmentPosition: Int, messageUID: String?)
        fun onFileClick(messagePosition: Int, attachmentPosition: Int, messageUID: String?)
        fun onVoiceClick(
            messagePosition: Int,
            attachmentPosition: Int,
            attachmentId: String?,
            messageUID: String?,
            timestamp: Long?
        )

        fun onFileLongClick(attachmentRealmObject: AttachmentRealmObject?, caller: View?)
        fun onDownloadCancel()
        fun onUploadCancel()
        fun onDownloadError(error: String?)
    }

    interface MessageClickListener {
        fun onMessageClick(caller: View, position: Int)
    }

    interface MessageLongClickListener {
        fun onLongMessageClick(position: Int)
    }

    open fun bind(messageRealmObject: MessageRealmObject, vhExtraData: MessageVhExtraData) {
        val chat = ChatManager.getInstance().getChat(
            messageRealmObject.account, messageRealmObject.user
        )

        // groupchat
        if (vhExtraData.groupMember != null) {
            if (!vhExtraData.groupMember.isMe) {
                val user = vhExtraData.groupMember
                messageHeader.text = user.nickname
                messageHeader.setTextColor(
                    ColorManager.changeColor(
                        ColorGenerator.MATERIAL.getColor(user.nickname),
                        0.8f
                    )
                )
                messageHeader.visibility = View.VISIBLE
            } else if (chat is GroupChat && chat.privacyType === GroupPrivacyType.INCOGNITO) {
                val user = vhExtraData.groupMember
                messageHeader.text = user.nickname
                messageHeader.setTextColor(
                    ColorManager.changeColor(
                        ColorGenerator.MATERIAL.getColor(user.nickname),
                        0.8f
                    )
                )
                messageHeader.visibility = View.VISIBLE
            }
        } else {
            messageHeader.visibility = View.GONE
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
                messageTextTv.setTextColor(itemView.context.getColor(R.color.grey_200))
            } else {
                messageTextTv.setTextColor(itemView.context.getColor(R.color.black))
            }
        }

        // Added .concat("&zwj;") and .concat(String.valueOf(Character.MIN_VALUE)
        // to avoid click by empty space after ClickableSpan
        // Try to decode to avoid ugly non-english links
        if (messageRealmObject.markupText != null && messageRealmObject.markupText.isNotEmpty()) {
            val spannable = Html.fromHtml(
                messageRealmObject.markupText.trim { it <= ' ' }.replace("\n", "<br/>") + "&zwj;",
                null,
                ClickTagHandler(
                    itemView.context, messageRealmObject.account
                )
            ) as SpannableStringBuilder
            val color: Int = if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                    ColorManager.getInstance().accountPainter.getAccountMainColor(
                        messageRealmObject.account
                    )
                } else {
                    ColorManager.getInstance().accountPainter.getAccountSendButtonColor(
                        messageRealmObject.account
                    )
                }
            modifySpannableWithCustomQuotes(
                spannable,
                itemView.context.resources.displayMetrics,
                color
            )
            messageTextTv.setText(spannable, TextView.BufferType.SPANNABLE)
        } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                messageTextTv.setText(
                    getDecodedSpannable(messageRealmObject.text.trim { it <= ' ' } + Character.MIN_VALUE.toString()),
                    TextView.BufferType.SPANNABLE
                )
            } else {
                messageTextTv.text =
                    messageRealmObject.text.trim { it <= ' ' } + Character.MIN_VALUE.toString()
            }
        }

        if (messageTextTv.text.isNotEmpty()) {
            messageStatusLayout.visibility = View.GONE
        }

        if (messageRealmObject.hasAttachments() || messageRealmObject.hasForwardedMessages()) {
            flexboxLayout.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } else {
            flexboxLayout.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        messageTextTv.movementMethod = CorrectlyTouchEventTextView.LocalLinkMovementMethod

        // set unread status
        isUnread = vhExtraData.isUnread

        // set date
        needDate = vhExtraData.isNeedDate
        date = getDateStringForMessage(messageRealmObject.timestamp)
        if (!vhExtraData.isNeedName) {
            messageHeader.visibility = View.GONE
        }

        if (messageRealmObject.text.isNullOrEmpty()
            && messageRealmObject.attachmentRealmObjects.none { it.isImage }
        ) {
            messageStatusLayout.visibility = View.VISIBLE
        }  else {
            messageStatusLayout.visibility = View.GONE
        }

        // setup CHECKED
        if (vhExtraData.isChecked) {
            itemView.setBackgroundColor(
                itemView.context.resources.getColor(R.color.unread_messages_background)
            )
        } else {
            itemView.background = null
        }

        setupTime(messageRealmObject)
        setupImageOrFile(messageRealmObject, vhExtraData)
    }

    protected fun setupTime(messageRealmObject: MessageRealmObject) {
        var time = getTimeText(Date(messageRealmObject.timestamp))
        messageRealmObject.delayTimestamp?.let {
            val delay = itemView.context.getString(
                if (messageRealmObject.isIncoming) R.string.chat_delay else R.string.chat_typed,
                getTimeText(Date(it))
            )
            time += " ($delay)"
        }
        messageRealmObject.editedTimestamp?.let {
            time += itemView.context.getString(
                R.string.edited,
                getTimeText(Date(it))
            )
        }
        messageTime.text = time
        bottomMessageTime.text = time
    }

    private fun setupImageOrFile(messageRealmObject: MessageRealmObject, vhExtraData: MessageVhExtraData) {
        rvFileList.visibility = View.GONE
        imageGridContainer.removeAllViews()
        imageGridContainer.visibility = View.GONE
        if (messageRealmObject.hasAttachments()) {
            setUpImage(messageRealmObject, vhExtraData)
            setUpFile(messageRealmObject.attachmentRealmObjects, vhExtraData)
        }
    }

    private fun setUpImage(message: MessageRealmObject, messageVhExtraData: MessageVhExtraData) {
        if (!SettingsManager.connectionLoadImages()) {
            return
        }
        message.attachmentRealmObjects
            ?.filter { it.isImage }
            ?.also { imageCount = it.size }
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                RealmList<AttachmentRealmObject>().apply {
                    addAll(it)
                }
            }
            ?.let {
                val gridBuilder = ImageGrid()
                val imageGridView = gridBuilder.inflateView(imageGridContainer, it.size)
                gridBuilder.bindView(imageGridView, message, this, messageVhExtraData) { v: View ->
                    onLongClick(v)
                    true
                }
                imageGridContainer.addView(imageGridView)
                imageGridContainer.visibility = View.VISIBLE
            }
    }

    private fun setUpFile(
        attachmentRealmObjects: RealmList<AttachmentRealmObject>, vhExtraData: MessageVhExtraData
    ) {
        attachmentRealmObjects.filter { !it.isImage }
            .also { fileCount = it.size }
            .takeIf { it.isNotEmpty() }
            ?.let {
                RealmList<AttachmentRealmObject>().apply { addAll(it) }
            }
            ?.let {
                rvFileList.apply {
                    layoutManager = LinearLayoutManager(itemView.context)
                    adapter = FilesAdapter(it, vhExtraData.mainMessageTimestamp, this@MessageVH)
                    visibility = View.VISIBLE
                }
            }
    }

    /** File list Listener  */
    override fun onFileClick(attachmentPosition: Int) {
        val messagePosition = adapterPosition
        if (messagePosition == RecyclerView.NO_POSITION) {
            LogManager.w(this, "onClick: no position")
            return
        }
        fileListener?.onFileClick(messagePosition, attachmentPosition, messageId)
    }

    override fun onVoiceClick(
        attachmentPosition: Int,
        attachmentId: String,
        saved: Boolean,
        mainMessageTimestamp: Long
    ) {
        val messagePosition = adapterPosition
        if (messagePosition == RecyclerView.NO_POSITION) {
            LogManager.w(this, "onClick: no position")
            return
        }
        if (!saved) {
            fileListener?.onVoiceClick(
                messagePosition,
                attachmentPosition,
                attachmentId,
                messageId,
                mainMessageTimestamp
            )
        } else {
            VoiceManager.getInstance().voiceClicked(
                messageId, attachmentPosition, mainMessageTimestamp
            )
        }
    }

    override fun onVoiceProgressClick(
        attachmentPosition: Int,
        attachmentId: String,
        timestamp: Long,
        current: Int,
        max: Int
    ) {
        val messagePosition = adapterPosition
        if (messagePosition == RecyclerView.NO_POSITION) {
            LogManager.w(this, "onClick: no position")
            return
        }
        VoiceManager.getInstance().seekAudioPlaybackTo(attachmentId, timestamp, current, max)
    }

    override fun onFileLongClick(attachmentRealmObject: AttachmentRealmObject, caller: View) {
        fileListener?.onFileLongClick(attachmentRealmObject, caller)
    }

    override fun onDownloadCancel() {
        fileListener?.onDownloadCancel()
    }

    override fun onDownloadError(error: String) {
        fileListener?.onDownloadError(error)
    }

    override fun onClick(v: View) {
        val adapterPosition = adapterPosition
        if (adapterPosition == RecyclerView.NO_POSITION) {
            LogManager.w(this, "onClick: no position")
            return
        }
        when (v.id) {
            R.id.ivImage0 -> fileListener?.onImageClick(adapterPosition, 0, messageId)
            R.id.ivImage1 -> fileListener?.onImageClick(adapterPosition, 1, messageId)
            R.id.ivImage2 -> fileListener?.onImageClick(adapterPosition, 2, messageId)
            R.id.ivImage3 -> fileListener?.onImageClick(adapterPosition, 3, messageId)
            R.id.ivImage4 -> fileListener?.onImageClick(adapterPosition, 4, messageId)
            R.id.ivImage5 -> fileListener?.onImageClick(adapterPosition, 5, messageId)
            R.id.ivCancelUpload -> fileListener?.onUploadCancel()
            else -> listener.onMessageClick(messageBalloon, adapterPosition)
        }
    }

    /** Upload progress subscription  */
    protected fun subscribeForUploadProgress() {
        fun setUpProgress(progressData: HttpFileUploadManager.ProgressData?) {
            if (progressData != null && messageId == progressData.messageId) {
                if (progressData.isCompleted) {
                    showProgress(false)
                    showFileProgressModified(rvFileList, fileCount, fileCount)
                    showProgressModified(false, 0, imageCount)
                } else if (progressData.error != null) {
                    showProgress(false)
                    showFileProgressModified(rvFileList, fileCount, fileCount)
                    showProgressModified(false, 0, imageCount)
                    fileListener?.onDownloadError(progressData.error)
                } else {
                    showProgress(true)
                    messageFileInfo.setText(R.string.message_status_uploading)
                    if (progressData.progress <= imageCount) {
                        showProgressModified(true, progressData.progress - 1, imageCount)
                    }
                    if (progressData.progress - imageCount <= fileCount) {
                        showFileProgressModified(
                            rvFileList,
                            progressData.progress - imageCount,
                            progressData.fileCount - imageCount
                        )
                    }
                }
            } else {
                showProgress(false)
                showFileProgressModified(rvFileList, fileCount, fileCount)
                showProgressModified(false, 0, imageCount)
            }
        }

        subscriptions.add(
            HttpFileUploadManager.getInstance()
                .subscribeForProgress()
                .doOnNext { progressData -> setUpProgress(progressData) }
                .subscribe()
        )
    }

    protected fun unsubscribeAll() {
        subscriptions.clear()
    }

    private fun showProgress(show: Boolean) {
        messageFileInfo.visibility = if (show) View.VISIBLE else View.GONE
        messageTime.visibility = if (show) View.GONE else View.VISIBLE
        bottomMessageTime.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showFileProgressModified(view: RecyclerView, startAt: Int, endAt: Int) {
        for (i in 0 until startAt) {
            showFileUploadProgress(view.getChildAt(i), false)
        }
        for (j in startAt.coerceAtLeast(0) until endAt) {
            showFileUploadProgress(view.getChildAt(j), true)
        }
    }

    private fun showFileUploadProgress(view: View, show: Boolean) {
        view.findViewById<ProgressBar>(R.id.uploadProgressBar)?.visibility =
            if (show) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }


    //todo yep, these methods must be in ImageGrid 🗿
    private fun showProgressModified(show: Boolean, current: Int, last: Int) {
        fun getProgressView(view: View, index: Int): ProgressBar {
            return when (index) {
                1 -> view.findViewById(R.id.uploadProgressBar1)
                2 -> view.findViewById(R.id.uploadProgressBar2)
                3 -> view.findViewById(R.id.uploadProgressBar3)
                4 -> view.findViewById(R.id.uploadProgressBar4)
                5 -> view.findViewById(R.id.uploadProgressBar5)
                else -> view.findViewById(R.id.uploadProgressBar0)
            }
        }

        fun getImageShadow(view: View, index: Int): ImageView {
            return when (index) {
                1 -> view.findViewById(R.id.ivImage1Shadow)
                2 -> view.findViewById(R.id.ivImage2Shadow)
                3 -> view.findViewById(R.id.ivImage3Shadow)
                4 -> view.findViewById(R.id.ivImage4Shadow)
                5 -> view.findViewById(R.id.ivImage5Shadow)
                else -> view.findViewById(R.id.ivImage0Shadow)
            }
        }

        if (show) {
            for (i in 0 until current) {
                getProgressView(imageGridContainer, i).visibility = View.GONE
                getImageShadow(imageGridContainer, i).visibility = View.GONE
            }
            for (j in current until last) {
                getProgressView(imageGridContainer, j).visibility = View.VISIBLE
                getImageShadow(imageGridContainer, j).visibility = View.VISIBLE
            }
        } else {
            for (i in 0 until last) {
                getProgressView(imageGridContainer, i).visibility = View.GONE
                getImageShadow(imageGridContainer, i).visibility = View.GONE
            }
        }
    }

    fun setupForwarded(messageRealmObject: MessageRealmObject, vhExtraData: MessageVhExtraData) {
        val forwardedIDs = messageRealmObject.forwardedIdsAsArray
        if (!forwardedIDs.contains(null)) {
            DatabaseManager.getInstance().defaultRealmInstance
                .where(MessageRealmObject::class.java)
                .`in`(MessageRealmObject.Fields.PRIMARY_KEY, forwardedIDs)
                .findAll()
                .sort(MessageRealmObject.Fields.TIMESTAMP, Sort.ASCENDING)
                .takeIf { it.isNotEmpty() }
                ?.let { forwardedMessages ->
                    forwardedMessagesRV.apply {
                        layoutManager = LinearLayoutManager(itemView.context)
                        adapter = ForwardedAdapter(forwardedMessages, vhExtraData)
                        visibility = View.VISIBLE
                    }
                }
        }
    }

    override fun onLongClick(v: View): Boolean {
        val adapterPosition = adapterPosition
        return if (adapterPosition == RecyclerView.NO_POSITION) {
            LogManager.w(this, "onClick: no position")
            false
        } else {
            longClickListener.onLongMessageClick(adapterPosition)
            true
        }
    }

    protected fun setUpMessageBalloonBackground(view: View, colorList: ColorStateList?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.background.setTintList(colorList)
        } else {
            val wrapDrawable = DrawableCompat.wrap(view.background)
            DrawableCompat.setTintList(wrapDrawable, colorList)
            val pL = view.paddingLeft
            val pT = view.paddingTop
            val pR = view.paddingRight
            val pB = view.paddingBottom
            view.background = wrapDrawable
            view.setPadding(pL, pT, pR, pB)
        }
    }

    private fun modifySpannableWithCustomQuotes(
        spannable: SpannableStringBuilder, displayMetrics: DisplayMetrics, color: Int
    ) {
        for (span in spannable.getSpans(0, spannable.length, QuoteSpan::class.java).reversed()){
            var spanEnd = spannable.getSpanEnd(span)
            var spanStart = spannable.getSpanStart(span)

            spannable.removeSpan(span)

            if (spanEnd < 0 || spanStart < 0) {
                break
            }

            var newlineCount = 0
            if ('\n' == spannable[spanEnd]) {
                newlineCount++
                if (spanEnd + 1 < spannable.length && '\n' == spannable[spanEnd + 1]) {
                    newlineCount++
                }
                if ('\n' == spannable[spanEnd - 1]) {
                    newlineCount++
                }
            }
            when (newlineCount) {
                3 -> {
                    spannable.delete(spanEnd - 1, spanEnd + 1)
                    spanEnd -= 2
                }
                2 -> {
                    spannable.delete(spanEnd, spanEnd + 1)
                    spanEnd--
                }
            }

            if (spanStart > 1 && '\n' == spannable[spanStart - 1]) {
                if ('\n' == spannable[spanStart - 2]) {
                    spannable.delete(spanStart - 2, spanStart - 1)
                    spanStart--
                }
            }

            spannable.setSpan(
                CustomQuoteSpan(color, displayMetrics),
                spanStart,
                spanEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            var current: Char
            var waitForNewLine = false
            var j = spanStart
            while (j < spanEnd) {
                if (j >= spannable.length) {
                    break
                }
                current = spannable[j]
                waitForNewLine =
                    if (waitForNewLine && current != '\n') {
                        j++
                        continue
                    } else {
                        false
                    }

                if (current == '>') {
                    spannable.delete(j, j + 1)
                    j--
                    waitForNewLine = true
                }
                j++
            }
        }
    }

    private fun getTimeText(timeStamp: Date): String {
        return DateFormat.getTimeFormat(Application.getInstance()).format(timeStamp)
    }

    init {
        ivCancelUpload?.setOnClickListener(this)
        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)
    }

}