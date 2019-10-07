//package com.xabber.android.ui.adapter;
//
//import androidx.annotation.NonNull;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentStatePagerAdapter;
//import androidx.viewpager.widget.PagerAdapter;
//import android.view.ViewGroup;
//
//import com.xabber.android.data.entity.AccountJid;
//import com.xabber.android.data.entity.UserJid;
//import com.xabber.android.data.extension.muc.MUCManager;
//import com.xabber.android.data.message.CrowdfundingChat;
//import com.xabber.android.ui.fragment.ChatFragment;
//import com.xabber.android.ui.fragment.ContactVcardViewerFragment;
//import com.xabber.android.ui.fragment.CrowdfundingChatFragment;
//import com.xabber.android.ui.fragment.OccupantListFragment;
//import com.xabber.android.ui.fragment.RecentChatFragment;
//
//public class ChatViewerAdapter extends FragmentStatePagerAdapter {
//
//    private static final String LOG_TAG = ChatViewerAdapter.class.getSimpleName();
//    public static final int PAGE_POSITION_RECENT_CHATS = 0;
//    public static final int PAGE_POSITION_CHAT = 1;
//    public static final int PAGE_POSITION_CHAT_INFO = 2;
//
//    public interface FinishUpdateListener {
//        void onChatViewAdapterFinishUpdate();
//    }
//
//    private AccountJid accountJid;
//    private UserJid userJid;
//
//    private int itemCount;
//
//    private FinishUpdateListener finishUpdateListener;
//
//    public ChatViewerAdapter(FragmentManager fragmentManager,
//                             FinishUpdateListener finishUpdateListener) {
//        super(fragmentManager);
//        this.finishUpdateListener = finishUpdateListener;
//        itemCount = 1;
//    }
//
//    public ChatViewerAdapter(FragmentManager fragmentManager,
//                             FinishUpdateListener finishUpdateListener,
//                             @NonNull AccountJid accountJid,
//                             @NonNull UserJid userJid) {
//        super(fragmentManager);
//        this.finishUpdateListener = finishUpdateListener;
//        setChat(accountJid, userJid);
//    }
//
//    public void selectChat(@NonNull AccountJid accountJid, @NonNull UserJid userJid) {
//        if (accountJid.equals(this.accountJid) && userJid.equals(this.userJid)) {
//            return;
//        }
//
//        setChat(accountJid, userJid);
//        notifyDataSetChanged();
//    }
//
//    private void setChat(@NonNull AccountJid accountJid, @NonNull UserJid userJid) {
//        // Crowdfunding chat have only 2 page, other chat types have 3 page
//        if (CrowdfundingChat.USER.equals(userJid.getBareJid().toString()))
//            itemCount = 2;
//        else itemCount = 3;
//        this.accountJid = accountJid;
//        this.userJid = userJid;
//    }
//
//    @Override
//    public Fragment getItem(int position) {
//        if (position == PAGE_POSITION_RECENT_CHATS) {
//            return RecentChatFragment.newInstance();
//        } else if (position == PAGE_POSITION_CHAT) {
//            if (CrowdfundingChat.USER.equals(userJid.getBareJid().toString()))
//                return CrowdfundingChatFragment.newInstance();
//            else return ChatFragment.newInstance(accountJid, userJid);
//        } else if (position == 2) {
//            if (MUCManager.getInstance().hasRoom(accountJid, userJid))
//                return OccupantListFragment.newInstance(accountJid, userJid.getBareUserJid());
//            else return ContactVcardViewerFragment.newInstance(accountJid, userJid);
//        } else {
//            return null;
//        }
//    }
//
//    @Override
//    public int getCount() {
//        return itemCount;
//    }
//
//
//    @Override
//    public void finishUpdate(ViewGroup container) {
//        super.finishUpdate(container);
//
//        finishUpdateListener.onChatViewAdapterFinishUpdate();
//    }
//
//    @Override
//    public float getPageWidth(int position) {
//        if (position == 0 || position == 2) return 0.85f;
//        else return 1;
//    }
//
//    @Override
//    public int getItemPosition(Object object) {
//        // refresh all fragments when data set changed
//        return PagerAdapter.POSITION_NONE;
//    }
//}