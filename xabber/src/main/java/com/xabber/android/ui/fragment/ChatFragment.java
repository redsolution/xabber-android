package com.xabber.android.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Slide;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.capability.ClientInfo;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.mam.LastHistoryLoadFinishedEvent;
import com.xabber.android.data.extension.mam.LastHistoryLoadStartedEvent;
import com.xabber.android.data.extension.mam.NextMamManager;
import com.xabber.android.data.extension.mam.PreviousHistoryLoadFinishedEvent;
import com.xabber.android.data.extension.mam.PreviousHistoryLoadStartedEvent;
import com.xabber.android.data.extension.otr.AuthAskEvent;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.extension.references.mutable.voice.VoiceManager;
import com.xabber.android.data.extension.references.mutable.voice.VoiceMessagePresenterManager;
import com.xabber.android.data.extension.rrr.RrrManager;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.ClipManager;
import com.xabber.android.data.message.ForwardManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.MessageUpdateEvent;
import com.xabber.android.data.message.NewIncomingMessageEvent;
import com.xabber.android.data.message.NewMessageEvent;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.RegularChat;
import com.xabber.android.data.message.chat.groupchat.GroupChat;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.roster.RosterManager.SubscriptionState;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.android.ui.activity.ContactViewerActivity;
import com.xabber.android.ui.activity.QuestionActivity;
import com.xabber.android.ui.adapter.CustomMessageMenuAdapter;
import com.xabber.android.ui.adapter.ResourceAdapter;
import com.xabber.android.ui.adapter.chat.IncomingMessageVH;
import com.xabber.android.ui.adapter.chat.MessageVH;
import com.xabber.android.ui.adapter.chat.MessagesAdapter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.dialog.ChatExportDialogFragment;
import com.xabber.android.ui.dialog.ChatHistoryClearDialog;
import com.xabber.android.ui.helper.PermissionsRequester;
import com.xabber.android.ui.text.CustomQuoteSpan;
import com.xabber.android.ui.widget.BottomMessagesPanel;
import com.xabber.android.ui.widget.CustomMessageMenu;
import com.xabber.android.ui.widget.IntroViewDecoration;
import com.xabber.android.ui.widget.MessageHeaderViewDecoration;
import com.xabber.android.ui.widget.PlayerVisualizerView;
import com.xabber.android.ui.widget.ReplySwipeCallback;
import com.xabber.android.utils.Utils;
import com.xabber.xmpp.uuu.ChatStateSubtype;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import github.ankushsachdeva.emojicon.EmojiconsPopup;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

