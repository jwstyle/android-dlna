package dlna.player.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ojalate
 * 
 */
public class PlaylistHelper {

	private static final PlaylistHelper instance = new PlaylistHelper();

	private PlaylistHelper() {
	}

	public static PlaylistHelper getInstance() {
		return instance;
	}
	
	private Set<Integer> playListIds = new HashSet<Integer>();
	private List<Item> playList = new ArrayList<Item>();

	/**
	 * @return the playListIds
	 */
	public Set<Integer> getPlayListIds() {
		return playListIds;
	}

	/**
	 * @param playListIds
	 *            the playListIds to set
	 */
	public void setPlayListIds(Set<Integer> playListIds) {
		this.playListIds = playListIds;
	}

	/**
	 * @return the playList
	 */
	public List<Item> getPlayList() {
		return playList;
	}

	/**
	 * @param playList
	 *            the playList to set
	 */
	public void setPlayList(List<Item> playList) {
		this.playList = playList;
	}

	public void addItem(Item item) {
		int id = item.getId();
		if (!playListIds.contains(id)) {
			playListIds.add(id);
			playList.add(item);
		}
	}

	public void removeItem(Item item) {
		int id = item.getId();
		if (playListIds.contains(id)) {
			playListIds.remove(id);
			playList.remove(item);
		}
	}
}
