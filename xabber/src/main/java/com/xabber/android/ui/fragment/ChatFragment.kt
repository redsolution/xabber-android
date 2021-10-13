package com.xabber.android.ui.fragment

import android.annotation.SuppressLint
import android.content.*
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.text.*
import android.util.TypedValue
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.connection.BaseIqResultUiListener
import com.xabber.android.data.connection.ConnectionState
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.repositories.MessageRepository
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.archive.MessageArchiveManager.loadNextMessagesPortionInChat
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.data.extension.chat_state.ChatStateManager
import com.xabber.android.data.extension.groups.GroupInviteManager.getLastInvite
import com.xabber.android.data.extension.groups.GroupInviteManager.hasActiveIncomingInvites
import com.xabber.android.data.extension.groups.GroupsManager.enableSendingPresenceToGroup
import com.xabber.android.data.extension.groups.GroupsManager.sendPinMessageRequest
import com.xabber.android.data.extension.groups.GroupsManager.sendUnPinMessageRequest
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager
import com.xabber.android.data.extension.references.mutable.voice.VoiceManager
import com.xabber.android.data.extension.references.mutable.voice.VoiceManager.PublishAudioProgress.AudioInfo
import com.xabber.android.data.extension.references.mutable.voice.VoiceMessagePresenterManager
import com.xabber.android.data.extension.retract.RetractManager
import com.xabber.android.data.extension.retract.RetractManager.isSupported
import com.xabber.android.data.extension.retract.RetractManager.sendMissedChangesInRemoteArchiveForChatRequest
import com.xabber.android.data.extension.retract.RetractManager.sendRemoteArchiveRetractVersionRequest
import com.xabber.android.data.extension.retract.RetractManager.sendRetractMessagesRequest
import com.xabber.android.data.extension.vcard.VCardManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.ClipManager
import com.xabber.android.data.message.ForwardManager
import com.xabber.android.data.message.MessageManager
import com.xabber.android.data.message.MessageStatus
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.message.chat.RegularChat
import com.xabber.android.data.notification.NotificationManager
import com.xabber.android.data.roster.PresenceManager.clearSubscriptionRequestNotification
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.*
import com.xabber.android.ui.activity.ChatActivity
import com.xabber.android.ui.activity.ContactViewerActivity
import com.xabber.android.ui.activity.GroupchatMemberActivity.Companion.createIntentForGroupchatAndMemberId
import com.xabber.android.ui.activity.MessagesActivity
import com.xabber.android.ui.adapter.chat.IncomingMessageVH.BindListener
import com.xabber.android.ui.adapter.chat.IncomingMessageVH.OnMessageAvatarClickListener
import com.xabber.android.ui.adapter.chat.MessageVH.MessageClickListener
import com.xabber.android.ui.adapter.chat.MessagesAdapter
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.dialog.ChatExportDialogFragment
import com.xabber.android.ui.helper.*
import com.xabber.android.ui.text.CustomQuoteSpan
import com.xabber.android.ui.widget.*
import com.xabber.android.ui.widget.BottomMessagesPanel.Purposes
import com.xabber.android.ui.widget.PlayerVisualizerView.onProgressTouch
import com.xabber.android.utils.*
import com.xabber.xmpp.chat_state.ChatStateSubtype
import github.ankushsachdeva.emojicon.EmojiconsPopup
import github.ankushsachdeva.emojicon.emoji.Emojicon
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.packet.XMPPError
import rx.Subscription
import java.util.*
import java.util.concurrent.TimeUnit

