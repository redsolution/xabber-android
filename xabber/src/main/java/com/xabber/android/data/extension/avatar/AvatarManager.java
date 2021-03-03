/*
  Copyright (c) 2013, Redsolution LTD. All rights reserved.
  <p>
  This file is part of Xabber project; you can redistribute it and/or
  modify it under the terms of the GNU General Public License, Version 3.
  <p>
  Xabber is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.
  <p>
  You should have received a copy of the GNU General Public License,
  along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.extension.avatar;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.OnLowMemoryListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.repositories.AvatarRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.data.groups.GroupMember;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.OnContactChangedListener;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.xmpp.avatar.UserAvatarManager;
import com.xabber.xmpp.vcardupdate.VCardUpdate;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Provides information about avatars (hashes and values). Store and retrieve
 * hashes from database and binary values from file system. Caches user's hashes
 * and avatar's values in memory. Handles changes in user's hashes. Requests
 * information from server when avatar for given hash don't exists locally.
 * <p/>
 * <p/>
 * This class is thread safe. All operation modification made from synchronized
 * blocks.
 * <p/>
 * <p/>
 * All requests to database / file system made in background thread or on
 * application load.
 *
 * @author alexander.ivanov
 */
public class AvatarManager implements OnLoadListener, OnLowMemoryListener, OnPacketListener {

    public static final String LOG_TAG = AvatarManager.class.getSimpleName();
    public static final String EMPTY_HASH = "";
    /**
     * Maximum image width / height to be loaded.
     */
    private static final int MAX_SIZE = 256;
    private static final Bitmap EMPTY_BITMAP = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
    private static AvatarManager instance;

    private final Application application;

    private final Map<BareJid, String> XEPHashes;
    /**
     * Map with hashes for specified users.
     * <p/>
     * {@link #EMPTY_HASH} is used to store <code>null</code> values.
     */
    private final Map<Jid, String> hashes;
    /**
     * Map with bitmaps for specified hashes.
     * <p/>
     * {@link #EMPTY_BITMAP} is used to store <code>null</code> values.
     */
    private final Map<String, Bitmap> bitmaps;
    /**
     * Map with drawable used in contact list only for specified uses.
     */
    private final Map<Jid, Drawable> contactListDrawables;
    private final Map<Jid, Drawable> contactListDefaultDrawables;
    private final Map<String, Drawable> groupchatMemberDrawables;
    private final Map<String, Drawable> groupchatMemberDefaultDrawables;

    private AvatarManager() {
        this.application = Application.getInstance();

        XEPHashes = new HashMap<>();
        hashes = new HashMap<>();
        bitmaps = new HashMap<>();
        contactListDrawables = new HashMap<>();
        contactListDefaultDrawables = new HashMap<>();
        groupchatMemberDrawables = new HashMap<>();
        groupchatMemberDefaultDrawables = new HashMap<>();
    }

    public static AvatarManager getInstance() {
        if (instance == null) {
            instance = new AvatarManager();
        }

        return instance;
    }

    /**
     * Make {@link Bitmap} from array of bytes.
     *
     * @return Bitmap. <code>null</code> can be returned if value is invalid or
     * is <code>null</code>.
     */
    private static Bitmap makeBitmap(byte[] value) {
        if (value == null) {
            return null;
        }

        // Load only size values
        BitmapFactory.Options sizeOptions = new BitmapFactory.Options();
        sizeOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(value, 0, value.length, sizeOptions);

        // Calculate factor to down scale image
        int scale = 1;
        int width_tmp = sizeOptions.outWidth;
        int height_tmp = sizeOptions.outHeight;
        while (width_tmp / 2 >= MAX_SIZE && height_tmp / 2 >= MAX_SIZE) {
            scale *= 2;
            width_tmp /= 2;
            height_tmp /= 2;
        }

        // Load image
        BitmapFactory.Options resultOptions = new BitmapFactory.Options();
        resultOptions.inSampleSize = scale;
        return BitmapFactory.decodeByteArray(value, 0, value.length, resultOptions);
    }

