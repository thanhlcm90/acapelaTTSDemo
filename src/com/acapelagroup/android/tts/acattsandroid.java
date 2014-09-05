//  
//  acattsandroid.java
//
//  This file is not intended to be modified by any other than Acapela Group
//	Any modification will break any support / compatibility obligation for Acapela Group
//
//  Copyright Acapela Group All rights reserved.
//


package com.acapelagroup.android.tts;

import android.util.Log;

import android.media.AudioTrack;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import android.os.Handler;
import android.util.Log;

public class acattsandroid implements OnPlaybackPositionUpdateListener {

	public static final int EVENT_TEXT_START = 0;
	public static final int EVENT_TEXT_END = 1;
	public static final int EVENT_WORD_POS = 2;
	public static final int EVENT_AUDIO_END = 3;

	private static final String TAG = "acattsandroid-java";

	// Word position event structure
	public class wordposevent {
		public wordposevent(long pos, long len, long sampval) {
			this.pos = pos;
			this.len = len;
			this.sampval = sampval;
		}

		public long pos;
		public long len;
		public long sampval;
	}

	// TTS events callback interface
	public interface iTTSEventsCallback {
		public void ttsevents(long type, long param1, long param2, long param3, long param4);
	};

	private iTTSEventsCallback pttseventcallback;

	// Used on enumeration to store Speaker list and corresponding ini file
	static ArrayList<String> iniVoicesArray;
	static ArrayList<String> speakersArray;

	// AudioTrack
	AudioTrack audioTrack;
	int audioTrackSize;
	int audioBufferSize;

	// Word positions events list
	List<wordposevent> wordposeventslist = new ArrayList<wordposevent>();

	// Word position indexes and flags
	int currenteventindex = 0;
	int lasteventindex = 0;
	long lasteventsampval = 0;
	long samploop = 0;
	private long endaudiosampval;
	private long endaudiosampval2;

	// Stop flag to stop word positions events
	int stopevents = 0;

	// Voice count
	int voiceFoundCount = 0;

	// Log flag
	boolean bLog = false;

	// Required to load native TTS library on startup
	static {
		System.loadLibrary("acattsandroid");
	}

	public acattsandroid(iTTSEventsCallback ttseventcallback) {
		nInitCallbacks();
		pttseventcallback = ttseventcallback;
	}

	synchronized public void addaudiomarker() {

		while (true) {
			do {
				try {
					wait();
				} catch (InterruptedException e) {
					log(Arrays.toString(e.getStackTrace()));
				}
			} while (currenteventindex >= wordposeventslist.size());

			// The last event is the totalsample played so no need to shift
			if (endaudiosampval > 0) {
				audioTrack.setNotificationMarkerPosition((int) wordposeventslist.get(currenteventindex).sampval);
				endaudiosampval2 = endaudiosampval;
				endaudiosampval = 0;
			} else {
				// Word pos events need to be shifted based on their current buffer
				int samplepos = (int) wordposeventslist.get(currenteventindex).sampval;
				if (samplepos < lasteventsampval) {
					samploop++;
				}
				lasteventsampval = wordposeventslist.get(currenteventindex).sampval;
				samplepos = (int) ((audioBufferSize / 2 * samploop) + samplepos); // (16bits -> /2)

				audioTrack.setNotificationMarkerPosition((int) samplepos);
			}
		}
	}

	// Word positions thread
	Thread wordposeventsthread = new Thread() {
		@Override
		public void run() {
			addaudiomarker();
		}
	};

	@Override
	public synchronized void onMarkerReached(AudioTrack track) {

		// Last audio event received
		if (track.getPlaybackHeadPosition() >= endaudiosampval2 && endaudiosampval2 > 0) {
			pttseventcallback.ttsevents(EVENT_AUDIO_END, endaudiosampval2, 0, 0, 0);
			endaudiosampval2 = 0;
		}
		// Audio marker received
		else if (wordposeventslist.size() > currenteventindex && stopevents == 0) {
			// Push the event to the application
			// pttseventcallback.ttsevents(EVENT_WORD_POS, wordposeventslist.get(currenteventindex).pos,
			// wordposeventslist.get(currenteventindex).len, wordposeventslist.get(currenteventindex).sampval, 0);

		}

		currenteventindex++;
		lasteventindex--;

		// Wake up word position event thread
		notify();

	}

	@Override
	public void onPeriodicNotification(AudioTrack track) {

	}

