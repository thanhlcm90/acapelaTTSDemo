package com.thanhle.androidTTSDemo;

import java.io.File;
import java.io.IOException;

import android.app.Application;

public class MyApplication extends Application {
	public static final String VOICE_FOLDER_NAME = "voices";

	@Override
	public void onCreate() {
		super.onCreate();

		// check and extract voice file
		final File f = new File(getCacheDir(), VOICE_FOLDER_NAME);
		// check and make dir
		if (!f.exists()) {
			f.mkdirs();
		}
		// check dir is empty, extra voice zip file
		if (f.list().length == 0) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						FileUtils.unzip(getAssets().open("voice.zip"),
								f.getAbsolutePath());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
}
