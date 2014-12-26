package com.afqa123.shareplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.widget.Toast;

import com.afqa123.log.Logger;
import com.afqa123.log.LoggerFactory;
import com.afqa123.shareplay.common.Constants;
import com.afqa123.shareplay.common.StreamProxy;
import com.afqa123.shareplay.data.Item;
import com.afqa123.shareplay.interfaces.MediaPlayerInterface;

public class MediaPlayerService extends Service {

	private static final Logger logger = LoggerFactory.getLogger(MediaPlayerService.class);
	private static final int NOTIFICATION_ID = 42;
	private static final String INCOMING_CALL_ACTION = "android.intent.action.PHONE_STATE";
	
	public final static String STATUS_UPDATE = "com.afqa123.shareplay.MediaPlayerService.STATUS_UPDATE";
	public final static String MSG_PLAY = "play";
	public final static String MSG_PAUSE = "pause";
	public final static String PARAM_MSG = "msg";
	public final static String PARAM_SONG = "song";	
	public final static String PARAM_ALBUM = "album";
	public final static String PARAM_ARTIST = "artist";
	
	private class PlayerController extends MediaPlayerInterface.Stub implements OnCompletionListener, OnPreparedListener, OnErrorListener, OnInfoListener {

		private final static int SKIP_POSITION = 3000;
		private final static int ERROR_RETRY = 5;
		// This is under the assumption that most playlists will not be larger than 1000 songs - in
		// which case this should provide us with a fairly decent spread when shuffling songs
		// Note: it might be interesting to make the shuffle increment dependent on the size of the
		// playlist, however I do not have the time to research efficient algorithms for this right now.
		private final static int SHUFFLE_INCREMENT = 547;
		
		private List<Item> _playlist;	
		private Item _item;	
		private int _pos;
		private long _serverId;
		private String _mediaURL;
		private boolean _prepared = false;
		private int _retryCount = 0;
		private boolean _shuffleMode;
		private int _shuffleIterator;
		private boolean _repeatMode;
		private Random _r;
				
		public PlayerController() {
			_playlist = new ArrayList<Item>();
			_r = new Random();
		}
			
		@Override
		public boolean rewind() {
			boolean result = false;
			int count = _playlist.size();
			
			// skip to beginning of current track
			if (player != null && player.isPlaying() && 
				(player.getCurrentPosition() > SKIP_POSITION || _pos == 0)) {
				player.seekTo(0);
				play();
				return true;
			}

			// go to previous track in shuffled order
			if (_shuffleMode) {
				_pos = _pos - SHUFFLE_INCREMENT;
				while (_pos < 0) {
					_pos = count + _pos;
				}
				
				_shuffleIterator--;
				if (_shuffleIterator >= 0) {
					prepare();
					result = true;
				}
			// go to previous track
			} else if (_pos > 0) {
				_pos--;
				prepare();			
				result = true;
			}

			return result;
		}
		
		@Override
		public boolean play() {
			boolean result = false;
			
			try {
				if (player != null && !player.isPlaying()) {
					if (_prepared) {
						player.start();
						scrobblerStart(_item);
						result = true;
					} else {
						prepare();
					}
				}				
			} catch (Exception ex) {
				logger.warn("Error starting media player.", ex);
				cleanup();
			} finally {
				postUpdate();
			}
			
			return result;
		}

		@Override
		public boolean pause() {
			boolean result = false;
			
			try {
				if (player != null && player.isPlaying()) {
					player.pause();
					scrobblerPause();
					result = true;
				}
			} catch (Exception ex) {
				logger.warn("Error pausing media player.", ex);
				cleanup();
			} finally {
				postUpdate();
			}
			
			return result;
		}

