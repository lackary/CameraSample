package tw.com.hgdata.www.camerasample;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.lackary.camera2tool.feature.CameraUtility;

public class MainActivity extends Activity {
    private final String TAG = this.getClass().getSimpleName();

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private TextureView cameraTxtureVw;

    private CameraUtility cameraUtility;

    private Size cameraSize;

    private HandlerThread backgroundThread;

    private Handler backgroundHandler;

    private String FRONT_CAMERA;

    private String BACK_CAMERA;

    private final int FRONT_CAMERA_ID = 0;

    private final int BACK_CAMERA_ID = 1;

    private String  CURRENT_CAMERA_ID;

    private Semaphore cameraOpenCloseLock = new Semaphore(1);

    //camera2 class
    public CameraDevice cameraDevice;

    private CaptureRequest.Builder previewRequestBuilder;

    private CaptureRequest previewRequest;

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * A CameraCaptureSession  for camera preview.
     */
    private CameraCaptureSession captureSession;

    /**
     * An ImageReader that handles still image capture.
     */
    private ImageReader imageReader;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #captureCallback
     */
    private int state = STATE_PREVIEW;

    //Camera2 API preview max size
    private int MAX_PREVIEW_WIDTH = 1920;
    private int MAX_PREVIEW_HEIGHT = 1080;

    private final TextureView.SurfaceTextureListener surfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.i(TAG, "onSurfaceTextureAvailable");
            Log.i(TAG, "width: " + width);
            Log.i(TAG, "height: "+ height);
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            //configureTransform(width, height);
            Log.i(TAG, "onSurfaceTextureSizeChanged");
            Log.i(TAG, "width: " + width);
            Log.i(TAG, "height: "+ height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    private final CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            Log.i(TAG, "onOpened " + cameraDevice);
            cameraOpenCloseLock.release();
            createCameraPreview(cameraDevice);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            cameraDevice = null;
            //Activity activity = getActivity();
            //if (null != activity) {
            //    activity.finish();
            //}
        }

    };

    private CameraCaptureSession.StateCallback captureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.i(TAG, "captureSessionStateCallback onConfigured");
            captureSession = session;
            try {
                previewRequest = previewRequestBuilder.build();
                captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
            } catch (CameraAccessException e) {
                Log.i(TAG, "captureSessionStateCallback CameraAccessException: " + e);
            }

        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.i(TAG, "captureSessionStateCallback onConfigureFailed: " + session);
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
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
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

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback captureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (state) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                /*
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
                */
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    private void openCamera(int width, int height) {
        Log.i(TAG, "openCamera");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(BACK_CAMERA, deviceStateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.i(TAG, "CameraAccessException: " + e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (null != captureSession) {
                captureSession.close();
                captureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            /*if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }*/
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
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

    private void createCameraPreview(CameraDevice device) {
        Log.i(TAG, "createCameraPreview");
        SurfaceTexture surfaceTexture = cameraTxtureVw.getSurfaceTexture();

        surfaceTexture.setDefaultBufferSize(cameraTxtureVw.getWidth(), cameraTxtureVw.getHeight());
        // This is the output Surface we need to start preview.
        Surface previewSurface = new Surface(surfaceTexture);
        try {
            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(previewSurface);
            // Here, we create a CameraCaptureSession for camera preview.
            device.createCaptureSession(Arrays.asList(previewSurface), captureSessionStateCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCameraPreview CameraAccessException: " + e);
            e.printStackTrace();
        }

    }

    public String getCameraID(){
        return CURRENT_CAMERA_ID;
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
            openCamera(cameraTxtureVw.getWidth(), cameraTxtureVw.getHeight());
        } else {
            cameraTxtureVw.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
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
