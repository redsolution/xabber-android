/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.extension.avatar;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.OnLowMemoryListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OAuthManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.avatar.VCardUpdate;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;

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

    /**
     * Maximum image width / height to be loaded.
     */
    private static final int MAX_SIZE = 256;

    private static final String EMPTY_HASH = "";
    private static final Bitmap EMPTY_BITMAP = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
    private final static AvatarManager instance;

    static {
        instance = new AvatarManager();
        Application.getInstance().addManager(instance);
    }

    private final Application application;
    /**
     * Map with hashes for specified users.
     * <p/>
     * {@link #EMPTY_HASH} is used to store <code>null</code> values.
     */
    private final Map<String, String> hashes;
    /**
     * Map with bitmaps for specified hashes.
     * <p/>
     * {@link #EMPTY_BITMAP} is used to store <code>null</code> values.
     */
    private final Map<String, Bitmap> bitmaps;
    /**
     * Map with drawable used in contact list only for specified uses.
     */
    private final Map<String, Drawable> contactListDrawables;
    /**
     * Users' default avatar set.
     */
    private final BaseAvatarSet userAvatarSet;
    /**
     * Rooms' default avatar set.
     */
    private final BaseAvatarSet roomAvatarSet;
    private final int[] accountColors;

    private AvatarManager() {
        this.application = Application.getInstance();
        userAvatarSet = new BaseAvatarSet(application, R.array.default_avatars_icons, R.array.default_avatars_colors);
        roomAvatarSet = new BaseAvatarSet(application, R.array.muc_avatars, R.array.default_avatars_colors);

        accountColors = application.getResources().getIntArray(R.array.account_action_bar);

        hashes = new HashMap<>();
        bitmaps = new HashMap<>();
        contactListDrawables = new HashMap<>();
    }

    public static AvatarManager getInstance() {
        return instance;
    }

    /**
     * Make {@link Bitmap} from array of bytes.
     *
     * @param value
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

    @Override
    public void onLoad() {
        final Map<String, String> hashes = new HashMap<>();
        final Map<String, Bitmap> bitmaps = new HashMap<>();
        Cursor cursor = AvatarTable.getInstance().list();
        try {
            if (cursor.moveToFirst()) {
                do {
                    String hash = AvatarTable.getHash(cursor);
                    hashes.put(AvatarTable.getUser(cursor), hash == null ? EMPTY_HASH : hash);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        for (String hash : new HashSet<>(hashes.values()))
            if (!hash.equals(EMPTY_HASH)) {
                Bitmap bitmap = makeBitmap(AvatarStorage.getInstance().read(hash));
                bitmaps.put(hash, bitmap == null ? EMPTY_BITMAP : bitmap);
            }
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onLoaded(hashes, bitmaps);
            }
        });
    }

    private void onLoaded(Map<String, String> hashes, Map<String, Bitmap> bitmaps) {
        this.hashes.putAll(hashes);
        this.bitmaps.putAll(bitmaps);
    }

    /**
     * Sets avatar's hash for user.
     *
     * @param bareAddress
     * @param hash        can be <code>null</code>.
     */
    private void setHash(final String bareAddress, final String hash) {
        hashes.put(bareAddress, hash == null ? EMPTY_HASH : hash);
        contactListDrawables.remove(bareAddress);
        application.runInBackground(new Runnable() {
            @Override
            public void run() {
                AvatarTable.getInstance().write(bareAddress, hash);
            }
        });
    }

    /**
     * Get avatar's value for user.
     *
     * @param bareAddress
     * @return avatar's value. <code>null</code> can be returned if user has no
     * avatar or avatar doesn't exists.
     */
    private Bitmap getBitmap(String bareAddress) {
        String hash = hashes.get(bareAddress);
        if (hash == null || hash.equals(EMPTY_HASH)) {
            return null;
        }
        Bitmap bitmap = bitmaps.get(hash);
        if (bitmap == EMPTY_BITMAP) {
            return null;
        } else {
            return bitmap;
        }
    }

    /**
     * Sets avatar's value.
     *
     * @param hash
     * @param value
     */
    private void setValue(final String hash, final byte[] value) {
        if (hash == null) {
            return;
        }
        Bitmap bitmap = makeBitmap(value);
        bitmaps.put(hash, bitmap == null ? EMPTY_BITMAP : bitmap);
        application.runInBackground(new Runnable() {
            @Override
            public void run() {
                AvatarStorage.getInstance().write(hash, value);
            }
        });
    }

    @Override
    public void onLowMemory() {
        contactListDrawables.clear();
        userAvatarSet.onLowMemory();
        roomAvatarSet.onLowMemory();
    }

    /**
     * Gets account's avatar.
     *
     * @param account
     * @return Avatar or default avatar if:
     * <ul>
     * <li>account has no avatar.</li>
     * </ul>
     */
    public Drawable getAccountAvatar(String account) {
        String jid = OAuthManager.getInstance().getAssignedJid(account);
        if (jid == null) {
            jid = account;
        }
        Bitmap value = getBitmap(Jid.getBareAddress(jid));
        if (value != null) {
            return new BitmapDrawable(application.getResources(), value);
        } else {
            Drawable[] layers = new Drawable[2];
            layers[0] = new ColorDrawable(accountColors[AccountManager.getInstance().getColorLevel(account)]);
            layers[1] = application.getResources().getDrawable(R.drawable.ic_avatar_1);
            return new LayerDrawable(layers);
        }
    }

    /**
     * Gets avatar for regular user.
     *
     * @param user
     * @return
     */
    public Drawable getUserAvatar(String user) {
        Bitmap value = getBitmap(user);
        if (value != null) {
            return new BitmapDrawable(application.getResources(), value);
        } else {
            return getDefaultAvatarDrawable(userAvatarSet.getResourceId(user));
        }
    }

    private Drawable getDefaultAvatarDrawable(BaseAvatarSet.DefaultAvatar defaultAvatar) {
        Drawable[] layers = new Drawable[2];
        layers[0] = new ColorDrawable(defaultAvatar.getBackgroundColor());
        layers[1] = application.getResources().getDrawable(defaultAvatar.getIconResource());


        return new LayerDrawable(layers);
    }

    /**
     * Gets bitmap with avatar for regular user.
     *
     * @param user
     * @return
     */
    public Bitmap getUserBitmap(String user) {
        Bitmap value = getBitmap(user);
        if (value != null) {
            return value;
        } else {
            return drawableToBitmap(getDefaultAvatarDrawable(userAvatarSet.getResourceId(user)));
        }
    }

    /**
     * Gets and caches drawable with avatar for regular user.
     *
     * @param user
     * @return
     */
    public Drawable getUserAvatarForContactList(String user) {
        Drawable drawable = contactListDrawables.get(user);
        if (drawable == null) {
            drawable = getUserAvatar(user);
            contactListDrawables.put(user, drawable);
        }
        return drawable;
    }

    /**
     * Gets avatar for the room.
     *
     * @param user
     * @return
     */
    public Drawable getRoomAvatar(String user) {
        return getDefaultAvatarDrawable(roomAvatarSet.getResourceId(user));
    }

    /**
     * Gets bitmap for the room.
     *
     * @param user
     * @return
     */
    public Bitmap getRoomBitmap(String user) {
        return drawableToBitmap(getRoomAvatar(user));
    }

    /**
     * Gets and caches drawable with room's avatar.
     *
     * @param user
     * @return
     */
    public Drawable getRoomAvatarForContactList(String user) {
        Drawable drawable = contactListDrawables.get(user);
        if (drawable == null) {
            drawable = getRoomAvatar(user);
            contactListDrawables.put(user, drawable);
        }
        return drawable;
    }

    /**
     * Gets avatar for occupant in the room.
     *
     * @param user
     * @return
     */
    public Drawable getOccupantAvatar(String user) {
        return getDefaultAvatarDrawable(userAvatarSet.getResourceId(user));
    }

    /**
     * Avatar was received.
     *
     * @param bareAddress
     * @param hash
     * @param value
     */
    public void onAvatarReceived(String bareAddress, String hash, byte[] value) {
        setValue(hash, value);
        setHash(bareAddress, hash);
    }

    @Override
    public void onPacket(ConnectionItem connection, String bareAddress,
                         Packet packet) {
        if (!(packet instanceof Presence) || bareAddress == null) {
            return;
        }
        if (!(connection instanceof AccountItem)) {
            return;
        }
        String account = ((AccountItem) connection).getAccount();
        Presence presence = (Presence) packet;
        if (presence.getType() == Presence.Type.error) {
            return;
        }
        for (PacketExtension packetExtension : presence.getExtensions()) {
            if (packetExtension instanceof VCardUpdate) {
                VCardUpdate vCardUpdate = (VCardUpdate) packetExtension;
                if (vCardUpdate.isValid() && vCardUpdate.isPhotoReady()) {
                    onPhotoReady(account, bareAddress, vCardUpdate);
                }
            }
        }
    }

    private void onPhotoReady(final String account, final String bareAddress, VCardUpdate vCardUpdate) {
        if (vCardUpdate.isEmpty()) {
            setHash(bareAddress, null);
            return;
        }
        final String hash = vCardUpdate.getPhotoHash();
        if (bitmaps.containsKey(hash)) {
            setHash(bareAddress, hash);
            return;
        }
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                loadBitmap(account, bareAddress, hash);
            }
        });
    }

    /**
     * Read bitmap in background.
     *
     * @param account
     * @param bareAddress
     * @param hash
     */
    private void loadBitmap(final String account, final String bareAddress, final String hash) {
        final byte[] value = AvatarStorage.getInstance().read(hash);
        final Bitmap bitmap = makeBitmap(value);
        Application.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onBitmapLoaded(account, bareAddress, hash, value, bitmap);
            }
        });
    }

    /**
     * Update data or request avatar on bitmap load.
     *
     * @param account
     * @param bareAddress
     * @param hash
     * @param value
     * @param bitmap
     */
    private void onBitmapLoaded(String account, String bareAddress,
                                String hash, byte[] value, Bitmap bitmap) {
        if (value == null) {
            if (SettingsManager.connectionLoadVCard()) {
                VCardManager.getInstance().request(account, bareAddress, hash);
            }
        } else {
            bitmaps.put(hash, bitmap == null ? EMPTY_BITMAP : bitmap);
            setHash(bareAddress, hash);
        }
    }

    /**
     * @param bitmap
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
