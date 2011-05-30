package dlna.player.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.amazon.advertising.api.sample.SignedRequestsHelper;

import dlna.player.activities.AudioPlayerActivity;

/**
 * @author ojalate
 *
 */
public class Tools {
	
	private static final String AWS_ACCESS_KEY_ID = "";

	private static final String AWS_SECRET_KEY = "";

	private static final String ENDPOINT = "ecs.amazonaws.com";

	public static Drawable getDrawable(String imageUrl) {
		if (imageUrl == null) {
			return null;
		}
		try {
			URL url = new URL(imageUrl);
			InputStream is;
			is = (InputStream) url.getContent();
			Drawable d = Drawable.createFromStream(is, "src");
			return d;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Bitmap getBitmap(String imageUrl) {
		if (imageUrl == null) {
			return null;
		}
		try {
			URL url = new URL(imageUrl);
			InputStream is;
			is = (InputStream) url.getContent();
			Bitmap b = BitmapFactory.decodeStream(is);
			return b;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean writeToFile(String playerDir, String imageUrl, String fileName) {
		
		File playerDataDir = new File(playerDir);
		playerDataDir.mkdir();

		File imageFile = new File(playerDataDir, fileName);
		InputStream in = null;
		FileOutputStream out = null;
		
		try {
			imageFile.createNewFile();
			
			out = new FileOutputStream(imageFile);

			URL url = new URL(imageUrl);
			
			in = (InputStream) url.getContent();

			byte buf[] = new byte[16384];
			int total = 0;
			do {

				int numread = in.read(buf);
				total += numread;
				if (numread <= 0) {

					// All reading done
					break;

				} else {

					out.write(buf, 0, numread);
					
				}

			} while (true);
			
//			System.out.println("Read "+total+" bytes");

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		finally {

			try {

				if (out != null) {
					out.close();
					out = null;
				}

			} catch (IOException e) {

				e.printStackTrace();

			}
			
			try {

				if (in != null) {
					in.close();
					in = null;
				}
				

			} catch (IOException e) {

				e.printStackTrace();

			}

		}
		return true;
	}
	
	/**
	 * @param from
	 * @param to
	 * @return
	 */
	public static boolean moveFile(String from, String to) {
		
		File fromFile = new File(from);
		File toFile = new File(to);
		return fromFile.renameTo(toFile);
		
	}

	/**
	 * @param originalImage
	 * @param title
	 * @return
	 */
	public static Bitmap makeReflectiveBitmap(Bitmap originalImage, String title) {

		final int reflectionGap = 4;

		int width = originalImage.getWidth();
		int height = originalImage.getHeight();
		
		// Flip over the Y axis
		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);

		// Create new image from flipped bitmap.
		// Take only the bottom half which is now on top.
		Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0,
				height / 2, width, height / 2, matrix, false);
		
		// Create a new bitmap large enough to fit reflection
		Bitmap bitmapWithReflection = Bitmap.createBitmap(width,
				(height + height / 2), Config.ARGB_8888);

		// Create a new Canvas for the new bitmap and draw the components of the new image
		Canvas canvas = new Canvas(bitmapWithReflection);
		canvas.drawBitmap(originalImage, 0, 0, null);
		Paint deafaultPaint = new Paint();
		canvas.drawRect(0, height, width, height + reflectionGap,
						deafaultPaint);
		canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);

		// Create linear gradient shader for reflection
		Paint paint = new Paint();
		LinearGradient shader = new LinearGradient(0,
				originalImage.getHeight(), 0, bitmapWithReflection.getHeight()
						+ reflectionGap, 0x70ffffff, 0x00ffffff, TileMode.CLAMP);
		paint.setShader(shader);
		paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		canvas.drawRect(0, height, width, bitmapWithReflection.getHeight()
				+ reflectionGap, paint);

		// Add song title to the image
		Paint textPaint = new Paint();
		textPaint.setColor(Color.WHITE);
		textPaint.setTextAlign(Paint.Align.LEFT);
		textPaint.setAntiAlias(true);
		float textSize = 16;
		textPaint.setTextSize(textSize);
		
		// Make sure the text fits inside the image
		float measureText = textPaint.measureText(title);
		float diff = originalImage.getWidth() - measureText;
		while (diff < 0) {
			--textSize;
			textPaint.setTextSize(textSize);
			measureText = textPaint.measureText(title);
			diff = originalImage.getWidth() - measureText;
		}
		canvas.drawText(title, diff / 2, originalImage.getHeight() + 20,
				textPaint);

		return bitmapWithReflection;

	}
	
	/**
	 * @param requestUrl
	 * @param item
	 * @return
	 */
	public static boolean fetchImagesToFile(String requestUrl, Item item) {
		boolean success = false;
		String imageUrl = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			URL url = new URL(requestUrl);
			InputStream is = (InputStream) url.getContent();
			Document doc = db.parse(is);
			
			Node imageNode = doc.getElementsByTagName("MediumImage").item(0);
//			Node imageNode = doc.getElementsByTagName("LargeImage").item(0);
			if (imageNode != null) {
				imageUrl = imageNode.getFirstChild().getFirstChild()
						.getNodeValue();
			}
			if (imageUrl != null) {
//				String[] split = imageUrl.split("\\.");
//				String suffix = split[split.length-1];
				
				String fileName = getFilename(item);
				
				String playerDir = "/sdcard/dlnaplayer";
				
				File file = new File(playerDir+"/"+fileName);
				if (file.exists()) {
					success = true;
				}
				else {
					success = Tools.writeToFile(playerDir, imageUrl, fileName);
				}
				
				if (success) {
					item.setCoverArtFile(playerDir+"/"+fileName);
				}

			}

		} catch (Exception e) {
			Log.e(AudioPlayerActivity.class.getName(), "fetchTitle error: "
					+ e.getMessage());
			e.printStackTrace();
		}
		return success;
	}
	
	/**
	 * @param requestUrl
	 * @param item
	 * @return
	 */
	public static List<Item> fetchAlternativeCovers(String requestUrl, Item item) {
		List<Item> result = new ArrayList<Item>();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();

			URL url = new URL(requestUrl);
			InputStream is = (InputStream) url.getContent();
			Document doc = db.parse(is);
			
			NodeList images = doc.getElementsByTagName("MediumImage");
			
			int imageCount = images.getLength();
			
			Set<String> imageUrls = new LinkedHashSet<String>();
			
			for (int i = 0; i < imageCount; ++i) {
				Node imageNode = images.item(i);
				String imageUrl = imageNode.getFirstChild().getFirstChild().getNodeValue();
				
				if (imageUrl != null) {
					imageUrls.add(imageUrl);
				}
			}
			int i = 0;
			for (String imageUrl : imageUrls) {
				String alternativeDir = "/sdcard/dlnaplayer/alternative";
				String filename = "alternative_"+i+".jpg";
				
				Tools.writeToFile(alternativeDir, imageUrl, filename);
				
				Item coverItem = new Item(i, null, null, null, null);
				coverItem.setRes(alternativeDir+"/"+filename);
				coverItem.setAlbum(item.getAlbum());
				coverItem.setArtist(item.getArtist());
				coverItem.setTitle(item.getTitle());
				result.add(coverItem);
				++i;
			}
			
			

		} catch (Exception e) {
			Log.e(AudioPlayerActivity.class.getName(), "fetchTitle error: "
					+ e.getMessage());
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * @param item
	 * @return
	 */
	public static boolean getCovertArt(Item item) {
		boolean success = false;

		String fileName = getFilename(item);
		
		String playerDir = "/sdcard/dlnaplayer";
		
		File file = new File(playerDir+"/"+fileName);
		if (file.exists()) {
			item.setCoverArtFile(playerDir+"/"+fileName);
			return true;
		}
		
		SignedRequestsHelper helper;
		try {
			helper = SignedRequestsHelper.getInstance(ENDPOINT,
					AWS_ACCESS_KEY_ID, AWS_SECRET_KEY);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		String requestUrl = null;

		if (item.getArtist() != null && item.getAlbum() != null) {
			Map<String, String> params = getAlbumParams(item.getArtist(), item.getAlbum());
			
			requestUrl = helper.sign(params);

			success = Tools.fetchImagesToFile(requestUrl, item);
		}
		
		if (!success && item.getArtist() != null && item.getTitle() != null) {
			Map<String, String> params = getSingleParams(item.getArtist(), item
					.getTitle());

			requestUrl = helper.sign(params);

			success = Tools.fetchImagesToFile(requestUrl, item);
		}

		return success;
	}
	
	/**
	 * @param item
	 * @return
	 */
	public static String getFilename(Item item) {
		String fileName = item.getArtist()+" "+item.getAlbum()+".jpg";
		
		fileName = fileName.replaceAll(" ", "_");
		fileName = fileName.replaceAll(":", "");
		
		return fileName;
	}
	
	/**
	 * @param item
	 * @return
	 */
	public static List<Item> getAlternativeCovers(Item item) {
		
		List<Item> alternativeCovers = new ArrayList<Item>();
		
		SignedRequestsHelper helper;
		try {
			helper = SignedRequestsHelper.getInstance(ENDPOINT,
					AWS_ACCESS_KEY_ID, AWS_SECRET_KEY);
		} catch (Exception e) {
			e.printStackTrace();
			return alternativeCovers;
		}
		
		String requestUrl = null;

		if (item.getArtist() != null && item.getAlbum() != null) {
			Map<String, String> params = getAlbumParams(item.getArtist(), item.getAlbum());
			
			requestUrl = helper.sign(params);

			alternativeCovers = Tools.fetchAlternativeCovers(requestUrl, item);
		}
		
		if ((alternativeCovers == null || alternativeCovers.size()==0) && item.getArtist() != null && item.getTitle() != null) {
			Map<String, String> params = getSingleParams(item.getArtist(), item
					.getTitle());

			requestUrl = helper.sign(params);

			alternativeCovers = Tools.fetchAlternativeCovers(requestUrl, item);
		}

		return alternativeCovers;
	}

	/**
	 * @param artist
	 * @param title
	 * @return
	 */
	private static Map<String, String> getSingleParams(String artist, String title) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("Service", "AWSECommerceService");
		params.put("Version", "2009-03-31");
		params.put("Operation", "ItemSearch");
		params.put("SearchIndex", "Music");
		params.put("Artist", artist);
		params.put("Title", title);
		params.put("ResponseGroup", "Images");
		return params;
	}

	
	/**
	 * @param artist
	 * @param album
	 * @return
	 */
	private static Map<String, String> getAlbumParams(String artist, String album) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("Service", "AWSECommerceService");
		params.put("Version", "2009-03-31");
		params.put("Operation", "ItemSearch");
		params.put("SearchIndex", "Music");
		params.put("Artist", artist);
		params.put("Album", album);
		params.put("ResponseGroup", "Images");
		return params;
	}

	/**
	 * @param coverArtFile
	 * @param title
	 * @param defaultCover
	 * @return
	 */
	public static Bitmap makeBitmapFromFile(String coverArtFile, String title, Bitmap defaultCover) {

		Bitmap b = BitmapFactory.decodeFile(coverArtFile);
		Bitmap reflectiveBitmap = null;
		
		if (b!=null) {
			reflectiveBitmap = Tools.makeReflectiveBitmap(b, title);
		}
		else {
			reflectiveBitmap = Tools.makeReflectiveBitmap(defaultCover, title);
		}
		return reflectiveBitmap;
	}
}
