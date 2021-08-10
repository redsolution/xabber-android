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
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.BaseIqResultUiListener
import com.xabber.android.data.NetworkException
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.repositories.MessageRepository
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.archive.MessageArchiveManager.loadNextMessagesPortionInChat
import com.xabber.android.data.extension.attention.AttentionManager
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.data.extension.blocking.BlockingManager.BlockContactListener
import com.xabber.android.data.extension.capability.CapabilitiesManager
import com.xabber.android.data.extension.capability.ClientInfo
import com.xabber.android.data.extension.chat_state.ChatStateManager
import com.xabber.android.data.extension.groups.GroupInviteManager.acceptInvitation
import com.xabber.android.data.extension.groups.GroupInviteManager.declineInvitation
import com.xabber.android.data.extension.groups.GroupInviteManager.getLastInvite
import com.xabber.android.data.extension.groups.GroupInviteManager.hasActiveIncomingInvites
import com.xabber.android.data.extension.groups.GroupsManager.enableSendingPresenceToGroup
import com.xabber.android.data.extension.groups.GroupsManager.sendPinMessageRequest
import com.xabber.android.data.extension.groups.GroupsManager.sendUnPinMessageRequest
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager
import com.xabber.android.data.extension.otr.OTRManager
import com.xabber.android.data.extension.otr.SecurityLevel
import com.xabber.android.data.extension.references.mutable.voice.VoiceManager
import com.xabber.android.data.extension.references.mutable.voice.VoiceManager.PublishAudioProgress.AudioInfo
import com.xabber.android.data.extension.references.mutable.voice.VoiceMessagePresenterManager
import com.xabber.android.data.extension.retract.RetractManager.isSupported
import com.xabber.android.data.extension.retract.RetractManager.sendMissedChangesInRemoteArchiveForChatRequest
import com.xabber.android.data.extension.retract.RetractManager.sendRemoteArchiveRetractVersionRequest
import com.xabber.android.data.extension.retract.RetractManager.sendReplaceMessageTextRequest
import com.xabber.android.data.extension.retract.RetractManager.sendRetractAllMessagesRequest
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
import com.xabber.android.data.roster.PresenceManager.acceptSubscription
import com.xabber.android.data.roster.PresenceManager.addAutoAcceptSubscription
import com.xabber.android.data.roster.PresenceManager.clearSubscriptionRequestNotification
import com.xabber.android.data.roster.PresenceManager.discardSubscription
import com.xabber.android.data.roster.PresenceManager.subscribeForPresence
import com.xabber.android.data.roster.PresenceManager.unsubscribeFromPresence
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.*
import com.xabber.android.ui.activity.ChatActivity
import com.xabber.android.ui.activity.ContactViewerActivity
import com.xabber.android.ui.activity.GroupchatMemberActivity.Companion.createIntentForGroupchatAndMemberId
import com.xabber.android.ui.activity.MessagesActivity
import com.xabber.android.ui.activity.QuestionActivity
import com.xabber.android.ui.adapter.CustomMessageMenuAdapter
import com.xabber.android.ui.adapter.ResourceAdapter
import com.xabber.android.ui.adapter.chat.IncomingMessageVH.BindListener
import com.xabber.android.ui.adapter.chat.IncomingMessageVH.OnMessageAvatarClickListener
import com.xabber.android.ui.adapter.chat.MessageVH.MessageClickListener
import com.xabber.android.ui.adapter.chat.MessagesAdapter
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.dialog.ChatExportDialogFragment
import com.xabber.android.ui.dialog.ChatHistoryClearDialog
import com.xabber.android.ui.helper.PermissionsRequester
import com.xabber.android.ui.text.CustomQuoteSpan
import com.xabber.android.ui.widget.*
import com.xabber.android.ui.widget.BottomMessagesPanel.Purposes
import com.xabber.android.ui.widget.PlayerVisualizerView.onProgressTouch
import com.xabber.android.utils.Utils
import com.xabber.xmpp.chat_state.ChatStateSubtype
import github.ankushsachdeva.emojicon.EmojiconsPopup
import github.ankushsachdeva.emojicon.emoji.Emojicon
import io.realm.RealmResults
import org.jivesoftware.smack.XMPPException.XMPPErrorException
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.packet.XMPPError
import org.jxmpp.jid.parts.Resourcepart
import org.jxmpp.stringprep.XmppStringprepException
import rx.Subscription
import java.util.*
import java.util.concurrent.TimeUnit

