package com.xabber.android.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.color.ColorManager;

import java.util.List;

/**
 * Created by valery.miller on 20.07.17.
 */

public class XMPPAccountAdapter extends RecyclerView.Adapter {

    private List<XMPPAccountSettings> items;
    private boolean isAllChecked;
    private Context context;

    public XMPPAccountAdapter(Context context) {
        this.context = context;
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

        viewHolder.avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (account.isSynchronization() || isAllChecked) {
                    if (account.getStatus() == XMPPAccountSettings.SyncStatus.localNewer) {
                        account.setStatus(XMPPAccountSettings.SyncStatus.remoteNewer);
                        account.setTimestamp(0);

                        // change settings
                        XMPPAccountSettings set = XabberAccountManager.getInstance().getAccountSettingsForSync(account.getJid());
                        if (set != null) {
                            account.setColor(set.getColor());
                        }
                    } else if (account.getStatus() == XMPPAccountSettings.SyncStatus.remoteNewer) {
                        account.setStatus(XMPPAccountSettings.SyncStatus.localNewer);
                        account.setTimestamp((int) (System.currentTimeMillis() / 1000L));

                        // change settings
                        AccountItem item = AccountManager.getInstance().getAccount(XabberAccountManager.getInstance().getExistingAccount(account.getJid()));
                        if (item != null) {
                            account.setColor(ColorManager.getInstance().convertIndexToColorName(item.getColorIndex()));
                        }
                    }
                    notifyDataSetChanged();
                }
            }
        });

        // set colors
        int colorId = ColorManager.getInstance().convertColorNameToId(account.getColor());
        viewHolder.color.setColorFilter(colorId);
        viewHolder.username.setTextColor(colorId);

        // set username
        if (account.getUsername() != null && !account.getUsername().isEmpty())
            viewHolder.username.setText(account.getUsername());
        else viewHolder.username.setText(account.getJid());

        // set jid
        //viewHolder.jid.setText(account.getJid());

        if (account.isSynchronization() || isAllChecked) {
            if (account.getStatus() != null) {
                switch (account.getStatus()) {
                    case local:
                        viewHolder.jid.setText("local");
                        viewHolder.avatar.setImageResource(R.drawable.ic_sync_upload);
                        break;
                    case remote:
                        viewHolder.jid.setText("remote");
                        viewHolder.avatar.setImageResource(R.drawable.ic_sync_download);
                        break;
                    case localNewer:
                        viewHolder.jid.setText("local > remote");
                        viewHolder.avatar.setImageResource(R.drawable.ic_sync_upload);
                        break;
                    case remoteNewer:
                        if (account.isDeleted()) {
                            viewHolder.jid.setText("remote account deleted");
                            viewHolder.avatar.setImageResource(R.drawable.ic_delete_grey);
                        } else {
                            viewHolder.jid.setText("local < remote");
                            viewHolder.avatar.setImageResource(R.drawable.ic_sync_download);
                        }
                        break;
                    case localEqualsRemote:
                        viewHolder.jid.setText("local = remote");
                        viewHolder.avatar.setImageResource(R.drawable.ic_sync_done);
                        break;
                    default:
                        break;
                }
            }
        } else {
            viewHolder.jid.setText("sync disabled");
            viewHolder.avatar.setImageResource(R.drawable.ic_sync_disable);
            viewHolder.username.setTextColor(context.getResources().getColor(R.color.black_text));
        }

        // set sync checkbox
        if (isAllChecked) {
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

        ImageView color;
        ImageView avatar;
        TextView username;
        TextView jid;
        CheckBox chkAccountSync;


        XMPPAccountVH(View itemView) {
            super(itemView);
            color = (ImageView) itemView.findViewById(R.id.color);
            avatar = (ImageView) itemView.findViewById(R.id.avatar);
            username = (TextView) itemView.findViewById(R.id.tvAccountName);
            jid = (TextView) itemView.findViewById(R.id.tvAccountJid);
            chkAccountSync = (CheckBox) itemView.findViewById(R.id.chkAccountSync);
            chkAccountSync.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    notifyDataSetChanged();
                }
            });
        }

    }
}
