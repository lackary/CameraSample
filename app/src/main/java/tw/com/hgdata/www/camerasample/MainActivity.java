package tw.com.hgdata.www.camerasample;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.LoginFilter;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private final String TAG = this.getClass().getSimpleName();

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private TextureView cameraTxtureVw;

    private Size cameraSize;

    private HandlerThread backgroundThread;

    private Handler backgroundHandler;

    private String FRONT_CAMERA;

    private String BACK_CAMERA;

    private final int FRONT_CAMERA_ID = 0;

    private final int BACK_CAMERA_ID = 1;

    private String  CURRENT_CAMERA_ID;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    //camera2 class
    public CameraDevice cameraDevice;

    private CaptureRequest.Builder previewRequestBuilder;

    private CaptureRequest previewRequest;

    //Camera2 API preview max size
    private int MAX_PREVIEW_WIDTH = 1920;
    private int MAX_PREVIEW_HEIGHT = 1080;

    private final TextureView.SurfaceTextureListener surfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.i(TAG, "onSurfaceTextureAvailable");
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            //configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            createCameraPreview(cameraDevice);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            //mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            //mCameraDevice = null;
            //Activity activity = getActivity();
            //if (null != activity) {
            //    activity.finish();
            //}
        }

    };

    private void requestCameraPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            //new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            //first time, AndroidManifest must set user-permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            Log.v(TAG, "requestPermissions");
        }
    }

    private void initCamera(){
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for(String cameraId :  cameraManager.getCameraIdList()) {
                Log.i(TAG, "Camera Id: " + cameraId);
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                Log.i(TAG, "facing: " + facing);
                switch (facing) {
                    case CameraCharacteristics.LENS_FACING_BACK:
                        Log.i(TAG, "LENS_FACING_BACK");
                        BACK_CAMERA  = cameraId;
                        break;
                    case CameraCharacteristics.LENS_FACING_FRONT:
                        Log.i(TAG, "LENS_FACING_FRONT");
                        FRONT_CAMERA  = cameraId;
                        break;
                    case CameraCharacteristics.LENS_FACING_EXTERNAL:
                        Log.i(TAG, "LENS_FACING_EXTERNAL");
                        break;
                }
                //List<CaptureRequest.Key<?>> captureRequestKeys = cameraCharacteristics.getAvailableCaptureRequestKeys();
                //Log.i(TAG, "captureRequestKeys" + captureRequestKeys.toString());
                //List<CaptureResult.Key<?>> captureResultKeys = cameraCharacteristics.getAvailableCaptureResultKeys();
                //Log.i(TAG, "captureResultKeys:" + captureResultKeys.toString());
                //List<CameraCharacteristics.Key<?>> cameraCharacteristicsKeys = cameraCharacteristics.getKeys();
                //Log.i(TAG, "cameraCharacteristicsKeys: " +cameraCharacteristicsKeys.toString());

                //int[] modes = cameraCharacteristics.get(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES);
                //for(int i : modes) {
                    //Log.i(TAG, "mode: " + i);
                //}
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(int width, int height) {
        Log.i(TAG, "openCamera");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(BACK_CAMERA, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    public void setCameraID(int id) {
        switch (id) {
            case FRONT_CAMERA_ID:
                CURRENT_CAMERA_ID = FRONT_CAMERA;
                break;
            case BACK_CAMERA_ID:
                CURRENT_CAMERA_ID = BACK_CAMERA;
                break;
        }
    }

    public String getCameraID(){
        return CURRENT_CAMERA_ID;
    }

    private void createCameraPreview(CameraDevice device) {
        SurfaceTexture surfaceTexture = cameraTxtureVw.getSurfaceTexture();

        surfaceTexture.setDefaultBufferSize(1280, 720);

        Surface previewSurface = new Surface(surfaceTexture);
        try {
            previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(previewSurface);
            previewRequestBuilder.build();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        } else {

        }

        cameraTxtureVw = (TextureView) findViewById(R.id.camera_preview);
        initCamera();

    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        Log.i(TAG, "cameraTxtureVw width" + cameraTxtureVw.getWidth());
        Log.i(TAG, "cameraTxtureVw height" + cameraTxtureVw.getHeight());
        cameraSize = new Size(cameraTxtureVw.getWidth(), cameraTxtureVw.getHeight());
        if (cameraTxtureVw.isAvailable()) {
            Log.i(TAG, "camera texture view is available ");
            openCamera(1280, 720);
        } else {
            cameraTxtureVw.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                //ErrorDialog.newInstance(getString(R.string.request_permission).show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