class ChatFragment : FileInteractionFragment(), MessageClickListener,
    MessagesAdapter.AdapterListener, OnAccountChangedListener, BindListener,
    OnMessageAvatarClickListener, OnNewIncomingMessageListener, OnNewMessageListener,
    OnGroupPresenceUpdatedListener, OnMessageUpdatedListener, OnLastHistoryLoadStartedListener,
    OnLastHistoryLoadFinishedListener, OnAuthAskListener, OnLastHistoryLoadErrorListener,
    BaseIqResultUiListener, OnConnectionStateChangedListener {

    private var bottomMessagesPanel: BottomMessagesPanel? = null
    private var topPinnedMessagePanel: PinnedMessagePanel? = null
    private var topPanel: ChatFragmentTopPanel? = null
    private lateinit var interactionView: View

    private var isInputEmpty = true
    private var skipOnTextChanges = false

    private lateinit var inputPanel: FrameLayout
    private lateinit var inputLayout: LinearLayout
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var attachButton: ImageButton
    private lateinit var recordButton: ImageButton

    private lateinit var rootView: View
    private lateinit var lastHistoryProgressBar: ProgressBar
    private lateinit var stubInviteFakeMessage: ViewStub

    private var blockedView: TextView? = null
    private lateinit var stubNotify: ViewStub
    private var notifyLayout: RelativeLayout? = null

    private lateinit var realmRecyclerView: RecyclerView
    private lateinit var chatMessageAdapter: MessagesAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var replySwipeCallback: ReplySwipeCallback

    private lateinit var tvNewReceivedCount: TextView
    private lateinit var btnScrollDown: RelativeLayout

    private val chat: AbstractChat
        get() {
            return ChatManager.getInstance().getChat(accountJid, contactJid)
                ?: ChatManager.getInstance().createRegularChat(accountJid, contactJid)
        }

    /**
     * Used to show messages of only one specified member of group
     */
    private var memberId: String? = null

    private var accountColor = 0
    private var userIsBlocked = false

    // OMG Voice message recorder variables!!!!
    private val handler = Handler()
    private var currentVoiceRecordingState = VoiceRecordState.NotRecording
    private var recordSaveAllowed = false
    private var recordingPath: String? = null

    private var lockViewHeightSize = 0
    private var lockViewMarginBottom = 0
    private var fabMicViewHeightSize = 0
    private var fabMicViewMarginBottom = 0
    private var rootViewHeight = 0f

    private lateinit var voiceMessageRecorderLayout: RelativeLayout
    private lateinit var recordButtonExpanded: FloatingActionButton
    private lateinit var recordLockView: View
    private var recordLockImage: ImageView? = null
    private var recordLockChevronImage: ImageView? = null
    private lateinit var recordTimer: Chronometer
    private var slideToCancelLayout: LinearLayout? = null
    private var cancelRecordingLayout: LinearLayout? = null
    private val postAnimation = Runnable { closeVoiceRecordPanel() }
    private var recordingPresenterLayout: LinearLayout? = null
    private lateinit var recordingPresenter: PlayerVisualizerView
    private var recordingPlayButton: ImageButton? = null
    private var recordingDeleteButton: ImageButton? = null
    private lateinit var recordingPresenterSendButton: ImageButton
    private lateinit var recordingPresenterDuration: TextView
    private var audioProgressSubscription: Subscription? = null
    private var listener: ChatViewerFragmentListener? = null
    private var stopTypingTimer: Timer? = Timer()
    private val timer = Runnable {
        changeStateOfInputViewButtonsTo(false)
        recordLockView.visibility = View.VISIBLE
        shortVibrate()
        recordButtonExpanded.show()
        recordLockView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in_200))
        recordLockView.animate()
            .y(rootViewHeight - (lockViewMarginBottom + lockViewHeightSize))
            .setDuration(300)
            .start()
        beginTimer(currentVoiceRecordingState == VoiceRecordState.InitiatedRecording)
    }

    private fun shortVibrate() {
        rootView.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    /* Fragment lifecycle methods */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountJid = arguments?.getParcelable(ARGUMENT_ACCOUNT)
            ?: throw IllegalArgumentException("ChatFragment arguments must contains an accountJid")
        contactJid = arguments?.getParcelable(ARGUMENT_USER)
            ?: throw IllegalArgumentException("ChatFragment arguments must contains an contactJid")

        arguments?.getString(ARGUMENT_MEMBER_ID)?.let {
            memberId = it
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        accountColor = ColorManager.getInstance().accountPainter.getAccountMainColor(accountJid)
        tvNewReceivedCount = view.findViewById(R.id.tvNewReceivedCount)

        btnScrollDown = view.findViewById(R.id.btnScrollDown)
        btnScrollDown.setOnClickListener {
            val unread = chat.unreadMessageCount
            val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
            if (unread == 0 || lastVisiblePosition + 2 >= chatMessageAdapter.itemCount - unread) {
                scrollDown()
            } else {
                scrollToFirstUnread(unread)
            }
        }

        rootView = view.findViewById(R.id.root_view)
        rootView.addOnLayoutChangeListener { view1: View, _: Int, _: Int, _: Int, _: Int, _: Int, topOld: Int, _: Int, bottomOld: Int ->
            val heightOld = bottomOld - topOld
            if (heightOld != view1.height) {
                rootViewHeight = view1.height.toFloat()
            }
        }
        inputPanel = view.findViewById(R.id.bottomContainer)

        sendButton = view.findViewById(R.id.button_send_message)
        sendButton.setColorFilter(ColorManager.getInstance().accountPainter.greyMain)

        attachButton = view.findViewById(R.id.button_attach)
        attachButton.setOnClickListener {
            onAttachButtonPressed()
            forwardIdsForAttachments(bottomMessagesPanel?.messagesIds)
        }

        recordButton = view.findViewById(R.id.button_record)
        recordButton.setOnTouchListener { _: View?, motionEvent: MotionEvent ->
            when (motionEvent.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> if (PermissionsRequester.requestRecordAudioPermissionIfNeeded(
                        activity, PERMISSIONS_REQUEST_RECORD_AUDIO
                    )
                ) if (PermissionsRequester.requestFileWritePermissionIfNeeded(
                        activity, PERMISSIONS_REQUEST_RECORD_AUDIO
                    )
                ) {
                    if (currentVoiceRecordingState == VoiceRecordState.NotRecording) {
                        recordButtonExpanded.setImageResource(R.drawable.ic_microphone)
                        recordSaveAllowed = false
                        slideToCancelLayout?.alpha = 1.0f
                        recordLockView.alpha = 1.0f
                        handler.postDelayed(record, 500)
                        handler.postDelayed(timer, 500)
                        currentVoiceRecordingState = VoiceRecordState.InitiatedRecording
                        activity?.lockScreenRotation(true)
                    }
                }
                MotionEvent.ACTION_UP -> if (currentVoiceRecordingState == VoiceRecordState.TouchRecording) {
                    sendVoiceMessage()
                    activity?.lockScreenRotation(false)
                } else if (currentVoiceRecordingState == VoiceRecordState.InitiatedRecording) {
                    handler.removeCallbacks(record)
                    handler.removeCallbacks(timer)
                    currentVoiceRecordingState = VoiceRecordState.NotRecording
                    activity?.lockScreenRotation(false)
                }
                MotionEvent.ACTION_MOVE -> {
                    //FAB movement
                    val lockParams =
                        recordLockChevronImage?.layoutParams as LinearLayout.LayoutParams
                    val yRecordDiff = rootViewHeight
                    -(fabMicViewHeightSize + fabMicViewMarginBottom) + motionEvent.y
                    val yLockDiff = rootViewHeight
                    -(lockViewMarginBottom + lockViewHeightSize) + motionEvent.y
                    if (currentVoiceRecordingState == VoiceRecordState.TouchRecording) {
                        when {
                            motionEvent.y > 0 -> {
                                recordButtonExpanded.animate()
                                    .y(rootViewHeight - (fabMicViewHeightSize + fabMicViewMarginBottom))
                                    .setDuration(0)
                                    .start()
                                recordLockView.animate()
                                    .y(rootViewHeight - (lockViewMarginBottom + lockViewHeightSize))
                                    .setDuration(0)
                                    .start()
                                recordLockChevronImage?.alpha = 1f
                            }
                            motionEvent.y > -200 -> { //200 = height to the "locked" state
                                recordButtonExpanded.animate()
                                    .y(yRecordDiff)
                                    .setDuration(0)
                                    .start()
                                recordLockView.animate()
                                    .y(yLockDiff)
                                    .setDuration(0)
                                    .start()

                                //lockParams.topMargin = (int) motionEvent.getY() / 3;
                                lockParams.topMargin = (motionEvent.y.toInt()
                                        * (recordLockChevronImage!!.height - recordLockImage!!.paddingTop)
                                        / 200)
                                recordLockChevronImage?.alpha = 1f + motionEvent.y / 200f
                                recordLockChevronImage?.layoutParams = lockParams
                            }
                            else -> {
                                currentVoiceRecordingState = VoiceRecordState.NoTouchRecording

                                //workaround for the https://issuetracker.google.com/issues/111316656 issue of
                                //the button's image not updating after setting a background tint manually.
                                recordButtonExpanded.hide()
                                recordButtonExpanded.setImageResource(R.drawable.ic_send_black_24dp)
                                recordButtonExpanded.show()
                                ////////////////////////////
                                recordButtonExpanded.animate()
                                    .y(rootViewHeight - (fabMicViewHeightSize + fabMicViewMarginBottom))
                                    .setDuration(100)
                                    .start()
                                recordLockView.animate()
                                    .y(rootViewHeight - (lockViewMarginBottom + lockViewHeightSize) + 50) // 50=temporary offset
                                    .setDuration(100)
                                    .start()
                                cancelRecordingLayout?.visibility = View.VISIBLE
                                recordLockImage?.setImageResource(R.drawable.ic_stop)
                                recordLockImage?.setPadding(0, 0, 0, 0)
                                recordLockImage?.setOnClickListener {
                                    if (currentVoiceRecordingState == VoiceRecordState.NoTouchRecording) {
                                        shortVibrate()
                                        stopRecording()
                                    }
                                }
                                lockParams.topMargin = -recordLockChevronImage!!.height
                                recordLockChevronImage?.layoutParams = lockParams
                                recordLockChevronImage?.alpha = 0f
                                shortVibrate()
                            }
                        }
                    }

                    //"Slide To Cancel" movement;
                    val alpha = 1f + motionEvent.x / 400f
                    if (currentVoiceRecordingState == VoiceRecordState.TouchRecording) {

                        if (motionEvent.x < 0) {
                            slideToCancelLayout?.animate()?.x(motionEvent.x)?.setDuration(0)
                                ?.start()
                        } else slideToCancelLayout?.animate()?.x(0f)?.setDuration(0)?.start()

                        slideToCancelLayout?.alpha = alpha

                        //since alpha and slide are tied together, we can cancel recording by checking transparency value
                        if (alpha <= 0) {
                            clearVoiceMessage()
                        }
                    }
                }
            }
            true
        }
        slideToCancelLayout = view.findViewById(R.id.slide_layout)
        cancelRecordingLayout = view.findViewById(R.id.cancel_record_layout)
        view.findViewById<TextView>(R.id.tv_cancel_recording).setOnClickListener {
            if (currentVoiceRecordingState == VoiceRecordState.NoTouchRecording) {
                clearVoiceMessage()
            }
        }
        recordingPresenterLayout = view.findViewById(R.id.recording_presenter_layout)

        view.findViewById<LinearLayout>(R.id.recording_playbar_layout)
            ?.background?.setColorFilter(accountColor, PorterDuff.Mode.SRC_IN)

        recordingPresenter = view.findViewById(R.id.voice_presenter_visualizer)
        recordingPresenter.setNotPlayedColor(Color.WHITE)
        recordingPresenter.setNotPlayedColorAlpha(127)
        recordingPresenter.setPlayedColor(Color.WHITE)

        recordingPlayButton = view.findViewById(R.id.voice_presenter_play)
        recordingDeleteButton = view.findViewById(R.id.voice_presenter_delete)
        recordingPresenterDuration = view.findViewById(R.id.voice_presenter_time)
        recordingPresenterSendButton = view.findViewById(R.id.voice_presenter_send)
        recordingPresenterSendButton.setColorFilter(accountColor)
        voiceMessageRecorderLayout = view.findViewById(R.id.record_layout)

        recordTimer = view.findViewById(R.id.chrRecordingTimer)
        recordTimer.setOnChronometerTickListener {
            recordSaveAllowed = SystemClock.elapsedRealtime() - recordTimer.base > 1000
        }

        recordButtonExpanded = view.findViewById(R.id.record_float_button)
        recordButtonExpanded.backgroundTintList = ColorStateList.valueOf(accountColor)
        recordButtonExpanded.setOnClickListener {
            if (currentVoiceRecordingState == VoiceRecordState.NoTouchRecording) {
                sendVoiceMessage()
            }
        }

        recordLockView = view.findViewById(R.id.record_lock_view)
        recordLockImage = view.findViewById(R.id.iv_record_lock)
        recordLockChevronImage = view.findViewById(R.id.iv_record_chevron_lock)

        lastHistoryProgressBar = view.findViewById(R.id.chat_last_history_progress_bar)

        // to avoid strange bug on some 4.x androids
        inputLayout = view.findViewById(R.id.input_layout)
        inputLayout.setBackgroundColor(ColorManager.getInstance().chatInputBackgroundColor)

        // interaction view
        interactionView = view.findViewById(R.id.interactionView)
        view.findViewById<View>(R.id.reply_tv).setOnClickListener {
            showBottomMessagesPanel(chatMessageAdapter.checkedItemIds.toList(), Purposes.FORWARDING)
            closeInteractionPanel()
        }
        view.findViewById<View>(R.id.reply_iv).setOnClickListener {
            showBottomMessagesPanel(chatMessageAdapter.checkedItemIds.toList(), Purposes.FORWARDING)
            closeInteractionPanel()
        }
        view.findViewById<View>(R.id.forward_iv).setOnClickListener {
            openChooserForForward(chatMessageAdapter.checkedItemIds)
        }
        view.findViewById<View>(R.id.forward_tv).setOnClickListener {
            openChooserForForward(chatMessageAdapter.checkedItemIds)
        }
        sendButton.setOnClickListener { sendMessage() }
        setUpInputView(view)
        setUpEmoji(view)

        realmRecyclerView = view.findViewById(R.id.chat_messages_recycler_view)
        layoutManager = LinearLayoutManager(activity)
        realmRecyclerView.layoutManager = layoutManager
        layoutManager.stackFromEnd = true
        realmRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0) {
                    requestToLoadHistoryIfNeed()
                }
                showScrollDownButtonIfNeed()
            }
        })
        stubNotify = view.findViewById(R.id.stubNotify)
        stubInviteFakeMessage = view.findViewById(R.id.stubInvite)
        NotificationManager.getInstance().removeMessageNotification(accountJid, contactJid)
        setChat(accountJid, contactJid)
        if (savedInstanceState != null) {
            val voiceRecordPath = savedInstanceState.getString(VOICE_MESSAGE)
            ignoreReceiver = savedInstanceState.getBoolean(VOICE_MESSAGE_RECEIVER_IGNORE)
            if (voiceRecordPath != null) {
                recordingPath = voiceRecordPath
                currentVoiceRecordingState = VoiceRecordState.StoppedRecording
                changeStateOfInputViewButtonsTo(false)
                voiceMessageRecorderLayout.visibility = View.VISIBLE
                setUpVoiceMessagePresenter()
            }
            savedInstanceState.getStringArray(BOTTOM_MESSAGE_PANEL_MESSAGES_SAVED_STATE)?.let { ids ->
                (savedInstanceState.getSerializable(BOTTOM_MESSAGE_PANEL_PURPOSE_SAVED_STATE) as? Purposes)?.let { purpose ->
                    showBottomMessagesPanel(listOf(*ids), purpose)
                    setUpInputViewButtons()
                }
            }
        }
        if (SettingsManager.chatsShowBackground()) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
                view.setBackgroundResource(R.color.black)
            } else {
                view.setBackgroundResource(R.drawable.chat_background_repeat)
            }
        } else {
            view.setBackgroundColor(ColorManager.getInstance().chatBackgroundColor)
        }
        rootView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    } else {
                        rootView.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    }
                    //measurements for the recording layout animations.
                    rootViewHeight = rootView.height.toFloat()
                    lockViewHeightSize = recordLockView.height
                    lockViewMarginBottom =
                        (recordLockView.layoutParams as RelativeLayout.LayoutParams).bottomMargin
                    fabMicViewHeightSize = recordButtonExpanded.height
                    fabMicViewMarginBottom =
                        (recordButtonExpanded.layoutParams as RelativeLayout.LayoutParams).bottomMargin
                }
            }
        )

        setupPinnedMessageView()
        return view
    }

    override fun onResume() {
        super.onResume()

        ChatStateManager.getInstance().onChatOpening(accountJid, contactJid)
        (chat as? GroupChat)?.let { enableSendingPresenceToGroup(it, true) }

        restoreState()

        updateContact()

        showHideNotifyIfNeed()
        ChatManager.getInstance().setVisibleChat(chat)

        Application.getInstance().addUIListener(OnAccountChangedListener::class.java, this)
        Application.getInstance().addUIListener(OnNewIncomingMessageListener::class.java, this)
        Application.getInstance().addUIListener(OnNewMessageListener::class.java, this)
        Application.getInstance().addUIListener(OnGroupPresenceUpdatedListener::class.java, this)
        Application.getInstance().addUIListener(OnMessageUpdatedListener::class.java, this)
        Application.getInstance().addUIListener(OnLastHistoryLoadStartedListener::class.java, this)
        Application.getInstance().addUIListener(OnLastHistoryLoadFinishedListener::class.java, this)
        Application.getInstance().addUIListener(OnAuthAskListener::class.java, this)
        Application.getInstance().addUIListener(OnConnectionStateChangedListener::class.java, this)

        requestToLoadHistoryIfNeed()

        (chat as? GroupChat)?.let {
            val retractVersion = it.retractVersion
            if (retractVersion == null || retractVersion.isEmpty()) {
                sendRemoteArchiveRetractVersionRequest(accountJid, contactJid)
            } else {
                sendMissedChangesInRemoteArchiveForChatRequest(accountJid, contactJid)
            }
        }

    }

    override fun onPause() {
        super.onPause()
        ChatStateManager.getInstance().onPaused(accountJid, contactJid)

        (chat as? GroupChat)?.let { enableSendingPresenceToGroup(it, false) }

        saveState()

        if (currentVoiceRecordingState == VoiceRecordState.NoTouchRecording
            || currentVoiceRecordingState == VoiceRecordState.TouchRecording
        ) {
            stopRecording()
        }

        ChatManager.getInstance().removeVisibleChat()
        Application.getInstance().removeUIListener(OnAccountChangedListener::class.java, this)
        Application.getInstance().removeUIListener(OnNewIncomingMessageListener::class.java, this)
        Application.getInstance().removeUIListener(OnNewMessageListener::class.java, this)
        Application.getInstance().removeUIListener(OnGroupPresenceUpdatedListener::class.java, this)
        Application.getInstance().removeUIListener(OnMessageUpdatedListener::class.java, this)
        Application.getInstance().removeUIListener(OnAuthAskListener::class.java, this)
        Application.getInstance().removeUIListener(
            OnLastHistoryLoadStartedListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnLastHistoryLoadFinishedListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnConnectionStateChangedListener::class.java, this
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatMessageAdapter.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(VOICE_MESSAGE, recordingPath)
        outState.putBoolean(VOICE_MESSAGE_RECEIVER_IGNORE, ignoreReceiver)
        bottomMessagesPanel?.let { panel ->
            outState.putStringArray(BOTTOM_MESSAGE_PANEL_MESSAGES_SAVED_STATE, panel.messagesIds.toTypedArray())
            outState.putSerializable(BOTTOM_MESSAGE_PANEL_PURPOSE_SAVED_STATE, panel.purpose)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as ChatViewerFragmentListener
            listener?.registerChatFragment(this)
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement ChatViewerFragmentListener")
        }

        registerOpusBroadcastReceiver()
    }

    override fun onDetach() {
        super.onDetach()
        listener?.unregisterChatFragment()
        listener = null
        handler.removeCallbacks(record)
        handler.removeCallbacks(postAnimation)
        audioProgressSubscription?.unsubscribe()
        cleanUpVoice(false)
        unregisterOpusBroadcastReceiver()
    }
    /* ^ Fragment lifecycle methods ^ */


    private fun saveState() {
        // Save typed but not sent yet text
        ChatManager.getInstance().setTyped(
            accountJid, contactJid, inputEditText.text.toString(),
            inputEditText.selectionStart, inputEditText.selectionEnd
        )

        // Save messages position
        layoutManager.findLastCompletelyVisibleItemPosition()
            .takeIf { it != -1 }
            ?.let {
                chat.saveLastPosition(if (it == chatMessageAdapter.itemCount - 1) 0 else it)
            }
    }

    private fun restoreState() { //todo rewrite it
        // Restore typed text
        ChatManager.getInstance().getTypedMessage(accountJid, contactJid)
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                skipOnTextChanges = true
                inputEditText.setText(it)
                inputEditText.setSelection(
                    ChatManager.getInstance().getSelectionStart(accountJid, contactJid),
                    ChatManager.getInstance().getSelectionEnd(accountJid, contactJid)
                )
                skipOnTextChanges = false
                if (inputEditText.text.toString().isNotEmpty()) {
                    inputEditText.requestFocus()
                }
        }

        // Restore scroll position
        val position = chat.lastPosition
        val unread = chat.unreadMessageCount
        if ((position == 0 || (activity as ChatActivity).needScrollToUnread()) && unread > 0) {
            scrollToFirstUnread(unread)
        } else if (position > 0) {
            layoutManager.scrollToPosition(position)
        }
        updateNewReceivedMessageCounter(unread)
    }

    override fun onLastHistoryLoadingError(
        accountJid: AccountJid,
        contactJid: ContactJid,
        errorText: String?
    ) {
        val text =
            errorText ?: getString(R.string.groupchat_error) //todo change to use specific string
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    private fun setupPinnedMessageView() {
        if (activity != null && activity?.isFinishing != true) {
            if (chat is GroupChat && (chat as? GroupChat)?.pinnedMessageId != null) {
                val message =
                    MessageRepository.getMessageFromRealmByStanzaId((chat as? GroupChat)?.pinnedMessageId)
                        ?: return
                val pinnedFragment = PinnedMessagePanel.newInstance(
                    message = message,
                    onClickListener = {
                        this.startActivity(
                            MessagesActivity.createIntentShowPinned(
                                requireContext(), message.primaryKey, contactJid, accountJid
                            )
                        )
                    },
                    { sendUnPinMessageRequest((chat as GroupChat)) }
                )

                pinnedFragment.also { panel ->
                    topPinnedMessagePanel = panel
                    with(childFragmentManager.beginTransaction()) {
                        replace(R.id.topPanelContainer, panel)
                        commit()
                    }
                }

            } else {
                topPinnedMessagePanel?.let { panel ->
                    with(childFragmentManager.beginTransaction()) {
                        remove(panel)
                        commit()
                    }
                }
                topPinnedMessagePanel = null
            }
        }
    }


    fun onToolbarInteractionCloseClick() {
        hideBottomMessagePanel()
        closeInteractionPanel()
    }

    fun onToolbarInteractionPinClick() {
        if (chat is GroupChat) {
            chatMessageAdapter.checkedMessageRealmObjects[0]?.let { sendPinMessageRequest(it) }
            hideBottomMessagePanel()
            closeInteractionPanel()
        }
    }

    fun onToolbarInteractionDeleteClick() {
        deleteMessage(ArrayList(chatMessageAdapter.checkedMessageRealmObjects))
    }

    fun onToolbarInteractionCopyClick() {
        ClipManager.copyMessagesToClipboard(
            ArrayList(chatMessageAdapter.checkedItemIds)
        )
        hideBottomMessagePanel()
        closeInteractionPanel()
    }

    fun onToolbarInteractionsEditClick() {
        chatMessageAdapter.checkedMessageRealmObjects[0]?.let { getReadyForMessageEditing(it) }
    }

    private fun setChat(accountJid: AccountJid, contactJid: ContactJid) {
        this.accountJid = accountJid
        this.contactJid = contactJid

        if (!accountJid.bareJid.toString().contains(contactJid.bareJid.toString())) {
            IntroViewDecoration.decorateRecyclerViewWithChatIntroView(
                realmRecyclerView,
                chat,
                accountColor
            )
        }
        if (hasActiveIncomingInvites(accountJid, contactJid)
            && getLastInvite(accountJid, contactJid)?.reason != null
            && getLastInvite(accountJid, contactJid)?.reason?.isNotEmpty() == true
        ) {
            getLastInvite(accountJid, contactJid)?.let { invite ->
                val senderName = when {
                    (VCardManager.getInstance().getName(invite.senderJid.jid) != null
                            && VCardManager.getInstance().getName(invite.senderJid.jid)
                        .isNotEmpty())
                    -> VCardManager.getInstance().getName(invite.senderJid.jid)

                    (RosterManager.getInstance()
                        .getName(invite.accountJid, invite.senderJid) != null
                            && RosterManager.getInstance()
                        .getName(invite.accountJid, invite.senderJid).isNotEmpty())
                    -> RosterManager.getInstance().getName(invite.accountJid, invite.senderJid)

                    else -> invite.senderJid.toString()
                }

                val senderAvatar = RosterManager.getInstance()
                    .getAbstractContact(accountJid, invite.senderJid.bareUserJid)
                    .getAvatar(true)

                inflateIncomingInviteFakeMessage(senderAvatar, senderName, invite.reason)
            }
        }

        memberId?.let {

        }
        chatMessageAdapter = MessagesAdapter(
            requireActivity(),
            memberId?.let {
                MessageRepository.getGroupMemberMessages(accountJid, contactJid, memberId)
            } ?: chat.messages,
            chat,
            this, this, this, this, this, this
        )
        realmRecyclerView.adapter = chatMessageAdapter
        realmRecyclerView.itemAnimator = null
        realmRecyclerView.addItemDecoration(MessageHeaderViewDecoration())
        replySwipeCallback = ReplySwipeCallback { position: Int ->
            val messageRealmObject = chatMessageAdapter.getMessageItem(position)
            if (messageRealmObject != null) {
                if (messageRealmObject.primaryKey != null) {
                    showBottomMessagesPanel(
                        listOf(messageRealmObject.primaryKey),
                        Purposes.FORWARDING
                    )
                    setUpInputViewButtons()
                }
            }
        }
        ItemTouchHelper(replySwipeCallback).attachToRecyclerView(realmRecyclerView)

        realmRecyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                replySwipeCallback.onDraw(c)
            }
        })
        updateContact()
    }

    private fun setUpInputView(view: View) {
        inputEditText = view.findViewById(R.id.chat_input)
        setUpIme()
        inputEditText.setOnEditorActionListener { _: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                return@setOnEditorActionListener true
            } else if (event != null && actionId == EditorInfo.IME_NULL) {
                if (SettingsManager.chatsSendByEnter() && event.action == KeyEvent.ACTION_DOWN) {
                    sendMessage()
                    return@setOnEditorActionListener true
                }
            }
            false
        }
        inputEditText.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
            if (keyCode == KeyEvent.KEYCODE_ENTER
                && SettingsManager.chatsSendByEnter()
                && event.action == KeyEvent.ACTION_DOWN
            ) {
                sendMessage()
                return@setOnKeyListener true
            }
            false
        }
        inputEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!skipOnTextChanges && stopTypingTimer != null) {
                    stopTypingTimer?.cancel()
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(text: Editable) {
                this@ChatFragment.afterTextChanged(text)
            }
        })
    }

    private fun afterTextChanged(text: Editable) {
        setUpInputViewButtons()
        if (skipOnTextChanges) {
            return
        }
        ChatStateManager.getInstance().onComposing(accountJid, contactJid, text)
        stopTypingTimer = Timer()
        stopTypingTimer?.schedule(
            object : TimerTask() {
                override fun run() {
                    Application.getInstance()
                        .runOnUiThread {
                            ChatStateManager.getInstance().onPaused(accountJid, contactJid)
                        }
                }
            },
            STOP_TYPING_DELAY
        )
    }

    private fun setUpIme() {
        if (SettingsManager.chatsSendByEnter()) {
            inputEditText.inputType =
                inputEditText.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE.inv()
        } else {
            inputEditText.inputType =
                inputEditText.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
    }

    private fun setUpEmoji(view: View) {
        val emojiButton = view.findViewById<ImageButton>(R.id.button_emoticon)
        val rootView = view.findViewById<View>(R.id.root_view)


        // Give the topmost view of your activity layout hierarchy. This will be used to measure soft keyboard height
        val popup = EmojiconsPopup(rootView, activity)

        //Will automatically set size according to the soft keyboard size
        popup.setSizeForSoftKeyboard()

        //If the emoji popup is dismissed, change emojiButton to smiley icon
        popup.setOnDismissListener {
            changeEmojiKeyboardIcon(
                emojiButton,
                R.drawable.ic_mood_black_24dp
            )
        }

        //If the text keyboard closes, also dismiss the emoji popup
        popup.setOnSoftKeyboardOpenCloseListener(object :
            EmojiconsPopup.OnSoftKeyboardOpenCloseListener {
            override fun onKeyboardOpen(keyBoardHeight: Int) {}
            override fun onKeyboardClose() {
                if (popup.isShowing) popup.dismiss()
            }
        })

        //On emoji clicked, add it to edittext
        popup.setOnEmojiconClickedListener { emojicon: Emojicon? ->
            if (emojicon == null) {
                return@setOnEmojiconClickedListener
            }
            val start = inputEditText.selectionStart
            val end = inputEditText.selectionEnd
            if (start < 0) {
                inputEditText.append(emojicon.emoji)
            } else {
                inputEditText.text.replace(
                    start.coerceAtMost(end),
                    start.coerceAtLeast(end), emojicon.emoji, 0,
                    emojicon.emoji.length
                )
            }
        }

        //On backspace clicked, emulate the KEYCODE_DEL key event
        popup.setOnEmojiconBackspaceClickedListener {
            val event = KeyEvent(
                0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL
            )
            inputEditText.dispatchKeyEvent(event)
        }

        // To toggle between text keyboard and emoji keyboard keyboard(Popup)
        emojiButton.setOnClickListener {

            //If popup is not showing => emoji keyboard is not visible, we need to show it
            if (!popup.isShowing) {

                //If keyboard is visible, simply show the emoji popup
                if (popup.isKeyBoardOpen) {
                    popup.showAtBottom()
                } else { //else, open the text keyboard first and immediately after that show the emoji popup
                    inputEditText.isFocusableInTouchMode = true
                    inputEditText.requestFocus()
                    popup.showAtBottomPending()

                    (activity?.getSystemService(Context.INPUT_METHOD_SERVICE)
                            as? InputMethodManager
                            )?.showSoftInput(inputEditText, InputMethodManager.SHOW_IMPLICIT)

                }
                changeEmojiKeyboardIcon(emojiButton, R.drawable.ic_keyboard_black_24dp)
            } else {
                popup.dismiss()
            }
        }
    }

    private fun requestToLoadHistoryIfNeed() {
        val messagesCount = chatMessageAdapter.itemCount
        val topVisible = layoutManager.findFirstVisibleItemPosition()
        if (topVisible <= 15 && topVisible != -1 && messagesCount != 0
            || topVisible == -1 && messagesCount <= 30
        ) {
//            if (memberId != null) {
//                tryToLoadPortionOfMemberMessagesInGroup(chat as GroupChat, memberId!!)
//            } else {
//
//            }
            loadNextMessagesPortionInChat(chat)
        }
    }

    override fun onLastHistoryLoadStarted(accountJid: AccountJid, contactJid: ContactJid) {
        Application.getInstance().runOnUiThread {
            if (accountJid == accountJid && contactJid == contactJid) {
                lastHistoryProgressBar.visibility = View.VISIBLE
            }
        }
    }

    override fun onLastHistoryLoadFinished(accountJid: AccountJid, contactJid: ContactJid) {
        Application.getInstance().runOnUiThread {
            if (accountJid == accountJid && contactJid == contactJid) {
                lastHistoryProgressBar.visibility = View.GONE
                requestToLoadHistoryIfNeed()
            }
        }
    }

    override fun onGroupPresenceUpdated(
        accountJid: AccountJid, groupJid: ContactJid, presence: Presence
    ) {
        Application.getInstance().runOnUiThread {
            if (this.accountJid === accountJid && this.contactJid === groupJid) {
                if (presence.type != Presence.Type.unsubscribe) {
                    setupPinnedMessageView()
                    //todo add toolbar updating
                } else {
                    try {
                        Thread.sleep(1000)
                        activity?.finish()
                    } catch (e: Exception) {
                        LogManager.exception(this, e)
                    }
                }
            }
        }
    }

    override fun onAction() {
        Application.getInstance().runOnUiThread {
            updateNewReceivedMessageCounter(chat.unreadMessageCount)
        }
    }

    override fun onNewIncomingMessage(
        accountJid: AccountJid, contactJid: ContactJid,
        message: MessageRealmObject?, needNotification: Boolean
    ) {
        Application.getInstance().runOnUiThread {
            if (accountJid == this.accountJid && this.contactJid == contactJid) {
                if (needNotification) {
                    playMessageSound()
                }
                NotificationManager.getInstance().removeMessageNotification(accountJid, contactJid)
                if (chat is GroupChat) {
                    setupPinnedMessageView()
                }
            }
        }
    }

    override fun onAuthAsk(accountJid: AccountJid, contactJid: ContactJid) {
        Application.getInstance().runOnUiThread {
            if (accountJid === accountJid && contactJid === contactJid) {
                showHideNotifyIfNeed()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_EXPORT_CHAT) {
            if (PermissionsRequester.isPermissionGranted(grantResults)) {
                showExportChatDialog()
            } else {
                Toast.makeText(
                    activity,
                    R.string.no_permission_to_write_files,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun changeEmojiKeyboardIcon(iconToBeChanged: ImageView, drawableResourceId: Int) {
        iconToBeChanged.setImageResource(drawableResourceId)
    }

    private fun setUpInputViewButtons() {
        var empty = inputEditText.text.toString().trim { it <= ' ' }.isEmpty()
        if (empty) {
            empty = bottomMessagesPanel != null
        }
        if (empty != isInputEmpty) {
            isInputEmpty = empty
        }
        if (isInputEmpty) {
            sendButton.visibility = View.GONE
            sendButton.setColorFilter(ColorManager.getInstance().accountPainter.greyMain)
            sendButton.isEnabled = false
            recordButton.visibility = View.VISIBLE
            attachButton.visibility = View.VISIBLE
        } else {
            sendButton.visibility = View.VISIBLE
            sendButton.isEnabled = true
            sendButton.setColorFilter(accountColor)
            recordButton.visibility = View.GONE
            attachButton.visibility = View.GONE
        }
    }

    fun sendMessage() {
        val editable = inputEditText.editableText
        val text: String
        var markupText: String? = null
        val spannable = SpannableStringBuilder(editable)
        val htmlMarkupBuilder = StringBuilder(spannable.toString())
        var htmlMarkupOffset =
            0 // the offset of the html string compared to the normal string without <tags>
        val quoteSpans = spannable.getSpans(0, spannable.length, CustomQuoteSpan::class.java)
        if (quoteSpans.isNotEmpty()) {
            val len = spannable.length
            for (span in quoteSpans) {
                val startSpan = spannable.getSpanStart(span)
                val endSpan = spannable.getSpanEnd(span)
                spannable.removeSpan(span)
                if (startSpan < 0 || endSpan < 0) continue
                if (startSpan >= len || endSpan > len || startSpan > endSpan) continue

                // make sure it's a paragraph
                // check top paragraph boundary
                if (startSpan != 0 && spannable[startSpan - 1] != '\n') continue
                // check bottom paragraph boundary
                if (endSpan != spannable.length && spannable[endSpan - 1] != '\n') continue

                // split the quote span into 1-line strings
                val originalQuoteString = spannable.subSequence(startSpan, endSpan).toString()
                val quoteLines = originalQuoteString.split("\n").toTypedArray()
                spannable.delete(startSpan, endSpan)
                htmlMarkupBuilder.delete(startSpan + htmlMarkupOffset, endSpan + htmlMarkupOffset)
                var variableStartSpan = startSpan

                // open the quote tag for the markup text
                htmlMarkupBuilder.insert(startSpan + htmlMarkupOffset, "<blockquote>")
                htmlMarkupOffset += "<blockquote>".length
                for (i in quoteLines.indices) {
                    // add > at the start of each line
                    quoteLines[i] = """
                        >${quoteLines[i]}
                        
                        """.trimIndent()
                    // add modified line back to the spannable and markup text
                    spannable.insert(variableStartSpan, quoteLines[i])
                    htmlMarkupBuilder.insert(variableStartSpan + htmlMarkupOffset, quoteLines[i])
                    variableStartSpan += quoteLines[i].length
                }
                htmlMarkupBuilder.insert(variableStartSpan + htmlMarkupOffset, "</blockquote>")
                htmlMarkupOffset += "</blockquote>".length
            }
            markupText = htmlMarkupBuilder.toString()
        }
        text = spannable.toString()
        clearInputText()
        scrollDown()
        if (bottomMessagesPanel != null) {
            if (bottomMessagesPanel?.purpose == Purposes.FORWARDING) {
                bottomMessagesPanel?.messagesIds?.let { ids ->
                    ForwardManager.forwardMessage(ids, accountJid, contactJid, text, markupText)
                    hideBottomMessagePanel()
                    setFirstUnreadMessageId(null)
                }
            } else if (bottomMessagesPanel?.purpose == Purposes.EDITING) {
                bottomMessagesPanel?.messagesIds?.first()?.let { id ->
                    RetractManager.sendReplaceMessageTextRequest(
                        accountJid, contactJid, id, text, this
                    )
                    hideBottomMessagePanel()
                }
            }
        } else if (text.isNotEmpty()) {
            MessageManager.getInstance().sendMessage(accountJid, contactJid, text, markupText)
            scrollDown()
            setFirstUnreadMessageId(null)
        } else {
            return
        }
        playMessageSound()
        listener?.onMessageSent()
        ChatStateManager.getInstance().cancelComposingSender()
        if (SettingsManager.chatsHideKeyboard() == SettingsManager.ChatsHideKeyboard.always
            || (activity?.resources?.getBoolean(R.bool.landscape) == true
                    && SettingsManager.chatsHideKeyboard() == SettingsManager.ChatsHideKeyboard.landscape)
        ) {
            activity?.tryToHideKeyboardIfNeed()
        }
        scrollDown()
    }

    fun updateContact() {
        updateBlockedState()
        Application.getInstance().runOnUiThreadDelay(1000) { showTopPanelIfNeed() }
    }

    private fun scrollDown() {
        realmRecyclerView.scrollToPosition(chatMessageAdapter.itemCount - 1)
    }

    private fun scrollToFirstUnread(unreadCount: Int) {
        layoutManager.scrollToPositionWithOffset(
            chatMessageAdapter.itemCount - unreadCount,
            200
        )
    }

    fun setInputText(additional: String) {
        skipOnTextChanges = true
        inputEditText.setText(additional)
        inputEditText.setSelection(additional.length)
        skipOnTextChanges = false
    }

    private fun setInputTextAtCursor(additional: String) {
        skipOnTextChanges = true
        val currentText = inputEditText.text.toString()
        if (currentText.isEmpty()) {
            inputEditText.setText(additional)
            inputEditText.setSelection(additional.length)
        } else {
            val cursorPosition = inputEditText.selectionStart
            val first = currentText.substring(0, cursorPosition)
            val second = currentText.substring(cursorPosition)
            inputEditText.setText(first + additional + second)
            inputEditText.setSelection(first.length + additional.length)
        }
        skipOnTextChanges = false
    }

    private fun setQuote(clickedMessageRealmObject: MessageRealmObject) {
        val quote: String =
            if (accountJid.bareJid.toString().contains(contactJid.bareJid.toString())
                && clickedMessageRealmObject.account.bareJid.toString()
                    .contains(clickedMessageRealmObject.user.bareJid.toString())
                && clickedMessageRealmObject.hasForwardedMessages()
                && MessageRepository.getForwardedMessages(clickedMessageRealmObject).size == 1
            ) {
                MessageRepository.getForwardedMessages(clickedMessageRealmObject)[0].text
            } else """
     ${clickedMessageRealmObject.text}
     
     """.trimIndent()

        skipOnTextChanges = true
        var spanStart = 0
        if (bottomMessagesPanel?.purpose == Purposes.EDITING) {
            setInputTextAtCursor(quote)
            return
        }
        val currentText = inputEditText.editableText
        if (currentText.isNotEmpty()) {
            val cursorPosition = inputEditText.selectionStart
            spanStart = cursorPosition
            if (cursorPosition != 0 && currentText[cursorPosition - 1] != '\n') {
                currentText.insert(cursorPosition, "\n")
                spanStart++
            }
        }
        val spanEnd: Int = spanStart + quote.length
        currentText.insert(spanStart, quote)
        currentText.setSpan(
            CustomQuoteSpan(accountColor, context?.resources?.displayMetrics),
            spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        inputEditText.setSelection(spanEnd)
        skipOnTextChanges = false
    }

    private fun clearInputText() {
        skipOnTextChanges = true
        inputEditText.text.clear()
        skipOnTextChanges = false
    }

    fun onExportChatClick() {
        if (PermissionsRequester.requestFileWritePermissionIfNeeded(
                this,
                PERMISSIONS_REQUEST_EXPORT_CHAT
            )
        ) {
            showExportChatDialog()
        }
    }

    private fun showExportChatDialog() {
        fragmentManager?.let {
            ChatExportDialogFragment.newInstance(accountJid, contactJid).show(
                it, "CHAT_EXPORT"
            ) //todo make const
        }
    }

    private fun deleteMessage(messages: ArrayList<MessageRealmObject?>) {
        val ids = messages.filterNotNull().map { it.primaryKey }.toMutableList()
        val isSavedMessages = accountJid.bareJid.toString().contains(contactJid.bareJid.toString())
        val isGroup = chat is GroupChat
        val size = ids.size
        if (isSupported(accountJid)) {
            val checkBoxView = View.inflate(context, R.layout.delete_for_companion_checkbox, null)
            val checkBox = checkBoxView.findViewById<CheckBox>(R.id.delete_for_all_checkbox)
            checkBox.text = String.format(
                resources.getString(R.string.delete_for_all),
                RosterManager.getInstance().getBestContact(accountJid, contactJid).name
            )
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle(resources.getQuantityString(R.plurals.delete_message_title, size, size))
                .setMessage(resources.getQuantityString(R.plurals.delete_message_question, size))
                .setPositiveButton(
                    R.string.delete
                ) { _: DialogInterface?, _: Int ->
                    sendRetractMessagesRequest(
                        accountJid,
                        contactJid,
                        ids,
                        (checkBox.isChecked || isGroup) && !isSavedMessages,
                        this
                    )
                }
                .setNegativeButton(R.string.cancel_action) { _: DialogInterface?, _: Int -> }
            if (!messages.filterNotNull().any { it.isIncoming } && !isSavedMessages && !isGroup) {
                dialog.setView(checkBoxView)
            }
            dialog.show()
        } else {
            AlertDialog.Builder(requireContext())
                .setMessage(resources.getQuantityString(R.plurals.delete_message_question, size))
                .setPositiveButton(R.string.delete) { _: DialogInterface?, _: Int ->
                    MessageManager.getInstance().removeMessage(ids)
                }
                .setNegativeButton(R.string.cancel_action) { _: DialogInterface?, _: Int -> }
                .show()
        }
        hideBottomMessagePanel()
        closeInteractionPanel()
    }

    private fun getReadyForMessageEditing(messageRealmObject: MessageRealmObject) {
        showBottomMessagesPanel(listOf(messageRealmObject.primaryKey), Purposes.EDITING)
        closeInteractionPanel()
        setInputText(messageRealmObject.text)
    }

    fun tryToResetEditingText(): Boolean {
        return if (bottomMessagesPanel != null
            && bottomMessagesPanel?.messagesIds?.isNotEmpty() == true
            && bottomMessagesPanel?.purpose == Purposes.EDITING
            && inputEditText.text.toString().isNotEmpty()
        ) {
            clearInputText()
            hideBottomMessagePanel()
            true
        } else {
            false
        }
    }

    override fun onMessageClick(caller: View, position: Int) {
        val itemViewType = chatMessageAdapter.getItemViewType(position)
        if (itemViewType != MessagesAdapter.VIEW_TYPE_GROUPCHAT_SYSTEM_MESSAGE
            && itemViewType != MessagesAdapter.VIEW_TYPE_ACTION_MESSAGE
        ) {
            val clickedMessageRealmObject = chatMessageAdapter.getMessageItem(position)
            if (clickedMessageRealmObject == null) {
                LogManager.w(
                    ChatFragment::class.java.simpleName,
                    "onMessageClick null message item. Position: $position"
                )
                return
            }

            MessageContextMenu(
                context = requireContext(),
                anchor = caller,
                message = clickedMessageRealmObject,
                chat = chat,
                onMessageRepeatClick = {
                    if (clickedMessageRealmObject.haveAttachments()) {
                        HttpFileUploadManager.getInstance().retrySendFileMessage(
                            clickedMessageRealmObject, activity
                        )
                    } else {
                        MessageManager.getInstance().sendMessage(
                            accountJid, contactJid,
                            clickedMessageRealmObject.text,
                            clickedMessageRealmObject.markupText
                        )
                    }
                },
                onMessageCopyClick = {
                    ClipManager.copyMessagesToClipboard(
                        mutableListOf(clickedMessageRealmObject.primaryKey)
                    )
                },
                onMessageStatusClick = {
                    if (clickedMessageRealmObject.messageStatus == MessageStatus.ERROR) {
                        showError(clickedMessageRealmObject.errorDescription)
                    }
                },
                onPinClick = { sendPinMessageRequest(clickedMessageRealmObject) },
                onMessageEditClick = { getReadyForMessageEditing(clickedMessageRealmObject) },
                onMessageRemoveClick = { deleteMessage(arrayListOf(clickedMessageRealmObject)) },
                onMentionUserClick = { mentionUser(clickedMessageRealmObject.resource.toString()) },
                onMessageQuoteClick = { setQuote(clickedMessageRealmObject) },
            ).show()

        }

    }

    override fun onMessageAvatarClick(position: Int) {
        val messageClicked = chatMessageAdapter.getMessageItem(position)

        if (chat is GroupChat) {
            messageClicked?.groupchatUserId?.let {
                startActivity(
                    createIntentForGroupchatAndMemberId(requireActivity(), it, chat as GroupChat)
                )
            }
        } else if (accountJid.bareJid.toString().contains(contactJid.bareJid.toString())
            && messageClicked!!.account.bareJid.toString()
                .contains(messageClicked.user.bareJid.toString())
            && messageClicked.hasForwardedMessages()
        ) {
            try {
                val innerMessage = MessageRepository.getForwardedMessages(messageClicked)[0]
                val contactJid =
                    if (innerMessage.originalFrom != null) {
                        ContactJid.from(innerMessage.originalFrom)
                    } else {
                        innerMessage.user
                    }
                if (innerMessage.groupchatUserId != null) {
                    startActivity(
                        createIntentForGroupchatAndMemberId(
                            requireActivity(),
                            innerMessage.groupchatUserId,
                            (ChatManager.getInstance().getChat(accountJid, contactJid) as GroupChat)
                        )
                    )
                } else {
                    startActivity(
                        ContactViewerActivity.createIntent(activity, accountJid, contactJid)
                    )
                }
            } catch (e: Exception) {
                LogManager.exception(this, e)
            }
        } else {
            LogManager.e(
                ChatFragment::class.java.simpleName,
                "onMessageAvatarClick cant handle avatar. It's regular chat or saved messages chat. Position: $position"
            )
        }
    }

    override fun onChangeCheckedItems(checkedItems: Int) {
        if (checkedItems > 0) {
            val clickedMessage =
                chatMessageAdapter.checkedMessageRealmObjects.firstOrNull() ?: return

            val isRightMessageStatusForEditing =
                clickedMessage.messageStatus == MessageStatus.DELIVERED
                        || clickedMessage.messageStatus == MessageStatus.RECEIVED
                        || clickedMessage.messageStatus == MessageStatus.DISPLAYED

            val isMessageAbleForEditing = !clickedMessage.isIncoming
                    && !clickedMessage.haveAttachments()
                    && isRightMessageStatusForEditing

            val isSingleCheckedItem = checkedItems == 1

            val isEditable =
                isSingleCheckedItem && isSupported(accountJid) && isMessageAbleForEditing

            val isPinnable = isSingleCheckedItem && chat is GroupChat

            interactionView.visibility = View.VISIBLE
            (activity as? ChatActivity)?.showToolbarInteractionsPanel(
                true, isEditable, isPinnable, checkedItems
            )
            replySwipeCallback.setSwipeEnabled(false)
        } else {
            interactionView.visibility = View.GONE
            replySwipeCallback.setSwipeEnabled(true)
            (activity as? ChatActivity)?.showToolbarInteractionsPanel(
                false, isEditable = false, isPinnable = false, messagesCount = checkedItems
            )
        }
    }

    private fun mentionUser(username: String) {
        setInputTextAtCursor("$username, ")
    }

    private fun showError(message: String) {
        context?.let {
            AlertDialog.Builder(it).setTitle(R.string.error_description_title)
                .setMessage(message)
                .setPositiveButton("Ok", null)
                .show()
        }
    }

    override fun onAccountsChanged(accounts: Collection<AccountJid>) {
        Application.getInstance().runOnUiThread { chatMessageAdapter.notifyDataSetChanged() }
    }

    private fun playMessageSound() {
        if (!SettingsManager.eventsInChatSounds()) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attr = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build()
            MediaPlayer.create(
                activity, R.raw.message_alert,
                attr, AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        } else {
            MediaPlayer.create(activity, R.raw.message_alert).apply {
                setAudioStreamType(AudioManager.STREAM_NOTIFICATION)
            }
        }.also {
            it.start()
            it.setOnCompletionListener(MediaPlayer::release)
        }
    }

    override fun scrollTo(position: Int) {
        layoutManager.scrollToPosition(position)
    }

    override fun onConnectionStateChanged(newConnectionState: ConnectionState) {
        if (newConnectionState == ConnectionState.connected) {
            requestToLoadHistoryIfNeed()
        }
    }

    override fun onMessagesUpdated() {
        requestToLoadHistoryIfNeed()
    }

    private fun showHideNotifyIfNeed() {
        (chat as? RegularChat)?.let {
            val notifyIntent = it.intent
            if (notifyIntent != null) {
                setupNotifyLayout(notifyIntent)
            } else {
                notifyLayout?.visibility = View.GONE
            }
        }
    }

    private fun setupNotifyLayout(notifyIntent: Intent) {
//        if (tvNotifyTitle == null || tvNotifyAction == null) {
//            val view = stubNotify.inflate()
//            tvNotifyTitle = view.findViewById(R.id.tvNotifyTitle)
//            tvNotifyAction = view.findViewById(R.id.tvNotifyAction)
//            notifyLayout = view.findViewById(R.id.notifyLayout)
//            notifyLayout?.setOnClickListener {
//                startActivity(notifyIntent)
//                it.visibility = View.GONE
//            }
//        }
//        notifyLayout?.visibility = View.VISIBLE
    }

    private fun inflateIncomingInviteFakeMessage(
        senderAvatar: Drawable, senderName: String, reasonText: String,
    ) {
        val view = stubInviteFakeMessage.inflate()
        view.findViewById<ImageView>(R.id.avatar)?.setImageDrawable(senderAvatar)
        view.findViewById<TextView>(R.id.message_text)?.text = reasonText
        view.findViewById<TextView>(R.id.message_header)?.text = senderName
        view.findViewById<View>(R.id.message_status_icon).visibility = View.GONE

        view.findViewById<View>(R.id.message_balloon).apply {
            val balloonDrawable = context?.resources?.let {
                ResourcesCompat.getDrawable(it, R.drawable.msg_in, null)
            }
            background = balloonDrawable
            setPadding(
                dipToPx(20f, context),
                dipToPx(8f, context),
                dipToPx(12f, context),
                dipToPx(8f, context)
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                background.setTintList(
                    ColorManager.getInstance().getChatIncomingBalloonColorsStateList(accountJid)
                )
            }
        }

        view.findViewById<View>(R.id.message_shadow).apply {
            val shadowDrawable = context?.resources?.let {
                ResourcesCompat.getDrawable(it, R.drawable.msg_in_shadow, null)
            }
            context?.resources?.let {
                shadowDrawable?.setColorFilter(
                    it.getColor(R.color.black), PorterDuff.Mode.MULTIPLY
                )
            }
            background = shadowDrawable

            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(
                        dipToPx(3f, context),
                        dipToPx(0f, context),
                        dipToPx(0f, context),
                        dipToPx(3f, context)
                    )
                }
        }
    }

    private fun deflateIncomingInvite() {
        Application.getInstance().runOnUiThread { stubInviteFakeMessage.visibility = View.GONE }
    }

    private fun updateBlockedState() {
        userIsBlocked =
            BlockingManager.getInstance().contactIsBlockedLocally(accountJid, contactJid)
        if (userIsBlocked) {
            showBlockedBar()
        } else {
            if (blockedView != null) {
                blockedView?.visibility = View.GONE
            }
            inputLayout.visibility = View.VISIBLE
        }
    }

    private fun showBlockedBar() {
        for (i in 0 until inputPanel.childCount) {
            val view = inputPanel.getChildAt(i)
            if (view != null && view.visibility == View.VISIBLE) {
                view.visibility = View.GONE
            }
        }
        if (blockedView == null) {
            blockedView = TextView(context)
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelOffset(R.dimen.input_view_height)
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                blockedView?.setTextAppearance(R.style.TextAppearance_AppCompat_Widget_Button)
            } else {
                blockedView?.setTextAppearance(
                    context, R.style.TextAppearance_AppCompat_Widget_Button
                )
            }
            blockedView?.setTextColor(accountColor)
            blockedView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            blockedView?.setText(R.string.blocked_contact_message)
            blockedView?.setBackgroundColor(
                R.attr.chat_input_background.getAttrColor(context)
            )
            blockedView?.layoutParams = layoutParams
            blockedView?.gravity = Gravity.CENTER
            blockedView?.setOnClickListener {
                startActivity(
                    ContactViewerActivity.createIntent(context, accountJid, contactJid)
                )
            }
            inputPanel.addView(blockedView)
        } else {
            blockedView?.visibility = View.VISIBLE
        }
    }

    private fun showTopPanelIfNeed() {
        fun removePanel() {
            topPanel?.let { panel ->
                with(childFragmentManager.beginTransaction()) {
                    remove(panel)
                    commit()
                }
            }
            topPanel = null
        }

        if (userIsBlocked) {
            removePanel()
            return
        }

        if (!VCardManager.getInstance().isRosterOrHistoryLoaded(accountJid)) {
            return
        }

        val subscriptionState =
            RosterManager.getInstance().getSubscriptionState(accountJid, contactJid)

        var show = false
        when (subscriptionState.subscriptionType) {
            RosterManager.SubscriptionState.FROM, RosterManager.SubscriptionState.NONE -> {
                if (subscriptionState.pendingSubscription == RosterManager.SubscriptionState.PENDING_NONE) {
                    if (!chat.isAddContactSuggested) {
                        show = true
                    }
                }
                if (subscriptionState.hasIncomingSubscription()) {
                    show = true
                }
            }
            RosterManager.SubscriptionState.TO -> {
                if (subscriptionState.hasIncomingSubscription()) {
                    show = true
                }
            }
        }

        if (hasActiveIncomingInvites(accountJid, contactJid)) {
            show = true
        }

        if (chat.account.bareJid.toString() == chat.contactJid.bareJid.toString()) {
            show = false
        }

        if (show) {
            ChatFragmentTopPanel.newInstance(chat).also { panel ->
                topPanel = panel
                if (isAdded){
                    with(childFragmentManager.beginTransaction()) {
                        replace(R.id.topPanelContainer, panel)
                        commit()
                    }
                }
            }
        } else {
            removePanel()
            clearSubscriptionRequestNotification(accountJid, contactJid)
        }

    }

    override fun onBind(message: MessageRealmObject) {
        if (message.isValid && !message.isRead) {
            chat.markAsRead(message, true)
            updateNewReceivedMessageCounter(chat.unreadMessageCount)
        }
    }

    private fun showScrollDownButtonIfNeed() {
        if (layoutManager.findLastVisibleItemPosition() >= chatMessageAdapter.itemCount - 1
            || currentVoiceRecordingState == VoiceRecordState.TouchRecording
            || currentVoiceRecordingState == VoiceRecordState.NoTouchRecording
        ) {
            btnScrollDown.visibility = View.GONE
        } else {
            btnScrollDown.visibility = View.VISIBLE
        }
    }

    private fun updateNewReceivedMessageCounter(count: Int) {
        tvNewReceivedCount.apply {
            text = count.toString()
            visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }

    private fun setFirstUnreadMessageId(id: String?) {
        chatMessageAdapter.setFirstUnreadMessageId(id)
        chatMessageAdapter.notifyDataSetChanged()
    }

    private fun closeInteractionPanel() {
        chatMessageAdapter.resetCheckedItems()
        setUpInputViewButtons()
    }

    private fun sendVoiceMessage() {
        manageVoiceMessage(recordSaveAllowed)
        scrollDown()
        setFirstUnreadMessageId(null)
    }

    fun clearVoiceMessage() {
        manageVoiceMessage(false)
    }

    private fun manageVoiceMessage(saveMessage: Boolean) {
        handler.removeCallbacks(record)
        handler.removeCallbacks(timer)
        if (bottomMessagesPanel != null && bottomMessagesPanel?.messagesIds?.isNotEmpty() == true) {
            stopRecordingAndSend(saveMessage, bottomMessagesPanel?.messagesIds)
        } else {
            stopRecordingAndSend(saveMessage)
        }
        cancelRecordingCompletely()
    }

    private fun stopRecording() {
        activity?.lockScreenRotation(false)
        if (recordSaveAllowed) {
            sendImmediately = false
            //ignore = false;
            VoiceManager.getInstance().stopRecording(false)
            endRecordingButtonsAnimation()
            beginTimer(false)
            currentVoiceRecordingState = VoiceRecordState.StoppedRecording
        } else {
            ignoreReceiver = true
            clearVoiceMessage()
        }
    }

    fun setVoicePresenterData(tempFilePath: String?) {
        recordingPath = tempFilePath
        if (recordingPath != null) {
            setUpVoiceMessagePresenter()
            showScrollDownButtonIfNeed()
        }
    }

    private fun setUpVoiceMessagePresenter() {
        val time = HttpFileUploadManager.getVoiceLength(recordingPath)
        recordingPresenterDuration.text = String.format(
            Locale.getDefault(), "%02d:%02d",
            TimeUnit.SECONDS.toMinutes(time),
            TimeUnit.SECONDS.toSeconds(time)
        )
        subscribeForRecordedAudioProgress()
        recordingPresenterLayout?.visibility = View.VISIBLE

        //recordingPresenter.updateVisualizerFromFile();
        VoiceMessagePresenterManager.getInstance()
            .sendWaveDataIfSaved(recordingPath, recordingPresenter)
        recordingPresenter.updatePlayerPercent(0f, false)
        recordingPresenter.setOnTouchListener(object : onProgressTouch() {
            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> return if (VoiceManager.getInstance()
                            .playbackInProgress("", null)
                    ) super.onTouch(view, motionEvent) else {
                        (view as PlayerVisualizerView).updatePlayerPercent(0f, true)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (VoiceManager.getInstance()
                                .playbackInProgress("", null)
                        ) VoiceManager.getInstance().seekAudioPlaybackTo(
                            "", null, motionEvent.x
                                .toInt(), view.width
                        )
                        view.performClick()
                        return super.onTouch(view, motionEvent)
                    }
                }
                return super.onTouch(view, motionEvent)
            }
        })
        recordingPlayButton?.setImageResource(R.drawable.ic_play)
        recordingPlayButton?.setOnClickListener {
            VoiceManager.getInstance().voiceClicked(recordingPath)
        }
        recordingDeleteButton?.setOnClickListener {
            releaseRecordedVoicePlayback(recordingPath)
            finishVoiceRecordLayout()
            recordingPath = null
            audioProgressSubscription?.unsubscribe()
        }
        recordingPresenterSendButton.setOnClickListener {
            if (bottomMessagesPanel != null
                && bottomMessagesPanel?.messagesIds?.isNotEmpty() == true
            ) {
                sendStoppedVoiceMessage(recordingPath, bottomMessagesPanel?.messagesIds)
                hideBottomMessagePanel()
            } else {
                sendStoppedVoiceMessage(recordingPath)
            }
            scrollDown()
            setFirstUnreadMessageId(null)
            finishVoiceRecordLayout()
            recordingPath = null
            audioProgressSubscription?.unsubscribe()
        }
    }

    private fun subscribeForRecordedAudioProgress() {
        val audioProgress = VoiceManager.PublishAudioProgress.getInstance().subscribeForProgress()
        audioProgressSubscription =
            audioProgress.doOnNext { info: AudioInfo -> setUpAudioProgress(info) }
                .subscribe()
    }

    fun cleanUpVoice(deleteTempFile: Boolean) {
        if (deleteTempFile) VoiceManager.getInstance().deleteRecordedFile()
        VoiceManager.getInstance().releaseMediaRecorder()
        VoiceManager.getInstance().releaseMediaPlayer()
    }

    private fun setUpAudioProgress(info: AudioInfo) {
        if (info.attachmentIdHash == 0) {
            recordingPresenter.updatePlayerPercent(
                info.currentPosition.toFloat() / info.duration,
                false
            )
            if (info.resultCode == VoiceManager.COMPLETED_AUDIO_PROGRESS
                || info.resultCode == VoiceManager.PAUSED_AUDIO_PROGRESS
            ) {
                recordingPlayButton?.setImageResource(R.drawable.ic_play)
            } else recordingPlayButton?.setImageResource(R.drawable.ic_pause)
        }
    }

    fun finishVoiceRecordLayout() {
        recordingPresenterLayout?.visibility = View.GONE
        recordingPresenter.updateVisualizer(null)
        currentVoiceRecordingState = VoiceRecordState.NotRecording
        closeVoiceRecordPanel()
        changeStateOfInputViewButtonsTo(true)
    }

    private fun closeVoiceRecordPanel() {
        recordLockView.visibility = View.GONE
        voiceMessageRecorderLayout.visibility = View.GONE
        cancelRecordingLayout?.visibility = View.GONE
        handler.removeCallbacks(postAnimation)
        showScrollDownButtonIfNeed()
    }

    private fun endRecordingButtonsAnimation() {
        recordButtonExpanded.hide()
        recordButtonExpanded.animate()
            .y(rootViewHeight - (fabMicViewHeightSize + fabMicViewMarginBottom))
            .setDuration(300)
            .start()
        recordLockView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out_200))
        recordLockView.animate()
            .y(rootViewHeight - (lockViewHeightSize + lockViewMarginBottom))
            .setDuration(300)
            .start()
    }

    private fun cancelRecordingCompletely() {
        changeStateOfInputViewButtonsTo(true)
        currentVoiceRecordingState = VoiceRecordState.NotRecording
        VoiceManager.getInstance().releaseMediaRecorder()
        endRecordingButtonsAnimation()
        voiceMessageRecorderLayout.startAnimation(
            AnimationUtils.loadAnimation(
                context, R.anim.slide_out_right_opaque
            )
        )
        handler.postDelayed(postAnimation, 295)
        beginTimer(false)
        shortVibrate()
        activity?.lockScreenRotation(false)
    }

    private fun changeStateOfInputViewButtonsTo(state: Boolean) {
        rootView.findViewById<View>(R.id.button_emoticon).isEnabled =
            state
        attachButton.isEnabled = state
    }

    private fun beginTimer(start: Boolean) {
        if (start) {
            voiceMessageRecorderLayout.visibility = View.VISIBLE
        }
        if (start) {
            ChatStateManager.getInstance().onComposing(
                accountJid, contactJid, null, ChatStateSubtype.voice
            )
            stopTypingTimer?.cancel()
            ignoreReceiver = false
            slideToCancelLayout?.animate()?.x(0f)?.setDuration(0)?.start()
            recordLockChevronImage?.alpha = 1f
            recordLockImage?.setImageResource(R.drawable.ic_security_plain_24dp)
            recordLockImage?.setPadding(0, dipToPx(4f, requireContext()), 0, 0)

            val layoutParams = recordLockChevronImage?.layoutParams as? LinearLayout.LayoutParams
            layoutParams?.topMargin = 0
            layoutParams?.let { recordLockChevronImage?.layoutParams = it }

            recordTimer.base = SystemClock.elapsedRealtime()
            recordTimer.start()
            currentVoiceRecordingState = VoiceRecordState.TouchRecording
            showScrollDownButtonIfNeed()
            manageScreenSleep(true)
        } else {
            recordTimer.stop()
            ChatStateManager.getInstance().onPaused(accountJid, contactJid)
            manageScreenSleep(false)
        }
    }

    private fun manageScreenSleep(keepScreenOn: Boolean) {
        if (keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun showBottomMessagesPanel(forwardIds: List<String>, purpose: Purposes) {
        bottomMessagesPanel = null
        if (activity != null && activity?.isFinishing != true) {
            setUpInputViewButtons()
            (BottomMessagesPanel.newInstance(forwardIds, purpose) { hideBottomMessagePanel() })
                .also { panel ->
                    bottomMessagesPanel = panel
                    with(childFragmentManager.beginTransaction()) {
                        replace(R.id.secondBottomContainer, panel)
                        commit()
                    }
                }
        }
    }

    fun hideBottomMessagePanel() {
        setUpInputViewButtons()
        if (bottomMessagesPanel?.purpose == Purposes.EDITING) {
            inputEditText.setText("")
        }
        if (activity != null && activity?.isFinishing != true) {
            bottomMessagesPanel?.let { panel ->
                with(childFragmentManager.beginTransaction()) {
                    remove(panel)
                    commit()
                }
            }
        }
        bottomMessagesPanel = null
    }

    private fun openChooserForForward(forwardIds: List<String>) {
        if (accountJid.bareJid.toString().contains(contactJid.bareJid.toString())) {
            val rightSavedMessagesIds = ArrayList<String>()
            for (messageId in forwardIds) {
                val message = MessageRepository.getMessageFromRealmByPrimaryKey(messageId)
                if (message.account.bareJid.toString().contains(message.user.bareJid.toString())
                    && message.hasForwardedMessages()
                ) {
                    rightSavedMessagesIds.addAll(
                        message.forwardedIds.map { it.forwardMessageId }
                    )
                } else {
                    rightSavedMessagesIds.add(messageId)
                }
            }
            (activity as ChatActivity?)?.forwardMessages(rightSavedMessagesIds)
        } else {
            (activity as ChatActivity?)?.forwardMessages(forwardIds)
        }
    }

    override fun onIqError(error: XMPPError) {
        Application.getInstance().runOnUiThread {
            Toast.makeText(requireContext(), error.conditionText, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSend() {}
    override fun onResult() {}
    override fun onOtherError(exception: Exception?) {}

    private enum class VoiceRecordState {
        NotRecording, InitiatedRecording, TouchRecording, NoTouchRecording, StoppedRecording
    }

    interface ChatViewerFragmentListener {
        fun onMessageSent()
        fun registerChatFragment(chatFragment: ChatFragment?)
        fun unregisterChatFragment()
    }

    companion object {
        private const val ARGUMENT_ACCOUNT = "ARGUMENT_ACCOUNT"
        private const val ARGUMENT_USER = "ARGUMENT_USER"
        private const val ARGUMENT_MEMBER_ID = "MEMBER_ID"

        private const val VOICE_MESSAGE = "VOICE_MESSAGE"
        private const val VOICE_MESSAGE_RECEIVER_IGNORE = "VOICE_MESSAGE_RECEIVER_IGNORE"
        private const val BOTTOM_MESSAGE_PANEL_MESSAGES_SAVED_STATE = "FORWARD_MESSAGES"
        private const val BOTTOM_MESSAGE_PANEL_PURPOSE_SAVED_STATE = "FORWARD_PURPOSE"

        private const val PERMISSIONS_REQUEST_EXPORT_CHAT = 22
        private const val STOP_TYPING_DELAY: Long = 2500 // in ms

        fun newInstance(accountJid: AccountJid, contactJid: ContactJid) =
            ChatFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGUMENT_ACCOUNT, accountJid)
                    putParcelable(ARGUMENT_USER, contactJid)
                }
            }

        fun newInstanceForGroupMemberMessages(
            accountJid: AccountJid, contactJid: ContactJid, memberId: String
        ) = ChatFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARGUMENT_ACCOUNT, accountJid)
                putParcelable(ARGUMENT_USER, contactJid)
                putString(ARGUMENT_MEMBER_ID, memberId)
            }
        }
    }

}