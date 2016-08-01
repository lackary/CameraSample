package com.lackary.camera2tool.feature;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by lackary on 2016/7/31.
 */
public class Camera2Instant {
    private final String TAG = this.getClass().getSimpleName();

    private static Camera2Instant ourInstance = new Camera2Instant();

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private Activity cameraActivity;



    private HandlerThread backgroundThread;

    private Handler backgroundHandler;

    private String frontCameraId;

    private String backCameraId;

    private String currentCameraId;

    private TextureView cameraTextureView;

    //private CameraTextureView cameraTextureView;

    /**
     * The android.util.Size of camera preview.
     */
    private Size previewSize;

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore cameraOpenCloseLock = new Semaphore(1);

    /**
     * Camera2 Class
     */
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;

    /**
     * A CameraCaptureSession  for camera preview.
     */
    private CameraCaptureSession captureSession;

    /**
     * CaptureRequest.Builder for the camera preview
     */
    private CaptureRequest.Builder previewRequestBuilder;

    /**
     * CaptureRequest generated by {@link #previewRequestBuilder}
     */
    private CaptureRequest previewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #captureCallback
     */
    private int currentState = STATE_PREVIEW;

    public static Camera2Instant getInstance() {
        return ourInstance;
    }

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

    /**
     * CameraDevice.StateCallback is called when CameraDevice changes its state.
     */
    private final CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {

        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };

    private CameraCaptureSession.StateCallback captureSessionStateCallback =  new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {

        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    private Camera2Instant() {
    }

    public void setCameraActivity(Activity cameraActivity) {
        this.cameraActivity = cameraActivity;
    }

    public void setCameraTextureView(TextureView textureView ) {
        this.cameraTextureView = textureView;
    }

    public void initCamera() {
        cameraManager = (CameraManager) cameraActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId :  cameraManager.getCameraIdList()) {
                Log.i(TAG, "Camera Id: " + cameraId);
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                Log.i(TAG, "facing: " + facing);
                switch (facing) {
                    case CameraCharacteristics.LENS_FACING_BACK:
                        Log.i(TAG, "LENS_FACING_BACK");
                        backCameraId  = cameraId;
                        break;
                    case CameraCharacteristics.LENS_FACING_FRONT:
                        Log.i(TAG, "LENS_FACING_FRONT");
                        frontCameraId  = cameraId;
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

    public void openCamera(int width, int height) {
        Log.i(TAG, "openCamera");
        if (ContextCompat.checkSelfPermission(cameraActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        CameraManager manager = (CameraManager) cameraActivity.getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(backCameraId, deviceStateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.i(TAG, "CameraAccessException: " + e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    public void closeCamera() {
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

    public void resumeCamera() {
        startBackgroundThread();
        previewSize = new Size(cameraTextureView.getWidth(), cameraTextureView.getHeight());
        if (cameraTextureView.isAvailable()) {
            Log.i(TAG, "camera texture view is available ");
            openCamera(cameraTextureView.getWidth(), cameraTextureView.getHeight());
        } else {
            cameraTextureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    public void pauseCamera() {
        closeCamera();
        stopBackgroundThread();
    }

    private void createCameraPreview(CameraDevice device) {
        Log.i(TAG, "createCameraPreview");
        SurfaceTexture surfaceTexture = cameraTextureView.getSurfaceTexture();

        surfaceTexture.setDefaultBufferSize(cameraTextureView.getWidth(), cameraTextureView.getHeight());
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

    /**
     * Starts a background thread and its Handler.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its  Handler.
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

    private void requestCameraPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(cameraActivity, Manifest.permission.CAMERA)) {
            //new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            //first time, AndroidManifest must set user-permission
            ActivityCompat.requestPermissions(cameraActivity, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            Log.v(TAG, "requestPermissions");
        }
    }
}
