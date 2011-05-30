package dlna.player.activities;

import java.util.List;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import dlna.player.R;
import dlna.player.common.Item;
import dlna.player.common.PlaylistHelper;
import dlna.player.services.DlnaService;

/**
 * @author ojalate
 *
 */
public class PlaylistActivity extends ListActivity {

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
				
				ImageView addToPlaylist = (ImageView) v.findViewById(R.id.addToPlaylist);
				addToPlaylist.setId(item.getId());
				if (PlaylistHelper.getInstance().getPlayList().contains(item)) {
					addToPlaylist.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
				}
				addToPlaylist.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {

						Item i = new Item(arg0.getId(), null, null, null, null);
						
						if (PlaylistHelper.getInstance().getPlayListIds().contains(arg0.getId())) {
							PlaylistHelper.getInstance().removeItem(i);
							ListView listView = PlaylistActivity.this.getListView();
							listView.invalidateViews();
						}
						
					}
				});
				
			} else if ("object.item.videoItem".equals(item.getObjectClass())) {
				v = inflater.inflate(R.layout.video_item, null);
			} else if ("object.item.imageItem".equals(item.getObjectClass())) {
				v = inflater.inflate(R.layout.picture_item, null);
			} else if ("object.container.album.musicAlbum".equals(item.getObjectClass())) {
				v = inflater.inflate(R.layout.album_item, null);
			} else if ("object.container".equals(item.getObjectClass())) {
				v = inflater.inflate(R.layout.container_item, null);
			} else {
				v = inflater.inflate(R.layout.list_item, null);
			}

			TextView title = (TextView) v.findViewById(R.id.title);
			if (item != null && title != null) {
				title.setText(item.getTitle());
			}

			return v;
		}
	}

	private DlnaService dlnaService;
	
	private ServiceConnection dlnaServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			dlnaService = ((DlnaService.DlnaServiceBinder) service)
					.getService();
			
			dlnaService.setCurrentLevelItems(PlaylistHelper.getInstance().getPlayList());
			dlnaService.getStack().clear();
			showItems(PlaylistHelper.getInstance().getPlayList());
			
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			dlnaService = null;
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);
		Intent service = new Intent(DlnaService.BIND_SERVICE);
		this.getApplicationContext().bindService(service,
				dlnaServiceConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (dlnaService!=null) {
			dlnaService.setCurrentLevelItems(PlaylistHelper.getInstance().getPlayList());
			dlnaService.getStack().clear();
			showItems(PlaylistHelper.getInstance().getPlayList());
		}
		
	}
	
	private void showItems(List<Item> items) {
		
		setListAdapter(new ContentListAdapter(this, items));

		ListView lv = (ListView) findViewById(android.R.id.list);
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				Item item = (Item) parent.getItemAtPosition(position);
				
				if ("object.item.audioItem.musicTrack".equals(item
						.getObjectClass())) {
					 Intent intent = new
					 Intent().setClass(PlaylistActivity.this,
					 AudioPlayerActivity.class);
					 intent.setData(Uri.parse(item.getRes()));
					 intent.putExtra("artist", item.getArtist());
					 intent.putExtra("title", item.getTitle());
					 intent.putExtra("album", item.getAlbum());
					 intent.putExtra("duration", item.getDuration());
					 intent.putExtra("track", position);
					 PlaylistActivity.this.startActivity(intent);
				} else if ("object.item.videoItem"
						.equals(item.getObjectClass())) {
				} else if ("object.item.imageItem"
						.equals(item.getObjectClass())) {
					 Intent intent = new Intent().setClass(PlaylistActivity.this,
					 PictureViewerActivity.class);
					 intent.setData(Uri.parse(item.getRes()));
					 intent.putExtra("title", item.getTitle());
					 PlaylistActivity.this.startActivity(intent);
				}

			}
		});
	}
	
}