public class ChatFragment extends FileInteractionFragment implements PopupMenu.OnMenuItemClickListener,
        View.OnClickListener, Toolbar.OnMenuItemClickListener, MessageVH.MessageClickListener,
        MessagesAdapter.Listener, AdapterView.OnItemClickListener, PopupWindow.OnDismissListener,
        OnAccountChangedListener, BottomMessagesPanel.OnCloseListener, IncomingMessageVH.BindListener {

    public static final String ARGUMENT_ACCOUNT = "ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "ARGUMENT_USER";
    public static final String VOICE_MESSAGE = "VOICE_MESSAGE";
    private static final String VOICE_MESSAGE_RECEIVER_IGNORE = "VOICE_MESSAGE_RECEIVER_IGNORE";
    private static final String FORWARD_STATE = "FORWARD_STATE";
    private static final String FORWARD_MESSAGES = "FORWARD_MESSAGES";
    private static final String FORWARD_PURPOSE = "FORWARD_PURPOSE";
    private static final String LOG_TAG = ChatFragment.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_EXPORT_CHAT = 22;
    private final long STOP_TYPING_DELAY = 2500; // in ms
    boolean isInputEmpty = true;
    private FrameLayout inputPanel;
    private EditText inputView;
    private ImageButton sendButton;
    private ImageButton securityButton;
    private ImageButton attachButton;
    private ImageButton recordButton;
    private View lastHistoryProgressBar;
    private View previousHistoryProgressBar;
    private TextView blockedView;
    private ViewStub stubNotify;
    private RelativeLayout notifyLayout;
    private TextView tvNotifyTitle;
    private TextView tvNotifyAction;
    private View rootView;
    private RecyclerView realmRecyclerView;
    private MessagesAdapter chatMessageAdapter;
    private LinearLayoutManager layoutManager;
    private ReplySwipeCallback replySwipe;
    private View placeholder;
    private LinearLayout inputLayout;
    private ViewStub stubIntro;
    private ViewGroup chatIntroLayout;
    private ViewStub stubNewContact;
    private ViewGroup newContactLayout;
    private TextView addContact;
    private TextView blockContact;
    private ImageButton closeNewContactLayout;
    private ViewStub stubJoin;
    private LinearLayout joinLayout;
    private LinearLayout actionJoin;
    private RelativeLayout btnScrollDown;
    private TextView tvNewReceivedCount;
    private View interactionView;
    private TextView tvCount;
    private ImageView ivEdit;
    private boolean skipOnTextChanges = false;

    private Handler handler = new Handler();
    private VoiceRecordState currentVoiceRecordingState = VoiceRecordState.NotRecording;
    private boolean recordSaveAllowed = false;
    private int lockViewHeightSize;
    private int lockViewMarginBottom;
    private int fabMicViewHeightSize;
    private int fabMicViewMarginBottom;
    private float rootViewHeight;
    private float rootViewWidth;
    private String recordingPath;

    //Voice message recorder layout
    private RelativeLayout voiceMessageRecorderLayout;
    private FloatingActionButton recordButtonExpanded;
    private View recordLockView;
    private ImageView recordLockImage;
    private ImageView recordLockChevronImage;
    private Chronometer recordTimer;
    private LinearLayout slideToCancelLayout;
    private LinearLayout cancelRecordingLayout;
    private final Runnable postAnimation = this::closeVoiceRecordPanel;
    private TextView cancelRecordingTextView;
    private LinearLayout recordingPresenterLayout;
    private LinearLayout recordingPresenterPlaybarLayout;
    private PlayerVisualizerView recordingPresenter;
    private ImageButton recordingPlayButton;
    private ImageButton recordingDeleteButton;
    private ImageButton recordingPresenterSendButton;
    private TextView recordingPresenterDuration;
    private PublishSubject<VoiceManager.PublishAudioProgress.AudioInfo> audioProgress;
    private Subscription audioProgressSubscription;
    private boolean isReply = false;
    private ChatViewerFragmentListener listener;
    private MessageRealmObject clickedMessageRealmObject;
    private Timer stopTypingTimer = new Timer();
    protected final Runnable timer = new Runnable() {
        @Override
        public void run() {
            changeStateOfInputViewButtonsTo(false);
            recordLockView.setVisibility(View.VISIBLE);
            Utils.performHapticFeedback(rootView);

            recordButtonExpanded.show();

            recordLockView.startAnimation(AnimationUtils.loadAnimation(getActivity()
                    .getApplication(), R.anim.fade_in_200));

            recordLockView.animate()
                    .y(rootViewHeight - (lockViewMarginBottom + lockViewHeightSize))
                    .setDuration(300)
                    .start();

            beginTimer(currentVoiceRecordingState == VoiceRecordState.InitiatedRecording);

        }
    };
    private boolean historyIsLoading = false;
    private RealmResults<MessageRealmObject> messageRealmObjects;
    private List<HashMap<String, String>> menuItems = null;
    private boolean userIsBlocked = false;
    private int checkedResource; // use only for alert dialog
    private float toolbarElevation;
    private int accountColor;
    private Intent notifyIntent;
    private boolean sendByEnter;
    private BottomMessagesPanel bottomMessagesPanel;
    private List<String> bottomPanelMessagesIds = new ArrayList<>();

    public static ChatFragment newInstance(AccountJid account, ContactJid user) {
        ChatFragment fragment = new ChatFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putParcelable(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (ChatViewerFragmentListener) activity;
            listener.registerChatFragment(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ChatViewerFragmentListener");
        }
        registerOpusBroadcastReceiver();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        account = args.getParcelable(ARGUMENT_ACCOUNT);
        user = args.getParcelable(ARGUMENT_USER);

        LogManager.i(this, "onCreate " + user);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final View view = inflater.inflate(R.layout.fragment_chat, container, false);

        accountColor = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);

        tvNewReceivedCount = view.findViewById(R.id.tvNewReceivedCount);
        btnScrollDown = view.findViewById(R.id.btnScrollDown);
        btnScrollDown.setOnClickListener(this);

        rootView = view.findViewById(R.id.root_view);
        rootView.addOnLayoutChangeListener((view1, left, top, right, bottom, leftOld, topOld,
                                            rightOld, bottomOld) -> {

            int heightOld = bottomOld - topOld;
            if (heightOld != view1.getHeight()) {
                rootViewHeight = view1.getHeight();
                rootViewWidth = view1.getWidth();
            }
        });

        inputPanel = view.findViewById(R.id.bottomPanel);

        sendButton = view.findViewById(R.id.button_send_message);
        sendButton.setColorFilter(ColorManager.getInstance().getAccountPainter().getGreyMain());

        attachButton = view.findViewById(R.id.button_attach);
        attachButton.setOnClickListener(v -> {
            onAttachButtonPressed();
            forwardIdsForAttachments(bottomPanelMessagesIds);
        });

        recordButton = view.findViewById(R.id.button_record);

        recordButton.setOnTouchListener((view12, motionEvent) -> {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    if (PermissionsRequester.requestRecordAudioPermissionIfNeeded(getActivity(),
                            PERMISSIONS_REQUEST_RECORD_AUDIO))
                        if (PermissionsRequester.requestFileWritePermissionIfNeeded(getActivity(),
                                PERMISSIONS_REQUEST_RECORD_AUDIO)) {
                            if (currentVoiceRecordingState == VoiceRecordState.NotRecording) {
                                LinearLayout.LayoutParams lParams = (LinearLayout.LayoutParams)
                                        view12.getLayoutParams();

                                recordButtonExpanded.setImageResource(R.drawable.ic_microphone);
                                recordSaveAllowed = false;
                                slideToCancelLayout.setAlpha(1.0f);
                                recordLockView.setAlpha(1.0f);
                                handler.postDelayed(record, 500);
                                handler.postDelayed(timer, 500);
                                currentVoiceRecordingState = VoiceRecordState.InitiatedRecording;
                                Utils.lockScreenRotation(getActivity(), true);
                            }
                        }
                    break;
                case MotionEvent.ACTION_UP:
                    if (currentVoiceRecordingState == VoiceRecordState.TouchRecording) {
                        sendVoiceMessage();
                        Utils.lockScreenRotation(getActivity(), false);
                    } else if (currentVoiceRecordingState == VoiceRecordState.InitiatedRecording) {
                        handler.removeCallbacks(record);
                        handler.removeCallbacks(timer);
                        currentVoiceRecordingState = VoiceRecordState.NotRecording;
                        Utils.lockScreenRotation(getActivity(), false);
                    }
                    break;

                case MotionEvent.ACTION_MOVE:

                    //FAB movement
                    LinearLayout.LayoutParams lockParams =
                            (LinearLayout.LayoutParams) recordLockChevronImage.getLayoutParams();

                    float yRecordDiff = rootViewHeight
                            - (fabMicViewHeightSize + fabMicViewMarginBottom) + motionEvent.getY();

                    float yLockDiff = rootViewHeight
                            - (lockViewMarginBottom + lockViewHeightSize) + motionEvent.getY();

                    if (currentVoiceRecordingState == VoiceRecordState.TouchRecording) {
                        if (motionEvent.getY() > 0) {
                            recordButtonExpanded.animate()
                                    .y(rootViewHeight - (fabMicViewHeightSize + fabMicViewMarginBottom))
                                    .setDuration(0)
                                    .start();
                            recordLockView.animate()
                                    .y(rootViewHeight - (lockViewMarginBottom + lockViewHeightSize))
                                    .setDuration(0)
                                    .start();
                            recordLockChevronImage.setAlpha(1f);
                        } else if (motionEvent.getY() > -200) { //200 = height to the "locked" state
                            recordButtonExpanded.animate()
                                    .y(yRecordDiff)
                                    .setDuration(0)
                                    .start();
                            recordLockView.animate()
                                    .y(yLockDiff)
                                    .setDuration(0)
                                    .start();

                            //lockParams.topMargin = (int) motionEvent.getY() / 3;
                            lockParams.topMargin = (int) motionEvent.getY()
                                    * (recordLockChevronImage.getHeight() - recordLockImage.getPaddingTop())
                                    / 200;
                            recordLockChevronImage.setAlpha(1f + (motionEvent.getY() / 200f));
                            recordLockChevronImage.setLayoutParams(lockParams);
                        } else {
                            currentVoiceRecordingState = VoiceRecordState.NoTouchRecording;

                            //workaround for the https://issuetracker.google.com/issues/111316656 issue of
                            //the button's image not updating after setting a background tint manually.
                            recordButtonExpanded.hide();
                            recordButtonExpanded.setImageResource(R.drawable.ic_send_black_24dp);
                            recordButtonExpanded.show();
                            ////////////////////////////

                            recordButtonExpanded.animate()
                                    .y(rootViewHeight - (fabMicViewHeightSize + fabMicViewMarginBottom))
                                    .setDuration(100)
                                    .start();
                            recordLockView.animate()
                                    .y(rootViewHeight - (lockViewMarginBottom + lockViewHeightSize) + 50) // 50=temporary offset
                                    .setDuration(100)
                                    .start();

                            cancelRecordingLayout.setVisibility(View.VISIBLE);
                            recordLockImage.setImageResource(R.drawable.ic_stop);
                            recordLockImage.setPadding(0, 0, 0, 0);
                            recordLockImage.setOnClickListener(view121 -> {
                                if (currentVoiceRecordingState == VoiceRecordState.NoTouchRecording) {
                                    Utils.performHapticFeedback(rootView);
                                    stopRecording();
                                }
                            });
                            lockParams.topMargin = -(recordLockChevronImage.getHeight());
                            recordLockChevronImage.setLayoutParams(lockParams);
                            recordLockChevronImage.setAlpha(0f);
                            Utils.performHapticFeedback(rootView);
                        }
                    }

                    //"Slide To Cancel" movement;
                    float alpha = (1f + motionEvent.getX() / 400f);
                    if (currentVoiceRecordingState == VoiceRecordState.TouchRecording) {
                        if (motionEvent.getX() < 0)
                            slideToCancelLayout.animate().x(motionEvent.getX()).setDuration(0).start();
                        else
                            slideToCancelLayout.animate().x(0).setDuration(0).start();

                        slideToCancelLayout.setAlpha(alpha);

                        //since alpha and slide are tied together, we can cancel recording by checking transparency value
                        if (alpha <= 0) {
                            clearVoiceMessage();
                        }
                    }
                    break;
            }
            return true;
        });

        slideToCancelLayout = view.findViewById(R.id.slide_layout);

        cancelRecordingLayout = view.findViewById(R.id.cancel_record_layout);
        cancelRecordingTextView = view.findViewById(R.id.tv_cancel_recording);
        cancelRecordingTextView.setOnClickListener(view13 -> {
            if (currentVoiceRecordingState == VoiceRecordState.NoTouchRecording) {
                clearVoiceMessage();
            }
        });

        recordingPresenterLayout = view.findViewById(R.id.recording_presenter_layout);
        recordingPresenterPlaybarLayout = view.findViewById(R.id.recording_playbar_layout);
        recordingPresenterPlaybarLayout.getBackground().setColorFilter(accountColor, PorterDuff.Mode.SRC_IN);
        recordingPresenter = view.findViewById(R.id.voice_presenter_visualizer);
        recordingPresenter.setNotPlayedColor(Color.WHITE);
        recordingPresenter.setNotPlayedColorAlpha(127);
        recordingPresenter.setPlayedColor(Color.WHITE);
        recordingPlayButton = view.findViewById(R.id.voice_presenter_play);
        recordingDeleteButton = view.findViewById(R.id.voice_presenter_delete);
        recordingPresenterDuration = view.findViewById(R.id.voice_presenter_time);
        recordingPresenterSendButton = view.findViewById(R.id.voice_presenter_send);
        recordingPresenterSendButton.setColorFilter(accountColor);

        voiceMessageRecorderLayout = view.findViewById(R.id.record_layout);

        recordTimer = view.findViewById(R.id.chrRecordingTimer);
        recordTimer.setOnChronometerTickListener(chronometer -> {
            long elapsedMillis = SystemClock.elapsedRealtime() - recordTimer.getBase();

            recordSaveAllowed = elapsedMillis > 1000;
        });

        recordButtonExpanded = view.findViewById(R.id.record_float_button);
        recordButtonExpanded.setBackgroundTintList(ColorStateList.valueOf(accountColor));
        recordButtonExpanded.setOnClickListener(view14 -> {
            if (currentVoiceRecordingState == VoiceRecordState.NoTouchRecording)
                sendVoiceMessage();
        });

        recordLockView = view.findViewById(R.id.record_lock_view);
        recordLockImage = view.findViewById(R.id.iv_record_lock);
        recordLockChevronImage = view.findViewById(R.id.iv_record_chevron_lock);

        lastHistoryProgressBar = view.findViewById(R.id.chat_last_history_progress_bar);
        previousHistoryProgressBar = view.findViewById(R.id.chat_previous_history_progress_bar);


        securityButton = view.findViewById(R.id.button_security);
        securityButton.setOnClickListener(v -> showSecurityMenu());

        // to avoid strange bug on some 4.x androids
        inputLayout = view.findViewById(R.id.input_layout);
        inputLayout.setBackgroundColor(ColorManager.getInstance().getChatInputBackgroundColor());
        sendByEnter = SettingsManager.chatsSendByEnter();

        // interaction view
        interactionView = view.findViewById(R.id.interactionView);
        tvCount = view.findViewById(R.id.tvCount);
        ImageView ivClose = view.findViewById(R.id.ivClose);
        ivClose.setOnClickListener(v -> {
            bottomPanelMessagesIds.clear();
            closeInteractionPanel();
        });
        ImageView ivReply = view.findViewById(R.id.ivReply);
        ivReply.setOnClickListener(v -> {
            bottomPanelMessagesIds = new ArrayList<>(chatMessageAdapter.getCheckedItemIds());
            isReply = true;
            showBottomMessagesPanel(bottomPanelMessagesIds, BottomMessagesPanel.Purposes.FORWARDING);
            closeInteractionPanel();
        });
        ImageView ivForward = view.findViewById(R.id.ivForward);
        ivForward.setOnClickListener(v -> {
            bottomPanelMessagesIds = new ArrayList<>(chatMessageAdapter.getCheckedItemIds());
            openChooserForForward((ArrayList<String>) bottomPanelMessagesIds);
        });
        ImageView ivDelete = view.findViewById(R.id.ivDelete);
        ivDelete.setOnClickListener(v ->
                deleteMessage(new ArrayList<>(chatMessageAdapter.getCheckedMessageRealmObjects())));
        ImageView ivCopy = view.findViewById(R.id.ivCopy);
        ivCopy.setOnClickListener(v -> {
            ClipManager.copyMessagesToClipboard(new ArrayList<>(chatMessageAdapter.getCheckedItemIds()));
            bottomPanelMessagesIds.clear();
            closeInteractionPanel();
        });
        ivEdit = view.findViewById(R.id.ivEdit);
        ivEdit.setOnClickListener(v ->
                getReadyForMessageEditing(chatMessageAdapter.getCheckedMessageRealmObjects().get(0)));

        sendButton.setOnClickListener(v -> sendMessage());

        setUpInputView(view);
        setUpEmoji(view);

        realmRecyclerView = view.findViewById(R.id.chat_messages_recycler_view);

        layoutManager = new LinearLayoutManager(getActivity());
        realmRecyclerView.setLayoutManager(layoutManager);

        layoutManager.setStackFromEnd(true);


        realmRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy < 0) loadHistoryIfNeed();
                showScrollDownButtonIfNeed();
            }
        });

        stubNotify = view.findViewById(R.id.stubNotify);
        stubJoin = view.findViewById(R.id.stubJoin);
        stubNewContact = view.findViewById(R.id.stubNewContact);
        stubIntro = view.findViewById(R.id.stubIntro);
        NotificationManager.getInstance().removeMessageNotification(account, user);
        setChat(account, user);
        if (savedInstanceState != null) {
            String voiceRecordPath = savedInstanceState.getString(VOICE_MESSAGE);
            ignoreReceiver = savedInstanceState.getBoolean(VOICE_MESSAGE_RECEIVER_IGNORE);
            if (voiceRecordPath != null) {
                recordingPath = voiceRecordPath;
                currentVoiceRecordingState = VoiceRecordState.StoppedRecording;
                changeStateOfInputViewButtonsTo(false);
                voiceMessageRecorderLayout.setVisibility(View.VISIBLE);
                setUpVoiceMessagePresenter();
            }
            String[] ids = savedInstanceState.getStringArray(FORWARD_MESSAGES);
            if (ids != null && ids.length != 0) {
                bottomPanelMessagesIds.addAll(Arrays.asList(ids));
                BottomMessagesPanel.Purposes purpose = (BottomMessagesPanel.Purposes)
                        savedInstanceState.getSerializable(FORWARD_PURPOSE);
                if (purpose == BottomMessagesPanel.Purposes.FORWARDING) {
                    isReply = savedInstanceState.getBoolean(FORWARD_STATE);
                }
                showBottomMessagesPanel(bottomPanelMessagesIds, purpose);
                setUpInputViewButtons();
            }
        }

        if (SettingsManager.chatsShowBackground()) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
                view.setBackgroundResource(R.color.black);
            } else {
                view.setBackgroundResource(R.drawable.chat_background_repeat);
            }
        } else {
            view.setBackgroundColor(ColorManager.getInstance().getChatBackgroundColor());
        }

        placeholder = view.findViewById(R.id.placeholder);
        placeholder.setOnClickListener(this);

        rootView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                        //measurements for the recording layout animations.
                        rootViewHeight = rootView.getHeight();
                        rootViewWidth = rootView.getWidth();
                        lockViewHeightSize = recordLockView.getHeight();
                        lockViewMarginBottom = ((RelativeLayout.LayoutParams)
                                recordLockView.getLayoutParams()).bottomMargin;

                        fabMicViewHeightSize = recordButtonExpanded.getHeight();

                        fabMicViewMarginBottom = ((RelativeLayout.LayoutParams)
                                recordButtonExpanded.getLayoutParams()).bottomMargin;
                    }
                });

        return view;
    }

    private void setChat(AccountJid accountJid, ContactJid contactJid) {
        this.account = accountJid;
        this.user = contactJid;

        AbstractChat abstractChat = getChat();
        showSecurityButton(true);

        if (abstractChat != null) {
            messageRealmObjects = abstractChat.getMessages();
        }
        if (messageRealmObjects.size() > 0) {
            setIntroView();
            inflateIntroView(false);
        } else {
            messageRealmObjects.addChangeListener(new RealmChangeListener<RealmResults<MessageRealmObject>>() {
                @Override
                public void onChange(RealmResults<MessageRealmObject> element) {
                    setIntroView();
                    inflateIntroView(false);
                    messageRealmObjects.removeChangeListener(this);
                }
            });
            inflateIntroView(true);
        }

        chatMessageAdapter = new MessagesAdapter(getActivity(), messageRealmObjects, abstractChat,
                this, this, this, this, this);
        realmRecyclerView.setAdapter(chatMessageAdapter);
        realmRecyclerView.setItemAnimator(null);
        realmRecyclerView.addItemDecoration(new MessageHeaderViewDecoration());

        replySwipe = new ReplySwipeCallback(position -> {
            MessageRealmObject messageRealmObject = chatMessageAdapter.getMessageItem(position);
            if (messageRealmObject != null) {
                if (messageRealmObject.getUniqueId() != null) {
                    bottomPanelMessagesIds.clear();
                    bottomPanelMessagesIds.add(messageRealmObject.getUniqueId());
                    isReply = true;
                    showBottomMessagesPanel(bottomPanelMessagesIds, BottomMessagesPanel.Purposes.FORWARDING);
                    setUpInputViewButtons();
                }
            }
        });
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(replySwipe);
        itemTouchHelper.attachToRecyclerView(realmRecyclerView);

        realmRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                replySwipe.onDraw(c);
            }
        });

        restoreInputState();

        updateContact();
    }

    private void setIntroView() {
        View introView = LayoutInflater.from(realmRecyclerView.getContext())
                .inflate(R.layout.chat_intro_helper_view, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            introView.getBackground().setTint(accountColor);
        }
        realmRecyclerView.addItemDecoration(new IntroViewDecoration(introView, account, user, getChat()));
    }

    private void inflateIntroView(boolean show) {
        if (chatIntroLayout == null) {
            if (show) {
                chatIntroLayout = (ViewGroup) stubIntro.inflate();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    chatIntroLayout.getBackground().setTint(accountColor);
                }
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) chatIntroLayout.getLayoutParams();
                Point size = new Point();
                getActivity().getWindowManager().getDefaultDisplay().getSize(size);
                lp.leftMargin = size.x / 20;
                lp.rightMargin = size.x / 20;
                chatIntroLayout.setLayoutParams(lp);
            }
        } else {
            if (show) {
                chatIntroLayout.setVisibility(View.VISIBLE);
            } else {
                chatIntroLayout.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        NextMamManager.getInstance().onChatOpen(getChat());
    }

    @Override
    public void onResume() {
        super.onResume();

        LogManager.i(this, "onResume");

        ChatStateManager.getInstance().onChatOpening(account, user);

        if (getChat() != null && getChat() instanceof GroupChat) {
            // TODO should probably move to groupchat manager
            try {
                PresenceManager.getInstance().sendPresenceToGroupchat(getChat(), true);
            } catch (NetworkException e) {
                e.printStackTrace();
            }
        }

        updateContact();
        //restoreInputState();
        restoreScrollState(((ChatActivity) getActivity()).needScrollToUnread());

        showHideNotifyIfNeed();

        showJoinButtonIfNeed();

        ChatManager.getInstance().setVisibleChat(getChat());

        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public void onPause() {
        super.onPause();

        ChatStateManager.getInstance().onPaused(account, user);

        if (getChat() != null && getChat() instanceof GroupChat) {
            // TODO should probably move to groupchat manager
            try {
                PresenceManager.getInstance().sendPresenceToGroupchat(getChat(), false);
            } catch (NetworkException e) {
                e.printStackTrace();
            }
        }

        saveInputState();
        saveScrollState();
        if (currentVoiceRecordingState == VoiceRecordState.NoTouchRecording
                || currentVoiceRecordingState == VoiceRecordState.TouchRecording)
            stopRecording();

        ChatManager.getInstance().removeVisibleChat();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        chatMessageAdapter.release();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (listener != null) {
            listener.unregisterChatFragment();
            listener = null;
        }
        handler.removeCallbacks(record);
        handler.removeCallbacks(postAnimation);
        if (audioProgressSubscription != null) audioProgressSubscription.unsubscribe();
        //if (currentVoiceRecordingState != VoiceRecordState.NotRecording) {
        //    stopRecordingIfPossibleAsync(false);
        //}
        cleanUpVoice(false);
        unregisterOpusBroadcastReceiver();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(VOICE_MESSAGE, recordingPath);
        outState.putBoolean(VOICE_MESSAGE_RECEIVER_IGNORE, ignoreReceiver);
        if (!bottomPanelMessagesIds.isEmpty()) {
            if (bottomMessagesPanel != null) {
                outState.putStringArray(FORWARD_MESSAGES, bottomPanelMessagesIds.toArray(new String[0]));
                if (bottomMessagesPanel.getPurpose().equals(BottomMessagesPanel.Purposes.FORWARDING)) {
                    outState.putSerializable(FORWARD_PURPOSE, bottomMessagesPanel.getPurpose());
                    outState.putBoolean(FORWARD_STATE, isReply);
                } else {
                    outState.putSerializable(FORWARD_PURPOSE, bottomMessagesPanel.getPurpose());
                }
            }
        }
    }

    private void setUpInputView(View view) {
        inputView = view.findViewById(R.id.chat_input);
        setUpIme();

        inputView.setOnEditorActionListener((v, actionId, event) -> {
            LogManager.d("InputViewDebug", "editorActionListener called, actionId = "
                    + actionId + ", event != null ? " + (event != null) + ", ");
            if (event != null) {
                LogManager.d("InputViewDebug", "event.getAction() = "
                        + event.getAction() + ", event.getKeyCode() = " + event.getKeyCode());
            }
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            } else if (event != null && actionId == EditorInfo.IME_NULL) {
                if (sendByEnter && event.getAction() == KeyEvent.ACTION_DOWN) {
                    sendMessage();
                    return true;
                }
            }
            return false;
        });
        inputView.setOnKeyListener((view1, keyCode, event) -> {
            if (sendByEnter) {
                if (keyCode < 29 || keyCode > 54 || event.getKeyCode() < 29 || event.getKeyCode() > 54) {
                    LogManager.d("InputViewDebug", "onKeyListener called, "
                            + "keyCode = " + keyCode
                            + ", event.getAction() = " + event.getAction()
                            + ", event.getKeyCode() = " + event.getKeyCode());
                }
            }
            if (keyCode == KeyEvent.KEYCODE_ENTER && sendByEnter
                    && event.getAction() == KeyEvent.ACTION_DOWN) {
                sendMessage();
                return true;
            }
            return false;
        });

        inputView.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!skipOnTextChanges && stopTypingTimer != null) {
                    stopTypingTimer.cancel();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                ChatFragment.this.afterTextChanged(text);
            }

        });
    }

    private void afterTextChanged(Editable text) {
        setUpInputViewButtons();

        if (skipOnTextChanges) {
            return;
        }

        long start = System.currentTimeMillis();
        ChatStateManager.getInstance().onComposing(account, user, text);
        LogManager.d("ChatStateComposingDebug", "time spent on finishing onComposing call = " + (System.currentTimeMillis() - start) + " ms");

        stopTypingTimer = new Timer();
        stopTypingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Application.getInstance().runOnUiThread(() ->
                        ChatStateManager.getInstance().onPaused(account, user));
            }

        }, STOP_TYPING_DELAY);
    }

    private void setUpIme() {
        if (sendByEnter) {
            inputView.setInputType(inputView.getInputType() & (~InputType.TYPE_TEXT_FLAG_MULTI_LINE));
        } else {
            inputView.setInputType(inputView.getInputType() | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        }
    }

    private void setUpEmoji(View view) {
        final ImageButton emojiButton = view.findViewById(R.id.button_emoticon);
        final View rootView = view.findViewById(R.id.root_view);


        // Give the topmost view of your activity layout hierarchy. This will be used to measure soft keyboard height
        final EmojiconsPopup popup = new EmojiconsPopup(rootView, getActivity());

        //Will automatically set size according to the soft keyboard size
        popup.setSizeForSoftKeyboard();

        //If the emoji popup is dismissed, change emojiButton to smiley icon
        popup.setOnDismissListener(() ->
                changeEmojiKeyboardIcon(emojiButton, R.drawable.ic_mood_black_24dp));

        //If the text keyboard closes, also dismiss the emoji popup
        popup.setOnSoftKeyboardOpenCloseListener(new EmojiconsPopup.OnSoftKeyboardOpenCloseListener() {

            @Override
            public void onKeyboardOpen(int keyBoardHeight) {

            }

            @Override
            public void onKeyboardClose() {
                if (popup.isShowing())
                    popup.dismiss();
            }
        });

        //On emoji clicked, add it to edittext
        popup.setOnEmojiconClickedListener(emojicon -> {
            if (inputView == null || emojicon == null) {
                return;
            }

            int start = inputView.getSelectionStart();
            int end = inputView.getSelectionEnd();
            if (start < 0) {
                inputView.append(emojicon.getEmoji());
            } else {
                inputView.getText().replace(Math.min(start, end),
                        Math.max(start, end), emojicon.getEmoji(), 0,
                        emojicon.getEmoji().length());
            }
        });

        //On backspace clicked, emulate the KEYCODE_DEL key event
        popup.setOnEmojiconBackspaceClickedListener(v -> {
            KeyEvent event = new KeyEvent(
                    0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
            inputView.dispatchKeyEvent(event);
        });

        // To toggle between text keyboard and emoji keyboard keyboard(Popup)
        emojiButton.setOnClickListener(v -> {

            //If popup is not showing => emoji keyboard is not visible, we need to show it
            if (!popup.isShowing()) {

                //If keyboard is visible, simply show the emoji popup
                if (popup.isKeyBoardOpen()) {
                    popup.showAtBottom();
                    changeEmojiKeyboardIcon(emojiButton, R.drawable.ic_keyboard_black_24dp);
                }

                //else, open the text keyboard first and immediately after that show the emoji popup
                else {
                    inputView.setFocusableInTouchMode(true);
                    inputView.requestFocus();
                    popup.showAtBottomPending();
                    final InputMethodManager inputMethodManager = (InputMethodManager)
                            getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.showSoftInput(inputView, InputMethodManager.SHOW_IMPLICIT);
                    changeEmojiKeyboardIcon(emojiButton, R.drawable.ic_keyboard_black_24dp);
                }
            }

            //If popup is showing, simply dismiss it to show the undelying text keyboard
            else {
                popup.dismiss();
            }
        });
    }

    private void loadHistoryIfNeed() {
        if (!historyIsLoading) {
            int invisibleMessagesCount = layoutManager.findFirstVisibleItemPosition();
            if (invisibleMessagesCount <= 15) {
                AbstractChat chat = getChat();
                if (chat != null) NextMamManager.getInstance().onScrollInChat(chat);
            }
        }
    }

    @Nullable
    private AbstractChat getChat() {
        return ChatManager.getInstance().getChat(account, user);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LastHistoryLoadStartedEvent event) {
        if (event.getAccount().equals(account) && event.getUser().equals(user)) {
            lastHistoryProgressBar.setVisibility(View.VISIBLE);
            historyIsLoading = true;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LastHistoryLoadFinishedEvent event) {
        if (event.getAccount().equals(account) && event.getUser().equals(user)) {
            lastHistoryProgressBar.setVisibility(View.GONE);
            historyIsLoading = false;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(PreviousHistoryLoadStartedEvent event) {
        if (event.getAccount().equals(account) && event.getUser().equals(user)) {
            LogManager.i(this, "PreviousHistoryLoadStartedEvent");
            previousHistoryProgressBar.setVisibility(View.VISIBLE);
            historyIsLoading = true;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(PreviousHistoryLoadFinishedEvent event) {
        if (event.getAccount().equals(account) && event.getUser().equals(user)) {
            LogManager.i(this, "PreviousHistoryLoadFinishedEvent");
            historyIsLoading = false;
            previousHistoryProgressBar.setVisibility(View.GONE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageUpdateEvent event) {
        if (account.equals(event.getAccount()) && user.equals(event.getUser())) {
            updateUnread();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(NewIncomingMessageEvent event) {
        if (event.getAccount().equals(account) && event.getUser().equals(user)) {
            listener.playIncomingAnimation();
            playMessageSound();
            NotificationManager.getInstance().removeMessageNotification(account, user);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(NewMessageEvent event) {
        updateUnread();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AuthAskEvent event) {
        if (event.getAccount() == getAccount() && event.getUser() == getUser()) {
            showHideNotifyIfNeed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_EXPORT_CHAT) {
            if (PermissionsRequester.isPermissionGranted(grantResults)) showExportChatDialog();
            else
                Toast.makeText(getActivity(), R.string.no_permission_to_write_files, Toast.LENGTH_SHORT).show();
        }
    }

    private void changeEmojiKeyboardIcon(ImageView iconToBeChanged, int drawableResourceId) {
        iconToBeChanged.setImageResource(drawableResourceId);
    }

    private void showSecurityMenu() {
        PopupMenu popup = new PopupMenu(getActivity(), securityButton);
        popup.inflate(R.menu.security);
        popup.setOnMenuItemClickListener(this);

        Menu menu = popup.getMenu();

        SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(account, user);

        if (securityLevel == SecurityLevel.plain) {
            menu.findItem(R.id.action_start_encryption).setVisible(true)
                    .setEnabled(SettingsManager.securityOtrMode() != SettingsManager.SecurityOtrMode.disabled);
        } else {
            menu.findItem(R.id.action_restart_encryption).setVisible(true);
        }

        boolean isEncrypted = securityLevel != SecurityLevel.plain;

        menu.findItem(R.id.action_stop_encryption).setEnabled(isEncrypted);
        menu.findItem(R.id.action_verify_with_fingerprint).setEnabled(isEncrypted);
        menu.findItem(R.id.action_verify_with_question).setEnabled(isEncrypted);
        menu.findItem(R.id.action_verify_with_shared_secret).setEnabled(isEncrypted);

        popup.show();
    }

    private void setUpInputViewButtons() {
        boolean empty = inputView.getText().toString().trim().isEmpty();
        if (empty) empty = (bottomPanelMessagesIds.isEmpty() || isReply);

        if (empty != isInputEmpty) {
            isInputEmpty = empty;
        }

        if (isInputEmpty) {
            sendButton.setVisibility(View.GONE);
            sendButton.setColorFilter(ColorManager.getInstance().getAccountPainter().getGreyMain());
            sendButton.setEnabled(false);
            showSecurityButton(true);
            recordButton.setVisibility(View.VISIBLE);
            attachButton.setVisibility(View.VISIBLE);
        } else {
            sendButton.setVisibility(View.VISIBLE);
            sendButton.setEnabled(true);
            sendButton.setColorFilter(accountColor);
            showSecurityButton(false);
            recordButton.setVisibility(View.GONE);
            attachButton.setVisibility(View.GONE);
        }
    }


    private void restoreInputState() {
        //if (!inputView.getText().equals("") && inputView.getText() != null) return;

        //if (userIsBlocked) {
        //    showBlockedBar();
        //    return;
        //}

        skipOnTextChanges = true;

        inputView.setText(ChatManager.getInstance().getTypedMessage(account, user));
        inputView.setSelection(ChatManager.getInstance().getSelectionStart(account, user),
                ChatManager.getInstance().getSelectionEnd(account, user));

        skipOnTextChanges = false;

        if (!inputView.getText().toString().isEmpty()) {
            inputView.requestFocus();
        }
    }

    private void saveInputState() {
        ChatManager.getInstance().setTyped(account, user, inputView.getText().toString(),
                inputView.getSelectionStart(), inputView.getSelectionEnd());
    }

    private void sendMessage() {
        Editable editable = inputView.getEditableText();
        String text;
        String markupText = null;

        SpannableStringBuilder spannable = new SpannableStringBuilder(editable);
        StringBuilder htmlMarkupBuilder = new StringBuilder(spannable.toString());
        int htmlMarkupOffset = 0; // the offset of the html string compared to the normal string without <tags>
        CustomQuoteSpan[] quoteSpans = spannable.getSpans(0, spannable.length(), CustomQuoteSpan.class);
        if (quoteSpans.length > 0) {
            int len = spannable.length();
            for (CustomQuoteSpan span : quoteSpans) {
                int startSpan = spannable.getSpanStart(span);
                int endSpan = spannable.getSpanEnd(span);
                spannable.removeSpan(span);

                if (startSpan < 0 || endSpan < 0) continue;
                if (startSpan >= len || endSpan > len || startSpan > endSpan) continue;

                // make sure it's a paragraph
                // check top paragraph boundary
                if (startSpan != 0 && (spannable.charAt(startSpan - 1) != '\n')) continue;
                // check bottom paragraph boundary
                if (endSpan != spannable.length() && spannable.charAt(endSpan - 1) != '\n')
                    continue;

                // split the quotespan into 1-line strings
                String originalQuoteString = spannable.subSequence(startSpan, endSpan).toString();
                String[] quoteLines = originalQuoteString.split("\n");

                spannable.delete(startSpan, endSpan);
                htmlMarkupBuilder.delete(startSpan + htmlMarkupOffset, endSpan + htmlMarkupOffset);

                int variableStartSpan = startSpan;

                // open the quote tag for the markup text
                htmlMarkupBuilder.insert(startSpan + htmlMarkupOffset, "<blockquote>");
                htmlMarkupOffset += "<blockquote>".length();
                for (int i = 0; i < quoteLines.length; i++) {
                    // add > at the start of each line
                    quoteLines[i] = '>' + quoteLines[i] + '\n';
                    // add modified line back to the spannable and markup text
                    spannable.insert(variableStartSpan, quoteLines[i]);
                    htmlMarkupBuilder.insert(variableStartSpan + htmlMarkupOffset, quoteLines[i]);
                    variableStartSpan += quoteLines[i].length();
                }
                htmlMarkupBuilder.insert(variableStartSpan + htmlMarkupOffset, "</blockquote>");
                htmlMarkupOffset += "</blockquote>".length();
            }

            markupText = htmlMarkupBuilder.toString();
        }

        text = spannable.toString();

        clearInputText();
        scrollDown();

        if (bottomPanelMessagesIds != null
                && !bottomPanelMessagesIds.isEmpty()
                && bottomMessagesPanel.getPurpose().equals(BottomMessagesPanel.Purposes.FORWARDING)) {
            sendForwardMessage(bottomPanelMessagesIds, text, markupText);
        } else if (bottomPanelMessagesIds != null
                && bottomMessagesPanel != null
                && !bottomPanelMessagesIds.isEmpty()
                && bottomMessagesPanel.getPurpose().equals(BottomMessagesPanel.Purposes.EDITING)) {
            RrrManager.getInstance().sendEditedMessage(account, user, bottomPanelMessagesIds.get(0), text);
            hideBottomMessagePanel();
        } else if (!text.isEmpty()) {
            sendMessage(text, markupText);
        } else {
            return;
        }

        playMessageSound();
        listener.onMessageSent();
        ChatStateManager.getInstance().cancelComposingSender();

        if (SettingsManager.chatsHideKeyboard() == SettingsManager.ChatsHideKeyboard.always
                || (getActivity().getResources().getBoolean(R.bool.landscape)
                && SettingsManager.chatsHideKeyboard() == SettingsManager.ChatsHideKeyboard.landscape)) {
            ChatActivity.hideKeyboard(getActivity());
        }
    }

    private void sendMessage(String text) {
        sendMessage(text, null);
    }

    private void sendMessage(String text, String markupText) {
        MessageManager.getInstance().sendMessage(account, user, text, markupText);
        setFirstUnreadMessageId(null);
    }


    public void updateContact() {
        updateSecurityButton();
        updateSendButtonSecurityLevel();
        updateBlockedState();
        showNewContactLayoutIfNeed();
    }

    private void onScrollDownClick() {
        AbstractChat chat = getChat();
        if (chat != null) {
            int unread = chat.getUnreadMessageCount();
            int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
            if (unread == 0 || lastVisiblePosition + 2 >= chatMessageAdapter.getItemCount() - unread) {
                // scroll down
                scrollDown();

                // scroll to unread
            } else scrollToFirstUnread(unread);
        }
    }

    private void scrollDown() {

        realmRecyclerView.scrollToPosition(chatMessageAdapter.getItemCount() - 1);
    }

    private void scrollToFirstUnread(int unreadCount) {
        layoutManager.scrollToPositionWithOffset(
                chatMessageAdapter.getItemCount() - unreadCount, 200);
    }

    private void updateSecurityButton() {
        SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(account, user);
        if (securityButton != null) {
            // strange null ptr happens
            securityButton.setImageLevel(securityLevel.getImageLevel());
        }
    }

    private void showSecurityButton(boolean show) {
        securityButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateSendButtonSecurityLevel() {
        SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(account, user);
        if (sendButton != null)
            sendButton.setImageLevel(securityLevel.getImageLevel());
    }

    public boolean isEqual(BaseEntity chat) {
        return chat != null && this.account.equals(chat.getAccount()) && this.user.equals(chat.getUser());
    }

    public void setInputText(String additional) {
        skipOnTextChanges = true;
        inputView.setText(additional);
        inputView.setSelection(additional.length());

        skipOnTextChanges = false;
    }

    public void setInputTextAtCursor(String additional) {
        skipOnTextChanges = true;
        String currentText = inputView.getText().toString();
        if (currentText.isEmpty()) {
            inputView.setText(additional);
            inputView.setSelection(additional.length());
        } else {
            int cursorPosition = inputView.getSelectionStart();
            String first = currentText.substring(0, cursorPosition);
            String second = currentText.substring(cursorPosition);
            inputView.setText(first.concat(additional).concat(second));
            inputView.setSelection(first.length() + additional.length());
        }
        skipOnTextChanges = false;
    }

    public void setQuote(String quote) {
        skipOnTextChanges = true;
        int spanStart = 0;
        int spanEnd;
        if (bottomMessagesPanel != null
                && bottomMessagesPanel.getPurpose() == BottomMessagesPanel.Purposes.EDITING) {
            setInputTextAtCursor(quote);
            return;
        }
        Editable currentText = inputView.getEditableText();
        if (currentText.length() != 0) {
            int cursorPosition = inputView.getSelectionStart();
            spanStart = cursorPosition;
            if (cursorPosition != 0 && currentText.charAt(cursorPosition - 1) != '\n') {
                currentText.insert(cursorPosition, "\n");
                spanStart++;
            }
        }
        spanEnd = spanStart + quote.length();
        currentText.insert(spanStart, quote);
        currentText.setSpan(new CustomQuoteSpan(accountColor, getContext()
                .getResources().getDisplayMetrics()), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        inputView.setSelection(spanEnd);
        skipOnTextChanges = false;
    }

    public AccountJid getAccount() {
        return account;
    }

    public ContactJid getUser() {
        return user;
    }

    private void clearInputText() {
        skipOnTextChanges = true;
        inputView.getText().clear();
        skipOnTextChanges = false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return ((ChatActivity) getActivity()).onMenuItemClick(item);
    }

    public void onExportChatClick() {
        if (PermissionsRequester.requestFileWritePermissionIfNeeded(this, PERMISSIONS_REQUEST_EXPORT_CHAT)) {
            showExportChatDialog();
        }

    }

    private void showExportChatDialog() {
        ChatExportDialogFragment.newInstance(account, user).show(getFragmentManager(), "CHAT_EXPORT");
    }

    public void stopEncryption(AccountJid account, ContactJid user) {
        try {
            OTRManager.getInstance().endSession(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    private void restartEncryption(AccountJid account, ContactJid user) {
        try {
            OTRManager.getInstance().refreshSession(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    private void startEncryption(AccountJid account, ContactJid user) {
        try {
            OTRManager.getInstance().startSession(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    private void deleteMessage(final ArrayList<MessageRealmObject> messages) {
        final List<String> ids = new ArrayList<>();
        boolean onlyOutgoing = true;
        for (MessageRealmObject messageRealmObject : messages) {
            ids.add(messageRealmObject.getUniqueId());
            if (messageRealmObject.isIncoming())
                onlyOutgoing = false;
        }
        int size = ids.size();
        if (RrrManager.getInstance().isSupported(account)) {
            View checkBoxView = getView().inflate(getContext(), R.layout.delete_for_companion_checkbox, null);
            final CheckBox checkBox = checkBoxView.findViewById(R.id.delete_for_all_checkbox);
            checkBox.setText(String.format(getContext().getString(R.string.delete_for_all),
                    RosterManager.getInstance().getBestContact(account, user).getName()));
            AlertDialog.Builder dialog = new AlertDialog.Builder(getContext())
                    .setTitle(size == 1 ? getString(R.string.delete_message_title) : getString(R.string.delete_messages_title, String.valueOf(size)))
                    .setMessage(size == 1 ? R.string.delete_message_question : R.string.delete_messages_question)
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (checkBox.isChecked())
                                RrrManager.getInstance().tryToRetractMessage(account, ids, true);
                            else RrrManager.getInstance().tryToRetractMessage(account, ids, false);
                        }
                    })
                    .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            if (onlyOutgoing) dialog.setView(checkBoxView);
            dialog.show();
        } else {
            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setMessage(getContext().getResources().getString(R.string.delete_messages_question))
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MessageManager.getInstance().removeMessage(ids);
                        }
                    })
                    .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
        bottomPanelMessagesIds.clear();
        closeInteractionPanel();
    }

    private void getReadyForMessageEditing(MessageRealmObject messageRealmObject) {
        List<String> arrayList = new ArrayList<String>();
        arrayList.add(messageRealmObject.getUniqueId());
        bottomPanelMessagesIds = arrayList;
        showBottomMessagesPanel(arrayList, BottomMessagesPanel.Purposes.EDITING);
        closeInteractionPanel();
        setInputText(messageRealmObject.getText());
    }

    public void showResourceChoiceAlert(final AccountJid account, final ContactJid user, final boolean restartSession) {
        final List<Presence> allPresences = RosterManager.getInstance().getPresences(account, user.getJid());

        final List<Map<String, String>> items = new ArrayList<>();
        for (Presence presence : allPresences) {
            Jid fromJid = presence.getFrom();
            ClientInfo clientInfo = CapabilitiesManager.getInstance().getCachedClientInfo(fromJid);

            String client = "";
            if (clientInfo == null) {
                CapabilitiesManager.getInstance().requestClientInfoByUser(account, fromJid);
            } else if (clientInfo == ClientInfo.INVALID_CLIENT_INFO) {
                client = getString(R.string.unknown);
            } else {
                String name = clientInfo.getName();
                if (name != null) {
                    client = name;
                }

                String type = clientInfo.getType();
                if (type != null) {
                    if (client.isEmpty()) {
                        client = type;
                    } else {
                        client = client + "/" + type;
                    }
                }
            }

            Map<String, String> map = new HashMap<>();
            if (!client.isEmpty()) {
                map.put(ResourceAdapter.KEY_CLIENT, client);
            }

            Resourcepart resourceOrNull = fromJid.getResourceOrNull();
            if (resourceOrNull != null) {
                map.put(ResourceAdapter.KEY_RESOURCE, resourceOrNull.toString());
                items.add(map);
            }
        }
        final ResourceAdapter adapter = new ResourceAdapter(getActivity(), items);
        adapter.setCheckedItem(checkedResource);

        if (items.size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.otr_select_resource);
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    checkedResource = adapter.getCheckedItem();
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    checkedResource = adapter.getCheckedItem();
                    try {
                        AbstractChat chat = getChat();
                        if (chat instanceof RegularChat) {
                            ((RegularChat) chat).setOTRresource(Resourcepart.from(items.get(checkedResource).get(ResourceAdapter.KEY_RESOURCE)));
                            if (restartSession) restartEncryption(account, user);
                            else startEncryption(account, user);
                        } else {
                            Toast.makeText(getActivity(), R.string.otr_select_toast_error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (XmppStringprepException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), R.string.otr_select_toast_error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setSingleChoiceItems(adapter, checkedResource, null).show();
        } else {
            Toast.makeText(getActivity(), R.string.otr_select_toast_resources_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.avatar) {
            showContactInfo();
        }
//        if (v.getId() == R.id.placeholder) {
//            ((ChatActivity)getActivity()).selectPage(1, true);
//        }
        if (v.getId() == R.id.btnScrollDown) {
            onScrollDownClick();
        }
    }

    public void showContactInfo() {
        Intent intent;
        intent = ContactViewerActivity.createIntent(getActivity(), account, user);
        startActivity(intent);
    }

    public void closeChat(AccountJid account, ContactJid user) { //TODO pay attention. Do we need this?
        ChatManager.getInstance().closeChat(account, user);
        NotificationManager.getInstance().removeMessageNotification(account, user);
        listener.onCloseChat();
    }

    public void clearHistory(AccountJid account, ContactJid user) {
        ChatHistoryClearDialog.newInstance(account, user).show(getFragmentManager(), ChatHistoryClearDialog.class.getSimpleName());
    }

    public void callAttention() {
        try {
            AttentionManager.getInstance().sendAttention(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }

    @Override
    public void onMessageClick(View caller, int position) {
        int itemViewType = chatMessageAdapter.getItemViewType(position);

        if (itemViewType == MessagesAdapter.VIEW_TYPE_INCOMING_MESSAGE
                || itemViewType == MessagesAdapter.VIEW_TYPE_OUTGOING_MESSAGE
                || itemViewType == MessagesAdapter.VIEW_TYPE_INCOMING_MESSAGE_NOFLEX
                || itemViewType == MessagesAdapter.VIEW_TYPE_OUTGOING_MESSAGE_NOFLEX
                || itemViewType == MessagesAdapter.VIEW_TYPE_OUTGOING_MESSAGE_IMAGE
                || itemViewType == MessagesAdapter.VIEW_TYPE_INCOMING_MESSAGE_IMAGE
                || itemViewType == MessagesAdapter.VIEW_TYPE_OUTGOING_MESSAGE_IMAGE_TEXT
                || itemViewType == MessagesAdapter.VIEW_TYPE_INCOMING_MESSAGE_IMAGE_TEXT) {

            clickedMessageRealmObject = chatMessageAdapter.getMessageItem(position);
            if (clickedMessageRealmObject == null) {
                LogManager.w(LOG_TAG, "onMessageClick null message item. Position: " + position);
                return;
            }
            showCustomMenu(caller);
        }
    }

    @Override
    public void onChangeCheckedItems(int checkedItems) {
        if (checkedItems > 0) {
            interactionView.setVisibility(View.VISIBLE);
            tvCount.setText(String.valueOf(checkedItems));
            replySwipe.setSwipeEnabled(false);
        } else {
            interactionView.setVisibility(View.GONE);
            replySwipe.setSwipeEnabled(true);
        }

        if (checkedItems == 1
                && RrrManager.getInstance().isSupported(account)
                && !chatMessageAdapter.getCheckedMessageRealmObjects().get(0).isIncoming()
                && !chatMessageAdapter.getCheckedMessageRealmObjects().get(0).haveAttachments()
                && (chatMessageAdapter.getCheckedMessageRealmObjects().get(0).isAcknowledged()
                || chatMessageAdapter.getCheckedMessageRealmObjects().get(0).isDelivered()
                || chatMessageAdapter.getCheckedMessageRealmObjects().get(0).isDisplayed())) {
            ivEdit.setVisibility(View.VISIBLE);
        } else ivEdit.setVisibility(View.GONE);
    }

    public void showCustomMenu(View anchor) {
        menuItems = new ArrayList<HashMap<String, String>>();

        if (clickedMessageRealmObject.isError()) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_repeat", getString(R.string.message_repeat));
        }

        if (!MessageRealmObject.isUploadFileMessage(clickedMessageRealmObject)) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_quote", getString(R.string.message_quote));
            CustomMessageMenu.addMenuItem(menuItems, "action_message_copy", getString(R.string.message_copy));
            CustomMessageMenu.addMenuItem(menuItems, "action_message_remove", getString(R.string.message_remove));
        }

        if (!clickedMessageRealmObject.isIncoming() && !clickedMessageRealmObject.haveAttachments()
                && (clickedMessageRealmObject.isAcknowledged()
                || clickedMessageRealmObject.isDelivered()
                || clickedMessageRealmObject.isDisplayed()))
            CustomMessageMenu.addMenuItem(menuItems, "action_message_edit", getString(R.string.message_edit));

        if (OTRManager.getInstance().isEncrypted(clickedMessageRealmObject.getText())) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_show_original_otr", getString(R.string.message_otr_show_original));
        }

        if (clickedMessageRealmObject.isError()) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_status", CustomMessageMenuAdapter.STATUS_ERROR);
        } else if (!clickedMessageRealmObject.isSent()) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_status", CustomMessageMenuAdapter.STATUS_NOT_SEND);
        } else if (clickedMessageRealmObject.isDisplayed()) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_status", CustomMessageMenuAdapter.STATUS_DISPLAYED);
        } else if (clickedMessageRealmObject.isDelivered()) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_status", CustomMessageMenuAdapter.STATUS_DELIVERED);
        } else if (clickedMessageRealmObject.isAcknowledged()) {
            CustomMessageMenu.addMenuItem(menuItems, "action_message_status", CustomMessageMenuAdapter.STATUS_ACK);
        }

        CustomMessageMenu.showMenu(getActivity(), anchor, menuItems, this, this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (menuItems != null && menuItems.size() > position) {
            HashMap<String, String> menuItem = menuItems.get(position);

            switch (menuItem.get(CustomMessageMenuAdapter.KEY_ID)) {
                case "action_message_repeat":
                    if (clickedMessageRealmObject.haveAttachments()) {
                        HttpFileUploadManager.getInstance()
                                .retrySendFileMessage(clickedMessageRealmObject, getActivity());
                    } else {
                        sendMessage(clickedMessageRealmObject.getText());
                    }
                    break;
                case "action_message_copy":
                    Spannable spannable = MessageRealmObject.getSpannable(clickedMessageRealmObject);
                    ((ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE))
                            .setPrimaryClip(ClipData.newPlainText(spannable, spannable));
                    break;
                case "action_message_appeal":
                    mentionUser(clickedMessageRealmObject.getResource().toString());
                    break;
                case "action_message_quote":
                    setQuote(clickedMessageRealmObject.getText() + "\n");
                    break;
                case "action_message_remove":
                    ArrayList<MessageRealmObject> arrayList = new ArrayList<>();
                    arrayList.add(clickedMessageRealmObject);
                    deleteMessage(arrayList);
                    break;
                case "action_message_show_original_otr":
                    chatMessageAdapter.addOrRemoveItemNeedOriginalText(clickedMessageRealmObject.getUniqueId());
                    chatMessageAdapter.notifyDataSetChanged();
                    break;
                case "action_message_status":
                    if (clickedMessageRealmObject.isError())
                        showError(clickedMessageRealmObject.getErrorDescription());
                    break;
                case "action_message_edit":
                    getReadyForMessageEditing(clickedMessageRealmObject);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onDismiss() {
        menuItems = null;
    }

    public void mentionUser(String username) {
        setInputTextAtCursor(username + ", ");
    }

    private void showError(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.error_description_title)
                .setMessage(message)
                .setPositiveButton("Ok", null)
                .show();
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        chatMessageAdapter.notifyDataSetChanged();
    }

    public void playMessageSound() {
        if (!SettingsManager.eventsInChatSounds()) return;

        final MediaPlayer mp;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build();
            mp = MediaPlayer.create(getActivity(), R.raw.message_alert,
                    attr, AudioManager.AUDIO_SESSION_ID_GENERATE);
        } else {
            mp = MediaPlayer.create(getActivity(), R.raw.message_alert);
            mp.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
        }

        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });
    }

    @Override
    public int getLastVisiblePosition() {
        return layoutManager.findLastVisibleItemPosition();
    }

    @Override
    public void scrollTo(int position) {
        layoutManager.scrollToPosition(position);
    }

    public void saveScrollState() {
        int position = layoutManager.findLastCompletelyVisibleItemPosition();
        AbstractChat chat = getChat();

        if (position == -1) return;
        if (position == chatMessageAdapter.getItemCount() - 1) position = 0;
        if (chat != null) chat.setChatstate(AbstractChat.ChatstateType.NORMAL);
        if (chat != null) chat.saveLastPosition(position);
    }

    public void restoreScrollState(boolean fromNotification) {
        AbstractChat chat = getChat();
        if (chat != null) {
            int position = chat.getLastPosition();
            int unread = chat.getUnreadMessageCount();
            if ((position == 0 || fromNotification) && unread > 0)
                scrollToFirstUnread(unread);
            else if (position > 0)
                layoutManager.scrollToPosition(position);
            setFirstUnreadMessageId(chat.getFirstUnreadMessageId());
            updateNewReceivedMessageCounter(unread);
        }
    }

    @Override
    public void onMessagesUpdated() {
        loadHistoryIfNeed();
    }

    public void showHideNotifyIfNeed() {
        AbstractChat chat = getChat();
        if (chat != null && chat instanceof RegularChat) {
            notifyIntent = ((RegularChat) chat).getIntent();
            if (notifyIntent != null) {
                setupNotifyLayout(notifyIntent);
            } else if (notifyLayout != null)
                notifyLayout.setVisibility(View.GONE);
        }
    }

    public void showPlaceholder(boolean show) {
        if (show) placeholder.setVisibility(View.VISIBLE);
        else placeholder.setVisibility(View.GONE);
    }

    private void inflateJoinLayout() {
        View view = stubJoin.inflate();
        joinLayout = view.findViewById(R.id.joinLayout);
        actionJoin = view.findViewById(R.id.actionJoin);
        actionJoin.setOnClickListener(this);
    }

    public void showJoinButtonIfNeed() {
        AbstractChat chat = getChat();
        if (chat != null) {
            if (joinLayout != null)
                joinLayout.setVisibility(View.GONE);
            inputView.setVisibility(View.VISIBLE);

        }
    }

    private void setupNotifyLayout(Intent notifyIntent) {
        if (notifyLayout == null || tvNotifyTitle == null || tvNotifyAction == null) {
            inflateNotifyLayout();
        }

        if (notifyIntent.getBooleanExtra(QuestionActivity.EXTRA_FIELD_CANCEL, false)) {
            tvNotifyTitle.setText(R.string.otr_verification_progress_title);
            tvNotifyAction.setText(R.string.otr_verification_notify_button_cancel);
        } else {
            tvNotifyTitle.setText(R.string.otr_verification_notify_title);
            tvNotifyAction.setText(R.string.otr_verification_notify_button);
        }
        notifyLayout.setVisibility(View.VISIBLE);
    }

    private void inflateNotifyLayout() {
        View view = stubNotify.inflate();
        tvNotifyTitle = view.findViewById(R.id.tvNotifyTitle);
        tvNotifyAction = view.findViewById(R.id.tvNotifyAction);
        notifyLayout = view.findViewById(R.id.notifyLayout);
        notifyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (notifyIntent != null) startActivity(notifyIntent);
                notifyLayout.setVisibility(View.GONE);
            }
        });
    }

    private void updateBlockedState() {
        userIsBlocked = BlockingManager.getInstance().contactIsBlockedLocally(account, user);
        if (userIsBlocked) {
            showBlockedBar();
        } else {
            if (blockedView != null) blockedView.setVisibility(View.GONE);
            inputLayout.setVisibility(View.VISIBLE);
        }
    }

    private void showBlockedBar() {
        for (int i = 0; i < inputPanel.getChildCount(); i++) {
            View view = inputPanel.getChildAt(i);
            if (view != null && view.getVisibility() == View.VISIBLE) {
                view.setVisibility(View.GONE);
            }
        }
        if (blockedView == null) {
            blockedView = new TextView(getContext());
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    getResources().getDimensionPixelOffset(R.dimen.input_view_height));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                blockedView.setTextAppearance(R.style.TextAppearance_AppCompat_Widget_Button);
            } else {
                blockedView.setTextAppearance(getContext(), R.style.TextAppearance_AppCompat_Widget_Button);
            }
            blockedView.setTextColor(accountColor);
            blockedView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            blockedView.setText(R.string.blocked_contact_message);

            blockedView.setBackgroundColor(Utils.getAttrColor(getContext(), R.attr.chat_input_background));
            blockedView.setLayoutParams(layoutParams);
            blockedView.setGravity(Gravity.CENTER);
            blockedView.setOnClickListener(v -> startActivity(ContactViewerActivity.createIntent(getContext(), account, user)));
            inputPanel.addView(blockedView);
        } else {
            blockedView.setVisibility(View.VISIBLE);
        }
    }

    private void showNewContactLayoutIfNeed() {

        //if (BlockingManager.getInstance().contactIsBlocked(account, user)) {
        //    if (newContactLayout != null) newContactLayout.setVisibility(View.GONE);
        //    return;
        //}
        if (userIsBlocked) {
            if (newContactLayout != null) newContactLayout.setVisibility(View.GONE);
            return;
        }

        if (!VCardManager.getInstance().isRosterOrHistoryLoaded(account)) {
            return;
        }

        AbstractChat chat = getChat();
        SubscriptionState subscriptionState = RosterManager.getInstance().getSubscriptionState(account, user);
        boolean inRoster = RosterManager.getInstance().getRosterContact(account, user) != null;
        boolean show = false;

        switch (subscriptionState.getSubscriptionType()) {
            case SubscriptionState.FROM:
            case SubscriptionState.NONE:
                //check both FROM and NONE types for the absence of pending subscriptions,
                //and whether or not user already closed this "suggestion" dialog
                if (subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_NONE) {
                    if (chat != null && !chat.isAddContactSuggested()) {
                        show = true;
                    }
                }
                //Since we need to check other pending subscription states of NONE, we skip a break; here
            case SubscriptionState.TO:
                //check all states for incoming + incoming & outgoinig types of pending subscriptions.
                //
                // NONE can be valid with both IN and IN_OUT
                // TO can be valid only with IN.(TO && any type of OUT request are incompatible).
                // FROM can not be valid with these checks. (FROM && any type of IN request are incompatible).
                if (subscriptionState.hasIncomingSubscription()) {
                    show = true;
                }
                break;
        }

        if (show) {
            inflateNewContactLayout(subscriptionState, inRoster);
        } else {
            if (newContactLayout != null) newContactLayout.setVisibility(View.GONE);
            PresenceManager.getInstance().clearSubscriptionRequestNotification(account, user);
        }
    }

    private void inflateNewContactLayout(final SubscriptionState subscriptionState,
                                         final boolean inRoster) {
        if (newContactLayout == null) {
            newContactLayout = (ViewGroup) stubNewContact.inflate();
        } else {
            newContactLayout.setVisibility(View.VISIBLE);
        }
        //intercept touch events to avoid clicking on the messages behind the panel.
        newContactLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        manageToolbarElevation(true);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
            newContactLayout.setBackgroundResource(R.color.grey_950);
        }

        final Transition transition = new Slide(Gravity.TOP);
        transition.setDuration(300);
        transition.addTarget(newContactLayout);

        AbstractContact bestContact = RosterManager.getInstance().getBestContact(getAccount(), getUser());
        final String name = bestContact != null ? bestContact.getName() : getUser().toString();

        addContact = newContactLayout.findViewById(R.id.add_contact);
        blockContact = newContactLayout.findViewById(R.id.block_contact);
        closeNewContactLayout = newContactLayout.findViewById(R.id.close_new_contact_layout);

        switch (subscriptionState.getSubscriptionType()) {
            case SubscriptionState.FROM:
            case SubscriptionState.NONE:
                if (subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_NONE) {
                    if (inRoster) {
                        // FROM = contact is subscribed to our presence. No pending subscription requests. Only in roster.
                        // NONE = No current subscriptions or requests. In roster.
                        setNewContactSubscribeLayout();
                    } else {
                        // NONE = No current subscriptions or requests. Not in roster.
                        setNewContactAddLayout();
                    }
                }
                if (subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_IN) {
                    // NONE + PENDING_IN = No current subscriptions and a pending request from contact to us.
                    setNewContactAddLayout();
                }
                if (subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_IN_OUT) {
                    // NONE + PENDING_IN_OUT = No current subscriptions, pending requests to each other.
                    setNewContactAllowLayout();
                }
                break;
            case SubscriptionState.TO:
                if (subscriptionState.getPendingSubscription() == SubscriptionState.PENDING_IN) {
                    // TO + PENDING_IN = We are subscribed to contact's presence. Contact sent us a subscription request.
                    setNewContactAllowLayout();
                }
        }

        addContact.setTextColor(ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(account));
        addContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Application.getInstance().runInBackgroundNetworkUserRequest(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (!inRoster) {
                                RosterManager.getInstance()                                                            // Create contact if not in roster.
                                        .createContact(getAccount(), getUser(), name, new ArrayList<String>());        // (subscription request is sent automatically)
                            } else {
                                if (subscriptionState.getSubscriptionType() == SubscriptionState.FROM                  // Either only an active subscription to us OR
                                        || subscriptionState.getSubscriptionType() == SubscriptionState.NONE) {        // No active subscriptions.

                                    if (!subscriptionState.hasOutgoingSubscription()) {                                // No outgoing subscription at the moment
                                        PresenceManager.getInstance().subscribeForPresence(account, user);             // So we try to subscribe for contact's presence.
                                    }
                                }
                            }
                            if (subscriptionState.getSubscriptionType() == SubscriptionState.TO) {                   // If we are currently subscribed to contact
                                PresenceManager.getInstance().addAutoAcceptSubscription(account, user);              // Preemptively allow incoming subscription request.
                            } else if (subscriptionState.getSubscriptionType() == SubscriptionState.NONE) {          // If there are no subscriptions
                                if (subscriptionState.hasIncomingSubscription()) {                                   // If we have incoming subscription request
                                    PresenceManager.getInstance().acceptSubscription(account, user, false);  // "quietly" accept it (since we are in the process of
                                } else {                                                                             // adding a contact, we don't need to create unnecessary Action messages
                                    PresenceManager.getInstance().addAutoAcceptSubscription(account, user);          // or Preemptively allow incoming request.
                                }
                            }
                        } catch (SmackException.NotLoggedInException
                                | XMPPException.XMPPErrorException
                                | SmackException.NotConnectedException
                                | InterruptedException
                                | SmackException.NoResponseException
                                | NetworkException e) {
                            e.printStackTrace();
                        }
                    }
                });
                TransitionManager.beginDelayedTransition((ViewGroup) rootView, transition);
                newContactLayout.setVisibility(View.GONE);
                manageToolbarElevation(false);
            }
        });

        blockContact.setTextColor(SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark ?
                getResources().getColor(R.color.red_700) : getResources().getColor(R.color.red_900));
        blockContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // fully discard subscription
                    PresenceManager.getInstance().discardSubscription(account, user);
                    PresenceManager.getInstance().unsubscribeFromPresence(account, user);
                } catch (NetworkException e) {
                    Application.getInstance().onError(R.string.CONNECTION_FAILED);
                }
                BlockingManager.getInstance().blockContact(account, user, new BlockingManager.BlockContactListener() {
                    @Override
                    public void onSuccessBlock() {
                        Toast.makeText(Application.getInstance(), R.string.contact_blocked_successfully, Toast.LENGTH_SHORT).show();
                        if (newContactLayout != null) {
                            if (newContactLayout.getVisibility() == View.VISIBLE)
                                newContactLayout.setVisibility(View.GONE);
                        }
                        getActivity().finish();
                    }

                    @Override
                    public void onErrorBlock() {
                        Toast.makeText(Application.getInstance(), R.string.error_blocking_contact, Toast.LENGTH_SHORT).show();
                    }
                });
                TransitionManager.beginDelayedTransition((ViewGroup) rootView, transition);
            }
        });

        closeNewContactLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (subscriptionState.hasIncomingSubscription()) {                              // check if we have an incoming (IN) subscription
                    try {
                        PresenceManager.getInstance().discardSubscription(account, user);       // discard it on "X"-press
                    } catch (NetworkException e) {
                        e.printStackTrace();
                    }
                }
                if (getChat() != null) {
                    getChat().setAddContactSuggested(true);                                     // remember "X"-press
                }
                TransitionManager.beginDelayedTransition((ViewGroup) rootView, transition);
                newContactLayout.setVisibility(View.GONE);
                manageToolbarElevation(false);
            }
        });
    }

    private void setNewContactSubscribeLayout() {
        TextView addContactMessage = newContactLayout.findViewById(R.id.add_contact_message);

        addContact.setText(R.string.chat_subscribe);
        addContactMessage.setText(R.string.chat_subscribe_request_outgoing);
        addContactMessage.setVisibility(View.VISIBLE);
        blockContact.setVisibility(View.GONE);
    }

    private void setNewContactAddLayout() {
        TextView addContactMessage = newContactLayout.findViewById(R.id.add_contact_message);

        addContactMessage.setVisibility(View.GONE);
        addContact.setText(R.string.contact_add);
        blockContact.setVisibility(View.VISIBLE);
    }

    private void setNewContactAllowLayout() {
        TextView addContactMessage = newContactLayout.findViewById(R.id.add_contact_message);

        addContact.setText(R.string.chat_allow);
        addContactMessage.setText(R.string.chat_subscribe_request_incoming);
        addContactMessage.setVisibility(View.VISIBLE);
        blockContact.setVisibility(View.GONE);
    }

    // remove/recreate activity's toolbar elevation.
    private void manageToolbarElevation(boolean remove) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (((ChatActivity) getActivity()).getToolbar().getElevation() != 0f)
                toolbarElevation = ((ChatActivity) getActivity()).getToolbar().getElevation();
            ((ChatActivity) getActivity()).getToolbar().setElevation(remove ? 0f : toolbarElevation);
        }
    }

    @Override
    public void onBind(MessageRealmObject message) {
        if (message != null && message.isValid() && !message.isRead()) {
            AbstractChat chat = getChat();
            if (chat != null) {
                chat.markAsRead(message, true);
                updateUnread();
            }
        }
    }

    private void updateUnread() {
        AbstractChat chat = getChat();
        if (chat != null) updateNewReceivedMessageCounter(chat.getUnreadMessageCount());
    }

    private void showScrollDownButtonIfNeed() {
        int pastVisibleItems = layoutManager.findLastVisibleItemPosition();
        boolean isBottom = pastVisibleItems >= chatMessageAdapter.getItemCount() - 1;

        if (isBottom
                || currentVoiceRecordingState == VoiceRecordState.TouchRecording
                || currentVoiceRecordingState == VoiceRecordState.NoTouchRecording) {
            btnScrollDown.setVisibility(View.GONE);
        } else btnScrollDown.setVisibility(View.VISIBLE);
    }

    private void updateNewReceivedMessageCounter(int count) {
        tvNewReceivedCount.setText(String.valueOf(count));
        if (count > 0)
            tvNewReceivedCount.setVisibility(View.VISIBLE);
        else tvNewReceivedCount.setVisibility(View.GONE);
    }

    private void setFirstUnreadMessageId(String id) {
        chatMessageAdapter.setFirstUnreadMessageId(id);
        chatMessageAdapter.notifyDataSetChanged();
    }

    private void closeInteractionPanel() {
        chatMessageAdapter.resetCheckedItems();
        setUpInputViewButtons();
    }

    public void onSensorNearState() {

    }

    private void sendVoiceMessage() {
        manageVoiceMessage(recordSaveAllowed);
        scrollDown();
        setFirstUnreadMessageId(null);
    }

    public void clearVoiceMessage() {
        manageVoiceMessage(false);
    }

    private void manageVoiceMessage(boolean saveMessage) {
        handler.removeCallbacks(record);
        handler.removeCallbacks(timer);
        if (bottomPanelMessagesIds != null && bottomPanelMessagesIds.size() > 0) {
            stopRecordingAndSend(saveMessage, bottomPanelMessagesIds);
        } else
            stopRecordingAndSend(saveMessage);
        cancelRecordingCompletely(true);
    }

    private void stopRecording() {
        Utils.lockScreenRotation(getActivity(), false);
        if (recordSaveAllowed) {
            sendImmediately = false;
            //ignore = false;
            VoiceManager.getInstance().stopRecording(false);
            endRecordingButtonsAnimation();
            beginTimer(false);
            currentVoiceRecordingState = VoiceRecordState.StoppedRecording;
        } else {
            ignoreReceiver = true;
            clearVoiceMessage();
        }
    }

    public void setVoicePresenterData(String tempFilePath) {
        recordingPath = tempFilePath;
        if (recordingPath != null) {
            setUpVoiceMessagePresenter();
            showScrollDownButtonIfNeed();
        }
    }

    private void setUpVoiceMessagePresenter() {
        long time = HttpFileUploadManager.getVoiceLength(recordingPath);

        recordingPresenterDuration.setText(String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.SECONDS.toMinutes(time),
                TimeUnit.SECONDS.toSeconds(time)));
        subscribeForRecordedAudioProgress();

        recordingPresenterLayout.setVisibility(View.VISIBLE);

        //recordingPresenter.updateVisualizerFromFile();
        VoiceMessagePresenterManager.getInstance().sendWaveDataIfSaved(recordingPath, recordingPresenter);
        recordingPresenter.updatePlayerPercent(0f, false);
        recordingPresenter.setOnTouchListener(new PlayerVisualizerView.onProgressTouch() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        if (VoiceManager.getInstance().playbackInProgress("", null))
                            return super.onTouch(view, motionEvent);
                        else {
                            ((PlayerVisualizerView) view).updatePlayerPercent(0, true);
                            return true;
                        }
                    case MotionEvent.ACTION_UP:
                        if (VoiceManager.getInstance().playbackInProgress("", null))
                            VoiceManager.getInstance().seekAudioPlaybackTo("", null, (int) motionEvent.getX(), view.getWidth());
                        return super.onTouch(view, motionEvent);

                }
                return super.onTouch(view, motionEvent);
            }
        });
        recordingPlayButton.setImageResource(R.drawable.ic_play);

        recordingPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VoiceManager.getInstance().voiceClicked(recordingPath);
            }
        });
        recordingDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                releaseRecordedVoicePlayback(recordingPath);
                finishVoiceRecordLayout();
                recordingPath = null;
                audioProgressSubscription.unsubscribe();
            }
        });
        recordingPresenterSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomPanelMessagesIds != null && bottomPanelMessagesIds.size() > 0) {
                    sendStoppedVoiceMessage(recordingPath, bottomPanelMessagesIds);
                    hideBottomMessagePanel();
                } else {
                    sendStoppedVoiceMessage(recordingPath);
                }
                scrollDown();
                setFirstUnreadMessageId(null);
                finishVoiceRecordLayout();
                recordingPath = null;
                audioProgressSubscription.unsubscribe();
            }
        });
    }

    private void subscribeForRecordedAudioProgress() {
        audioProgress = VoiceManager.PublishAudioProgress.getInstance().subscribeForProgress();
        audioProgressSubscription = audioProgress.doOnNext(new Action1<VoiceManager.PublishAudioProgress.AudioInfo>() {
            @Override
            public void call(VoiceManager.PublishAudioProgress.AudioInfo audioInfo) {
                setUpAudioProgress(audioInfo);
            }
        }).subscribe();
    }

    public void cleanUpVoice(boolean deleteTempFile) {
        if (deleteTempFile) VoiceManager.getInstance().deleteRecordedFile();
        VoiceManager.getInstance().releaseMediaRecorder();
        VoiceManager.getInstance().releaseMediaPlayer();
    }

    private void setUpAudioProgress(VoiceManager.PublishAudioProgress.AudioInfo info) {
        if (info.getAttachmentIdHash() == 0) {
            recordingPresenter.updatePlayerPercent((float) info.getCurrentPosition() / info.getDuration(), false);
            if (info.getResultCode() == VoiceManager.COMPLETED_AUDIO_PROGRESS
                    || info.getResultCode() == VoiceManager.PAUSED_AUDIO_PROGRESS)
                recordingPlayButton.setImageResource(R.drawable.ic_play);
            else recordingPlayButton.setImageResource(R.drawable.ic_pause);
        }
    }

    public void finishVoiceRecordLayout() {
        recordingPresenterLayout.setVisibility(View.GONE);
        recordingPresenter.updateVisualizer(null);
        currentVoiceRecordingState = VoiceRecordState.NotRecording;
        closeVoiceRecordPanel();
        changeStateOfInputViewButtonsTo(true);
    }

    private void closeVoiceRecordPanel() {
        recordLockView.setVisibility(View.GONE);
        voiceMessageRecorderLayout.setVisibility(View.GONE);
        cancelRecordingLayout.setVisibility(View.GONE);
        handler.removeCallbacks(postAnimation);
        showScrollDownButtonIfNeed();
    }

    private void endRecordingButtonsAnimation() {
        recordButtonExpanded.hide();
        recordButtonExpanded.animate()
                .y(rootViewHeight - (fabMicViewHeightSize + fabMicViewMarginBottom))
                .setDuration(300)
                .start();
        recordLockView.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplication(), R.anim.fade_out_200));
        recordLockView.animate()
                .y(rootViewHeight - (lockViewHeightSize + lockViewMarginBottom))
                .setDuration(300)
                .start();
    }

    private void cancelRecordingCompletely(boolean enableInputButtons) {
        changeStateOfInputViewButtonsTo(enableInputButtons);
        currentVoiceRecordingState = VoiceRecordState.NotRecording;
        VoiceManager.getInstance().releaseMediaRecorder();

        endRecordingButtonsAnimation();
        voiceMessageRecorderLayout.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplication(), R.anim.slide_out_right_opaque));
        handler.postDelayed(postAnimation, 295);

        beginTimer(false);
        Utils.performHapticFeedback(rootView);
        Utils.lockScreenRotation(getActivity(), false);
    }

    private void changeStateOfInputViewButtonsTo(boolean state) {
        rootView.findViewById(R.id.button_emoticon).setEnabled(state);
        attachButton.setEnabled(state);
        securityButton.setEnabled(state);
    }

    public void beginTimer(boolean start) {
        if (start) voiceMessageRecorderLayout.setVisibility(View.VISIBLE);
        if (start) {
            ChatStateManager.getInstance().onComposing(account, user, null, ChatStateSubtype.voice);
            stopTypingTimer.cancel();
            ignoreReceiver = false;
            slideToCancelLayout.animate().x(0).setDuration(0).start();
            recordLockChevronImage.setAlpha(1f);
            recordLockImage.setImageResource(R.drawable.ic_security_plain_24dp);
            recordLockImage.setPadding(0, Utils.dipToPx(4, getActivity()), 0, 0);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) recordLockChevronImage.getLayoutParams();
            layoutParams.topMargin = 0;
            recordLockChevronImage.setLayoutParams(layoutParams);
            recordTimer.setBase(SystemClock.elapsedRealtime());
            recordTimer.start();
            currentVoiceRecordingState = VoiceRecordState.TouchRecording;
            showScrollDownButtonIfNeed();
            manageScreenSleep(true);
        } else {
            recordTimer.stop();
            ChatStateManager.getInstance().onPaused(account, user);
            manageScreenSleep(false);
        }
    }

    private void manageScreenSleep(boolean keepScreenOn) {
        if (getActivity() != null) {
            if (keepScreenOn) {
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    /**
     * Bottom message Panel (forwarding or editing)
     */

    @Override
    public void onClose() {
        hideBottomMessagePanel();
    }

    public void hideBottomMessagePanel() {
        bottomPanelMessagesIds.clear();
        setUpInputViewButtons();
        if (bottomMessagesPanel.getPurpose().equals(BottomMessagesPanel.Purposes.EDITING))
            inputView.setText("");
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            FragmentManager fragmentManager = getChildFragmentManager();
            FragmentTransaction fTrans = fragmentManager.beginTransaction();
            fTrans.remove(bottomMessagesPanel);
            fTrans.commit();
        }
    }

    public void setBottomPanelMessagesIds(List<String> bottomPanelMessagesIds, BottomMessagesPanel.Purposes purpose) {
        this.bottomPanelMessagesIds = bottomPanelMessagesIds;
        isReply = false;
        setUpInputViewButtons();
        showBottomMessagesPanel(bottomPanelMessagesIds, purpose);
    }

    private void showBottomMessagesPanel(List<String> forwardIds, BottomMessagesPanel.Purposes purpose) {
        List<String> ids = new ArrayList<>(forwardIds);
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            FragmentManager fragmentManager = getChildFragmentManager();
            bottomMessagesPanel = BottomMessagesPanel.newInstance(ids, purpose);
            FragmentTransaction fTrans = fragmentManager.beginTransaction();
            fTrans.replace(R.id.secondBottomPanel, bottomMessagesPanel);
            fTrans.commit();
        }
    }

    private void sendForwardMessage(List<String> messages, String text, String markup) {
        ForwardManager.forwardMessage(messages, account, user, text, markup);
        hideBottomMessagePanel();
        setFirstUnreadMessageId(null);
    }

    private void openChooserForForward(ArrayList<String> forwardIds) {
        ((ChatActivity) getActivity()).forwardMessages(forwardIds);
    }

    private enum VoiceRecordState {
        NotRecording,
        InitiatedRecording,
        TouchRecording,
        NoTouchRecording,
        StoppedRecording
    }

    public interface ChatViewerFragmentListener {
        void onCloseChat();

        void onMessageSent();

        void registerChatFragment(ChatFragment chatFragment);

        void unregisterChatFragment();

        void playIncomingAnimation();
    }
}
