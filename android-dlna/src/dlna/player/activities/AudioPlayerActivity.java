package dlna.player.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import dlna.player.R;
import dlna.player.common.AnimationHelper;
import dlna.player.common.Item;
import dlna.player.common.Tools;
import dlna.player.custom.PlayerImageView;
import dlna.player.services.DlnaService;
import dlna.player.services.MediaPlayerService;

public class AudioPlayerActivity extends Activity implements
		OnSeekBarChangeListener, OnSeekCompleteListener, OnTouchListener,
		OnItemSelectedListener {

	public class ImageAdapter extends BaseAdapter {
		private List<Item> items;

		private Context mContext;

		public ImageAdapter(Context c, List<Item> items) {
			mContext = c;
			this.items = items;
		}

		public int getCount() {
			if (items != null) {
				return items.size();
			}
			return 0;
		}

		public Object getItem(int position) {
			return items.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			PlayerImageView i = new PlayerImageView(mContext);
			i.setPadding(15, 0, 15, 0);
			i.setId(position);
			if (items.size()>position) {
				Item item = items.get(position);
				Bitmap bm = Tools.makeBitmapFromFile(item
						.getCoverArtFile(), item.getTitle(), defaultBitmap);
				i.setImageBitmap(bm);
			}

			return i;
		}
	}
	
	private static final int DIALOG1_KEY = 0;

	private Runnable coverUpdater = new Runnable() {

		@Override
		public void run() {

			int count = 0;
			// Wait until the gallery widget has been initialized
			while (g.getLastVisiblePosition()<0 && count < 5) {
				try {
					System.out.println("waiting:"+count);
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				++count;
			}
			// Update first visible covers and let the rest be updated in
			// background
			runOnUiThread(visibleImageUpdater);

			for (int i=0; i<g.getCount();++i) {
				int firstVisiblePosition = g.getFirstVisiblePosition();
				int lastVisiblePosition = g.getLastVisiblePosition();
				if (!(i > firstVisiblePosition && i <lastVisiblePosition)) {
					Tools.getCovertArt((Item)g.getItemAtPosition(i));
				}
			}

		}
	};

	private List<Item> currentPlaylist = null;

	private TextView d;

	private Bitmap defaultBitmap;

	private boolean delayedStart;

	private ProgressDialog dialog;

	private DlnaService dlnaService;

	private ServiceConnection dlnaServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			dlnaService = ((DlnaService.DlnaServiceBinder) service)
					.getService();
			if (dlnaServiceDelayedStart) {
				isDlnaServiceReady = true;
				if (isMediaPlayerReady) {
					initializePlayback();
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			dlnaService = null;
		}

	};
	private boolean dlnaServiceDelayedStart;
	private int duration = 0;

	private Gallery g;
	

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			SeekBar seekBar = (SeekBar) AudioPlayerActivity.this
					.findViewById(R.id.SeekBar01);
			switch (msg.what) {
			case MediaPlayerService.BUFFERING_UPDATE:
				int buffer = data.getInt("buffer");
				seekBar.setSecondaryProgress(buffer);
				break;
			case MediaPlayerService.PROGRESS_UPDATE:
				int progress = data.getInt("progress");
				seekBar.setProgress(progress);
				position.setText(data.getString("timeAsString"));
				break;
			case MediaPlayerService.PLAYBACK_COMPLETE:
				int selectedItemPosition = g.getSelectedItemPosition();
				if (selectedItemPosition < g.getCount()-1) {
					g.setSelection(selectedItemPosition+1);
				}
				break;
			case MediaPlayerService.DURATION_UPDATE:
				duration = data.getInt("duration");
				d.setText(MediaPlayerService.getTimeAsString(duration));
				break;
			}
			
		}

	};

	private boolean isDlnaServiceReady = false;

	private boolean isMediaPlayerReady = false;

	boolean isUp = true;

	private MediaPlayerService mediaPlayerService;

	private ServiceConnection mediaPlayerServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mediaPlayerService = ((MediaPlayerService.MediaPlayerServiceBinder) service)
					.getService();
			mediaPlayerService.setGUIHandler(handler);
			if (delayedStart) {

				delayedStart = false;
				isMediaPlayerReady = true;
				
				if (isDlnaServiceReady) {
					initializePlayback();
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mediaPlayerService = null;
		}

	};

	private Item nowPlaying = new Item();

	private TextView position = null;
	private SeekBar seekBar = null;

	boolean shortcutResume = false;

	private Uri songUri = null;

	private Item soonPlaying;
	
	boolean startWhenUp = false;

	private int track;

	private Runnable visibleImageUpdater = new Runnable() {

		@Override
		public void run() {

			int firstVisiblePosition = g.getFirstVisiblePosition();
			int lastVisiblePosition = g.getLastVisiblePosition();

			int viewPosition = 0;

			for (int i = firstVisiblePosition; i <= lastVisiblePosition; ++i) {
				PlayerImageView childAt = (PlayerImageView) g
						.getChildAt(viewPosition);
				Item item = (Item) g.getItemAtPosition(i);
				Tools.getCovertArt(item);
				if (item.getCoverArtFile() != null && childAt != null) {
					Bitmap reflectiveBitmap = Tools.makeBitmapFromFile(item
							.getCoverArtFile(), item.getTitle(), defaultBitmap);
					childAt.setImageBitmap(reflectiveBitmap);
				}
				++viewPosition;
			}

		}
	};

	private void initializePlayback() {

		if (shortcutResume) {
			track = mediaPlayerService.getCurrentTrack();
			currentPlaylist = mediaPlayerService.getCurrentPlaylist();
			if (currentPlaylist!=null && currentPlaylist.size()>0) {
				nowPlaying = currentPlaylist.get(track);
			}
		}
		else {
			currentPlaylist = dlnaService.getCurrentLevelItems();
		}
		
		TextView pl = (TextView) AudioPlayerActivity.this
		.findViewById(R.id.playlistsize);
		pl.setText("" + (currentPlaylist.size()));
		
		TextView trackView = (TextView) this.findViewById(R.id.track);
		trackView.setText(track + "");
		
		populateGallery();
		g.setSelection(track);
		
		new Thread(coverUpdater).start();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);

		Bitmap decodeResource = BitmapFactory.decodeResource(getResources(),
				R.drawable.default_cover);

		defaultBitmap = Bitmap.createScaledBitmap(decodeResource, 160, 160,
				false);

		position = (TextView) this.findViewById(R.id.position);
		
		d = (TextView) this.findViewById(R.id.duration);
		
		g = (Gallery) findViewById(R.id.Gallery01);
		g.setAdapter(new ImageAdapter(this, new ArrayList<Item>()));
		g.setCallbackDuringFling(false);
		g.setOnTouchListener(this);

		registerForContextMenu(g);

		ImageButton play = (ImageButton) this.findViewById(R.id.playButton);
		play.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				ImageButton play = (ImageButton) AudioPlayerActivity.this
						.findViewById(R.id.playButton);

				if (nowPlaying != null) {
					if (mediaPlayerService.isPaused()
							|| !mediaPlayerService.isPlaying()) {
						mediaPlayerService.startPlaying(nowPlaying.getRes(),track, currentPlaylist);
						play.setImageResource(android.R.drawable.ic_media_pause);
					} else {
						mediaPlayerService.pausePlaying();
						play.setImageResource(android.R.drawable.ic_media_play);
					}
				}
			}
		});
		
		seekBar = (SeekBar) findViewById(R.id.SeekBar01);
		seekBar.setOnSeekBarChangeListener(this);

		Intent service = new Intent(this, MediaPlayerService.class);
		this.getApplicationContext().bindService(service,
				mediaPlayerServiceConnection, Context.BIND_AUTO_CREATE);

		service = new Intent(DlnaService.BIND_SERVICE);
		this.getApplicationContext().bindService(service,
				dlnaServiceConnection, Context.BIND_AUTO_CREATE);
	}

