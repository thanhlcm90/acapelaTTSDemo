package com.thanhle.androidTTSDemo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.util.Log;

/**
 * File Utils class
 * 
 * @author thanh.lecaominh
 * 
 */
public class FileUtils {
	private static final int BUFFER_SIZE = 1024 * 32;

	/**
	 * Unzip a zip file. Will overwrite existing files.
	 * 
	 * @author thanh.lecaominh
	 * @param zipFile
	 *            Full path of the zip file you'd like to unzip.
	 * @param location
	 *            Full path of the directory you'd like to unzip to (will be
	 *            created if it doesn't exist).
	 * @param filter
	 *            file name filter, if not null, only extract file with
	 *            extension filter
	 * @throws IOException
	 */
	public static boolean unzip(String zipFile, String location,
			String... filter) throws IOException {
		return unzip(new FileInputStream(zipFile), location, filter);
	}

	public static boolean unzip(InputStream inputStream, String location,
			String... filter) throws IOException {
		int size;
		byte[] buffer = new byte[BUFFER_SIZE];

		try {
			if (!location.endsWith("/")) {
				location += "/";
			}
			File f = new File(location);
			if (!f.isDirectory()) {
				if (!f.mkdirs()) {
					return false;
				}
			}
			ZipInputStream zin = new ZipInputStream(new BufferedInputStream(
					inputStream, BUFFER_SIZE));
			try {
				ZipEntry ze = null;
				while ((ze = zin.getNextEntry()) != null) {
					String entryName = ze.getName();
					String path = location + entryName;
					File unzipFile = new File(path);
					String extension = entryName.substring(
							entryName.length() - 3).toLowerCase(Locale.US);
					boolean checkExtension = true;
					if (filter != null) {
						for (int i = 0; i < filter.length; i++) {
							checkExtension = checkExtension
									|| extension.equals(filter[i]);
							if (checkExtension)
								break;
						}
					}
					// skip macosx folder
					if (!entryName.startsWith("__MACOSX")) {
						if (ze.isDirectory()) {
							if (!unzipFile.isDirectory()) {
								unzipFile.mkdirs();
							}
						} else if (checkExtension) {
							// check for and create parent directories if they
							// don't exist
							File parentDir = unzipFile.getParentFile();
							if (null != parentDir) {
								if (!parentDir.isDirectory()) {
									parentDir.mkdirs();
								}
							}

							// unzip the file
							FileOutputStream out = new FileOutputStream(
									unzipFile, false);
							BufferedOutputStream fout = new BufferedOutputStream(
									out, BUFFER_SIZE);
							try {
								while ((size = zin.read(buffer, 0, BUFFER_SIZE)) != -1) {
									fout.write(buffer, 0, size);
								}

								zin.closeEntry();
							} finally {
								fout.flush();
								fout.close();
							}
						}
					}
				}
				zin.close();
				if (f.listFiles().length == 0) {
					f.delete();
				}
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				zin.close();
			}
			return false;
		} catch (Exception e) {
			Log.e("ZipUtils", "Unzip exception", e);
		}
		return false;
	}

	public static void copy(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[BUFFER_SIZE];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}
}
