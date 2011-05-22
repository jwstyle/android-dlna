package dlna.player.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.UPnPStatus;
import org.cybergarage.upnp.device.DeviceChangeListener;
import org.cybergarage.upnp.device.InvalidDescriptionException;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import dlna.player.common.DeviceData;
import dlna.player.common.Item;

/**
 * @author ojalate
 *
 */
public class DlnaService extends Service implements DeviceChangeListener {

	public class DlnaServiceBinder extends Binder {
		public DlnaService getService() {
			return DlnaService.this;
		}
	}
	public static final String BIND_SERVICE = "dlna.player.BIND_SERVICE";
	public static final String GET_ITEM_LIST = "dlna.player.GET_ITEM_LIST";
	public static final String ITEM_LIST_RESULT = "dlna.player.ITEM_LIST_RESULT";
	public static final String NEW_DEVICES_FOUND = "dlna.player.NEW_DEVICES_FOUND";
	public static final String RESET_STACK = "dlna.player.RESET_STACK";
	public static final String SEARCH_DEVICES = "dlna.player.SEARCH_DEVICES";
	public static List<Item> parseResult(Argument result) {
		
		List<Item> list = new ArrayList<Item>();
		
		DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;
		try {
			documentBuilder = dfactory.newDocumentBuilder();
			InputStream is = new ByteArrayInputStream(result.getValue()
					.getBytes("UTF-8"));

			Document doc = documentBuilder.parse(is);

			NodeList containers = doc.getElementsByTagName("container");
			for (int j = 0; j < containers.getLength(); ++j) {
				Node container = containers.item(j);
				String title = null;
				String objectClass = null;
				int id = 0;
				NodeList childNodes = container.getChildNodes();
				for (int l = 0; l < childNodes.getLength(); ++l) {
					Node childNode = childNodes.item(l);
					
					if (childNode.getNodeName().equals("dc:title")) {
						title = childNode.getFirstChild().getNodeValue();
						id = Integer.parseInt(container.getAttributes()
								.getNamedItem("id").getNodeValue());
						
					}
					else if (childNode.getNodeName().equals("upnp:class")) {
						objectClass = childNode.getFirstChild().getNodeValue();
					}
				}
				Item i = new Item(id, title, null, null, objectClass);
				list.add(i);
			}

			NodeList items = doc.getElementsByTagName("item");
			for (int j = 0; j < items.getLength(); ++j) {
				Node item = items.item(j);
				int id = 0;
				String title = null;
				String artist = null;
				String album = null;
				String objectClass = null;
				String res = null;
				String duration = null;

				id = Integer.parseInt(item.getAttributes().getNamedItem("id").getNodeValue());

				NodeList childNodes = item.getChildNodes();
				for (int l = 0; l < childNodes.getLength(); ++l) {
					Node childNode = childNodes.item(l);

					if (childNode.getNodeName().equals("dc:title")) {
						title = childNode.getFirstChild().getNodeValue();

					} else if (childNode.getNodeName().equals("upnp:artist")) {
						artist = childNode.getFirstChild().getNodeValue();
					} else if (childNode.getNodeName().equals("upnp:album")) {
						album = childNode.getFirstChild().getNodeValue();
					} else if (childNode.getNodeName().equals("upnp:class")) {
						objectClass = childNode.getFirstChild().getNodeValue();
					} else if (childNode.getNodeName().equals("res")) {
						res = childNode.getFirstChild().getNodeValue();
						if (childNode.getAttributes().getNamedItem("duration")!=null) {
							duration = childNode.getAttributes().getNamedItem("duration").getNodeValue();
						}
					}

				}
				Item i = new Item(id, title, artist, album, objectClass);
				if ("object.item.audioItem.musicTrack".equals(objectClass) ||
						"object.item.videoItem".equals(objectClass) ||
						"object.item.imageItem".equals(objectClass)) {
					i.setRes(res);
					i.setDuration(duration);
				}
				list.add(i);
			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}
	private final IBinder binder = new DlnaServiceBinder();
	private ControlPoint c;
	
	
	private List<Item> currentLevelItems;
	
	private Stack<Integer> stack = new Stack<Integer>();
	
	boolean started = false;
	
	@Override
	public void deviceAdded(Device dev) {
		if ("urn:schemas-upnp-org:device:MediaServer:1".equals(dev.getDeviceType())) {
			DeviceData.getInstance().addDevice(dev);
			Intent i = new Intent(NEW_DEVICES_FOUND);
			sendBroadcast(i);
		}
	}
	
	@Override
	public void deviceRemoved(Device dev) {
		
	}
	
	public List<Item> getCurrentLevelItems() {
		return currentLevelItems;
	}

	public List<Item> getItems(int id) {
		
		if (currentLevelItems!=null && currentLevelItems.size()>0 &&  !stack.empty() && stack.peek().equals(id)) {
			return currentLevelItems;
		}
		if (DeviceData.getInstance().getSelectedDevice()==null) {
			return null;
		}
		
		org.cybergarage.upnp.Service service = DeviceData.getInstance().getSelectedDevice()
		.getService("urn:schemas-upnp-org:service:ContentDirectory:1");
		
		Action action = service.getAction("Browse");
		ArgumentList argumentList = action.getArgumentList();
		argumentList.getArgument("ObjectID").setValue(id);
		argumentList.getArgument("BrowseFlag").setValue("BrowseDirectChildren");
		argumentList.getArgument("StartingIndex").setValue("0");
		argumentList.getArgument("RequestedCount").setValue("0");
		argumentList.getArgument("Filter").setValue("*");
		argumentList.getArgument("SortCriteria").setValue("");

		if (action.postControlAction()) {
			ArgumentList outArgList = action.getOutputArgumentList();
			Argument result = outArgList.getArgument("Result");
			System.out.println("Result:" + result.getValue());
			List<Item> items = parseResult(result);
			if (stack.size()==0 || !stack.peek().equals(id)) {
				stack.push(id);
			}
			currentLevelItems=items;
			return items;
		} else {
			UPnPStatus err = action.getControlStatus();
			System.out.println("Error Code = " + err.getCode());
			System.out.println("Error Desc = " + err.getDescription());
		}
		return null;
	}
	
	public Stack<Integer> getStack() {
		return stack;
	}

	public void initControlPoint() {
		c = new ControlPoint();
		c.addDeviceChangeListener(this);
		c.addSearchResponseListener(new SearchResponseListener() {
			
			@Override
			public void deviceSearchResponseReceived(SSDPPacket ssdpPacket) {
			}
		});
//		c.start();
	}
	
	public void moveUp() {
		stack.pop();
		currentLevelItems=null;
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		initControlPoint();
	}
	/* (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
				
		if (DlnaService.SEARCH_DEVICES.equals(intent.getAction())) {
			refreshDevices();
		}
		else if (DlnaService.RESET_STACK.equals(intent.getAction())) {
			this.stack.clear();
		}
		
		return Service.START_NOT_STICKY;
	}
	
	private void refreshDevices() {
		multicastSearch();
	}

	public void directConnection() {

		Device d = null;
		try {
			try {
				URL url = new URL("http://10.4.53.12:49152/description.xml");
//				URL url = new URL("http://192.168.0.101:49152/description.xml");
				d = new Device(url.openStream());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("loaded:" + d.getFriendlyName());
		} catch (InvalidDescriptionException e2) {
			e2.printStackTrace();
		}

		deviceAdded(d);
	}
	
	@SuppressWarnings("unchecked")
	public void multicastSearch() {
		
		DeviceList deviceList = c.getDeviceList();
		Iterator iterator = deviceList.iterator();
		while (iterator.hasNext()) {
			Device next = (Device)iterator.next();
			System.out.println(next.getFriendlyName());
		}
		if (!started) {
			c.start();
			started = true;
		}
		else {
			c.search();
		}
		

	}

	public void setCurrentLevelItems(List<Item> items) {
		currentLevelItems = items;
	}
}