	// Callback that receives audio sample from TTS
	private void samplesCallback(short[] buff, long samples) {
		// Write the samples to the opened track
		if (audioTrack != null)
			audioTrack.write(buff, 0, (short) samples);

	}

	Handler handle = new Handler();

	// Callback that receives events from TTS
	private synchronized long eventsCallback(long type, final long param1, final long param2, final long param3,
			long param4) {

		// Push events only if requested by the application
		if (pttseventcallback == null)
			return 0;

		// Push back events to application
		if (type == EVENT_TEXT_START) {
			pttseventcallback.ttsevents(type, param1, param2, param3, param4);
		}

		if (type == EVENT_TEXT_END) {
			pttseventcallback.ttsevents(type, param1, param2, param3, param4);
			if (stopevents == 1) {
				pttseventcallback.ttsevents(EVENT_AUDIO_END, param2, 0, 0, 0);
			} else {
				wordposeventslist.add(new wordposevent(0, 0, param2));
				endaudiosampval = param2;
				if (lasteventindex == 0) {
					notify();
				}
				lasteventindex++;
			}

		}

		if (type == EVENT_WORD_POS) {

			// If offset is 0 move it a little bit otherwise audio is not able to reach marker with 0
			// if (param3 == 0)
			// param3 = 40;

			// New word position event. Add it in the list
			// if (stopevents == 1) {
			handle.post(new Runnable() {
				public void run() {
					pttseventcallback.ttsevents(EVENT_WORD_POS, param1, param2, param3, 0);
				}
			});

			// }
			wordposeventslist.add(new wordposevent(param1, param2, param3));

			// We already consumed all the events so wake up the event thread for this new one
			if (lasteventindex == 0) {
				notify();
			}
			lasteventindex++;
		}

		return 0;
	}

	// Initialize audio with voice sample rate
	private int initAudio(int sample_rate) {
		if (sample_rate != 0) {

			audioTrackSize = android.media.AudioTrack.getMinBufferSize(sample_rate,
					AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
			audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sample_rate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT, audioTrackSize, AudioTrack.MODE_STREAM);
			if (audioTrack == null)
				return -1;

			audioBufferSize = nGetAudioBufferSize();
			audioTrack.setPlaybackPositionUpdateListener(this);
		}

		return 0;
	}

	// Release voice and tts
	public int shutdown() {

		stopevents = 1;

		log("shutdown");
		nStop();

		if (audioTrack != null) {
			audioTrack.stop();
			audioTrack.flush();
		}

		nShutdown();

		if (audioTrack != null) {
			audioTrack.release();
			audioTrack = null;
		}

		return 0;
	}

	// Load voice
	public int load(String voice) {

		// Enumeration not done or no voice found
		if (voiceFoundCount <= 0)
			return -2;

		// Shutdown previous voice and audio
		shutdown();

		// Retrieve string list for both speaker and ini array
		String voicesList[] = (String[]) speakersArray.toArray(new String[speakersArray.size()]);
		String iniVoicesList[] = (String[]) iniVoicesArray.toArray(new String[iniVoicesArray.size()]);

		int index = 0;

		// Find the corresponding .ini file for the voice to be loaded
		for (String s : voicesList) {
			// Voice found
			if (s.equals(voice)) {
				break;
			}
			index++;
		}

		int sample_rate = nLoadVoice(iniVoicesList[index]);

		if (sample_rate == 8000 || sample_rate == 11025 || sample_rate == 16000 || sample_rate == 22050) {
			initAudio(sample_rate);
		} else
			return sample_rate;

		if (wordposeventsthread.isAlive() == false)
			wordposeventsthread.start();

		return 0;
	}

	// Speak Text - Stop the current TTS speaking - Stop the current TTS speaking
	public int speak(String text) {
		log("speak");
		stop();

		resetindexes();

		if (audioTrack != null) {
			audioTrack.play();
		}

		final String textToPlay = text;
		new Thread(new Runnable() {
			public void run() {
				nSpeak(textToPlay);
			}
		}).start();
		return 0;
	}

	// Synthesize Text To File - Stop the current TTS speaking
	public int synthesizeToFile(String text, String fileName) {
		log("synthesizeToFile");
		stop();
		final String textToPlay = text;
		final String outputFileName = fileName;
		new Thread(new Runnable() {
			public void run() {
				nSpeakToFile(textToPlay, outputFileName);
			}
		}).start();
		return 0;
	}

