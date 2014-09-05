package com.thanhle.androidTTSDemo;

import java.io.File;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.acapelagroup.android.tts.acattsandroid;
import com.acapelagroup.android.tts.acattsandroid.iTTSEventsCallback;

public class TTSDemoActivity extends Activity implements iTTSEventsCallback {
	Button loadButton;
	Button unloadButton;
	Button speakButton;
	Button speakToFileButton;
	Button stopButton;
	Button addTextButton;
	Button pauseButton;
	Button resumeButton;
	EditText textBox;
	TextView tv;
	Spinner spinner;

	String texttospeak;

	private static final String TAG = "AndroidTTSDemo";
	acattsandroid TTS = null;

	final static String WAV_FILE_PATH = "/sdcard/tts.wav";

	// Called when the activity is first created.
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the application to only modify stream_music volume even when no
		// speaking (otherwise it modified ring volume)
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);

		setContentView(R.layout.main);
		textBox = (EditText) findViewById(R.id.textBox);
		loadButton = (Button) findViewById(R.id.loadButton);
		loadButton.setText("Load");
		loadButton.setOnClickListener(new clicker());
		unloadButton = (Button) findViewById(R.id.unloadButton);
		unloadButton.setText("unLoad");
		unloadButton.setOnClickListener(new clicker());
		speakButton = (Button) findViewById(R.id.speakButton);
		speakButton.setText("Speak");
		speakButton.setOnClickListener(new clicker());
		stopButton = (Button) findViewById(R.id.stopButton);
		stopButton.setText("Stop");
		stopButton.setOnClickListener(new clicker());
		addTextButton = (Button) findViewById(R.id.addTextButton);
		addTextButton.setText("Add Text");
		addTextButton.setOnClickListener(new clicker());
		speakToFileButton = (Button) findViewById(R.id.speakToFileButton);
		speakToFileButton.setText("Wav File");
		speakToFileButton.setOnClickListener(new clicker());
		pauseButton = (Button) findViewById(R.id.pauseButton);
		pauseButton.setText("Pause");
		pauseButton.setOnClickListener(new clicker());
		resumeButton = (Button) findViewById(R.id.resumeButton);
		resumeButton.setText("Resume");
		resumeButton.setOnClickListener(new clicker());
		tv = (TextView) findViewById(R.id.lbl1);
		tv.setText("Acapela TTS For Android");

		TTS = new acattsandroid(this);
		TTS.setLog(true);

		// A license is required and is linked to a voice pack.
		TTS.setLicense(
				0x444e4153,
				0x00306483,
				"\"6095 0 SAND #EVALUATION#Acapela Group Android SDK\"\nWCu3N!wM2nTDH9oID$B6cwmsz7GoYWHlcCg5rYDOXb55O!scsulDB8@gRJ7UyFeSszm#\nYmhJ7Gf8mL6Z@$iQJNU!QX3CuRWsziIXV2W%$D%zu6ESLiSD\nWCuZw6xz!PLAmumZDO8xmT##\n");

		String version = TTS.getVersion();
		Log.i(TAG, "Version : " + version);

		// Get Voices list
		final File f = new File(getCacheDir(), MyApplication.VOICE_FOLDER_NAME);
		String[] voiceDirPaths = { f.getAbsolutePath() };
		String[] voicesList = TTS.getVoicesList(voiceDirPaths);

		spinner = (Spinner) findViewById(R.id.spinner);
		ArrayAdapter<?> adapter = new ArrayAdapter<Object>(this,
				android.R.layout.simple_spinner_item, voicesList);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

		for (String s : voicesList) {
			Log.i(TAG, s);
		}

	}

	class clicker implements Button.OnClickListener {
		public void onClick(View v) {

			Spannable WordtoSpan;

			switch (v.getId()) {
			case R.id.loadButton:
				String voice = (String) spinner.getSelectedItem();
				TTS.load(voice);
				TTS.getLanguage();
				break;
			case R.id.unloadButton:
				TTS.shutdown();
				break;
			case R.id.speakButton:
				WordtoSpan = (Spannable) textBox.getText();
				WordtoSpan.setSpan(new BackgroundColorSpan(0x00000000), 0,
						(int) (textBox.getText().length()),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

				texttospeak = textBox.getText().toString();
				TTS.speak(texttospeak);
				break;
			case R.id.addTextButton:
				String textToAdd = textBox.getText().toString();
				texttospeak += textBox.getText().toString();
				TTS.queueText(textToAdd);
				break;
			case R.id.speakToFileButton:
				String textToSynthetize = textBox.getText().toString();
				TTS.synthesizeToFile(textToSynthetize, WAV_FILE_PATH);
				break;
			case R.id.stopButton:
				WordtoSpan = (Spannable) textBox.getText();
				WordtoSpan.setSpan(new BackgroundColorSpan(0x00000000), 0,
						(int) (textBox.getText().length()),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

				TTS.stop();
				break;
			case R.id.pauseButton:
				TTS.pause();
				break;
			case R.id.resumeButton:
				TTS.resume();
				break;
			}
		}
	}

	@Override
	protected void onDestroy() {
		if (TTS != null)
			TTS.shutdown();
		super.onDestroy();
	}

	public class MyOnItemSelectedListener implements OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			Spannable WordtoSpan = (Spannable) textBox.getText();
			WordtoSpan.setSpan(new BackgroundColorSpan(0x00000000), 0,
					(int) (textBox.getText().length()),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			String voice = (String) spinner.getSelectedItem();
			TTS.load(voice);
		}

		public void onNothingSelected(AdapterView<?> parent) {
		}
	}

	// Phone events
	private PhoneStateListener mPhoneListener = new PhoneStateListener() {
		public void onCallStateChanged(int state, String incomingNumber) {
			switch (state) {
			case TelephonyManager.CALL_STATE_RINGING:
				Log.i(TAG, "Phone ringing");
				TTS.pause();
				break;
			case TelephonyManager.DATA_DISCONNECTED:
				Log.i(TAG, "Phone offhook");
				TTS.resume();
				break;
			default:
				// Log.i(TAG, "Unknown phone state=" + state);
			}
		}
	};

	// TTS Events
	public void ttsevents(long type, long param1, long param2, long param3,
			long param4) {
		if (type == acattsandroid.EVENT_TEXT_START)
			Log.i(TAG, "Text " + param1 + " started");
		else if (type == acattsandroid.EVENT_TEXT_END)
			Log.i(TAG, "Text " + param1 + " processed");
		else if (type == acattsandroid.EVENT_AUDIO_END) {
			Log.i(TAG, "Audio processed");
		} else if (type == acattsandroid.EVENT_WORD_POS) {
			// Check word position is not out of range
			if (param1 >= texttospeak.length() && param2 == 0) {
				// in evalation (free), if param2 = 0 and param1 > text length,
				// stop speak
				Log.i(TAG, "evaltation speak");
				TTS.stop();
			} else if ((int) (param1 + param2) <= texttospeak.length()) {
				// Highlight word
				Spannable WordtoSpan = (Spannable) textBox.getText();
				WordtoSpan.setSpan(new BackgroundColorSpan(0x00000000), 0,
						(int) (texttospeak.length()),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				WordtoSpan.setSpan(new BackgroundColorSpan(0x80000000),
						(int) param1, (int) (param1 + param2),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
	}

}