package dlna.player.common;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class Item {
	private String title;
	private String artist;
	private String album;
	private int id;
	private String objectClass;
	private String res;
	private String duration;
	private Drawable smallDrawable;
	private Drawable mediumDrawable;
	private Bitmap mediumBitmap;
	private String coverArtFile;
	
	/**
	 * @return the coverArtFile
	 */
	public String getCoverArtFile() {
		return coverArtFile;
	}

	/**
	 * @param coverArtFile the coverArtFile to set
	 */
	public void setCoverArtFile(String coverArtFile) {
		this.coverArtFile = coverArtFile;
	}

	/**
	 * @return the smallDrawable
	 */
	public Drawable getSmallDrawable() {
		return smallDrawable;
	}

	/**
	 * @param smallDrawable the smallDrawable to set
	 */
	public void setSmallDrawable(Drawable smallDrawable) {
		this.smallDrawable = smallDrawable;
	}

	/**
	 * @return the mediumDrawable
	 */
	public Drawable getMediumDrawable() {
		return mediumDrawable;
	}

	/**
	 * @param mediumDrawable the mediumDrawable to set
	 */
	public void setMediumDrawable(Drawable mediumDrawable) {
		this.mediumDrawable = mediumDrawable;
	}

	public Item() {
	}
	
	public Item(int id, String title, String artist, String album, String objectClass) {
		this.id=id;
		this.title=title;
		this.artist=artist;
		this.album=album;
		this.objectClass=objectClass;
	}
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getArtist() {
		return artist;
	}
	public void setArtist(String artist) {
		this.artist = artist;
	}
	@Override
	public String toString() {
		return artist+":"+title;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setObjectClass(String objectClass) {
		this.objectClass = objectClass;
	}

	public String getObjectClass() {
		return objectClass;
	}

	public void setRes(String res) {
		this.res = res;
	}

	public String getRes() {
		return res;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getAlbum() {
		return album;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getDuration() {
		return duration;
	}

	public void setMediumBitmap(Bitmap mediumBitmap) {
		this.mediumBitmap = mediumBitmap;
	}

	public Bitmap getMediumBitmap() {
		return mediumBitmap;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof Item) {
			System.out.println(this.getTitle()+":"+this.getId()+"=="+((Item) o).getId()+":"+((Item) o).getTitle());
			return this.getId()==((Item) o).getId();
		}
		return false;
	}
	
	
}
