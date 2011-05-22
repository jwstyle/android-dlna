package dlna.player.activities;

import java.util.List;
import java.util.Stack;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import dlna.player.R;
import dlna.player.common.Item;
import dlna.player.common.PlaylistHelper;
import dlna.player.services.DlnaService;

/**
 * @author ojalate
 * 
 */
public class ContentActivity extends ListActivity {

	public class ContentListAdapter extends BaseAdapter {

		private LayoutInflater inflater;
		private List<Item> itemList;

		public ContentListAdapter(Context context, List<Item> itemList) {
			this.itemList = itemList;
			this.inflater = LayoutInflater.from(context);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getCount()
		 */
		@Override
		public int getCount() {
			return itemList.size();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getItem(int)
		 */
		@Override
		public Object getItem(int arg0) {
			return itemList.get(arg0);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getItemId(int)
		 */
		@Override
		public long getItemId(int position) {
			return position;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getView(int, android.view.View,
		 * android.view.ViewGroup)
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Item item = itemList.get(position);
			View v = null;

			if ("object.item.audioItem.musicTrack"
					.equals(item.getObjectClass())) {
				v = inflater.inflate(R.layout.audio_item, null);
				TextView artist = (TextView) v.findViewById(R.id.artist);
				artist.setText(item.getArtist());
				TextView album = (TextView) v.findViewById(R.id.album);
				album.setText(item.getAlbum());

				ImageView addToPlaylist = (ImageView) v
						.findViewById(R.id.addToPlaylist);
				addToPlaylist.setId(item.getId());
				if (PlaylistHelper.getInstance().getPlayList().contains(item)) {
					addToPlaylist
							.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
				}
				addToPlaylist.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {

						ImageView v = (ImageView) arg0;

						Item i = new Item(arg0.getId(), null, null, null, null);

						if (!PlaylistHelper.getInstance().getPlayListIds()
								.contains(arg0.getId())) {
							int indexOf = dlnaService.getCurrentLevelItems()
									.indexOf(i);
							if (indexOf != -1) {
								PlaylistHelper.getInstance().addItem(
										dlnaService.getCurrentLevelItems().get(
												indexOf));
								v.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
							}
						} else {
							PlaylistHelper.getInstance().removeItem(i);
							v.setImageResource(android.R.drawable.ic_menu_add);
						}

					}
				});
			} else if ("object.item.videoItem".equals(item.getObjectClass())) {
				v = inflater.inflate(R.layout.video_item, null);
			} else if ("object.item.imageItem".equals(item.getObjectClass())) {
				v = inflater.inflate(R.layout.picture_item, null);
			} else if ("object.container.album.musicAlbum".equals(item
					.getObjectClass())) {
				v = inflater.inflate(R.layout.album_item, null);
			} else if (item.getObjectClass()!=null && item.getObjectClass().startsWith("object.container")) {
				v = inflater.inflate(R.layout.container_item, null);
			} else {
				System.out.println(item.getObjectClass());
				v = inflater.inflate(R.layout.list_item, null);
			}

			TextView title = (TextView) v.findViewById(R.id.title);
			if (item != null && title != null) {
				title.setText(item.getTitle());
			}

			return v;
		}
	}

	private boolean delayedGetList = false;

	private DlnaService dlnaService;
	
	private ServiceConnection dlnaServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			dlnaService = ((DlnaService.DlnaServiceBinder) service)
					.getService();
			if (delayedGetList) {
				scrollStack.clear();
				getList(0);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			dlnaService = null;
		}

	};
	
	private ListView lv;
	
	Handler mHandler = new Handler();
	
	private WindowManager mWindowManager;

	private ImageView playerShortcut;

	private Stack<Integer> scrollStack = new Stack<Integer>();

	private void getList(int id) {
		
		List<Item> items = dlnaService.getItems(id);
		if (items != null) {
			showItems(items);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		if (!dlnaService.getStack().isEmpty()
				&& dlnaService.getStack().size() > 1) {
			Integer currentLevel = dlnaService.getStack().peek();
			if (currentLevel != 0) {
				dlnaService.moveUp();
				Integer newLevel = dlnaService.getStack().peek();
				getList(newLevel);
				Integer pop = scrollStack.pop();
				lv.setSelection(pop);
			}
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);
		
		mWindowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
		
		LayoutInflater inflate = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
		playerShortcut = (ImageView) inflate.inflate(R.layout.player_shortcut, null);
		playerShortcut.setVisibility(View.VISIBLE);
		playerShortcut.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent intent = new Intent().setClass(ContentActivity.this,
						AudioPlayerActivity.class);
				ContentActivity.this.startActivity(intent);
				
			}
		});
        mHandler.post(new Runnable() {

            public void run() {
            	Display defaultDisplay = mWindowManager.getDefaultDisplay();
            	int width = defaultDisplay.getWidth();
            	int height = defaultDisplay.getHeight();
            	
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                        width - 20,height - 20,
                        WindowManager.LayoutParams.TYPE_APPLICATION,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
                mWindowManager.addView(playerShortcut, lp);
            }});
        
		lv = (ListView) findViewById(android.R.id.list);
		
		Intent service = new Intent(DlnaService.BIND_SERVICE);
		this.getApplicationContext().bindService(service,
				dlnaServiceConnection, Context.BIND_AUTO_CREATE);

	}

	@Override
	protected void onResume() {
		super.onResume();

		if (dlnaService != null) {
			if (dlnaService.getStack().isEmpty()) {
				scrollStack.clear();
				getList(0);
			} else {
				getList(dlnaService.getStack().peek());
				if (!scrollStack.isEmpty()) {
					lv.scrollTo(0, scrollStack.peek());
				}
			}
		} else {
			delayedGetList = true;
		}
	}

	private void showItems(List<Item> items) {

		setListAdapter(new ContentListAdapter(this, items));
		
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				Item item = (Item) parent.getItemAtPosition(position);

				String title = item.getTitle();
				Toast.makeText(getApplicationContext(), title,
						Toast.LENGTH_SHORT).show();
				
				scrollStack.push(position);
				
				if ("object.item.audioItem.musicTrack".equals(item
						.getObjectClass())) {

					Intent intent = new Intent().setClass(ContentActivity.this,
							AudioPlayerActivity.class);
					intent.setData(Uri.parse(item.getRes()));
					intent.putExtra("artist", item.getArtist());
					intent.putExtra("title", item.getTitle());
					intent.putExtra("album", item.getAlbum());
					intent.putExtra("duration", item.getDuration());
					intent.putExtra("track", position);
					ContentActivity.this.startActivity(intent);
				} else if ("object.item.videoItem"
						.equals(item.getObjectClass())) {
				} else if ("object.item.imageItem"
						.equals(item.getObjectClass())) {
					
					Intent intent = new Intent().setClass(ContentActivity.this,
							PictureViewerActivity.class);
					intent.setData(Uri.parse(item.getRes()));
					intent.putExtra("title", item.getTitle());
					ContentActivity.this.startActivity(intent);
				} else {
					getList(item.getId());
				}

			}
		});
	}

}
