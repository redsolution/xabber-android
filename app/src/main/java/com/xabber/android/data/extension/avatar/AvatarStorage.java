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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.OnClearListener;
import com.xabber.android.data.OnLoadListener;

/**
 * Manager for avatar files.
 * 
 * @author alexander.ivanov
 */
class AvatarStorage implements OnLoadListener, OnClearListener {

	private final File folder;

	private static AvatarStorage instance;

	static {
		instance = new AvatarStorage(Application.getInstance());
		Application.getInstance().addManager(instance);
	}

	public static AvatarStorage getInstance() {
		return instance;
	}

	private AvatarStorage(Application application) {
		folder = new File(application.getFilesDir(), "avatars");
	}

	@Override
	public void onLoad() {
		if (!folder.exists())
			folder.mkdirs();
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
		} catch (FileNotFoundException e) {
			value = null;
		} catch (IOException e) {
			value = null;
		}
		return value;
	}

	void write(String hash, byte[] value) {
		try {
			FileOutputStream outputStream = new FileOutputStream(getFile(hash));
			outputStream.write(value);
			outputStream.close();
		} catch (FileNotFoundException e) {
			LogManager.exception(this, e);
		} catch (IOException e) {
			LogManager.exception(this, e);
		}
	}

	@Override
	public void onClear() {
		for (File file : folder.listFiles())
			file.delete();
	}
}
