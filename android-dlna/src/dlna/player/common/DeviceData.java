package dlna.player.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.cybergarage.upnp.Device;

/**
 * @author ojalate
 * 
 */
public class DeviceData {

	private static final DeviceData instance = new DeviceData();

	private DeviceData() {
	}

	public static DeviceData getInstance() {
		return instance;
	}

	private List<Device> devices = new ArrayList<Device>();
	private Device selectedDevice;

	/**
	 * @return the devices
	 */
	public Device[] getDevices() {
		if (devices.size()==0) {
			return new Device[0];
		}
		return devices.toArray(new Device[]{});
	}

	/**
	 * @param devices
	 *            the devices to set
	 */
	public void setDevices(Device[] devices) {
		this.devices = Arrays.asList(devices);
	}

	public void setDevices(Iterator<Device> iterator) {
		ArrayList<Device> ds = new ArrayList<Device>();
		while (iterator.hasNext()) {
			Device d = iterator.next();
			ds.add(d);
		}
		devices = ds;
	}

	/**
	 * @return the selectedDevice
	 */
	public Device getSelectedDevice() {
		return selectedDevice;
	}

	/**
	 * @param selectedDevice
	 *            the selectedDevice to set
	 */
	public void setSelectedDevice(Device selectedDevice) {
		this.selectedDevice = selectedDevice;
	}

	public CharSequence[] getDeviceNames() {
		CharSequence[] deviceNames = new CharSequence[devices.size()];

		for (int i = 0; i < devices.size(); i++) {
			CharSequence name = devices.get(i).getFriendlyName();
			deviceNames[i] = name;
		}

		return deviceNames;
	}
	
	public void addDevice(Device d) {
		System.out.println("devices size:"+devices.size());
		devices.add(d);
	}
	
}
