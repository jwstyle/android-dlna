package dlna.player.activities;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import dlna.player.R;
import dlna.player.common.Item;
import dlna.player.common.Tools;
import dlna.player.services.DlnaService;

public class PictureViewerActivity extends Activity {

	public class ImageAdapter extends BaseAdapter {
		private List<Item> items;
		private Context mContext;

		int mGalleryItemBackground;

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

			ImageView i = new ImageView(mContext);
			i.setLayoutParams(new Gallery.LayoutParams(150, 100));
			i.setScaleType(ImageView.ScaleType.FIT_XY);
			i.setBackgroundResource(mGalleryItemBackground);
			
			Drawable drawable = Tools.getDrawable(items.get(position).getRes());
			
			if (drawable!=null) {
				i.setImageDrawable(drawable);
			}

			return i;
		}
	}

	private DlnaService dlnaService;

	private ServiceConnection dlnaServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			dlnaService = ((DlnaService.DlnaServiceBinder) service)
					.getService();
			populateGallery();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			dlnaService = null;
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pictureviewer);

		Gallery g = (Gallery) findViewById(R.id.Gallery01);
		g.setAdapter(new ImageAdapter(this, new ArrayList<Item>()));

		g.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				Item itemAtPosition = (Item) parent.getItemAtPosition(position);
				Toast.makeText(PictureViewerActivity.this,
						itemAtPosition.getTitle(), Toast.LENGTH_SHORT).show();
				ImageView pic = (ImageView) PictureViewerActivity.this
						.findViewById(R.id.ImageView01);

				try {
					URL url = new URL(itemAtPosition.getRes());
					InputStream is;
					is = (InputStream) url.getContent();
					Drawable d = Drawable.createFromStream(is, "src");
					pic.setImageDrawable(d);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		Intent service = new Intent(this, DlnaService.class);
		this.getApplicationContext().bindService(service,
				dlnaServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (dlnaService != null) {
			populateGallery();
		}

	}

	public void populateGallery() {
		if (dlnaService.getStack().size() > 1) {
			List<Item> siblings = dlnaService.getItems(dlnaService.getStack()
					.peek());

			if (siblings!=null) {
				Gallery g = (Gallery) findViewById(R.id.Gallery01);
				g.setAdapter(new ImageAdapter(this, siblings));
			}
		}
	}
	
	public void removeBackItem(List<Item> items){
		if (items!=null) {
			for(int i=0; i<items.size();++i) {
				Item item = items.get(i);
				if ("..".equals(item.getTitle())) {
					items.remove(i);
					break;
				}
			}
		}
	}
}