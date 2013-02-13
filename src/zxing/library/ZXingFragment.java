package zxing.library;

import java.io.IOException;

import com.google.zxing.Result;
import com.google.zxing.client.android.FinishListener;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.ViewfinderView;
import com.google.zxing.client.android.camera.CameraManager;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * A fragment that provides all of the UI/processing required to handle barcode
 * decoding
 * 
 * @author kennydude
 * 
 */
public class ZXingFragment extends Fragment implements SurfaceHolder.Callback {
	public ZXingFragment() {
	}

	private static final String TAG = "zxing-frag";
	boolean hasSurface;
	private CameraManager cameraManager;
	private ViewfinderView viewfinderView;
	private FragmentHandler handler;
	private Result savedResultToShow;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_capture, container, false);
	}

	@Override
	public void onStart() {
		super.onStart();

		// This is a good forced option
		Window window = getActivity().getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		hasSurface = false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onResume() {
		super.onResume();

		// Setup camera view
		cameraManager = new CameraManager(getActivity().getApplication());
		cameraManager.setManualFramingRect(getView().getWidth(), getView().getHeight());

		viewfinderView = (ViewfinderView) getView().findViewById(
				R.id.viewfinder_view);
		viewfinderView.setCameraManager(cameraManager);

		SurfaceView surfaceView = (SurfaceView) getView().findViewById(
				R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
	}

	@Override
	public void onPause() {
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		// inactivityTimer.onPause();
		// ambientLightManager.stop();
		cameraManager.closeDriver();
		if (!hasSurface) {
			SurfaceView surfaceView = (SurfaceView) getView().findViewById(
					R.id.preview_view);
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			surfaceHolder.removeCallback(this);
		}
		super.onPause();
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			throw new IllegalStateException("No SurfaceHolder provided");
		}
		if (cameraManager.isOpen()) {
			Log.w(TAG,
					"initCamera() while already open -- late SurfaceView callback?");
			return;
		}
		try {
			cameraManager.openDriver(surfaceHolder);
			// Creating the handler starts the preview, which can also throw a
			// RuntimeException.
			if (handler == null) {
				// TODO: Replace with getArguments()... for custom setups
				handler = new FragmentHandler(this, null, null, cameraManager);
			}
			decodeOrStoreSavedBitmap(null, null);
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			displayFrameworkBugMessageAndExit();
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			Log.w(TAG, "Unexpected error initializing camera", e);
			displayFrameworkBugMessageAndExit();
		}
	}

	private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
		// Bitmap isn't used yet -- will be used soon
		if (handler == null) {
			savedResultToShow = result;
		} else {
			if (result != null) {
				savedResultToShow = result;
			}
			if (savedResultToShow != null) {
				Message message = Message.obtain(handler,
						R.id.decode_succeeded, savedResultToShow);
				handler.sendMessage(message);
			}
			savedResultToShow = null;
		}
	}

	private void displayFrameworkBugMessageAndExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getString(R.string.app_name));
		builder.setMessage(getString(R.string.msg_camera_framework_bug));
		builder.setPositiveButton(R.string.button_ok, new FinishListener(
				getActivity()));
		builder.setOnCancelListener(new FinishListener(getActivity()));
		builder.show();
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surface created");
		if (holder == null) {
			Log.e(TAG,
					"*** WARNING *** surfaceCreated() gave us a null surface!");
		}
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		hasSurface = false;
	}

	public ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

	public CameraManager getCameraManager() {
		return cameraManager;
	}

	public Handler getHandler() {
		return handler;
	}

	DecodeCallback dc = null;

	public void setDecodeCallback(DecodeCallback callback) {
		this.dc = callback;
	}

	public void handleDecode(Result obj, Bitmap barcode, float scaleFactor) {
		if (this.dc != null) {
			this.dc.handleBarcode(obj, barcode, scaleFactor);
		}
	}
	
	public void restartScanning(){
		handler.sendEmptyMessage(R.id.restart_preview);
	}
	
	public void restartScanningIn(int millis){
		handler.sendEmptyMessageDelayed(R.id.restart_preview, millis);
	}

}
