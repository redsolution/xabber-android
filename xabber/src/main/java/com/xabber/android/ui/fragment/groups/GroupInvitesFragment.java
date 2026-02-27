package com.xabber.android.ui.fragment.groups;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groups.GroupInviteManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.GroupChat;
import com.xabber.android.ui.OnGroupSelectorListToolbarActionResultListener;
import com.xabber.android.ui.activity.GroupSettingsActivity.GroupchatSelectorListToolbarActions;
import com.xabber.android.ui.adapter.GroupchatInvitesAdapter;
import com.xabber.android.ui.fragment.groups.GroupchatInfoFragment.GroupchatSelectorListItemActions;
import com.xabber.android.ui.widget.DividerItemDecoration;

import org.jetbrains.annotations.NotNull;
import org.jivesoftware.smack.ExceptionCallback;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Stanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GroupInvitesFragment extends Fragment implements GroupchatSelectorListToolbarActions,
        OnGroupSelectorListToolbarActionResultListener, StanzaListener, ExceptionCallback {

    private static final String ARG_ACCOUNT = "com.xabber.android.ui.fragment.groups.GroupchatInvitesFragment.ARG_ACCOUNT";
    private static final String ARG_GROUPCHAT_CONTACT = "com.xabber.android.ui.fragment.groups.GroupchatInvitesFragment.ARG_GROUPCHAT_CONTACT";

    private AccountJid account;
    private ContactJid groupchatContact;
    private GroupChat groupChat;

    private RecyclerView invitesList;
    private GroupchatInvitesAdapter adapter;
    private TextView placeholder;

    private GroupchatSelectorListItemActions invitesListListener;

    public static GroupInvitesFragment newInstance(AccountJid account, ContactJid groupchatContact) {
        GroupInvitesFragment fragment = new GroupInvitesFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ACCOUNT, account);
        args.putParcelable(ARG_GROUPCHAT_CONTACT, groupchatContact);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        if (requireActivity() instanceof GroupchatSelectorListItemActions) {
            invitesListListener = (GroupchatSelectorListItemActions) requireActivity();
        } else {
            throw new RuntimeException(requireActivity().toString()
                    + " must implement GroupchatSettingsActivity.GroupchatElementListActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        invitesListListener = null;
        adapter.removeListener();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            account = args.getParcelable(ARG_ACCOUNT);
            groupchatContact = args.getParcelable(ARG_GROUPCHAT_CONTACT);
        } else {
            requireActivity().finish();
        }

        AbstractChat chat = ChatManager.getInstance().getChat(account, groupchatContact);
        if (chat instanceof GroupChat) {
            groupChat = (GroupChat) chat;
            GroupInviteManager.INSTANCE.requestGroupInvitationsList(account, groupchatContact, this, this);
        } else {
            requireActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnGroupSelectorListToolbarActionResultListener.class, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnGroupSelectorListToolbarActionResultListener.class, this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_groupchat_settings_list, container, false);
        placeholder = view.findViewById(R.id.groupchatSettingsPlaceholderTV);
        invitesList = view.findViewById(R.id.groupchatSettingsElementList);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        invitesList.setLayoutManager(llm);
        DividerItemDecoration divider = new DividerItemDecoration(invitesList.getContext(), llm.getOrientation());
        divider.skipDividerOnLastItem(true);
        invitesList.addItemDecoration(divider);
        adapter = new GroupchatInvitesAdapter();
        adapter.setListener(invitesListListener);
        invitesList.setAdapter(adapter);
        setupList();
        return view;
    }

    private void setupList(){
        if (groupChat.getListOfInvites() != null && groupChat.getListOfInvites().size() > 0)
            adapter.setInvites(groupChat.getListOfInvites());
        setupPlaceholder();
    }

    private void setupPlaceholder() {
        if (groupChat.getListOfInvites() != null && groupChat.getListOfInvites().size() > 0) {
            invitesList.setVisibility(View.VISIBLE);
            placeholder.setVisibility(View.GONE);
        } else {
            invitesList.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
            placeholder.setText(getString(R.string.groupchat_invite_lis_empty));
        }
    }

    private void setupError(String errorString){
        if (groupChat.getListOfInvites() != null && groupChat.getListOfInvites().size() > 0) {
            invitesList.setVisibility(View.VISIBLE);
            placeholder.setVisibility(View.GONE);
        } else {
            invitesList.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
            placeholder.setText(errorString);
        }
    }

    @Override
    public void processException(Exception exception) {
        Application.getInstance().runOnUiThread(() -> {
            if (exception instanceof XMPPException.XMPPErrorException){
                setupError(((XMPPException.XMPPErrorException)exception).getXMPPError().getCondition().name());
            } else {
                setupError(getContext().getText(R.string.groupchat_error).toString());
            }
        });
    }

    @Override
    public void processStanza(Stanza packet) {
        Application.getInstance().runOnUiThread(this::setupList);
    }

    public void actOnSelection() {
        adapter.disableItemClicks(true);
        Set<String> checkedInvites = adapter.getCheckedInvites();
        if (checkedInvites.size() == 0) {
            adapter.disableItemClicks(false);
            return;
        }
        if (checkedInvites.size() == 1) {
            GroupInviteManager.INSTANCE.revokeGroupchatInvitation(account, groupchatContact, checkedInvites.iterator().next());
        } else {
            GroupInviteManager.INSTANCE.revokeGroupchatInvitations(account, groupchatContact, checkedInvites);
        }
    }

    public void cancelSelection() {
        adapter.setCheckedInvites(new ArrayList<>());
    }

    @Override
    public void onActionSuccess(AccountJid account, ContactJid groupchatJid, List<String> successfulJids) {
        if (checkIfWrongEntity(account, groupchatJid)) return;
        Application.getInstance().runOnUiThread(() -> {
            adapter.setInvites(groupChat.getListOfInvites());
            adapter.removeCheckedInvites(successfulJids);
            adapter.disableItemClicks(false);
            setupPlaceholder();
        });
    }

    @Override
    public void onPartialSuccess(AccountJid account, ContactJid groupchatJid, List<String> successfulJids, List<String> failedJids) {
        Application.getInstance().runOnUiThread(() -> {
            onActionSuccess(account, groupchatJid, successfulJids);
            onActionFailure(account, groupchatJid, failedJids);
        });
    }

    @Override
    public void onActionFailure(AccountJid account, ContactJid groupchatJid, List<String> failedJids) {
        if (checkIfWrongEntity(account, groupchatJid)) return;
        Application.getInstance().runOnUiThread(() -> {
            adapter.disableItemClicks(false);
            Toast.makeText(getContext(), getString(R.string.groupchat_failed_to_revoke_invitation) + failedJids, Toast.LENGTH_SHORT).show();
        });
    }

    private boolean checkIfWrongEntity(AccountJid account, ContactJid groupchatJid) {
        if (account == null) return true;
        if (groupchatJid == null) return true;
        if (!account.getBareJid().equals(this.account.getBareJid())) return true;
        return !groupchatJid.getBareJid().equals(this.groupchatContact.getBareJid());
    }
}
