package com.xabber.android.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.ui.color.ColorManager;

import org.jxmpp.stringprep.XmppStringprepException;

import java.util.List;

/**
 * Created by valery.miller on 20.07.17.
 */

public class XMPPAccountAdapter extends RecyclerView.Adapter {

    private List<XMPPAccountSettings> items;
    private boolean isAllChecked;
    private Context context;
    private Listener listener;

    public interface Listener {
        void onChkClick();
    }

    public XMPPAccountAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<XMPPAccountSettings> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setAllChecked(boolean checked) {
        isAllChecked = checked;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new XMPPAccountVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_xmpp_account, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final XMPPAccountSettings account = items.get(position);
        XMPPAccountVH viewHolder = (XMPPAccountVH) holder;

        viewHolder.chkAccountSync.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                account.setSynchronization(isChecked);
            }
        });

        // set colors
        int colorId = ColorManager.getInstance().convertColorNameToId(account.getColor());

        // set username
        if (account.getUsername() != null && !account.getUsername().isEmpty())
            viewHolder.username.setText(account.getUsername());
        else viewHolder.username.setText(account.getJid());

        // set avatar
        try {
            AccountJid accountJid = AccountJid.from(account.getJid() + "/resource");
            viewHolder.avatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatarForSync(accountJid, colorId));
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        if (account.isSynchronization() || isAllChecked) {
            if (account.getStatus() != null) {
                switch (account.getStatus()) {
                    case local:
                        viewHolder.jid.setText(R.string.sync_status_local);
                        break;
                    case remote:
                        viewHolder.jid.setText(R.string.sync_status_remote);
                        break;
                    case localNewer:
                        viewHolder.jid.setText(R.string.sync_status_local);
                        break;
                    case remoteNewer:
                        if (account.isDeleted()) {
                            viewHolder.jid.setText(R.string.sync_status_deleted);
                        } else {
                            viewHolder.jid.setText(R.string.sync_status_remote);
                        }
                        break;
                    case localEqualsRemote:
                        viewHolder.jid.setText(R.string.sync_status_ok);
                        break;
                    default:
                        break;
                }
            }
        } else {
            viewHolder.jid.setText(R.string.sync_status_no);
        }

        // set sync checkbox
        if (account.isSyncNotAllowed()) {
            viewHolder.jid.setText(R.string.sync_status_not_allowed);
            viewHolder.chkAccountSync.setEnabled(false);
        } else if (isAllChecked) {
            viewHolder.chkAccountSync.setEnabled(false);
            viewHolder.chkAccountSync.setChecked(true);
        } else {
            viewHolder.chkAccountSync.setEnabled(true);
            viewHolder.chkAccountSync.setChecked(account.isSynchronization());
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private class XMPPAccountVH extends RecyclerView.ViewHolder {

        ImageView avatar;
        TextView username;
        TextView jid;
        Switch chkAccountSync;


        XMPPAccountVH(View itemView) {
            super(itemView);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
            username = (TextView) itemView.findViewById(R.id.tvAccountName);
            jid = (TextView) itemView.findViewById(R.id.tvAccountJid);
            chkAccountSync = (Switch) itemView.findViewById(R.id.chkAccountSync);
            chkAccountSync.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    notifyDataSetChanged();
                    listener.onChkClick();
                }
            });
        }

    }
}