		@Override
		public boolean forward() {
			boolean result = false;
			int count = _playlist.size();
			
			if (!_shuffleMode) {
				// skip to next track
				if (_pos < count - 1) {
					_pos++;
					prepare();
					result = true;
				// start from beginning
				} else if (_repeatMode) {
					_pos = 0;
					prepare();
					result = true;
				}
			} else {
				// next shuffle track
				if (_shuffleIterator < count) {
					_pos = (_pos + SHUFFLE_INCREMENT) % count;
					_shuffleIterator++;
					prepare();
					result = true;
				// restart shuffle
				} else if (_repeatMode) {
					// set to 1, since we are already playing first song
					_shuffleIterator = 1;
					_pos = _r.nextInt(count);
					prepare();
					result = true;
				}
			}
			
			return result;
		}
	
		@Override
		public void load(final List<Item> songs) {
			cleanup();

			_playlist = songs;
			if (!_shuffleMode) {
				_pos = 0;
			} else {
				_pos = _r.nextInt(_playlist.size());
			}
			
			postUpdate();
		}

		@Override
		public void select(final long songId) {
			if (player != null && player.isPlaying()) {
				player.stop();
			}
			
			for (_pos = 0; _pos < _playlist.size(); _pos++) {
				_item = _playlist.get(_pos);
				if (_item.getId() == songId)
					break;
			}

			// set to 1, because we are already playing the first song
			_shuffleIterator = 1;
			
			prepare();
		}
		
		@Override
		public void setServer(final long serverId, final String url) {
			_serverId = serverId;
			_mediaURL = url;
		}	
		
		@Override
		public void quit() {
			stopSelf();
		}
		
		@Override
		public void update() {
			postUpdate();
		}
		
		@Override
		public void setShuffleMode(final boolean shuffleMode) {
			_shuffleMode = shuffleMode;
			if (player != null && player.isPlaying()) {
				_shuffleIterator = 1;
			} else {
				_shuffleIterator = 0;
			}
		}
		
		@Override
		public void setRepeatMode(final boolean repeatMode) {
			_repeatMode = repeatMode;
		}
		
		@Override
		public boolean isShuffleMode() {
			return _shuffleMode;
		}
		
		@Override
		public boolean isRepeatMode() {
			return _repeatMode;
		}
		
		@Override
		public void onCompletion(MediaPlayer mp) {
			if (!forward()) {
				scrobblerStop();
			}
			postUpdate();
		}
		
		@Override
		public void onPrepared(MediaPlayer arg0) {
			_prepared = true;
			_retryCount = 0;
			play();
		}
		
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			if (++_retryCount < ERROR_RETRY) {
				logger.error("MediaPlayer returned error (" + what + "," + extra + ") - retrying...");

				// this seems to take care of the issue of songs not loading 
				// on the first try.
				try {
					Thread.sleep(1000);
				} catch (Exception ex) {
					
				}
				prepare();
			} else {
				logger.error("MediaPlayer returned error (" + what + "," + extra + ") - giving up!");
				_retryCount = 0;				
				postUpdate();
				postError(getString(R.string.message_error));
			}
			
