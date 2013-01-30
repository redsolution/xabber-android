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
package com.xabber.android.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.debugger.ConsoleDebugger;
import org.jivesoftware.smack.util.ReaderListener;
import org.jivesoftware.smack.util.WriterListener;

import android.os.Environment;

/**
 * Writer for connection log to the file.
 * 
 * @author alexander.ivanov
 * 
 */
public class FileLogDebugger extends ConsoleDebugger {
	private FileWriter writer;
	private boolean readerClosed;
	private boolean writerClosed;

	private final static SimpleDateFormat FILE_NAME_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd-HH-mm-ss");

	public FileLogDebugger(Connection connection, Writer writer, Reader reader) {
		super(connection, writer, reader);
		File dir = new File(Environment.getExternalStorageDirectory(),
				"xabber-log");
		dir.mkdirs();
		File file = new File(dir, FILE_NAME_FORMAT.format(new Date()) + " - "
				+ connection.hashCode() + ".xml");
		try {
			this.writer = new FileWriter(file);
			this.writer.write("<xml>");
			this.writer.write("\n");
			this.writer.flush();
		} catch (IOException e) {
			LogManager.forceException(this, e);
		}
	}

	@Override
	protected ReaderListener createReaderListener() {
		final ReaderListener inherited = super.createReaderListener();
		return new ReaderListener() {
			@Override
			public void read(String str) {
				inherited.read(str);
				synchronized (this) {
					if (writerClosed && readerClosed)
						return;
					try {
						writer.write("\n");
						writer.write(dateFormatter.format(new Date()));
						writer.write(" RCV ");
						writer.write(str);
						writer.flush();
					} catch (IOException e) {
						LogManager.forceException(this, e);
					}
				}
			}

			@Override
			public void close() {
				inherited.close();
				System.out.println(dateFormatter.format(new Date())
						+ " RCV CLOSED (" + connection.hashCode() + ")");
				synchronized (this) {
					if (readerClosed)
						return;
					try {
						writer.write("\n");
						writer.write(dateFormatter.format(new Date()));
						writer.write(" RCV - CLOSED ");
						writer.flush();
					} catch (IOException e) {
						LogManager.exception(this, e);
					}
					readerClosed = true;
					onClose();
				}
			}
		};
	}

	@Override
	protected WriterListener createWriterListener() {
		final WriterListener inherited = super.createWriterListener();
		return new WriterListener() {
			@Override
			public void write(String str) {
				inherited.write(str);
				synchronized (this) {
					if (writerClosed && readerClosed)
						return;
					try {
						writer.write("\n");
						writer.write(dateFormatter.format(new Date()));
						writer.write(" SNT ");
						writer.write(str);
						writer.flush();
					} catch (IOException e) {
						LogManager.forceException(this, e);
					}
				}
			}

			@Override
			public void close() {
				inherited.close();
				System.out.println(dateFormatter.format(new Date())
						+ " SENT CLOSED (" + connection.hashCode() + ")");
				synchronized (this) {
					if (writerClosed)
						return;
					try {
						writer.write("\n");
						writer.write(dateFormatter.format(new Date()));
						writer.write(" SNT - CLOSED");
						writer.flush();
					} catch (IOException e) {
						LogManager.exception(this, e);
					}
					writerClosed = true;
					onClose();
				}
			}
		};
	}

	private void onClose() {
		if (writerClosed && readerClosed) {
			try {
				writer.write("\n");
				writer.write("</xml>");
				writer.close();
			} catch (IOException e) {
				LogManager.exception(this, e);
			}
		}
	}
}
