package dlna.player.custom;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.ImageView;
import dlna.player.common.AnimationHelper;

/**
 * @author ojalate
 * 
 */
public class PlayerImageView extends ImageView {

	@Override
	public void draw(Canvas canvas) {
		int sc = canvas.save();

		int distance = 0;
		int[] location = new int[2];
		getLocationOnScreen(location);

		int offset = (this.getDrawable().getIntrinsicWidth() + this
				.getPaddingLeft()) / 2;

		int diff = location[0] + offset - AnimationHelper.getDisplayCenter();
		boolean out = false;
		if (diff > 0) {
			out = true;
		}
		distance = Math.abs(diff);

		Transformation t = new Transformation();
		Matrix matrix = t.getMatrix();

		float animStart = this.getDrawable().getIntrinsicWidth();

		if (distance < animStart) {
			float position = animStart - distance;
			float interpolatedTime = position / animStart;
			if (out) {

				doZoomOut(canvas, matrix, position, offset, interpolatedTime,
						animStart);

			} else {

				doZoomIn(canvas, matrix, position, offset, interpolatedTime,
						animStart);

			}
		}

		canvas.concat(matrix);

		super.draw(canvas);
		canvas.restoreToCount(sc);
	}

	private void doZoomIn(Canvas canvas, Matrix matrix, float position,
			int offset, float interpolatedTime, float animStart) {

		AccelerateDecelerateInterpolator ac = new AccelerateDecelerateInterpolator();
		float interpolation = ac.getInterpolation(interpolatedTime);
		float scale = 1.0f + interpolation * 0.5f;
		matrix.postScale(scale, scale);
		canvas.concat(matrix);

		float degrees = 0.0f;

		float animCenter = animStart / 2.0f;

		if (position < animCenter) {
			degrees = 360 - interpolation * 170;
		} else {
			degrees = 360 - (170 - interpolation * 170);
		}

		Camera camera = new Camera();
		camera.save();

		float move = (this.getDrawable().getIntrinsicWidth() * scale - this
				.getDrawable().getIntrinsicWidth()) / 2;
		camera.translate(-move / scale * interpolatedTime, move / scale
				* interpolatedTime, 0.0f);
		camera.rotateY(degrees);

		camera.getMatrix(matrix);

		camera.restore();

		int[] location = new int[2];
		getLocationOnScreen(location);

		float centerX = new Float(this.getWidth() / 2.0f);
		float centerY = new Float(this.getHeight() / 2.0f);
		matrix.preTranslate(-centerX, -centerY);
		matrix.postTranslate(centerX, centerY);

	}

	private void doZoomOut(Canvas canvas, Matrix matrix, float position,
			int offset, float interpolatedTime, float animStart) {

		AccelerateDecelerateInterpolator ac = new AccelerateDecelerateInterpolator();
		float interpolation = ac.getInterpolation(interpolatedTime);
		float scale = 1.0f + interpolation * 0.5f;
		matrix.postScale(scale, scale);
		canvas.concat(matrix);

		float degrees = 0.0f;

		float animCenter = animStart / 2.0f;

		if (position < animCenter) {
			degrees = 360 + 170 * interpolation;
		} else {
			degrees = 360 + (170 - 170 * interpolation);
		}

		Camera camera = new Camera();
		camera.save();
		float move = (this.getDrawable().getIntrinsicWidth() * scale - this
				.getDrawable().getIntrinsicWidth()) / 2;

		camera.rotateY(degrees);

		camera.translate(-move / scale * interpolatedTime, move / scale
				* interpolatedTime, 0.0f);

		camera.getMatrix(matrix);

		camera.restore();

		int[] location = new int[2];
		getLocationOnScreen(location);

		float centerX = new Float(this.getWidth() / 2.0f);
		float centerY = new Float(this.getHeight() / 2.0f);
		matrix.preTranslate(-centerX, -centerY);
		matrix.postTranslate(centerX, centerY);

	}

	/**
	 * @param context
	 */
	public PlayerImageView(Context context) {
		super(context);
	}

}
