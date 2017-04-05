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

import com.xabber.android.data.Application;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.OnClearListener;
import com.xabber.android.data.OnLoadListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Manager for avatar files.
 *
 * @author alexander.ivanov
 */
public class AvatarStorage implements OnLoadListener, OnClearListener {

    private final File folder;

    private static AvatarStorage instance;

    public static AvatarStorage getInstance() {
        if (instance == null) {
            instance = new AvatarStorage();
        }

        return instance;
    }

    private AvatarStorage() {
        folder = new File(Application.getInstance().getFilesDir(), "avatars");
    }

    @Override
    public void onLoad() {
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    private File getFile(String hash) {
        return new File(folder, hash);
    }

    byte[] read(String hash) {
        byte[] value;
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(getFile(hash));
            value = new byte[inputStream.available()];
            inputStream.read(value);
            inputStream.close();
        } catch (IOException e) {
            LogManager.exception(this, e);
            value = null;
        }
        return value;
    }

    void write(String hash, byte[] value) {
        try {
            FileOutputStream outputStream = new FileOutputStream(getFile(hash));
            outputStream.write(value);
            outputStream.close();
        } catch (IOException e) {
            LogManager.exception(this, e);
        }
    }

    @Override
    public void onClear() {
        for (File file : folder.listFiles()) {
            file.delete();
        }
    }
}