//	private int getDurationMsecs(String duration) {
//		if (duration != null) {
//			System.out.println("duration:"+duration);
//			String[] split = duration.split(":");
//			double parsedDouble = Double.parseDouble(split[split.length - 1]) * 1000;
////			int d = Integer.parseInt(split[split.length - 1]) * 1000;
//			int d = (int)parsedDouble;
//			d = d + Integer.parseInt(split[split.length - 2]) * 1000 * 60;
//			return d;
//		}
//		return 0;
//	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 * android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

		final Item item = currentPlaylist.get(info.position);
		
		showDialog(DIALOG1_KEY);
		
		Runnable searcher = new Runnable() {
			public void run() {

				final List<Item> alternativeCovers = Tools.getAlternativeCovers(item);
				
				Runnable updateUI = new Runnable() {

					@Override
					public void run() {
						
						AlertDialog.Builder builder = new AlertDialog.Builder(AudioPlayerActivity.this);
						final AlertDialog alert = builder.create();
						
						ArrayAdapter<Item> arrayAdapter = new ArrayAdapter<Item>(AudioPlayerActivity.this, 0,
								alternativeCovers) {

							private LayoutInflater inflater = LayoutInflater
									.from(AudioPlayerActivity.this);

							/*
							 * (non-Javadoc)
							 * 
							 * @see android.widget.ArrayAdapter#getView(int, android.view.View,
							 * android.view.ViewGroup)
							 */
							@Override
							public View getView(int position, View convertView, ViewGroup parent) {

								View v = inflater.inflate(R.layout.cover_list_item, null);
								v.setId(position);
								ImageView icon = (ImageView) v.findViewById(R.id.coverIcon);
								
								Item cover = getItem(position);
								
								if (cover.getMediumBitmap()==null) {
									Bitmap decodeFile = BitmapFactory.decodeFile(cover.getRes());
									cover.setMediumBitmap(decodeFile);
								}
								
								icon.setImageBitmap(cover.getMediumBitmap());
								
								v.setOnClickListener(new OnClickListener() {

									@Override
									public void onClick(View v) {

										Item selectedCover = alternativeCovers.get(v.getId());

										String fileName = selectedCover.getArtist() + " "
												+ selectedCover.getAlbum() + ".jpg";

										fileName = fileName.replaceAll(" ", "_");

										String playerDir = "/sdcard/dlnaplayer";

										File file = new File(playerDir + "/" + fileName);
										if (file.exists()) {
											file.delete();
										}

										Tools.moveFile(selectedCover.getRes(), playerDir + "/" + fileName);

										runOnUiThread(visibleImageUpdater);

										alert.hide();
									}
								});

								return v;
							}
						};

						ListView lv = new ListView(AudioPlayerActivity.this);
						lv.setScrollingCacheEnabled(false);
						lv.setAdapter(arrayAdapter);

						alert.setTitle(item.getArtist() + " - " + item.getTitle());
						alert.setView(lv);
						
						dialog.hide();
						
						alert.show();
					}
					
				};
				
				runOnUiThread(updateUI);
			}
		};
		new Thread(searcher).start();

	}
	protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG1_KEY: {
                dialog = new ProgressDialog(this);
                dialog.setMessage("Searching alternative covers...");
                dialog.setIndeterminate(true);
                dialog.setCancelable(true);
                return dialog;
            }
        }
        return null;
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.AdapterView.OnItemSelectedListener#onItemSelected(android
	 * .widget.AdapterView, android.view.View, int, long)
	 */
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {

		Item selectedItem = (Item) g.getSelectedItem();
		track = position;
		if (isUp) {
			if (!selectedItem.getRes().equals(nowPlaying.getRes())) {
				setItemData(selectedItem);
				mediaPlayerService.startPlaying(selectedItem.getRes(),track, currentPlaylist);
				ImageButton play = (ImageButton) this.findViewById(R.id.playButton);
				play.setImageResource(android.R.drawable.ic_media_pause);
				nowPlaying = selectedItem;
			}
		} else {
			soonPlaying = selectedItem;
			startWhenUp = true;
		}
		
		if (shortcutResume) {
			setItemData(selectedItem);
			if (!mediaPlayerService.isPaused()) {
				ImageButton play = (ImageButton) this.findViewById(R.id.playButton);
				play.setImageResource(android.R.drawable.ic_media_pause);
			}
			else {
				this.position.setText(mediaPlayerService.getCurrentTime());
				this.seekBar.setProgress(mediaPlayerService.getCurrentProgress());
			}
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onLowMemory()
	 */
	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.AdapterView.OnItemSelectedListener#onNothingSelected(android
	 * .widget.AdapterView)
	 */
	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.SeekBar.OnSeekBarChangeListener#onProgressChanged(android
	 * .widget.SeekBar, int, boolean)
	 */
	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean fromUser) {
		if (fromUser) {
			int point = duration * arg1 / 100;
			mediaPlayerService.seekTo(point);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		songUri = this.getIntent().getData();

		if (mediaPlayerService == null) {
			delayedStart = true;
		}

		if (dlnaService == null) {
			dlnaServiceDelayedStart = true;
		}
		
		if (songUri != null) {
			track = this.getIntent().getIntExtra("track", -1);
		}
		else {
			shortcutResume = true;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.media.MediaPlayer.OnSeekCompleteListener#onSeekComplete(android
	 * .media.MediaPlayer)
	 */
	@Override
	public void onSeekComplete(MediaPlayer mp) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.SeekBar.OnSeekBarChangeListener#onStartTrackingTouch(android
	 * .widget.SeekBar)
	 */
	@Override
	public void onStartTrackingTouch(SeekBar arg0) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.SeekBar.OnSeekBarChangeListener#onStopTrackingTouch(android
	 * .widget.SeekBar)
	 */
	@Override
	public void onStopTrackingTouch(SeekBar arg0) {

	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnTouchListener#onTouch(android.view.View,
	 * android.view.MotionEvent)
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (MotionEvent.ACTION_DOWN == event.getAction()) {
			isUp = false;
		} else if (MotionEvent.ACTION_MOVE == event.getAction()) {

		} else if (MotionEvent.ACTION_UP == event.getAction()) {
			isUp = true;
			if (startWhenUp) {
				if (!soonPlaying.equals(nowPlaying)) {
					setItemData(soonPlaying);
					mediaPlayerService.startPlaying(soonPlaying.getRes(),track, currentPlaylist);
					nowPlaying = soonPlaying;
					startWhenUp = false;
					ImageButton play = (ImageButton) this.findViewById(R.id.playButton);
					play.setImageResource(android.R.drawable.ic_media_pause);
				}
			}
		} 
		return false;
	}
	
	public void populateGallery() {

		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		int displayWidth = display.getWidth();
		AnimationHelper.setDisplayWidth(displayWidth);

		g = (Gallery) findViewById(R.id.Gallery01);
		g.setAdapter(new ImageAdapter(this, currentPlaylist));
		g.setOnItemSelectedListener(this);

	}
	
	private void setItemData(Item item) {
		TextView t = (TextView) this.findViewById(R.id.title);
		t.setText(item.getTitle());

		TextView a = (TextView) this.findViewById(R.id.artist);
		a.setText(item.getArtist());

		TextView al = (TextView) this.findViewById(R.id.album);
		al.setText(item.getAlbum());

		TextView trackView = (TextView) this.findViewById(R.id.track);
		trackView.setText((track + 1) + "");
	}
}