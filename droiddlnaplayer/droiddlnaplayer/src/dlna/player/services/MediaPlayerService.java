package dlna.player.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import dlna.player.common.Item;

/**
 * @author ojalate
 * 
 */
public class MediaPlayerService extends Service {

	public class MediaPlayerServiceBinder extends Binder {
		public MediaPlayerService getService() {
			return MediaPlayerService.this;
		}
	}

	private class PlayBackThread extends Thread {
		public PlayBackThread(String threadName) {
			super(threadName);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#destroy()
		 */
		@Override
		public void destroy() {
			super.destroy();
			mp.release();
		}

		private void initPlayer() {
			mp = new MediaPlayer();
			mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

				@Override
				public void onPrepared(MediaPlayer mp) {
					System.out.println("prepared, starting playback on "
							+ Thread.currentThread().getName());
					mp.start();
					handler.removeCallbacks(progressUpdater);
					handler.postDelayed(progressUpdater, 100);

					Message msg = new Message();
					msg.what = MediaPlayerService.DURATION_UPDATE;
					Bundle b = new Bundle();
					b.putInt("duration", mp.getDuration());
					msg.setData(b);
					guiHandler.sendMessage(msg);
				}
			});

			mp
					.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

						@Override
						public void onBufferingUpdate(MediaPlayer mp,
								int percent) {
							Message msg = new Message();
							msg.what = MediaPlayerService.BUFFERING_UPDATE;
							Bundle b = new Bundle();
							b.putInt("buffer", percent);
							msg.setData(b);
							guiHandler.sendMessage(msg);
						}
					});

			mp.setOnCompletionListener(new OnCompletionListener() {

				@Override
				public void onCompletion(MediaPlayer mp) {

					if (binder.isBinderAlive()) {
						System.out.println("binder is alive");
						// Audio player is alive so send message
						Message msg = new Message();
						msg.what = MediaPlayerService.PLAYBACK_COMPLETE;
						guiHandler.sendMessage(msg);
					} else {
						if (currentTrack < currentPlaylist.size() - 1) {
							Item next = currentPlaylist.get(currentTrack + 1);
							startPlaying(next.getRes(), currentTrack + 1,
									currentPlaylist);
						}
					}
				}
			});
		}

		public void run() {
			Looper.prepare();

			playerLooper = Looper.myLooper();

			try {
				initPlayer();
				mp.setDataSource(currentUrl);
				mp.prepareAsync();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Looper.loop();

			// Clean up before exiting
			handler.removeCallbacks(progressUpdater);
			mp.release();

		}
	}

	public static final int BUFFERING_UPDATE = 2;
	public static final int DURATION_UPDATE = 7;
	public static final int PAUSE_PLAYBACK = 5;
	public static final int PLAYBACK_COMPLETE = 5;
	public static final int PLAYER_READY = 1;
	public static final int PROGRESS_UPDATE = 3;
	public static final int SEEK_TO = 6;
	public static final String START_PLAYBACK = "dlna.player.START_PLAYBACK";
	public static final int START_PLAYBACK_INT = 0;
	public static final String STOP_PLAYBACK = "dlna.player.STOP_PLAYBACK";
	public static final int STOP_PLAYBACK_INT = 4;

	public static String getTimeAsString(int time) {
		int seconds = time / 1000;
		int minutes = seconds / 60;
		seconds = seconds % 60;

		String minutesOut = minutes < 10 ? "0" + minutes : minutes + "";
		String secondsOut = seconds < 10 ? ("0" + seconds) : seconds + "";

		return minutesOut + ":" + secondsOut;
	}

	private final IBinder binder = new MediaPlayerServiceBinder();
	private List<Item> currentPlaylist = new ArrayList<Item>();
	private int currentTrack;
	private String currentUrl = null;

	private Handler guiHandler;

	private Handler handler = new Handler();

	private MediaPlayer mp;

	private boolean paused;

	private Looper playerLooper;

	private PlayBackThread playerThread;

	private String previousTimeAsString = null;

	private Runnable progressUpdater = new Runnable() {
		public void run() {

			int currentPosition = mp.getCurrentPosition();

			String timeAsString = getTimeAsString(currentPosition);

			double position = ((double) currentPosition) / mp.getDuration();
			int seekBarProgress = (int) (position * 100);

			if (!timeAsString.equals(previousTimeAsString)) {
				Message msg = new Message();
				msg.what = MediaPlayerService.PROGRESS_UPDATE;
				Bundle b = new Bundle();
				b.putInt("progress", seekBarProgress);
				b.putString("timeAsString", timeAsString);

				msg.setData(b);
				guiHandler.sendMessage(msg);
			}

			previousTimeAsString = timeAsString;

			handler.postAtTime(this, SystemClock.uptimeMillis() + 200);
		}
	};

	String TAG = "MediaPlayerService";

	public List<Item> getCurrentPlaylist() {
		return currentPlaylist;
	}

	public int getCurrentPosition() {
		int position = 0;
		try {
			position = mp.getCurrentPosition();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return position;
	}

	public int getCurrentProgress() {
		int percent = 0;
		try {
			percent = (100 * mp.getCurrentPosition() / mp.getDuration());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return percent;
	}

	public String getCurrentTime() {
		String time = "00:00";

		try {
			time = getTimeAsString(mp.getCurrentPosition());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return time;
	}

	public int getCurrentTrack() {
		return currentTrack;
	}

	public String getCurrentUrl() {
		return currentUrl;
	}

	public int getDuration() {
		int duration = 0;
		try {
			duration = mp.getDuration();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return duration;
	}

	public boolean isPaused() {
		return paused;
	}

	public boolean isPlaying() {
		if (mp == null) {
			return false;
		}
		return mp.isPlaying();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (MediaPlayerService.START_PLAYBACK.equals(intent.getAction())) {

		} else if (MediaPlayerService.STOP_PLAYBACK.equals(intent.getAction())) {

		}

		return START_NOT_STICKY;
	}

	public void pausePlaying() {
		try {
			mp.pause();
			paused = true;
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		handler.removeCallbacks(progressUpdater);
	}

	public void seekTo(int point) {
		try {
			if (mp != null) {
				mp.seekTo(point);
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	public void setGUIHandler(Handler handler) {
		guiHandler = handler;
	}

	public void startPlaying(String url, int track, List<Item> playList) {

		if (url != null && !url.equals(currentUrl)) {
			currentTrack = track;
			currentUrl = url;
			currentPlaylist = playList;

			if (playerLooper != null) {
				playerLooper.quit();
			}

			if (playerThread != null && playerThread.isAlive()) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(300);
							if (playerLooper != null) {
								playerLooper.quit();
							}
							playerThread = new PlayBackThread(
									"AudioPlayerThread");
							playerThread.start();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				}).start();
			} else {
				playerThread = new PlayBackThread("AudioPlayerThread");
				playerThread.start();
			}
			paused = false;
		} else if (paused) {
			try {
				mp.start();
				paused = false;
				handler.postDelayed(progressUpdater, 100);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}

	}

	public void stopPlaying() {
		handler.removeCallbacks(progressUpdater);
		try {
			mp.stop();
			playerLooper.quit();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}
}
