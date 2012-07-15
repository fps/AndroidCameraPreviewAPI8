package io.fps.cameratest;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

public class Main extends Activity implements SurfaceHolder.Callback {
	static String logTag = Main.class.toString();

	/*
	 * first is width, second is height. This is set by the onWindowFocusChanged
	 * callback below, as that is the first time when we know the size of the
	 * relative layout
	 */
	Pair<Integer, Integer> layoutSize = null;

	Camera camera = null;
	Camera.Size previewSize = null;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.main);

		((SurfaceView) findViewById(R.id.surface)).getHolder()
				.addCallback(this);

		/*
		 * This is necessary because we target Api level 8 (android 2.2). Later
		 * versions make this unnecessary
		 */
		((SurfaceView) findViewById(R.id.surface)).getHolder().setType(
				SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(logTag, "onResume");

		if (null == camera) {
			camera = Camera.open();
			
			Camera.Parameters parameters = camera.getParameters();

			/*
			 * Get the list of supported preview sizes and sort them in
			 * ascending order according to their respective pixel counts
			 */
			List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
			Collections.sort(sizes, new Comparator<Camera.Size>() {

				@Override
				public int compare(Camera.Size lhs, Camera.Size rhs) {
					if (lhs.width * lhs.height > rhs.width * rhs.height) {
						return 1;
					}
					if (lhs.width * lhs.height < rhs.width * rhs.height) {
						return -1;
					}
					return 0;
				}
			});

			/*
			 * Take the smallest preview size. You might need something else
			 * here. We store the size we choose in the member variable so
			 * it is available when the surface was created
			 */
			previewSize = sizes.get(0);
			parameters.setPreviewSize(previewSize.width, previewSize.height);

			camera.setParameters(parameters);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(logTag, "onPause");

		if (null != camera) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(logTag, "surfaceChanged");

		try {

			/*
			 * Resize the surface view such that it fills the layout but keeps
			 * the aspect ratio
			 */
			SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface);
			ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();

			double widthFactor = (double) layoutSize.first
					/ (double) previewSize.width;

			double heightFactor = (double) layoutSize.second
					/ (double) previewSize.height;

			double scaleFactor = Math.max(widthFactor, heightFactor);

			layoutParams.width = (int) Math.ceil(scaleFactor
					* previewSize.width);
			layoutParams.height = (int) Math.ceil(scaleFactor
					* previewSize.height);

			Log.d(logTag, "new size: " + layoutParams.width + " "
					+ layoutParams.height);

			surfaceView.setLayoutParams(layoutParams);

			findViewById(R.id.layout).requestLayout();

			camera.setPreviewDisplay(holder);
			camera.startPreview();
		} catch (Exception e) {
			e.printStackTrace();

			/*
			 * If something went wrong, release the camera...
			 */
			camera.release();
			camera = null;
		}

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(logTag, "surfaceCreated");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(logTag, "surfaceDestroyed");
	}

	/*
	 * We use this callback as a hack, since at this point the layout pass has
	 * been done and we can determine the size of the layout (which we need to
	 * scale the surface view with the fitting aspect ratio.
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if (false == hasFocus) {
			return;
		}

		View layout = findViewById(R.id.layout);
		layoutSize = new Pair<Integer, Integer>(layout.getWidth(),
				layout.getHeight());

		Log.d(logTag, "onWindowFocusChanged. layout size: " + layoutSize.first
				+ " " + layoutSize.second);
	}
}
