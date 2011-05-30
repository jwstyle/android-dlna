package dlna.player.common;

/**
 * @author ojalate
 *
 */
public class AnimationHelper {

	private static long animationPosition = 0;
	private static int displayWidth = 0;
	/**
	 * @param animationPosition the animationPosition to set
	 */
	public static void setAnimationPosition(long animationPosition) {
		AnimationHelper.animationPosition = animationPosition;
	}

	/**
	 * @return the animationPosition
	 */
	public static long getAnimationPosition() {
		return animationPosition;
	}
	

	/**
	 * @param displayWidth the displayWidth to set
	 */
	public static void setDisplayWidth(int displayWidth) {
		AnimationHelper.displayWidth = displayWidth;
	}

	/**
	 * @return the displayWidth
	 */
	public static int getDisplayWidth() {
		return displayWidth;
	}

	public static int getDisplayCenter() {
		return displayWidth/2;
	}
}