    private static Bitmap makeXEPBitmap(byte[] value) {
        //int MAX_SIZE = 512;

        if (value == null) {
            return null;
        }

        // Load only size values
        BitmapFactory.Options sizeOptions = new BitmapFactory.Options();
        sizeOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(value, 0, value.length, sizeOptions);

        // Calculate factor to down scale image
        int scale = 1;
        int width_tmp = sizeOptions.outWidth;
        int height_tmp = sizeOptions.outHeight;
        while (width_tmp / 2 >= MAX_SIZE && height_tmp / 2 >= MAX_SIZE) {
            scale *= 2;
            width_tmp /= 2;
            height_tmp /= 2;
        }

        // Load image
        BitmapFactory.Options resultOptions = new BitmapFactory.Options();
        resultOptions.inSampleSize = scale;
        return BitmapFactory.decodeByteArray(value, 0, value.length, resultOptions);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        if (bitmap.getWidth() != bitmap.getHeight()) {
            int min = Math.min(bitmap.getWidth(), bitmap.getHeight());
            int max = Math.max(bitmap.getWidth(), bitmap.getHeight());
            int x = bitmap.getWidth() > min ? ((max - min) / 2) : 0;
            int y = bitmap.getHeight() > min ? ((max - min) / 2) : 0;
            bitmap = Bitmap.createBitmap(bitmap, x, y, min, min);
        }
        final int size = bitmap.getWidth();
        final Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        final Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, size, size);
        final float r = size >> 1;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xff424242);
        canvas.drawCircle(r, r, r, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public static String getAvatarHash(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

        digest.update(bytes);
        return StringUtils.encodeHex(digest.digest());
    }

    @Override
    public void onLoad() {
        Map<BareJid, String> hashes = AvatarRepository.getHashesMapFromRealm();
        Map<String, Bitmap> bitmaps = new HashMap<>();
        Map<BareJid, String> XEPHashes = AvatarRepository.getPepHashesMapFromRealm();

        for (String hash : new HashSet<>(hashes.values()))
            if (!hash.equals(EMPTY_HASH)) {
                Bitmap bitmap = makeBitmap(AvatarStorage.getInstance().read(hash));
                bitmaps.put(hash, bitmap == null ? EMPTY_BITMAP : bitmap);
            }
        for (String hash : new HashSet<>(XEPHashes.values()))
            if (!hash.equals(EMPTY_HASH)) {
                Bitmap bitmap = makeXEPBitmap(AvatarStorage.getInstance().read(hash));
                bitmaps.put(hash, bitmap == null ? EMPTY_BITMAP : bitmap);
            }
        Application.getInstance().runOnUiThread(() ->
                onLoaded(XEPHashes, hashes, bitmaps)
        );
    }

    private void onLoaded(Map<BareJid, String> XEPHashes, Map<BareJid, String> hashes, Map<String, Bitmap> bitmaps) {
        this.XEPHashes.putAll(XEPHashes);
        this.hashes.putAll(hashes);
        this.bitmaps.putAll(bitmaps);
        for (OnContactChangedListener onContactChangedListener :
                Application.getInstance().getUIListeners(OnContactChangedListener.class)) {
            onContactChangedListener.onContactsChanged(Collections.emptyList());
        }
    }

    /**
     * Sets avatar's hash for user.
     *
     * @param hash can be <code>null</code>.
     */
    private void setHash(final BareJid jid, final String hash) {
        hashes.put(jid, hash == null ? EMPTY_HASH : hash);
        contactListDrawables.remove(jid);
        contactListDefaultDrawables.remove(jid);
        AvatarRepository.saveHashToRealm(jid, hash);
    }

    private void setXEPHash(final Jid jid, final String hash) {
        XEPHashes.put(jid.asBareJid(), hash == null ? EMPTY_HASH : hash);
        contactListDrawables.remove(jid);
        contactListDefaultDrawables.remove(jid);
        AvatarRepository.savePepHashToRealm(jid, hash);
    }

    /**
     * Get avatar's value for user.
     *
     * @return avatar's value. <code>null</code> can be returned if user has no
     * avatar or avatar doesn't exists.
     */
    private Bitmap getBitmap(Jid jid) {
        String xepHash = getXEPHash(jid);
        String hash = getHash(jid);
        Bitmap bitmap;

        if (xepHash == null || xepHash.equals(EMPTY_HASH)) {
            if (hash == null || hash.equals(EMPTY_HASH)) {
                return null;
            } else bitmap = bitmaps.get(hash);
        } else bitmap = bitmaps.get(xepHash);

        if (bitmap == EMPTY_BITMAP) {
            return null;
        } else {
            return bitmap;
        }
    }

    private Bitmap getBitmapByHash(String hash) {
        Bitmap bitmap = EMPTY_BITMAP;

        if (!hash.equals(EMPTY_HASH))
            bitmap = bitmaps.get(hash);

        if (bitmap == EMPTY_BITMAP) {
            return null;
        } else {
            return bitmap;
        }
    }

    public String getCurrentXEPHash(Jid jid) {
        return getXEPHash(jid);
    }

    public void setXEPHashAsCurrent(Jid jid, String hash) {
        setXEPHash(jid, hash);
    }

    @Nullable
    public String getHash(Jid bareAddress) {
        return hashes.get(bareAddress.asBareJid());
    }

    @Nullable
    public String getXEPHash(Jid bareAddress) {
        return XEPHashes.get(bareAddress);
    }

    /**
     * Sets avatar's value.
     */
    private void setValue(final String hash, final byte[] value, String type) {
        if (hash == null) {
            return;
        }
        Bitmap bitmap;
        if (type.equals("vcard")) {
            bitmap = makeBitmap(value);
        } else {
            bitmap = makeXEPBitmap(value);
        }
        bitmaps.put(hash, bitmap == null ? EMPTY_BITMAP : bitmap);
        application.runInBackground(() -> AvatarStorage.getInstance().write(hash, value));
    }

    @Override
    public void onLowMemory() {
        contactListDrawables.clear();
        contactListDefaultDrawables.clear();
    }

    /**
     * Gets main account's avatar.
     *
     * @return Avatar or default avatar if:
     * <ul>
     * <li>account has no avatar.</li>
     * </ul>
     */
    public Drawable getMainAccountAvatar() {
        if (AccountManager.getInstance().getFirstAccount() == null || (bitmaps.isEmpty()
                || !hashes.containsKey(AccountManager.getInstance().getFirstAccount().getBareJid().asBareJid())
                || !XEPHashes.containsKey(AccountManager.getInstance().getFirstAccount().getBareJid().asBareJid()))) {
            if (SettingsManager.getMainAvatarHash().equals("0")) {
                AccountJid account = AccountManager.getInstance().getFirstAccount();
                if (account == null) return null;
                return getDefaultAccountAvatar(account);
            }
            else {
                Bitmap value = makeBitmap(AvatarStorage.getInstance().read(SettingsManager.getMainAvatarHash()));
                return new BitmapDrawable(application.getResources(), value);
            }
        } else {
            AccountJid account = AccountManager.getInstance().getFirstAccount();
            if (account == null) return null;
            Bitmap value = getBitmap(account.getFullJid().asBareJid());
            if (value != null) {
                return new BitmapDrawable(application.getResources(), value);
            } else {
                return getDefaultAccountAvatar(account);
            }
        }
    }

    public Drawable getAccountAvatar(AccountJid account) {
        Bitmap value = getBitmap(account.getFullJid().asBareJid());
        if (value != null) {
            return new BitmapDrawable(application.getResources(), value);
        } else {
            return getDefaultAccountAvatar(account);
        }
    }

    public Drawable getAccountAvatarNoDefault(AccountJid account) {
        Bitmap value = getBitmap(account.getFullJid().asBareJid());
        if (value != null) {
            return new BitmapDrawable(application.getResources(), value);
        }
        return null;
    }

    public Drawable getAccountAvatarForSync(AccountJid account, int color) {
        Bitmap value = getBitmap(account.getFullJid().asBareJid());
        if (value != null) {
            return new BitmapDrawable(application.getResources(), value);
        } else {
            return getDefaultAccountAvatarForSync(account, color);
        }
    }

    @NonNull
    public Drawable getDefaultAccountAvatar(AccountJid account) {
        String name = AccountManager.getInstance().getVerboseName(account);
        int color = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);
        return generateDefaultAvatar(name, color);
    }

    @NonNull
    public Drawable getDefaultAccountAvatarForSync(AccountJid account, int color) {
        String name = AccountManager.getInstance().getVerboseName(account);
        return generateDefaultAvatar(name, color);
    }

    /**
     * Gets and caches drawable with avatar for regular user.
     * Or generate and caches text-based avatar.
     */
    public Drawable getUserAvatarForContactList(ContactJid user, String name) {
        Drawable drawable = contactListDrawables.get(user.getJid());
        if (drawable == null) {
            drawable = getUserAvatar(user);
            if (drawable != null) {
                contactListDrawables.put(user.getJid(), drawable);
                contactListDefaultDrawables.remove(user.getJid());
                return drawable;
            } else {
                return getDefaultAvatar(user, name);
            }
        }
        return drawable;
    }

    public Drawable getUserAvatarForVcard(ContactJid user) {
        Drawable drawable = contactListDrawables.get(user.getJid());
        if (drawable == null) {
            drawable = getUserAvatar(user);
            if (drawable != null) {
                contactListDrawables.put(user.getJid(), drawable);
                contactListDefaultDrawables.remove(user.getJid());
                return drawable;
            }
        }
        return drawable;
    }

    /**
     * Gets and caches drawable with room's avatar.
     * Or generate and caches text-based avatar.
     */
    public Drawable getRoomAvatarForContactList(ContactJid user) {
        Drawable drawable = contactListDrawables.get(user.getJid());
        if (drawable == null) {
            drawable = getRoomAvatar(user);
            if (drawable != null) {
                contactListDrawables.put(user.getJid(), drawable);
                contactListDefaultDrawables.remove(user.getJid());
                return drawable;
            } else {
                return getDefaultRoomAvatar(user);
            }
        }
        return drawable;
    }

    public Drawable getGroupMemberAvatar(GroupMember member, AccountJid account) {
        String avatarHash = member.getAvatarHash();
        Drawable drawable;
        if (avatarHash != null) {
            drawable = groupchatMemberDrawables.get(avatarHash);
            if (drawable == null) {
                drawable = getMemberAvatar(avatarHash);
                if (drawable != null) {
                    groupchatMemberDrawables.put(avatarHash, drawable);
                    groupchatMemberDefaultDrawables.remove(member.getBestName());
                } else {
                    if (account != null) {
                        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                        checkIfMemberAvatarIsSavedLocallyAndLoad(member, accountItem);
                    }
                    drawable = getDefaultMemberAvatar(member);
                }
            }
        } else {
            drawable = getDefaultMemberAvatar(member);
        }
        return drawable;
    }

    private void checkIfMemberAvatarIsSavedLocallyAndLoad(GroupMember member, AccountItem accountItem) {
        Application.getInstance().runInBackgroundUserRequest(() -> {
            byte[] avatarValue = AvatarStorage.getInstance().read(member.getAvatarHash());
            if (avatarValue != null) {
                Bitmap bitmap = makeBitmap(avatarValue);
                bitmaps.put(member.getAvatarHash(), bitmap == null ? EMPTY_BITMAP : bitmap);
            } else {
                if (accountItem != null) {
                    UserAvatarManager.getInstanceFor(accountItem.getConnection()).requestAvatarOfGroupchatMember(member);
                }
            }
        });
    }

    /**
     * Gets bitmap with avatar for regular user.
     */
    public Bitmap getContactCircleBitmap(ContactJid user, String name) {
        return getCircleBitmap(drawableToBitmap(getUserAvatarForContactList(user, name)));
    }

    public Bitmap getGroupMemberCircleBitmap(GroupMember groupMember, AccountJid accountJid){
        return getCircleBitmap(drawableToBitmap(getGroupMemberAvatar(groupMember, accountJid)));
    }

    /**
     * Generate text-based avatar for regular user.
     */
    public Drawable generateDefaultAvatar(@NonNull String jid, @NonNull String name) {
        return generateDefaultAvatar(name, ColorGenerator.MATERIAL.getColor(jid));
    }

    /**
     * Gets avatar drawable for regular user from bitmap.
     */
    private Drawable getUserAvatar(ContactJid user) {
        Bitmap value = getBitmap(user.getJid());
        if (value != null) {
            return new BitmapDrawable(application.getResources(), value);
        }
        return null;
    }

    /**
     * Gets avatar drawable for room from bitmap.
     */
    private Drawable getRoomAvatar(ContactJid user) {
        Bitmap value = getBitmap(user.getJid());
        if (value != null) {
            return new BitmapDrawable(application.getResources(), value);
        }
        return null;
    }

    private Drawable getMemberAvatar(String avatarHash) {
        Bitmap value = getBitmapByHash(avatarHash);
        if (value != null) {
            return new BitmapDrawable(application.getResources(), value);
        }
        return null;
    }

    /**
     * Gets and caches text-base avatar for regular user from cached drawables.
     */
    private Drawable getDefaultAvatar(ContactJid user, String name) {
        Drawable drawable = contactListDefaultDrawables.get(user.getJid());
        if (drawable == null) {
            drawable = generateDefaultAvatar(user.getBareJid().toString(), name);
            contactListDefaultDrawables.put(user.getJid(), drawable);
        }
        return drawable;
    }

    /**
     * Gets and caches text-base avatar for room from cached drawables.
     */
    private Drawable getDefaultRoomAvatar(ContactJid user) {
        Drawable drawable = contactListDefaultDrawables.get(user.getJid());
        if (drawable == null) {
            drawable = generateDefaultRoomAvatar(user.getBareJid().toString());
            contactListDefaultDrawables.put(user.getJid(), drawable);
        }
        return drawable;
    }

    private Drawable getDefaultMemberAvatar(GroupMember member) {
        Drawable drawable = groupchatMemberDefaultDrawables.get(member.getBestName());
        if (drawable == null) {
            drawable = generateDefaultRoomAvatar(member.getBestName());
            groupchatMemberDefaultDrawables.put(member.getBestName(), drawable);
        }
        return drawable;
    }

    /**
     * Generate text-based avatar for regular user.
     */
    private Drawable generateDefaultAvatar(@NonNull String name, int color) {
        String[] words = name.split("\\s+");
        String chars = "";

        if (words.length >= 1 && words[0].length() > 0)
            chars = chars + words[0].substring(0, 1);

        if (words.length >= 2 && words[1].length() > 0)
            chars = chars + words[1].substring(0, 1);

        return TextDrawable.builder()
                .beginConfig().fontSize(60).bold().width(150).height(150).endConfig()
                .buildRound(chars.toUpperCase(), color);
    }

    /**
     * Generate text-based avatar for room.
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private Drawable generateDefaultRoomAvatar(@NonNull String jid) {
        Drawable[] layers = new Drawable[2];
        layers[0] = new ColorDrawable(ColorGenerator.MATERIAL.getColor(jid));
        layers[1] = application.getResources().getDrawable(R.drawable.ic_conference_white);

        LayerDrawable layerDrawable = new LayerDrawable(layers);
        layerDrawable.setLayerInset(1, 25, 25, 25, 30);

        return layerDrawable;
    }

    /**
     * Gets avatar for occupant in the room.
     */
    public Drawable getOccupantAvatar(ContactJid user, String nick) {
        Bitmap value = getBitmap(user.getJid());
        if (value != null) {
            return new BitmapDrawable(application.getResources(), value);
        } else {
            return generateDefaultAvatar(nick, nick);
        }
    }

    /**
     * Avatar was received.
     */
    public void onAvatarReceived(Jid jid, String hash, byte[] value, String type) {
        if (hash != null) {
            if (type.equals("vcard")) {
                setValue(hash, value, type);
                setHash(jid.asBareJid(), hash);
            } else {
                //XEP-0084-avi
                setValue(hash, value, type);
                setXEPHash(jid, hash);
            }
            AccountJid account = AccountManager.getInstance().getFirstAccount();
            if (account != null && account.getBareJid().toString().equals(jid.toString()))
                SettingsManager.setMainAvatarHash(hash);
        }
    }

    public void onGroupchatMemberAvatarReceived(String hash, byte[] value) {
        if (hash != null) {
            setValue(hash, value, "group");
        }
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza stanza) {
        if (!(stanza instanceof Presence)) {
            return;
        }

        AccountJid account = connection.getAccount();
        Presence presence = (Presence) stanza;
        if (presence.getType() == Presence.Type.error) {
            return;
        }
        for (ExtensionElement packetExtension : presence.getExtensions()) {
            if (packetExtension instanceof VCardUpdate) {
                VCardUpdate vCardUpdate = (VCardUpdate) packetExtension;
                if (vCardUpdate.isValid() && vCardUpdate.isPhotoReady()) {
                    try {
                        onPhotoReady(account, ContactJid.from(stanza.getFrom()), vCardUpdate);
                    } catch (ContactJid.ContactJidCreateException e) {
                        LogManager.exception(this, e);
                    }
                }
            }
        }
    }

    private void onPhotoReady(final AccountJid account, final ContactJid user, VCardUpdate vCardUpdate) {
        if (vCardUpdate.isEmpty()) {
            setHash(user.getJid().asBareJid(), EMPTY_HASH);
            return;
        }
        final String hash = vCardUpdate.getPhotoHash();
        if (bitmaps.containsKey(hash)) {
            setHash(user.getJid().asBareJid(), hash);
            return;
        }
        Application.getInstance().runInBackground(() -> loadBitmap(account, user.getJid(), hash));
    }

    /**
     * Read bitmap in background.
     */
    private void loadBitmap(final AccountJid account, final Jid jid, final String hash) {
        final byte[] value = AvatarStorage.getInstance().read(hash);
        final Bitmap bitmap = makeBitmap(value);
        Application.getInstance().runOnUiThread(() -> onBitmapLoaded(account, jid, hash, value, bitmap));
    }

    /**
     * Update data or request avatar on bitmap load.
     */
    private void onBitmapLoaded(AccountJid account, Jid jid,
                                String hash, byte[] value, Bitmap bitmap) {
        if (value == null) {
            if (SettingsManager.connectionLoadVCard()) {
                VCardManager.getInstance().request(account, jid);
            }
        } else {
            bitmaps.put(hash, bitmap == null ? EMPTY_BITMAP : bitmap);
            setHash(jid.asBareJid(), hash);
        }
    }

    /**
     * @return Scaled bitmap to be used for shortcut.
     */
    public Bitmap createShortcutBitmap(Bitmap bitmap) {
        int size = getLauncherLargeIconSize();
        int max = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (max == size) {
            return bitmap;
        }
        double scale = ((double) size) / max;
        int width = (int) (bitmap.getWidth() * scale);
        int height = (int) (bitmap.getHeight() * scale);
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private int getLauncherLargeIconSize() {
        return HoneycombShortcutHelper.getLauncherLargeIconSize();
    }

}