			return true;
		}
		
		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			logger.info("MediaPlayer.OnInfo: " + what + " " + extra);
			return false;
		}
						
		private void prepare() {
			try {
				cleanup();
				_item = _playlist.get(_pos);
				//Log.d(Constants.LOG_SOURCE, "Preparing: " + _item);
				String url = String.format(_mediaURL, _item.getId());

				final SharedPreferences prefs = MediaPlayerService.this.getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
				if (prefs.getBoolean(Constants.PREFERENCE_USE_PROXY, false)) {
					// Log.d(Constants.LOG_SOURCE, "Using proxy.");
					url = String.format("http://127.0.0.1:%d/%s", proxy.getPort(), url);
				}
				
				if (player == null) {
					player = new MediaPlayer();
					player.setOnCompletionListener(control);
					player.setOnPreparedListener(control);
					player.setOnErrorListener(control);	
					//_player.setOnInfoListener(_control);
				}
				
				player.setDataSource(url);
				player.setAudioStreamType(AudioManager.STREAM_MUSIC);
				player.prepareAsync();
			} catch (Exception ex) {
				logger.error("Error preparing song.", ex);
				postError(getString(R.string.message_error_loading_song));
				cleanup();
			}
		}
		
		private void cleanup() {
			_prepared = false;
			if (player != null) {				
				player.stop();
				player.reset();
			}
		}
	};
		
	private final BroadcastReceiver receiver = new BroadcastReceiver() {

		private boolean isPausedByCall;
		
		@Override
		public void onReceive(Context context, Intent intent) {
			final Bundle extras = intent.getExtras();
			if (extras == null)
				return;
			
			final String state = extras.getString(TelephonyManager.EXTRA_STATE);
			if (state == null) 
				return;

			// stop / restart playback if necessary depending on phone state
			if (state.equals(TelephonyManager.EXTRA_STATE_RINGING) || state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
				isPausedByCall = control.pause();
			} else if (isPausedByCall && state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
				control.play();
				isPausedByCall = false;
			}			
		}
	};
	
	private MediaPlayer player;
	private final StreamProxy proxy;
	private final PlayerController control;
	
	public MediaPlayerService() {
		proxy = new StreamProxy();
		control = new PlayerController();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		registerReceiver(receiver, new IntentFilter(INCOMING_CALL_ACTION));
		proxy.init();
		proxy.start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (player != null) {
			player.stop();
		}
		// make sure we cancel any notifications
		postUpdate();
		scrobblerStop();
		control.cleanup();
		proxy.stop();		
		// Unregister update receiver
		unregisterReceiver(receiver);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return control;
	}
	
	private void postUpdate() {
		Intent i = new Intent(STATUS_UPDATE);
		if (player != null && player.isPlaying()) {
			final String song = control._item.getName();
			final String album = control._item.getAlbum();
			final String artist = control._item.getArtist();			
			i.putExtra(PARAM_MSG, MSG_PLAY);
			i.putExtra(PARAM_SONG, song);
			i.putExtra(PARAM_ALBUM, album);
			i.putExtra(PARAM_ARTIST, artist);
			
			// Don't restart this activity, but just restore it
			Intent notificationIntent = new Intent(this, SelectionActivity.class);
			notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			notificationIntent.putExtra(SelectionActivity.PARAM_SERVER_ID, control._serverId);
			
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

			final Notification n = new Notification(R.drawable.icon32_app, getString(R.string.label_now_playing) + song, System.currentTimeMillis());
			n.setLatestEventInfo(this, song, "by " + artist + " from " + album, contentIntent);
			n.flags = Notification.FLAG_ONGOING_EVENT;
			startForeground(NOTIFICATION_ID, n);			
		} else {
			i.putExtra(PARAM_MSG, MSG_PAUSE);				
			stopForeground(true);
		}
			
        sendBroadcast(i);			
    }
		
	private void postError(final String message) {
		Toast msg = Toast.makeText(this, message, Toast.LENGTH_LONG);
		msg.setGravity(Gravity.CENTER, msg.getXOffset() / 2, msg.getYOffset() / 2);
		msg.show();
		scrobblerStop();
	}
	
	private void scrobblerStart(final Item item) {
		if (item == null || player == null) {
			return;
		}		
		final Intent i = new Intent(Constants.INTENT_METACHANGED);
		i.putExtra("artist", item.getArtist());
		i.putExtra("album", item.getAlbum());
		i.putExtra("track", item.getName());
		i.putExtra("duration", (long)player.getDuration());
		i.putExtra("position", (long)player.getCurrentPosition());
		sendBroadcast(i);
	}
	
	private void scrobblerPause() {
		sendBroadcast(new Intent(Constants.INTENT_PLAYBACKPAUSED));
	}
	
	private void scrobblerStop() {
		sendBroadcast(new Intent(Constants.INTENT_PLAYBACKCOMPLETE));
	}
}