class ChatFragment : FileInteractionFragment(), View.OnClickListener, MessageClickListener,
    MessagesAdapter.Listener, PopupWindow.OnDismissListener, OnAccountChangedListener,
    BindListener, OnMessageAvatarClickListener, OnNewIncomingMessageListener, OnNewMessageListener,
    OnGroupPresenceUpdatedListener, OnMessageUpdatedListener, OnLastHistoryLoadStartedListener,
    OnLastHistoryLoadFinishedListener, OnAuthAskListener, OnLastHistoryLoadErrorListener,
    BaseIqResultUiListener {

    private var isInputEmpty = true
    private var inputPanel: FrameLayout? = null

    private lateinit var inputView: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var securityButton: ImageButton
    private lateinit var attachButton: ImageButton
    private lateinit var recordButton: ImageButton

    private var lastHistoryProgressBar: View? = null
    private var blockedView: TextView? = null
    private var stubNotify: ViewStub? = null
    private var stubInvite: ViewStub? = null
    private var notifyLayout: RelativeLayout? = null
    private var tvNotifyTitle: TextView? = null
    private var tvNotifyAction: TextView? = null
    private lateinit var rootView: View
    private lateinit var realmRecyclerView: RecyclerView
    private var chatMessageAdapter: MessagesAdapter? = null
    private lateinit var layoutManager: LinearLayoutManager
    private var replySwipe: ReplySwipeCallback? = null
    private lateinit var inputLayout: LinearLayout
    private var stubNewContact: ViewStub? = null
    private var newContactLayout: ViewGroup? = null
    private lateinit var addContact: TextView
    private lateinit var blockContact: TextView
    private lateinit var btnScrollDown: RelativeLayout
    private var tvNewReceivedCount: TextView? = null
    private var interactionView: View? = null
    private var skipOnTextChanges = false
    private val handler = Handler()
    private var currentVoiceRecordingState = VoiceRecordState.NotRecording
    private var recordSaveAllowed = false
    private var lockViewHeightSize = 0
    private var lockViewMarginBottom = 0
    private var fabMicViewHeightSize = 0
    private var fabMicViewMarginBottom = 0
    private var rootViewHeight = 0f
    private var recordingPath: String? = null

    //Voice message recorder layout
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
        Utils.performHapticFeedback(rootView)
        recordButtonExpanded.show()
        recordLockView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in_200))
        recordLockView.animate()
            .y(rootViewHeight - (lockViewMarginBottom + lockViewHeightSize))
            .setDuration(300)
            .start()
        beginTimer(currentVoiceRecordingState == VoiceRecordState.InitiatedRecording)
    }
    private var historyIsLoading = false
    private var messageRealmObjects: RealmResults<MessageRealmObject?>? = null
    private var menuItems: List<HashMap<String, String>>? = null
    private var userIsBlocked = false
    private var checkedResource = 0 // use only for alert dialog
    private var toolbarElevation = 0f
    private var accountColor = 0
    private var notifyIntent: Intent? = null
    private var sendByEnter = false

    private var bottomMessagesPanel: BottomMessagesPanel? = null
    private var topPinnedMessagePanel: PinnedMessagePanel? = null

    /* Fragment lifecycle overrided methods */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountJid = arguments?.getParcelable(ARGUMENT_ACCOUNT)
            ?: throw IllegalArgumentException("ChatFragment arguments must contains an accountJid")
        contactJid = arguments?.getParcelable(ARGUMENT_USER)
            ?: throw IllegalArgumentException("ChatFragment arguments must contains an contactJid")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        accountColor = ColorManager.getInstance().accountPainter.getAccountMainColor(accountJid)
        tvNewReceivedCount = view.findViewById(R.id.tvNewReceivedCount)

        btnScrollDown = view.findViewById(R.id.btnScrollDown)
        btnScrollDown.setOnClickListener(this)

        rootView = view.findViewById(R.id.root_view)
        rootView.addOnLayoutChangeListener { view1: View, _: Int, _: Int, _: Int, _: Int, _: Int, topOld: Int, _: Int, bottomOld: Int ->
            val heightOld = bottomOld - topOld
            if (heightOld != view1.height) {
                rootViewHeight = view1.height.toFloat()
            }
        }
        inputPanel = view.findViewById(R.id.bottomPanel)

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
                        activity,
                        PERMISSIONS_REQUEST_RECORD_AUDIO
                    )
                ) if (PermissionsRequester.requestFileWritePermissionIfNeeded(
                        activity,
                        PERMISSIONS_REQUEST_RECORD_AUDIO
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
                        Utils.lockScreenRotation(activity, true)
                    }
                }
                MotionEvent.ACTION_UP -> if (currentVoiceRecordingState == VoiceRecordState.TouchRecording) {
                    sendVoiceMessage()
                    Utils.lockScreenRotation(activity, false)
                } else if (currentVoiceRecordingState == VoiceRecordState.InitiatedRecording) {
                    handler.removeCallbacks(record)
                    handler.removeCallbacks(timer)
                    currentVoiceRecordingState = VoiceRecordState.NotRecording
                    Utils.lockScreenRotation(activity, false)
                }
                MotionEvent.ACTION_MOVE -> {

                    //FAB movement
                    val lockParams =
                        recordLockChevronImage!!.layoutParams as LinearLayout.LayoutParams
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
                                recordLockChevronImage!!.alpha = 1f
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
                                recordLockChevronImage!!.alpha = 1f + motionEvent.y / 200f
                                recordLockChevronImage!!.layoutParams = lockParams
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
                                cancelRecordingLayout!!.visibility = View.VISIBLE
                                recordLockImage!!.setImageResource(R.drawable.ic_stop)
                                recordLockImage!!.setPadding(0, 0, 0, 0)
                                recordLockImage!!.setOnClickListener { view121: View? ->
                                    if (currentVoiceRecordingState == VoiceRecordState.NoTouchRecording) {
                                        Utils.performHapticFeedback(rootView)
                                        stopRecording()
                                    }
                                }
                                lockParams.topMargin = -recordLockChevronImage!!.height
                                recordLockChevronImage!!.layoutParams = lockParams
                                recordLockChevronImage!!.alpha = 0f
                                Utils.performHapticFeedback(rootView)
                            }
                        }
                    }

                    //"Slide To Cancel" movement;
                    val alpha = 1f + motionEvent.x / 400f
                    if (currentVoiceRecordingState == VoiceRecordState.TouchRecording) {
                        if (motionEvent.x < 0) slideToCancelLayout!!.animate().x(motionEvent.x)
                            .setDuration(0).start() else slideToCancelLayout!!.animate().x(0f)
                            .setDuration(0).start()
                        slideToCancelLayout!!.alpha = alpha

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
        val cancelRecordingTextView = view.findViewById<TextView>(R.id.tv_cancel_recording)
        cancelRecordingTextView.setOnClickListener {
            if (currentVoiceRecordingState == VoiceRecordState.NoTouchRecording) {
                clearVoiceMessage()
            }
        }
        recordingPresenterLayout = view.findViewById(R.id.recording_presenter_layout)
        val recordingPresenterPlaybarLayout =
            view.findViewById<LinearLayout>(R.id.recording_playbar_layout)
        recordingPresenterPlaybarLayout.background.setColorFilter(
            accountColor,
            PorterDuff.Mode.SRC_IN
        )

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

        securityButton = view.findViewById(R.id.button_security)
        securityButton.setOnClickListener { showSecurityMenu() }

        // to avoid strange bug on some 4.x androids
        inputLayout = view.findViewById(R.id.input_layout)
        inputLayout.setBackgroundColor(ColorManager.getInstance().chatInputBackgroundColor)

        sendByEnter = SettingsManager.chatsSendByEnter()

        // interaction view
        interactionView = view.findViewById(R.id.interactionView)
        view.findViewById<View>(R.id.reply_tv).setOnClickListener {
            showBottomMessagesPanel(chatMessageAdapter!!.checkedItemIds, Purposes.FORWARDING)
            closeInteractionPanel()
        }
        view.findViewById<View>(R.id.reply_iv).setOnClickListener {
            showBottomMessagesPanel(chatMessageAdapter!!.checkedItemIds, Purposes.FORWARDING)
            closeInteractionPanel()
        }
        view.findViewById<View>(R.id.forward_iv).setOnClickListener {
            openChooserForForward(chatMessageAdapter!!.checkedItemIds)
        }
        view.findViewById<View>(R.id.forward_tv).setOnClickListener {
            openChooserForForward(chatMessageAdapter!!.checkedItemIds)
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
                if (dy < 0) loadHistoryIfNeed()
                showScrollDownButtonIfNeed()
            }
        })
        stubNotify = view.findViewById(R.id.stubNotify)
        stubNewContact = view.findViewById(R.id.stubNewContact)
        stubInvite = view.findViewById(R.id.stubInvite)
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
            savedInstanceState.getStringArray(FORWARD_MESSAGES)?.let { ids ->
                (savedInstanceState.getSerializable(FORWARD_PURPOSE) as? Purposes)?.let { purpose ->
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
            })

        setupPinnedMessageView()
        return view
    }

    override fun onResume() {
        super.onResume()

        ChatStateManager.getInstance().onChatOpening(accountJid, contactJid)
        if (chat is GroupChat) {
            enableSendingPresenceToGroup((chat as GroupChat?)!!, true)
        }
        restoreState()

        updateContact()

        showHideNotifyIfNeed()
        ChatManager.getInstance().setVisibleChat(chat)
        Application.getInstance().addUIListener(
            OnAccountChangedListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnNewIncomingMessageListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnNewMessageListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnGroupPresenceUpdatedListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnMessageUpdatedListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnLastHistoryLoadStartedListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnLastHistoryLoadFinishedListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnAuthAskListener::class.java, this
        )
        loadHistoryIfNeed()
        if (chat is GroupChat) {
            val retractVersion = (chat as GroupChat?)!!.retractVersion
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
        if (chat is GroupChat) {
            enableSendingPresenceToGroup((chat as GroupChat?)!!, false)
        }
        saveState()
        if (currentVoiceRecordingState == VoiceRecordState.NoTouchRecording
            || currentVoiceRecordingState == VoiceRecordState.TouchRecording
        ) stopRecording()
        ChatManager.getInstance().removeVisibleChat()
        Application.getInstance().removeUIListener(
            OnAccountChangedListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnNewIncomingMessageListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnNewMessageListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnGroupPresenceUpdatedListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnMessageUpdatedListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnLastHistoryLoadStartedListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnLastHistoryLoadFinishedListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnAuthAskListener::class.java, this
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(VOICE_MESSAGE, recordingPath)
        outState.putBoolean(VOICE_MESSAGE_RECEIVER_IGNORE, ignoreReceiver)
        bottomMessagesPanel?.let { panel ->
            outState.putStringArray(FORWARD_MESSAGES, panel.messagesIds.toTypedArray())
            outState.putSerializable(FORWARD_PURPOSE, panel.purpose)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatMessageAdapter!!.release()
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
        if (audioProgressSubscription != null) {
            audioProgressSubscription!!.unsubscribe()
        }
        cleanUpVoice(false)
        unregisterOpusBroadcastReceiver()
    }
    /* ^ Fragment lifecycle overrided methods ^ */


    private fun saveState() {
        // Save typed but not sent yet text
        ChatManager.getInstance().setTyped(
            accountJid, contactJid, inputView.text.toString(),
            inputView.selectionStart, inputView.selectionEnd
        )

        // Save messages position
        // todo rewrite it
        var position = layoutManager.findLastCompletelyVisibleItemPosition()
        if (position == -1) {
            return
        }
        if (position == chatMessageAdapter!!.itemCount - 1) {
            position = 0
        }
        chat.saveLastPosition(position)
    }

    private fun restoreState() { //todo rewrite it
        // Restore typed text
        skipOnTextChanges = true
        inputView.setText(ChatManager.getInstance().getTypedMessage(accountJid, contactJid))
        inputView.setSelection(
            ChatManager.getInstance().getSelectionStart(accountJid, contactJid),
            ChatManager.getInstance().getSelectionEnd(accountJid, contactJid)
        )
        skipOnTextChanges = false
        if (inputView.text.toString().isNotEmpty()) {
            inputView.requestFocus()
        }

        // Restore scroll position
        val position = chat.lastPosition
        val unread = chat.unreadMessageCount
        if ((position == 0 || (activity as ChatActivity?)!!.needScrollToUnread()) && unread > 0) { //todo rewrite it also
            scrollToFirstUnread(unread)
        } else if (position > 0) {
            layoutManager.scrollToPosition(position)
        }
        setFirstUnreadMessageId(chat.firstUnreadMessageId)
        updateNewReceivedMessageCounter(unread)
    }


    override fun onLastHistoryLoadingError(
        accountJid: AccountJid, contactJid: ContactJid,
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
            sendPinMessageRequest(
                chatMessageAdapter!!.checkedMessageRealmObjects[0]!!
            )
            hideBottomMessagePanel()
            closeInteractionPanel()
        }
    }

    fun onToolbarInteractionDeleteClick() {
        deleteMessage(ArrayList(chatMessageAdapter!!.checkedMessageRealmObjects))
    }

    fun onToolbarInteractionCopyClick() {
        ClipManager.copyMessagesToClipboard(
            ArrayList(chatMessageAdapter!!.checkedItemIds)
        )
        hideBottomMessagePanel()
        closeInteractionPanel()
    }

    fun onToolbarInteractionsEditClick() {
        getReadyForMessageEditing(chatMessageAdapter!!.checkedMessageRealmObjects[0]!!)
    }

    private fun setChat(accountJid: AccountJid, contactJid: ContactJid) {
        this.accountJid = accountJid
        this.contactJid = contactJid

        showSecurityButton(chat is RegularChat)
        messageRealmObjects = chat.messages
        if (!accountJid.bareJid.toString().contains(contactJid.bareJid.toString())) {
            IntroViewDecoration.decorateRecyclerViewWithChatIntroView(
                realmRecyclerView,
                chat,
                accountColor
            )
        }
        if (hasActiveIncomingInvites(accountJid, contactJid)
            && getLastInvite(accountJid, contactJid)!!.reason != null && getLastInvite(
                accountJid,
                contactJid
            )!!.reason.isNotEmpty()
        ) {
            val invite = getLastInvite(accountJid, contactJid)
            var senderName = invite!!.senderJid.toString()
            if (VCardManager.getInstance().getName(invite.senderJid.jid) != null
                && VCardManager.getInstance().getName(invite.senderJid.jid).isNotEmpty()
            ) {
                senderName = VCardManager.getInstance().getName(
                    invite.senderJid.jid
                )
            } else if (RosterManager.getInstance()
                    .getName(invite.accountJid, invite.senderJid) != null
                && RosterManager.getInstance().getName(invite.accountJid, invite.senderJid)
                    .isNotEmpty()
            ) {
                senderName =
                    RosterManager.getInstance().getName(invite.accountJid, invite.senderJid)
            }
            val senderAvatar = RosterManager.getInstance()
                .getAbstractContact(accountJid, invite.senderJid.bareUserJid)
                .getAvatar(true)
            inflateIncomingInvite(senderAvatar, senderName, invite.reason, accountColor)
        }
        chatMessageAdapter = MessagesAdapter(
            activity!!, messageRealmObjects!!, chat,
            this, this, this, this, this, this
        )
        realmRecyclerView.adapter = chatMessageAdapter
        realmRecyclerView.itemAnimator = null
        realmRecyclerView.addItemDecoration(MessageHeaderViewDecoration())
        replySwipe = ReplySwipeCallback { position: Int ->
            val messageRealmObject = chatMessageAdapter!!.getMessageItem(position)
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
        val itemTouchHelper = ItemTouchHelper(replySwipe!!)
        itemTouchHelper.attachToRecyclerView(realmRecyclerView)
        realmRecyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                replySwipe!!.onDraw(c)
            }
        })
        updateContact()
    }

    private fun setUpInputView(view: View) {
        inputView = view.findViewById(R.id.chat_input)
        setUpIme()
        inputView.setOnEditorActionListener { _: TextView?, actionId: Int, event: KeyEvent? ->
            LogManager.d(
                "InputViewDebug", "editorActionListener called, actionId = "
                        + actionId + ", event != null ? " + (event != null) + ", "
            )
            if (event != null) {
                LogManager.d(
                    "InputViewDebug", "event.getAction() = "
                            + event.action + ", event.getKeyCode() = " + event.keyCode
                )
            }
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                return@setOnEditorActionListener true
            } else if (event != null && actionId == EditorInfo.IME_NULL) {
                if (sendByEnter && event.action == KeyEvent.ACTION_DOWN) {
                    sendMessage()
                    return@setOnEditorActionListener true
                }
            }
            false
        }
        inputView.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
            if (sendByEnter) {
                if (keyCode < 29 || keyCode > 54 || event.keyCode < 29 || event.keyCode > 54) {
                    LogManager.d(
                        "InputViewDebug", "onKeyListener called, "
                                + "keyCode = " + keyCode
                                + ", event.getAction() = " + event.action
                                + ", event.getKeyCode() = " + event.keyCode
                    )
                }
            }
            if (keyCode == KeyEvent.KEYCODE_ENTER && sendByEnter
                && event.action == KeyEvent.ACTION_DOWN
            ) {
                sendMessage()
                return@setOnKeyListener true
            }
            false
        }
        inputView.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!skipOnTextChanges && stopTypingTimer != null) {
                    stopTypingTimer!!.cancel()
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
        stopTypingTimer!!.schedule(object : TimerTask() {
            override fun run() {
                Application.getInstance()
                    .runOnUiThread {
                        ChatStateManager.getInstance().onPaused(accountJid, contactJid)
                    }
            }
        }, STOP_TYPING_DELAY)
    }

    private fun setUpIme() {
        if (sendByEnter) {
            inputView.inputType =
                inputView.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE.inv()
        } else {
            inputView.inputType =
                inputView.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE
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
            val start = inputView.selectionStart
            val end = inputView.selectionEnd
            if (start < 0) {
                inputView.append(emojicon.emoji)
            } else {
                inputView.text.replace(
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
            inputView.dispatchKeyEvent(event)
        }

        // To toggle between text keyboard and emoji keyboard keyboard(Popup)
        emojiButton.setOnClickListener {

            //If popup is not showing => emoji keyboard is not visible, we need to show it
            if (!popup.isShowing) {

                //If keyboard is visible, simply show the emoji popup
                if (popup.isKeyBoardOpen) {
                    popup.showAtBottom()
                } else { //else, open the text keyboard first and immediately after that show the emoji popup
                    inputView.isFocusableInTouchMode = true
                    inputView.requestFocus()
                    popup.showAtBottomPending()
                    val inputMethodManager =
                        activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.showSoftInput(inputView, InputMethodManager.SHOW_IMPLICIT)
                }
                changeEmojiKeyboardIcon(emojiButton, R.drawable.ic_keyboard_black_24dp)
            } else {
                popup.dismiss()
            }
        }
    }

    private fun loadHistoryIfNeed() {
        if (!historyIsLoading) {
            val messagesCount = chatMessageAdapter!!.itemCount
            val topVisible = layoutManager.findFirstVisibleItemPosition()
            if (topVisible <= 15 && topVisible != -1 && messagesCount != 0
                || topVisible == -1 && messagesCount <= 30
            ) {
                loadNextMessagesPortionInChat(chat)
            }
        }
    }

    private val chat: AbstractChat
        get() {
            return ChatManager.getInstance().getChat(accountJid, contactJid)
                ?: ChatManager.getInstance().createRegularChat(accountJid, contactJid)
        }

    override fun onLastHistoryLoadStarted(accountJid: AccountJid, contactJid: ContactJid) {
        Application.getInstance().runOnUiThread {
            if (accountJid == accountJid && contactJid == contactJid) {
                lastHistoryProgressBar!!.visibility = View.VISIBLE
                historyIsLoading = true
            }
        }
    }

    override fun onLastHistoryLoadFinished(accountJid: AccountJid, contactJid: ContactJid) {
        Application.getInstance().runOnUiThread {
            if (accountJid == accountJid && contactJid == contactJid) {
                lastHistoryProgressBar!!.visibility = View.GONE
                historyIsLoading = false
            }
        }
    }

    override fun onGroupPresenceUpdated(
        accountJid: AccountJid, groupJid: ContactJid,
        presence: Presence
    ) {
        Application.getInstance().runOnUiThread {
            if (this.accountJid === accountJid && this.contactJid === groupJid) {
                if (presence.type != Presence.Type.unsubscribe) {
                    setupPinnedMessageView()
                    //todo add toolbar updating
                } else {
                    try {
                        Thread.sleep(1000)
                        activity!!.finish()
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
            if (accountJid == accountJid && contactJid == contactJid) {
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
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
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

    private fun showSecurityMenu() {
        val popup = PopupMenu(activity, securityButton)
        popup.inflate(R.menu.security)
        popup.setOnMenuItemClickListener(activity as PopupMenu.OnMenuItemClickListener?)
        val menu = popup.menu
        val securityLevel = OTRManager.getInstance().getSecurityLevel(accountJid, contactJid)
        if (securityLevel == SecurityLevel.plain) {
            menu.findItem(R.id.action_start_encryption)
                .setVisible(true).isEnabled =
                SettingsManager.securityOtrMode() != SettingsManager.SecurityOtrMode.disabled
        } else {
            menu.findItem(R.id.action_restart_encryption).isVisible = true
        }
        val isEncrypted = securityLevel != SecurityLevel.plain
        menu.findItem(R.id.action_stop_encryption).isEnabled = isEncrypted
        menu.findItem(R.id.action_verify_with_fingerprint).isEnabled = isEncrypted
        menu.findItem(R.id.action_verify_with_question).isEnabled = isEncrypted
        menu.findItem(R.id.action_verify_with_shared_secret).isEnabled = isEncrypted
        popup.show()
    }

    private fun setUpInputViewButtons() {
        var empty = inputView.text.toString().trim { it <= ' ' }.isEmpty()
        if (empty) {
            empty = bottomMessagesPanel == null
        }
        if (empty != isInputEmpty) {
            isInputEmpty = empty
        }
        if (isInputEmpty) {
            sendButton.visibility = View.GONE
            sendButton.setColorFilter(ColorManager.getInstance().accountPainter.greyMain)
            sendButton.isEnabled = false
            showSecurityButton(chat is RegularChat)
            recordButton.visibility = View.VISIBLE
            attachButton.visibility = View.VISIBLE
        } else {
            sendButton.visibility = View.VISIBLE
            sendButton.isEnabled = true
            sendButton.setColorFilter(accountColor)
            showSecurityButton(false)
            recordButton.visibility = View.GONE
            attachButton.visibility = View.GONE
        }
    }

    fun sendMessage() {
        val editable = inputView.editableText
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

                // split the quotespan into 1-line strings
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
                    sendForwardMessage(ids, text, markupText)
                }
            } else if (bottomMessagesPanel?.purpose == Purposes.EDITING) {
                bottomMessagesPanel?.messagesIds?.first()?.let { id ->
                    sendReplaceMessageTextRequest(accountJid, contactJid, id, text, this)
                    hideBottomMessagePanel()
                }
            }
        } else if (text.isNotEmpty()) {
            sendMessage(text, markupText, false)
        } else {
            return
        }
        playMessageSound()
        listener!!.onMessageSent()
        ChatStateManager.getInstance().cancelComposingSender()
        if (SettingsManager.chatsHideKeyboard() == SettingsManager.ChatsHideKeyboard.always
            || (activity!!.resources.getBoolean(R.bool.landscape)
                    && SettingsManager.chatsHideKeyboard() == SettingsManager.ChatsHideKeyboard.landscape)
        ) {
            Utils.hideKeyboard(activity)
        }
    }

    private fun sendMessage(
        text: String,
        markupText: String? = null,
        needToPullDown: Boolean = true
    ) {
        MessageManager.getInstance().sendMessage(accountJid, contactJid, text, markupText)
        if (needToPullDown) {
            setFirstUnreadMessageId(null)
        }
    }

    fun updateContact() {
        updateSecurityButton()
        updateSendButtonSecurityLevel()
        updateBlockedState()
        showNewContactLayoutIfNeed()
    }

    private fun onScrollDownClick() {
        val unread = chat.unreadMessageCount
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        if (unread == 0 || lastVisiblePosition + 2 >= chatMessageAdapter!!.itemCount - unread) {
            // scroll down
            scrollDown()

            // scroll to unread
        } else scrollToFirstUnread(unread)
    }

    private fun scrollDown() {
        realmRecyclerView.scrollToPosition(chatMessageAdapter!!.itemCount - 1)
    }

    private fun scrollToFirstUnread(unreadCount: Int) {
        layoutManager.scrollToPositionWithOffset(
            chatMessageAdapter!!.itemCount - unreadCount,
            200
        )
    }

    private fun updateSecurityButton() {
        securityButton.setImageLevel(
            OTRManager.getInstance().getSecurityLevel(accountJid, contactJid).imageLevel
        )
    }

    private fun showSecurityButton(show: Boolean) {
        val isSavedMessages = accountJid.bareJid.toString().contains(contactJid.bareJid.toString())
        securityButton.visibility = if (show && !isSavedMessages) View.VISIBLE else View.GONE
    }

    private fun updateSendButtonSecurityLevel() {
        sendButton.setImageLevel(
            OTRManager.getInstance().getSecurityLevel(accountJid, contactJid).imageLevel
        )
    }

    fun setInputText(additional: String) {
        skipOnTextChanges = true
        inputView.setText(additional)
        inputView.setSelection(additional.length)
        skipOnTextChanges = false
    }

    private fun setInputTextAtCursor(additional: String) {
        skipOnTextChanges = true
        val currentText = inputView.text.toString()
        if (currentText.isEmpty()) {
            inputView.setText(additional)
            inputView.setSelection(additional.length)
        } else {
            val cursorPosition = inputView.selectionStart
            val first = currentText.substring(0, cursorPosition)
            val second = currentText.substring(cursorPosition)
            inputView.setText(first + additional + second)
            inputView.setSelection(first.length + additional.length)
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
        val currentText = inputView.editableText
        if (currentText.isNotEmpty()) {
            val cursorPosition = inputView.selectionStart
            spanStart = cursorPosition
            if (cursorPosition != 0 && currentText[cursorPosition - 1] != '\n') {
                currentText.insert(cursorPosition, "\n")
                spanStart++
            }
        }
        val spanEnd: Int = spanStart + quote.length
        currentText.insert(spanStart, quote)
        currentText.setSpan(
            CustomQuoteSpan(
                accountColor,
                context!!.resources.displayMetrics
            ), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        inputView.setSelection(spanEnd)
        skipOnTextChanges = false
    }

    private fun clearInputText() {
        skipOnTextChanges = true
        inputView.text.clear()
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
        ChatExportDialogFragment.newInstance(accountJid, contactJid)
            .show(fragmentManager!!, "CHAT_EXPORT")
    }

    fun stopEncryption(account: AccountJid?, user: ContactJid?) {
        try {
            OTRManager.getInstance().endSession(account, user)
        } catch (e: NetworkException) {
            Application.getInstance().onError(e)
        }
    }

    private fun restartEncryption(account: AccountJid, user: ContactJid) {
        try {
            OTRManager.getInstance().refreshSession(account, user)
        } catch (e: NetworkException) {
            Application.getInstance().onError(e)
        }
    }

    private fun startEncryption(account: AccountJid, user: ContactJid) {
        try {
            OTRManager.getInstance().startSession(account, user)
        } catch (e: NetworkException) {
            Application.getInstance().onError(e)
        }
    }

    private fun deleteMessage(messages: ArrayList<MessageRealmObject?>) {
        val ids: MutableList<String> = ArrayList()
        var onlyOutgoing = true
        val isSavedMessages = accountJid.bareJid.toString().contains(contactJid.bareJid.toString())
        val isGroup = chat is GroupChat
        for (messageRealmObject in messages) {
            ids.add(messageRealmObject!!.primaryKey)
            if (messageRealmObject.isIncoming) onlyOutgoing = false
        }
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
            if (onlyOutgoing && !isSavedMessages && !isGroup) dialog.setView(checkBoxView)
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
            && inputView.text.toString().isNotEmpty()
        ) {
            clearInputText()
            hideBottomMessagePanel()
            true
        } else {
            false
        }
    }

    fun showResourceChoiceAlert(account: AccountJid, user: ContactJid, restartSession: Boolean) {
        val allPresences = RosterManager.getInstance().getPresences(account, user.jid)
        val items: MutableList<Map<String, String>> = ArrayList()
        for (presence in allPresences) {
            val fromJid = presence.from
            val clientInfo = CapabilitiesManager.getInstance().getCachedClientInfo(fromJid)
            var client = ""
            if (clientInfo == null) {
                CapabilitiesManager.getInstance().requestClientInfoByUser(account, fromJid)
            } else if (clientInfo === ClientInfo.INVALID_CLIENT_INFO) {
                client = getString(R.string.unknown)
            } else {
                val name = clientInfo.name
                if (name != null) {
                    client = name
                }
                val type = clientInfo.type
                if (type != null) {
                    client = if (client.isEmpty()) {
                        type
                    } else {
                        "$client/$type"
                    }
                }
            }
            val map: MutableMap<String, String> = HashMap()
            if (client.isNotEmpty()) {
                map[ResourceAdapter.KEY_CLIENT] = client
            }
            val resourceOrNull = fromJid.resourceOrNull
            if (resourceOrNull != null) {
                map[ResourceAdapter.KEY_RESOURCE] = resourceOrNull.toString()
                items.add(map)
            }
        }
        val adapter = ResourceAdapter(activity, items)
        adapter.checkedItem = checkedResource
        if (items.size > 0) {
            val builder = AlertDialog.Builder(
                activity!!
            )
            builder.setTitle(R.string.otr_select_resource)
            builder.setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                checkedResource = adapter.checkedItem
            }
            builder.setPositiveButton(R.string.ok) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                checkedResource = adapter.checkedItem
                try {
                    val chat = chat
                    if (chat is RegularChat) {
                        chat.otRresource =
                            Resourcepart.from(items[checkedResource][ResourceAdapter.KEY_RESOURCE])
                        if (restartSession) restartEncryption(account, user) else startEncryption(
                            account,
                            user
                        )
                    } else {
                        Toast.makeText(
                            activity,
                            R.string.otr_select_toast_error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: XmppStringprepException) {
                    LogManager.exception(javaClass.simpleName, e)
                    Toast.makeText(activity, R.string.otr_select_toast_error, Toast.LENGTH_SHORT)
                        .show()
                }
            }
            builder.setSingleChoiceItems(adapter, checkedResource, null).show()
        } else {
            Toast.makeText(
                activity,
                R.string.otr_select_toast_resources_not_found,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.avatar) {
            showContactInfo()
        } else if (v.id == R.id.btnScrollDown) onScrollDownClick()
    }

    fun showContactInfo() {
        startActivity(
            ContactViewerActivity.createIntent(activity, accountJid, contactJid)
        )
    }

    fun clearHistory(account: AccountJid, user: ContactJid) {
        if (account.bareJid.toString().contains(user.bareJid.toString())) {
            AlertDialog.Builder(context!!)
                .setTitle(resources.getString(R.string.dialog_delete_saved_messages__header))
                .setMessage(resources.getString(R.string.dialog_delete_saved_messages__confirm))
                .setPositiveButton(R.string.delete) { _: DialogInterface?, _: Int ->
                    if (isSupported(account)) {
                        sendRetractAllMessagesRequest(account, user, this)
                    } else {
                        MessageManager.getInstance().clearHistory(account, user)
                    }
                }
                .setNegativeButton(R.string.cancel_action) { _: DialogInterface?, _: Int -> }
                .show()
        } else {
            ChatHistoryClearDialog.newInstance(account, user).show(
                requireFragmentManager(),
                ChatHistoryClearDialog::class.java.simpleName
            )
        }
    }

    fun callAttention() {
        try {
            AttentionManager.getInstance().sendAttention(accountJid, contactJid)
        } catch (e: NetworkException) {
            Application.getInstance().onError(e)
        }
    }

    override fun onMessageClick(caller: View, position: Int) {
        val itemViewType = chatMessageAdapter!!.getItemViewType(position)
        if (itemViewType != MessagesAdapter.VIEW_TYPE_GROUPCHAT_SYSTEM_MESSAGE
            && itemViewType != MessagesAdapter.VIEW_TYPE_ACTION_MESSAGE
        ) {
            val clickedMessageRealmObject = chatMessageAdapter!!.getMessageItem(position)
            if (clickedMessageRealmObject == null) {
                LogManager.w(
                    ChatFragment::class.java.simpleName,
                    "onMessageClick null message item. Position: $position"
                )
                return
            }
            showCustomMenu(caller, clickedMessageRealmObject)
        }
    }

    override fun onMessageAvatarClick(position: Int) {
        val messageClicked = chatMessageAdapter!!.getMessageItem(position)
        if (chat is GroupChat) {
            val memberId = messageClicked!!.groupchatUserId
            startActivity(
                createIntentForGroupchatAndMemberId(
                    activity!!,
                    memberId,
                    (chat as GroupChat?)!!
                )
            )
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
                            activity!!,
                            innerMessage.groupchatUserId,
                            (ChatManager.getInstance()
                                .getChat(accountJid, contactJid) as GroupChat?)!!
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
        val isEditable = (checkedItems == 1 && isSupported(accountJid)
                && !chatMessageAdapter!!.checkedMessageRealmObjects[0]!!.isIncoming
                && !chatMessageAdapter!!.checkedMessageRealmObjects[0]!!.haveAttachments()
                && (chatMessageAdapter!!.checkedMessageRealmObjects[0]!!
            .messageStatus == MessageStatus.DELIVERED || chatMessageAdapter!!.checkedMessageRealmObjects[0]!!
            .messageStatus == MessageStatus.RECEIVED || chatMessageAdapter!!.checkedMessageRealmObjects[0]!!
            .messageStatus == MessageStatus.DISPLAYED))
        val isPinnable = checkedItems == 1 && chat is GroupChat
        if (checkedItems > 0) {
            interactionView!!.visibility = View.VISIBLE
            (activity as ChatActivity?)!!.showToolbarInteractionsPanel(
                true, isEditable,
                isPinnable, checkedItems
            )
            replySwipe!!.setSwipeEnabled(false)
        } else {
            interactionView!!.visibility = View.GONE
            replySwipe!!.setSwipeEnabled(true)
            (activity as ChatActivity?)!!.showToolbarInteractionsPanel(
                isVisible = false, isEditable = false,
                isPinnable = false, messagesCount = checkedItems
            )
        }
    }

    private fun showCustomMenu(anchor: View?, clickedMessageRealmObject: MessageRealmObject) {
        menuItems = ArrayList()
        if (clickedMessageRealmObject.messageStatus == MessageStatus.ERROR) {
            CustomMessageMenu.addMenuItem(
                menuItems,
                "action_message_repeat",
                getString(R.string.message_repeat)
            )
        }
        if (clickedMessageRealmObject.messageStatus != MessageStatus.UPLOADING) {
            CustomMessageMenu.addMenuItem(
                menuItems,
                "action_message_quote",
                getString(R.string.message_quote)
            )
            CustomMessageMenu.addMenuItem(
                menuItems,
                "action_message_copy",
                getString(R.string.message_copy)
            )
            CustomMessageMenu.addMenuItem(
                menuItems,
                "action_message_remove",
                getString(R.string.message_remove)
            )
        }
        if (!clickedMessageRealmObject.isIncoming && !clickedMessageRealmObject.haveAttachments()
            && (clickedMessageRealmObject.messageStatus == MessageStatus.DELIVERED
                    || clickedMessageRealmObject.messageStatus == MessageStatus.DISPLAYED
                    || clickedMessageRealmObject.messageStatus == MessageStatus.RECEIVED)
        ) {
            CustomMessageMenu.addMenuItem(
                menuItems,
                "action_message_edit",
                getString(R.string.message_edit)
            )
        }
        if (OTRManager.getInstance().isEncrypted(clickedMessageRealmObject.text)) {
            CustomMessageMenu.addMenuItem(
                menuItems,
                "action_message_show_original_otr",
                getString(R.string.message_otr_show_original)
            )
        }
        if (chat is GroupChat) {
            CustomMessageMenu.addMenuItem(
                menuItems,
                "action_message_pin",
                getString(R.string.message_pin)
            )
        }
        if (accountJid.bareJid.toString().contains(contactJid.bareJid.toString())
            && clickedMessageRealmObject.isIncoming && clickedMessageRealmObject.hasForwardedMessages()
        ) {
            val innerMessage = MessageRepository.getForwardedMessages(clickedMessageRealmObject)[0]
            CustomMessageMenu.addTimestamp(menuItems, innerMessage.timestamp)
        }
        CustomMessageMenu.addStatus(menuItems, clickedMessageRealmObject.messageStatus)
        val listener =
            AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                if (menuItems != null && menuItems?.size ?: 0 > position) {
                    val menuItem = menuItems?.get(position)
                    when (menuItem?.get(CustomMessageMenuAdapter.KEY_ID)) {
                        "action_message_repeat" -> if (clickedMessageRealmObject.haveAttachments()) {
                            HttpFileUploadManager.getInstance()
                                .retrySendFileMessage(clickedMessageRealmObject, activity)
                        } else sendMessage(clickedMessageRealmObject.text)
                        "action_message_copy" -> {
                            ClipManager.copyMessagesToClipboard(
                                mutableListOf(clickedMessageRealmObject.primaryKey)
                            )
                        }
                        "action_message_appeal" -> mentionUser(clickedMessageRealmObject.resource.toString())
                        "action_message_quote" -> setQuote(clickedMessageRealmObject)
                        "action_message_remove" -> {
                            val arrayList = ArrayList<MessageRealmObject?>()
                            arrayList.add(clickedMessageRealmObject)
                            deleteMessage(arrayList)
                        }
                        "action_message_show_original_otr" -> {
                            chatMessageAdapter!!.addOrRemoveItemNeedOriginalText(
                                clickedMessageRealmObject.primaryKey
                            )
                            chatMessageAdapter!!.notifyDataSetChanged()
                        }
                        CustomMessageMenuAdapter.KEY_ID_STATUS -> if (clickedMessageRealmObject.messageStatus == MessageStatus.ERROR) {
                            showError(clickedMessageRealmObject.errorDescription)
                        }
                        "action_message_edit" -> getReadyForMessageEditing(clickedMessageRealmObject)
                        "action_message_pin" -> sendPinMessageRequest(clickedMessageRealmObject)
                        else -> {
                        }
                    }
                }
            }
        CustomMessageMenu.showMenu(activity, anchor, menuItems, listener, this)
    }

    override fun onDismiss() {
        menuItems = null
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
        Application.getInstance().runOnUiThread { chatMessageAdapter!!.notifyDataSetChanged() }
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

    override val lastVisiblePosition: Int
        get() = layoutManager.findLastVisibleItemPosition()

    override fun scrollTo(position: Int) {
        layoutManager.scrollToPosition(position)
    }

    override fun onMessagesUpdated() {
        loadHistoryIfNeed()
    }

    private fun showHideNotifyIfNeed() {
        (chat as? RegularChat)?.let {
            notifyIntent = it.intent
            if (notifyIntent != null) {
                setupNotifyLayout(notifyIntent!!)
            } else {
                notifyLayout?.visibility = View.GONE
            }
        }
    }

    private fun setupNotifyLayout(notifyIntent: Intent) {
        if (tvNotifyTitle == null || tvNotifyAction == null) {
            inflateNotifyLayout()
        }
        if (notifyIntent.getBooleanExtra(QuestionActivity.EXTRA_FIELD_CANCEL, false)) {
            tvNotifyTitle!!.setText(R.string.otr_verification_progress_title)
            tvNotifyAction!!.setText(R.string.otr_verification_notify_button_cancel)
        } else {
            tvNotifyTitle!!.setText(R.string.otr_verification_notify_title)
            tvNotifyAction!!.setText(R.string.otr_verification_notify_button)
        }
        notifyLayout?.visibility = View.VISIBLE
    }

    private fun inflateNotifyLayout() {
        val view = stubNotify!!.inflate()
        tvNotifyTitle = view.findViewById(R.id.tvNotifyTitle)
        tvNotifyAction = view.findViewById(R.id.tvNotifyAction)
        notifyLayout = view.findViewById(R.id.notifyLayout)
        notifyLayout?.setOnClickListener {
            if (notifyIntent != null) {
                startActivity(notifyIntent)
            }
            it.visibility = View.GONE
        }
    }

    private fun inflateIncomingInvite(
        senderAvatar: Drawable,
        senderName: String,
        reasonText: String,
        balloonColor: Int
    ) {
        val view = stubInvite!!.inflate()
        (view.findViewById<View>(R.id.avatar) as ImageView).setImageDrawable(senderAvatar)
        (view.findViewById<View>(R.id.message_text) as TextView).text = reasonText
        (view.findViewById<View>(R.id.message_header) as TextView).text = senderName
        view.findViewById<View>(R.id.message_status_icon).visibility =
            View.GONE
        view.findViewById<View>(R.id.message_encrypted_icon).visibility = View.GONE
        val messageBalloon = view.findViewById<View>(R.id.message_balloon)
        val messageShadow = view.findViewById<View>(R.id.message_shadow)
        val balloonDrawable = context?.resources?.let {
            ResourcesCompat.getDrawable(it, R.drawable.msg_in, null)
        }
        val shadowDrawable = context?.resources?.let {
            ResourcesCompat.getDrawable(it, R.drawable.msg_in_shadow, null)
        }
        context?.resources?.let {
            shadowDrawable?.setColorFilter(
                it.getColor(R.color.black),
                PorterDuff.Mode.MULTIPLY
            )
        }
        messageBalloon.background = balloonDrawable
        messageShadow.background = shadowDrawable

        // setup BALLOON margins
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.setMargins(
            Utils.dipToPx(3f, context),
            Utils.dipToPx(0f, context),
            Utils.dipToPx(0f, context),
            Utils.dipToPx(3f, context)
        )
        messageShadow.layoutParams = layoutParams

        // setup MESSAGE padding
        messageBalloon.setPadding(
            Utils.dipToPx(20f, context),
            Utils.dipToPx(8f, context),
            Utils.dipToPx(12f, context),
            Utils.dipToPx(8f, context)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            messageBalloon.background.setTintList(
                ColorManager.getInstance().getChatIncomingBalloonColorsStateList(accountJid)
            )
        }
    }

    private fun deflateIncomingInvite() {
        Application.getInstance().runOnUiThread { stubInvite!!.visibility = View.GONE }
    }

    private fun updateBlockedState() {
        userIsBlocked =
            BlockingManager.getInstance().contactIsBlockedLocally(accountJid, contactJid)
        if (userIsBlocked) {
            showBlockedBar()
        } else {
            if (blockedView != null) {
                blockedView!!.visibility = View.GONE
            }
            inputLayout.visibility = View.VISIBLE
        }
    }

    private fun showBlockedBar() {
        for (i in 0 until inputPanel!!.childCount) {
            val view = inputPanel!!.getChildAt(i)
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
                blockedView!!.setTextAppearance(R.style.TextAppearance_AppCompat_Widget_Button)
            } else {
                blockedView!!.setTextAppearance(
                    context,
                    R.style.TextAppearance_AppCompat_Widget_Button
                )
            }
            blockedView!!.setTextColor(accountColor)
            blockedView!!.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            blockedView!!.setText(R.string.blocked_contact_message)
            blockedView!!.setBackgroundColor(
                Utils.getAttrColor(
                    context,
                    R.attr.chat_input_background
                )
            )
            blockedView!!.layoutParams = layoutParams
            blockedView!!.gravity = Gravity.CENTER
            blockedView!!.setOnClickListener {
                startActivity(
                    ContactViewerActivity.createIntent(
                        context, accountJid, contactJid
                    )
                )
            }
            inputPanel!!.addView(blockedView)
        } else {
            blockedView!!.visibility = View.VISIBLE
        }
    }

    private fun showNewContactLayoutIfNeed() {
        if (userIsBlocked) {
            if (newContactLayout != null) {
                newContactLayout!!.visibility = View.GONE
            }
            return
        }
        if (!VCardManager.getInstance().isRosterOrHistoryLoaded(accountJid)) {
            return
        }
        val subscriptionState =
            RosterManager.getInstance().getSubscriptionState(accountJid, contactJid)
        val inRoster = RosterManager.getInstance().getRosterContact(accountJid, contactJid) != null
        var show = false
        when (subscriptionState.subscriptionType) {
            RosterManager.SubscriptionState.FROM, RosterManager.SubscriptionState.NONE -> {
                //check both FROM and NONE types for the absence of pending subscriptions,
                //and whether or not user already closed this "suggestion" dialog
                if (subscriptionState.pendingSubscription == RosterManager.SubscriptionState.PENDING_NONE) {
                    if (!chat.isAddContactSuggested) {
                        show = true
                    }
                }
                //check all states for incoming + incoming & outgoinig types of pending subscriptions.
                //
                // NONE can be valid with both IN and IN_OUT
                // TO can be valid only with IN.(TO && any type of OUT request are incompatible).
                // FROM can not be valid with these checks. (FROM && any type of IN request are incompatible).
                if (subscriptionState.hasIncomingSubscription()) {
                    show = true
                }
            }
            RosterManager.SubscriptionState.TO -> if (subscriptionState.hasIncomingSubscription()) {
                show = true
            }
        }
        if (hasActiveIncomingInvites(accountJid, contactJid)) {
            show = true
        }
        if (chat.account.bareJid.toString() == chat.contactJid.bareJid.toString()) {
            show = false
        }
        if (show) {
            inflateNewContactLayout(subscriptionState, inRoster)
        } else {
            if (newContactLayout != null) {
                newContactLayout!!.visibility = View.GONE
            }
            clearSubscriptionRequestNotification(accountJid, contactJid)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun inflateNewContactLayout(
        subscriptionState: RosterManager.SubscriptionState,
        inRoster: Boolean
    ) {
        if (newContactLayout == null) {
            newContactLayout = stubNewContact!!.inflate() as ViewGroup
        }

        //intercept touch events to avoid clicking on the messages behind the panel.
        newContactLayout!!.setOnTouchListener { _: View?, _: MotionEvent? -> true }
        manageToolbarElevation(true)
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
            newContactLayout!!.setBackgroundResource(R.color.grey_950)
        }
        val transition: Transition = Slide(Gravity.TOP)
        transition.duration = 300
        transition.addTarget(newContactLayout!!)
        val bestContact = RosterManager.getInstance().getBestContact(accountJid, contactJid)
        val name = if (bestContact != null) bestContact.name else contactJid.toString()
        addContact = newContactLayout!!.findViewById(R.id.add_contact)
        blockContact = newContactLayout!!.findViewById(R.id.block_contact)
        val closeNewContactLayout =
            newContactLayout!!.findViewById<ImageButton>(R.id.close_new_contact_layout)
        when (subscriptionState.subscriptionType) {
            RosterManager.SubscriptionState.FROM, RosterManager.SubscriptionState.NONE -> when (subscriptionState.pendingSubscription) {
                RosterManager.SubscriptionState.PENDING_NONE -> if (inRoster) {
                    // FROM = contact is subscribed to our presence. No pending subscription requests. Only in roster.
                    // NONE = No current subscriptions or requests. In roster.
                    setNewContactSubscribeLayout()
                } else {
                    // NONE = No current subscriptions or requests. Not in roster.
                    if (hasActiveIncomingInvites(accountJid, contactJid)) {
                        setInvitedToGroupLayout()
                    } else {
                        setNewContactAddLayout()
                    }
                }
                RosterManager.SubscriptionState.PENDING_IN ->                         // NONE + PENDING_IN = No current subscriptions and a pending request from contact to us.
                    setNewContactAddLayout()
                RosterManager.SubscriptionState.PENDING_IN_OUT ->                         // NONE + PENDING_IN_OUT = No current subscriptions, pending requests to each other.
                    setNewContactAllowLayout()
            }
            RosterManager.SubscriptionState.TO -> if (subscriptionState.pendingSubscription == RosterManager.SubscriptionState.PENDING_IN) {
                // TO + PENDING_IN = We are subscribed to contact's presence. Contact sent us a subscription request.
                setNewContactAllowLayout()
            }
        }
        addContact.setTextColor(
            ColorManager.getInstance().accountPainter.getAccountMainColor(
                accountJid
            )
        )
        addContact.setOnClickListener {
            Application.getInstance().runInBackgroundNetworkUserRequest {
                try {
                    if (hasActiveIncomingInvites(accountJid, contactJid)) {
                        acceptInvitation(accountJid, contactJid)
                        deflateIncomingInvite()
                    } else {
                        if (!inRoster) {
                            RosterManager.getInstance().createContact(
                                accountJid,
                                contactJid,
                                name,
                                ArrayList()
                            ) // Create contact if not in roster. (subscription request is sent automatically)
                        } else {
                            if (subscriptionState.subscriptionType == RosterManager.SubscriptionState.FROM // Either only an active subscription to us OR
                                || subscriptionState.subscriptionType == RosterManager.SubscriptionState.NONE
                            ) {        // No active subscriptions.
                                if (!subscriptionState.hasOutgoingSubscription()) {                                // No outgoing subscription at the moment
                                    subscribeForPresence(
                                        accountJid,
                                        contactJid
                                    ) // So we try to subscribe for contact's presence.
                                }
                            }
                        }
                        if (subscriptionState.subscriptionType == RosterManager.SubscriptionState.TO) {                   // If we are currently subscribed to contact
                            addAutoAcceptSubscription(
                                accountJid,
                                contactJid
                            ) // Preemptively allow incoming subscription request.
                        } else if (subscriptionState.subscriptionType == RosterManager.SubscriptionState.NONE) {          // If there are no subscriptions
                            if (subscriptionState.hasIncomingSubscription()) {                                   // If we have incoming subscription request
                                acceptSubscription(
                                    accountJid,
                                    contactJid,
                                    false
                                ) // "quietly" accept it (since we are in the process of
                            } else {                                                                             // adding a contact, we don't need to create unnecessary Action messages
                                addAutoAcceptSubscription(
                                    accountJid,
                                    contactJid
                                ) // or Preemptively allow incoming request.
                            }
                        }
                    }
                } catch (e: Exception) {
                    LogManager.exception(ChatFragment::class.java.simpleName, e)
                }
            }
            TransitionManager.beginDelayedTransition((rootView as ViewGroup?)!!, transition)
            newContactLayout!!.visibility = View.GONE
            manageToolbarElevation(false)
        }
        blockContact.setTextColor(
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) resources.getColor(
                R.color.red_700
            ) else resources.getColor(R.color.red_900)
        )
        blockContact.setOnClickListener {
            try {
                // fully discard subscription
                if (hasActiveIncomingInvites(accountJid, contactJid)) {
                    declineInvitation(accountJid, contactJid)
                }
                discardSubscription(accountJid, contactJid)
                unsubscribeFromPresence(accountJid, contactJid)
            } catch (e: NetworkException) {
                Application.getInstance().onError(R.string.CONNECTION_FAILED)
            }
            if (!hasActiveIncomingInvites(accountJid, contactJid)) BlockingManager.getInstance()
                .blockContact(accountJid, contactJid, object : BlockContactListener {
                    override fun onSuccessBlock() {
                        Toast.makeText(
                            Application.getInstance(),
                            R.string.contact_blocked_successfully,
                            Toast.LENGTH_SHORT
                        ).show()
                        if (newContactLayout != null) {
                            if (newContactLayout!!.visibility == View.VISIBLE) newContactLayout!!.visibility =
                                View.GONE
                        }
                        activity!!.finish()
                    }

                    override fun onErrorBlock() {
                        Toast.makeText(
                            Application.getInstance(),
                            R.string.error_blocking_contact,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            TransitionManager.beginDelayedTransition((rootView as ViewGroup?)!!, transition)
        }
        closeNewContactLayout.setOnClickListener {
            if (subscriptionState.hasIncomingSubscription()) {                              // check if we have an incoming (IN) subscription
                try {
                    discardSubscription(accountJid, contactJid) // discard it on "X"-press
                } catch (e: NetworkException) {
                    LogManager.exception(javaClass.simpleName, e)
                }
            }
            chat.isAddContactSuggested = true
            TransitionManager.beginDelayedTransition((rootView as ViewGroup?)!!, transition)
            newContactLayout!!.visibility = View.GONE
            manageToolbarElevation(false)
        }
    }

    private fun setInvitedToGroupLayout() {
        val addContactMessage = newContactLayout!!.findViewById<TextView>(R.id.add_contact_message)
        addContactMessage.visibility = View.GONE
        addContact.setText(R.string.groupchat_join)
        blockContact.setText(R.string.groupchat_decline)
        blockContact.visibility = View.VISIBLE
    }

    private fun setNewContactSubscribeLayout() {
        val addContactMessage = newContactLayout!!.findViewById<TextView>(R.id.add_contact_message)
        addContact.setText(R.string.chat_subscribe)
        addContactMessage.setText(R.string.chat_subscribe_request_outgoing)
        addContactMessage.visibility = View.VISIBLE
        blockContact.visibility = View.GONE
    }

    private fun setNewContactAddLayout() {
        val addContactMessage = newContactLayout!!.findViewById<TextView>(R.id.add_contact_message)
        addContactMessage.visibility = View.GONE
        addContact.setText(R.string.contact_add)
        blockContact.visibility = View.VISIBLE
    }

    private fun setNewContactAllowLayout() {
        val addContactMessage = newContactLayout!!.findViewById<TextView>(R.id.add_contact_message)
        addContact.setText(R.string.chat_allow)
        addContactMessage.setText(R.string.chat_subscribe_request_incoming)
        addContactMessage.visibility = View.VISIBLE
        blockContact.visibility = View.GONE
    }

    // remove/recreate activity's toolbar elevation.
    private fun manageToolbarElevation(remove: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if ((activity as ChatActivity?)!!.toolbar.elevation != 0f) {
                toolbarElevation = (activity as ChatActivity?)!!.toolbar.elevation
            }
            (activity as ChatActivity?)!!.toolbar.elevation = if (remove) 0f else toolbarElevation
        }
    }

    override fun onBind(message: MessageRealmObject) {
        if (message.isValid && !message.isRead) {
            chat.markAsRead(message, true)
            updateNewReceivedMessageCounter(chat.unreadMessageCount)
        }
    }

    private fun showScrollDownButtonIfNeed() {
        if (layoutManager.findLastVisibleItemPosition() >= chatMessageAdapter!!.itemCount - 1
            || currentVoiceRecordingState == VoiceRecordState.TouchRecording
            || currentVoiceRecordingState == VoiceRecordState.NoTouchRecording
        ) {
            btnScrollDown.visibility = View.GONE
        } else {
            btnScrollDown.visibility = View.VISIBLE
        }
    }

    private fun updateNewReceivedMessageCounter(count: Int) {
        tvNewReceivedCount!!.text = count.toString()
        if (count > 0) {
            tvNewReceivedCount!!.visibility = View.VISIBLE
        } else {
            tvNewReceivedCount!!.visibility = View.GONE
        }
    }

    private fun setFirstUnreadMessageId(id: String?) {
        chatMessageAdapter!!.setFirstUnreadMessageId(id)
        chatMessageAdapter!!.notifyDataSetChanged()
    }

    private fun closeInteractionPanel() {
        chatMessageAdapter!!.resetCheckedItems()
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
        Utils.lockScreenRotation(activity, false)
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
        recordingPresenterLayout!!.visibility = View.VISIBLE

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
        recordingPlayButton!!.setImageResource(R.drawable.ic_play)
        recordingPlayButton!!.setOnClickListener {
            VoiceManager.getInstance().voiceClicked(recordingPath)
        }
        recordingDeleteButton!!.setOnClickListener {
            releaseRecordedVoicePlayback(recordingPath)
            finishVoiceRecordLayout()
            recordingPath = null
            audioProgressSubscription!!.unsubscribe()
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
            audioProgressSubscription!!.unsubscribe()
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
                recordingPlayButton!!.setImageResource(R.drawable.ic_play)
            } else recordingPlayButton!!.setImageResource(R.drawable.ic_pause)
        }
    }

    fun finishVoiceRecordLayout() {
        recordingPresenterLayout!!.visibility = View.GONE
        recordingPresenter.updateVisualizer(null)
        currentVoiceRecordingState = VoiceRecordState.NotRecording
        closeVoiceRecordPanel()
        changeStateOfInputViewButtonsTo(true)
    }

    private fun closeVoiceRecordPanel() {
        recordLockView.visibility = View.GONE
        voiceMessageRecorderLayout.visibility = View.GONE
        cancelRecordingLayout!!.visibility = View.GONE
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
        Utils.performHapticFeedback(rootView)
        Utils.lockScreenRotation(activity, false)
    }

    private fun changeStateOfInputViewButtonsTo(state: Boolean) {
        rootView.findViewById<View>(R.id.button_emoticon).isEnabled =
            state
        attachButton.isEnabled = state
        securityButton.isEnabled = state
    }

    private fun beginTimer(start: Boolean) {
        if (start) {
            voiceMessageRecorderLayout.visibility = View.VISIBLE
        }
        if (start) {
            ChatStateManager.getInstance()
                .onComposing(accountJid, contactJid, null, ChatStateSubtype.voice)
            stopTypingTimer!!.cancel()
            ignoreReceiver = false
            slideToCancelLayout!!.animate().x(0f).setDuration(0).start()
            recordLockChevronImage!!.alpha = 1f
            recordLockImage!!.setImageResource(R.drawable.ic_security_plain_24dp)
            recordLockImage!!.setPadding(0, Utils.dipToPx(4f, activity), 0, 0)
            val layoutParams = recordLockChevronImage!!.layoutParams as LinearLayout.LayoutParams
            layoutParams.topMargin = 0
            recordLockChevronImage!!.layoutParams = layoutParams
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
        if (activity != null) {
            if (keepScreenOn) {
                activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
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
                        replace(R.id.secondBottomPanel, panel)
                        commit()
                    }
                }
        }
    }

    fun hideBottomMessagePanel() {
        setUpInputViewButtons()
        if (bottomMessagesPanel?.purpose == Purposes.EDITING) {
            inputView.setText("")
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

    private fun sendForwardMessage(messages: List<String>, text: String, markup: String?) {
        ForwardManager.forwardMessage(messages, accountJid, contactJid, text, markup)
        hideBottomMessagePanel()
        setFirstUnreadMessageId(null)
    }

    private fun openChooserForForward(forwardIds: List<String>) {
        if (accountJid.bareJid.toString().contains(contactJid.bareJid.toString())) {
            val rightSavedMessagesIds = ArrayList<String>()
            for (messageId in forwardIds) {
                val message = MessageRepository.getMessageFromRealmByPrimaryKey(messageId)
                if (message.account.bareJid.toString().contains(message.user.bareJid.toString())
                    && message.hasForwardedMessages()
                ) {
                    for (innerMessageId in message.forwardedIds) {
                        rightSavedMessagesIds.add(innerMessageId.forwardMessageId)
                    }
                } else {
                    rightSavedMessagesIds.add(messageId)
                }
            }
            (activity as ChatActivity?)!!.forwardMessages(rightSavedMessagesIds)
        } else {
            (activity as ChatActivity?)!!.forwardMessages(forwardIds)
        }
    }

    override fun onSend() {}
    override fun onResult() {}
    override fun onOtherError(exception: Exception?) {}
    override fun processException(exception: Exception?) {
        Application.getInstance().runOnUiThread {
            Toast.makeText(
                requireContext(),
                (exception as? XMPPErrorException?)?.xmppError?.stanza?.toXML().toString(),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onIqError(error: XMPPError) {
        Application.getInstance().runOnUiThread {
            Toast.makeText(
                requireContext(),
                error.conditionText,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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

        private const val VOICE_MESSAGE = "VOICE_MESSAGE"
        private const val VOICE_MESSAGE_RECEIVER_IGNORE = "VOICE_MESSAGE_RECEIVER_IGNORE"
        private const val FORWARD_MESSAGES = "FORWARD_MESSAGES"
        private const val FORWARD_PURPOSE = "FORWARD_PURPOSE"

        private const val PERMISSIONS_REQUEST_EXPORT_CHAT = 22
        private const val STOP_TYPING_DELAY: Long = 2500 // in ms

        fun newInstance(accountJid: AccountJid, contactJid: ContactJid) = ChatFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARGUMENT_ACCOUNT, accountJid)
                putParcelable(ARGUMENT_USER, contactJid)
            }
        }

    }

}