package dlna.player.activities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Icon;
import org.cybergarage.upnp.IconList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import dlna.player.R;
import dlna.player.common.DeviceData;
import dlna.player.common.Tools;
import dlna.player.services.DlnaService;

/**
 * @author ojalate
 * 
 */
public class NetworkActivity extends Activity {

	private class DeviceListAdapter extends BaseAdapter {
		private Device[] devices;
		private LayoutInflater mInflater;

		public DeviceListAdapter(Context context, Device[] devices) {
			mInflater = LayoutInflater.from(context);
			this.devices = devices;
			for (int i = 0; i < devices.length; i++) {
				Device device = devices[i];
				System.out.println("device:"+device.getFriendlyName());
			}
		}

		/**
		 * The number of items in the list is determined by the number of
		 * speeches in our array.
		 * 
		 * @see android.widget.ListAdapter#getCount()
		 */
		public int getCount() {
			return devices.length;
		}

		/**
		 * Since the data comes from an array, just returning the index is
		 * sufficent to get at the data. If we were using a more complex data
		 * structure, we would return whatever object represents one row in the
		 * list.
		 * 
		 * @see android.widget.ListAdapter#getItem(int)
		 */
		public Object getItem(int position) {
			return devices[position];
		}

		/**
		 * Use the array index as a unique id.
		 * 
		 * @see android.widget.ListAdapter#getItemId(int)
		 */
		public long getItemId(int position) {
			return position;
		}

		/**
		 * Make a view to hold each row.
		 * 
		 * @see android.widget.ListAdapter#getView(int, android.view.View,
		 *      android.view.ViewGroup)
		 */
		@SuppressWarnings("unchecked")
		public View getView(int position, View convertView, ViewGroup parent) {

			View v = mInflater.inflate(R.layout.device_list_item, null);
			

			TextView text = (TextView) v
			.findViewById(R.id.deviceName);
			ImageView image = (ImageView) v
			.findViewById(R.id.deviceIcon);
			Device device = this.devices[position];
			if (device != null) {
				text.setText(device.getFriendlyName());

				IconList iconList = device.getIconList();
				Iterator iterator = iconList.iterator();
				while (iterator.hasNext()) {
					Icon icon = (Icon) iterator.next();
					System.out.println("icon url:" + icon.getURL()+" "+icon.getWidth());
					if (icon.getWidth() > 40 && icon.getWidth() < 50) {
						String iconUrl = icon.getURL();
						String urlBase = device.getURLBase();
						if (iconUrl.startsWith("/") && urlBase.endsWith("/")) {
							iconUrl = iconUrl.substring(1);
						}
						System.out.println("urlBase:"+urlBase);
						if (urlBase != null && urlBase.length() > 0) {
							try {
								Drawable drawable = Tools.getDrawable(urlBase
										+ iconUrl);
								image.setImageDrawable(drawable);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						break;
					}
				}
			}

			return v;
		}
	}

	public class DeviceSearchReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (DlnaService.NEW_DEVICES_FOUND.equals(intent.getAction())) {
				
				ListView lv = (ListView) findViewById(R.id.deviceList);
				
				lv.setAdapter(new DeviceListAdapter(NetworkActivity.this,
						DeviceData.getInstance().getDevices()));
				
				lv.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {

						
						Device device = (Device) parent
								.getItemAtPosition(position);

						DeviceData.getInstance().setSelectedDevice(device);

						ListView sdl = (ListView) findViewById(R.id.selectedDeviceList);
						List<Device> selected = new ArrayList<Device>();
						selected.add(device);
						
						DeviceListAdapter deviceListAdapter = new DeviceListAdapter(
								NetworkActivity.this, selected
										.toArray(new Device[] {}));
						
						sdl.setAdapter(deviceListAdapter);

						Toast.makeText(getApplicationContext(),
								device.getFriendlyName(), Toast.LENGTH_SHORT)
								.show();
					}
				});
			}

		}

	}

	DeviceSearchReceiver receiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.network);

		ImageButton cmd_search = (ImageButton) this.findViewById(R.id.searchButton);
		cmd_search.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				startService(new Intent(DlnaService.SEARCH_DEVICES));
				
			}
		});

		receiver = new DeviceSearchReceiver();
		registerReceiver(receiver, new IntentFilter(
				DlnaService.NEW_DEVICES_FOUND));

		Device selectedDevice = DeviceData.getInstance().getSelectedDevice();
		ListView lv = (ListView) findViewById(R.id.selectedDeviceList);
		List<Device> selected = new ArrayList<Device>();
		if (selectedDevice != null) {
			selected.add(selectedDevice);
			lv.setAdapter(new DeviceListAdapter(this, selected
					.toArray(new Device[] {})));
		} else {
			List<String> list = new ArrayList<String>();
			list.add("No device selected");
			lv.setAdapter(new ArrayAdapter<String>(this,
					R.id.selectedDeviceList, list) {
				@Override
				public View getView(int position, View convertView,
						ViewGroup parent) {

					TextView tv = new TextView(NetworkActivity.this);
					tv.setText(getItem(position));

					return tv;
				}
			});
		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		ListView lv = (ListView) findViewById(R.id.deviceList);
		lv.setAdapter(new DeviceListAdapter(this, DeviceData.getInstance()
				.getDevices()));
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				Device device = (Device) parent.getItemAtPosition(position);

				DeviceData.getInstance().setSelectedDevice(device);

				ListView lv = (ListView) findViewById(R.id.selectedDeviceList);
				List<Device> selected = new ArrayList<Device>();
				selected.add(device);
				lv.setAdapter(new DeviceListAdapter(NetworkActivity.this,
						selected.toArray(new Device[] {})));

				startService(new Intent(DlnaService.RESET_STACK));
				
				Toast.makeText(getApplicationContext(),
						device.getFriendlyName(), Toast.LENGTH_SHORT).show();
			}
		});
	}
}