	// Add Text to TTS queue - Start TTS if no text is speaking
	public int queueText(String text) {
		log("queueText");

		resetindexes();

		if (audioTrack != null)
			audioTrack.play();

		int textIndex = nGetTextIndex();
		final String textToPlay = text;
		new Thread(new Runnable() {
			public void run() {
				nQueueText(textToPlay);
			}
		}).start();
		return textIndex;
	}

	// Stop speaking
	public int stop() {
		log("stop");
		stopevents = 1;
		nStop();

		if (audioTrack != null) {
			log("stopping audio " + audioTrack);
			audioTrack.stop();
			audioTrack.flush();
		}

		return 0;
	}

	// Return the TTS speaking status
	public boolean isSpeaking() {
		int status = nIsSpeaking();
		if (status == 1)
			return true;
		else
			return false;
	}

	// Pause speaking
	public int pause() {

		log("pause");
		nPause();
		if (audioTrack != null) {
			audioTrack.pause();
		}
		return 0;
	}

	// Resume speaking
	public int resume() {

		log("resume");
		if (audioTrack != null) {
			audioTrack.play();
		}
		nResume();
		return 0;
	}

	// Search for Voice
	public void searchVoices(File dir) {

		if (dir.isDirectory()) {
			// Look in sub-directories recursively
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				searchVoices(new File(dir, children[i]));
			}
		} else {
			// File found - Check if it is an ini voice file
			if (dir.getAbsolutePath().contains(".ini")) {
				voiceFoundCount++;
				// Store ini voice file needed to load it later
				iniVoicesArray.add(dir.getAbsolutePath());

				// Get Speaker name based on ini file
				String voiceName = nGetVoiceName(dir.getAbsolutePath());
				speakersArray.add(voiceName);

				log("voice found " + voiceName);
			}
		}
	}

	// Get Voices List
	public String[] getVoicesList(String[] voiceDirPaths) {

		voiceFoundCount = 0;
		speakersArray = new ArrayList<String>();
		iniVoicesArray = new ArrayList<String>();

		for (String path : voiceDirPaths) {
			File dir = new File(path);
			searchVoices(dir);
		}

		if (voiceFoundCount == 0) {
			String voicesList[] = { "" };
			return voicesList;
		} else {
			String voicesList[] = (String[]) speakersArray.toArray(new String[speakersArray.size()]);
			return voicesList;
		}
	}

	// Set License information
	public int setLicense(long userId, long passwd, String license) {

		return nSetLicense(userId, passwd, license);
	}

	// Set Speech Rate
	public int setSpeechRate(float speechRate) {

		return nSetSpeechRate(speechRate);
	}

	// Set Pitch
	public int setPitch(float pitch) {

		return nSetShape(pitch);
	}

	// Get Language
	public String getLanguage() {

		return nGetLanguage();
	}

	// Get Last Error
	public int getLastError() {

		return nGetLastError();
	}

	// Get Version
	public String getVersion() {

		return nGetVersion();
	}

	// Activate the log in the naitve part
	public void setLog(boolean log) {

		bLog = log;
		nSetLog(log);

	}

	// Natives functions from TTS library
	private native int nInitCallbacks();

	private native int nLoadVoice(String voicePath);

	private native int nSpeak(String text);

	private native int nSpeakToFile(String text, String fileName);

	private native int nQueueText(String text);

	private native int nStop();

	private native int nPause();

	private native int nResume();

	private native int nShutdown();

	private native int nIsSpeaking();

	private native int nIsPaused();

	private native int nSetShape(float shapeValue);

	private native int nSetSpeechRate(float speechRate);

	private native int nSetLicense(long userId, long passwd, String license);

	private native String nGetLanguage();

	private native String nGetVoiceName(String iniVoiceFile);

	private native int nGetTextIndex();

	private native int nGetLastError();

	private native String nGetVersion();

	private native int nGetAudioBufferSize();

	private native void nSetLog(boolean log);

	private native int nSetTTSSetting(String settingname, float settingvalue);

	// Reset flags and index
	private void resetindexes() {

		lasteventindex = 0;
		currenteventindex = 0;
		samploop = 0;
		stopevents = 0;
		wordposeventslist.clear();
		lasteventsampval = 0;
		endaudiosampval = 0;
		endaudiosampval2 = 0;
	}

	private void log(String logmessage) {

		if (bLog == true)
			Log.i(TAG, logmessage);

	}

}
