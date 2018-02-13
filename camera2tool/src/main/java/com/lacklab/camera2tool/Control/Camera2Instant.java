package com.lacklab.camera2tool.control;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageButton;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.lacklab.camera2tool.utility.CameraTextureView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by lackary on 2016/7/31.
 */
public class Camera2Instant {
    private final String TAG = this.getClass().getSimpleName();

    private static Camera2Instant ourInstance = new Camera2Instant();

    private static final int REQUEST_CAMERA = 1;

    private static final int  REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private Activity cameraActivity;

    private HandlerThread backgroundThread;

    private Handler backgroundHandler;

    private String frontCameraId;

    private String backCameraId;

    private String currentCameraId;

    private List<String> filePathList = new ArrayList<String>();

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    /**
     * A Texture view that handles preview
     */
    //private TextureView cameraTextureView;

    private CameraTextureView cameraTextureView;

    /**
     * A thumbnail
     */
    private ImageButton thumbnailImgBtn;

    /**
     *  An ImageReader that handles still image.
     */
    private ImageReader imageReader;

    /**
     *  An MediaRecorder that handler record video.
     */
    private MediaRecorder mediaRecorder;

    /**
     * The android.util.Size of camera preview.
     */
    private Size previewSize;

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private Size largestPic;

    /**
     * Orientation of the camera sensor
     */
    private int sensorOrientation;

    /**
     * Orientation of the device sensor
     */
    private int deviceOrientation;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore cameraOpenCloseLock = new Semaphore(1);

    /**
     * Camera2 Class
     */
    private CameraManager cameraManager;
    private CameraDevice currentCameraDevice;

    private CameraCharacteristics cameraCharacteristics;

    /**
     * A CameraCaptureSession  for camera preview.
     */
    private CameraCaptureSession captureSession;

    /**
     * CaptureRequest.Builder for the camera preview
     */
    private CaptureRequest.Builder previewRequestBuilder;
    /**
     * CaptureRequest.Builder for the camera still
     */
    private CaptureRequest.Builder stillCaptureRequestBuilder;

    /**
     * CaptureRequest.Builder for the camera record
     */
    private CaptureRequest.Builder recordCaptureRequstBuilder;

    /**
     * CaptureRequest generated by {@link #previewRequestBuilder}
     */
    private CaptureRequest previewRequest;

    private File cameraDir;

    /**
     * the streamConfiguration of Camera Device
     */

    private StreamConfigurationMap streamConfigurationMap;

    private int currentState = STATE_PREVIEW;

    public CameraFeature cameraFeature;

    public static ImageListener imageListener;

    public static Camera2Instant getInstance() {
        return ourInstance;
    }

    private Camera2Instant() {
    }

    //public interface

    public interface ImageListener {
        void onImageByte(byte[] bytes);
    }

    public File getCameraDir() {
        return cameraDir;
    }

    private void setUpCameraPreview(int width, int height) {

        // Find out if we need to swap dimension to get the preview size relative to sensor
        // coordinate.
        int displayRotation = cameraActivity.getWindowManager().getDefaultDisplay().getRotation();
        //noinspection ConstantConditions
        sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        boolean swappedDimensions = false;
        switch (displayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true;
                }
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true;
                }
                break;
            default:
                Log.e(TAG, "Display rotation is invalid: " + displayRotation);
        }

        Point displaySize = new Point();
        cameraActivity.getWindowManager().getDefaultDisplay().getSize(displaySize);
        int rotatedPreviewWidth = width;
        int rotatedPreviewHeight = height;
        int maxPreviewWidth = displaySize.x;
        int maxPreviewHeight = displaySize.y;

        if (swappedDimensions) {
            rotatedPreviewWidth = height;
            rotatedPreviewHeight = width;
            maxPreviewWidth = displaySize.y;
            maxPreviewHeight = displaySize.x;
        }

        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
            maxPreviewWidth = MAX_PREVIEW_WIDTH;
        }

        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
            maxPreviewHeight = MAX_PREVIEW_HEIGHT;
        }

        // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
        // bus' bandwidth limitation, resulting in  previews but the storage of
        // garbage capture data.
        previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class),
                rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                maxPreviewHeight, largestPic);

        // We fit the aspect ratio of TextureView to the size of preview we picked.
        int orientation = cameraActivity.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            cameraTextureView.setAspectRatio(
                    previewSize.getWidth(), previewSize.getHeight());
        } else {
            cameraTextureView.setAspectRatio(
                    previewSize.getHeight(), previewSize.getWidth());
        }

    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.i(TAG, "onSurfaceTextureAvailable width: " + width + " height: " + height);
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            Log.i(TAG, "onSurfaceTextureAvailable width: " + width + " height: " + height);
            //configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            Log.i(TAG, "onSurfaceTextureDestroyed");
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
            // If Preview was started, texture always updated
            //Log.i(TAG, "onSurfaceTextureUpdated");
        }

    };

    /**
     * CameraDevice.StateCallback is called when CameraDevice changes its state.
     */
    private final CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            // This method is called when the camera is opened.  We start camera preview here.
            Log.i(TAG, "onOpened " + camera);
            cameraOpenCloseLock.release();
            currentCameraDevice = camera;
            createCameraPreview(currentCameraDevice);
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraOpenCloseLock.release();
            currentCameraDevice.close();
            currentCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.i(TAG, "onError " + camera + " error " + error);
            cameraOpenCloseLock.release();
            currentCameraDevice.close();
            currentCameraDevice = null;
            if (cameraActivity != null) {
                cameraActivity.finish();
            }
            //Activity activity = getActivity();
            //if (null != activity) {
            //    activity.finish();
            //}
        }
    };

    private CameraCaptureSession.CaptureCallback capturePreviewCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (currentState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }

                case STATE_WAITING_LOCK: {
                    Log.i(TAG, "STATE_WAITING_LOCK");
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        Log.i(TAG, "afState was null");
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        Log.i(TAG, "afState was af focused or not focused");
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            currentState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            Log.i(TAG, "runPreCaptureSequence");
                            //runPreCaptureSequence();
                        }
                    } else if (CaptureResult.CONTROL_AF_MODE_OFF == afState){
                        Log.i(TAG, "afState was off: " + afState);
                        currentState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    } else{
                        Log.i(TAG, "afState was unknown: " + afState);
                    }
                    break;
                }

                case STATE_WAITING_PRECAPTURE: {
                    Log.i(TAG, "STATE_WAITING_PRECAPTURE");
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        currentState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    Log.i(TAG, "STATE_WAITING_NON_PRECAPTURE");
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        currentState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }

            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            //Log.i(TAG, "onCaptureProgressed");
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            //Log.i(TAG,"Picture Saved");
            process(result);
        }
    };

    private CameraCaptureSession.CaptureCallback captureStillCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            //showToast("Saved: " + mFile);
            Log.d(TAG, "still Capture completed");
            unlockFocus();
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            super.onCaptureSequenceAborted(session, sequenceId);
        }

        @Override
        public void onCaptureBufferLost(CameraCaptureSession session, CaptureRequest request, Surface target, long frameNumber) {
            super.onCaptureBufferLost(session, request, target, frameNumber);
        }
    };

    private CameraCaptureSession.StateCallback captureSessionStateCallback =  new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.i(TAG, "captureSessionStateCallback onConfigured");
            if (currentCameraDevice == null) {
                return;
            }
            captureSession = session;
            try {
                previewRequest = previewRequestBuilder.build();
                captureSession.setRepeatingRequest(previewRequest, capturePreviewCallback, backgroundHandler);
            } catch (CameraAccessException e) {
                Log.i(TAG, "captureSessionStateCallback CameraAccessException: " + e);
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.i(TAG, "captureSessionStateCallback onConfigureFailed: " + session);
        }
    };

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener imageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            //Log.i(TAG, "System.currentTimeMillis: " + System.currentTimeMillis());
            //SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            //String currentDateandTime = sdf.format(new Date());
            //Log.i(TAG, "currentDateandTime: " + currentDateandTime);

            //get now time with millisecond
            Calendar calendar = Calendar.getInstance();
            long timeInMillis  = calendar.getTimeInMillis();
            Log.i(TAG, "millisecond: " + timeInMillis);
            File file = new File(cameraDir, "FC_"+ timeInMillis +".jpg");
            backgroundHandler.post(new ImageSaver(reader.acquireNextImage(), file));
        }

    };

    public void setCameraActivity(Activity cameraActivity) {
        this.cameraActivity = cameraActivity;
    }

    public void setCameraTextureView(CameraTextureView textureView ) {
        this.cameraTextureView = textureView;
        Log.i(TAG, "cameraTextureView width: " + cameraTextureView.getWidth() + " height: " + cameraTextureView.getHeight());
    }

    public void initCamera(int lens) {

        cameraManager = (CameraManager) cameraActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId : cameraManager.getCameraIdList()) {
                Log.i(TAG, "Camera Id: " + cameraId);

                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                /*
                Log.i(TAG, "facing: " + facing);
                switch (facing) {
                    case CameraCharacteristics.LENS_FACING_BACK:
                        Log.i(TAG, "LENS_FACING_BACK");
                        backCameraId = cameraId;
                        break;
                    case CameraCharacteristics.LENS_FACING_FRONT:
                        Log.i(TAG, "LENS_FACING_FRONT");
                        frontCameraId = cameraId;
                        break;
                    case CameraCharacteristics.LENS_FACING_EXTERNAL:
                        Log.i(TAG, "LENS_FACING_EXTERNAL");
                        break;
                }
                */
                if (facing != lens) {
                    continue;
                }
                currentCameraId = cameraId;

                streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                // For still image captures, we use the largest available size.
                largestPic = Collections.max(
                        Arrays.asList(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                //imageReader = ImageReader.newInstance(largestPic.getWidth(), largestPic.getHeight(),
                //       ImageFormat.JPEG, /*maxImages*/1);
                //imageReader.setOnImageAvailableListener(
                //        imageAvailableListener, backgroundHandler);


                getCameraAbility(cameraId);

                /*
                Log.i(TAG, "streamConfigurationMap: " + streamConfigurationMap);
                //format
                Size[] surfaceTextureOutputSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
                for(Size size : surfaceTextureOutputSizes) {
                    Log.i(TAG, "Format:Surface map width: " + size.getWidth() + " height: " + size.getHeight());
                }
                Size[] JPEGOutputSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
                for(Size size : JPEGOutputSizes) {
                    Log.i(TAG, "Format:JPEG map width: " + size.getWidth() + " height: " + size.getHeight());
                }

                */
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean isPermissionGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(cameraActivity, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("MissingPermission")
    public void openCamera(int width, int height) {
        Log.i(TAG, "openCamera");
        if (!isPermissionGranted(CAMERA_PERMISSIONS)) {
            requestCameraPermission();
            return;
        }
        setUpCameraPreview(width, height);

        //cameraManager = (CameraManager) cameraActivity.getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            cameraManager.openCamera(cameraFeature.getCameraId(), deviceStateCallback, backgroundHandler);
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
            if (null != currentCameraDevice) {
                currentCameraDevice.close();
                currentCameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    public void resumeCamera() {
        startBackgroundThread();
        imageReader = ImageReader.newInstance(largestPic.getWidth(), largestPic.getHeight(),
                ImageFormat.JPEG, /*maxImages*/1);
        imageReader.setOnImageAvailableListener(
                imageAvailableListener, backgroundHandler);
        previewSize = new Size(cameraTextureView.getWidth(), cameraTextureView.getHeight());
        if (cameraTextureView.isAvailable()) {
            Log.i(TAG, "camera texture view was available ");
            openCamera(cameraTextureView.getWidth(), cameraTextureView.getHeight());
        } else {
            Log.i(TAG, "camera texture view was not available ");
            cameraTextureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    public void pauseCamera() {
        closeCamera();
        stopBackgroundThread();
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        // For Nexus 7 the orientation of front camera was 270
        // For Nexus 7 the orientation of back camera was 90
        Log.i(TAG, "ORIENTATIONS: " + ORIENTATIONS.get(rotation));
        int fixOrientation = -1;
        if (sensorOrientation == 270) {
            switch (rotation) {
                case Surface.ROTATION_90:
                    fixOrientation = 3;
                    break;
                case Surface.ROTATION_270:
                    fixOrientation = 1;
                    break;
                default:
                    fixOrientation = rotation;
            }
        } else {
            fixOrientation = rotation;
        }
        return (ORIENTATIONS.get(fixOrientation) + sensorOrientation) % 360;
    }
    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                   int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.i(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private CameraFeature getCameraAbility(String id) {
        cameraFeature = new CameraFeature();
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(id);
            cameraFeature.setCameraId(id);

            //Log.i(TAG, "streamConfigurationMap: " + streamConfigurationMap);
            cameraFeature.setJEPGFormatSizes(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG));
            // For still image captures, we use the largest available size.
            cameraFeature.setLargestPicture(largestPic);

            //int deviceLevel = cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            //Log.i(TAG, "deviceLevel: " + deviceLevel);
            cameraFeature.setDeviceLevel(cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL));

            //check flash support
            cameraFeature.setFlashSupport(cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE));

            //get AE compensation range
            //Range<Integer> aeRange = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
            //Log.i(TAG, "AE compensation Range: " + aeRange);
            cameraFeature.setAeCompensationRange(cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE));

            //get AE compensation step
            Rational aeStep = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
            Log.i(TAG, "AE compensation step: " + aeStep);

            //get Apertures lens
            float[] lensApertures = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
            Log.i(TAG, "Apertures of lens: " + lensApertures);

            //get auto exposure modes
            int[] AEModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
            Log.i(TAG, "AEModes: " + AEModes.length);
            //get auto focus modes
            int[] AFModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            Log.i(TAG, "AFModes: " + AFModes.length);
            //get the maximum number of metering region
            int MXRAF = cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
            Log.i(TAG, "Max region of AF: " +  MXRAF);
            //get auto white balance modes

            int[] AWBModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
            Log.i(TAG, "AWBModes: " + AWBModes.length);

            //
            //get auto white balance range


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return cameraFeature;
    }


    public void setCameraPath(String folder) {
        if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.i(TAG, "This Phone did not have mount");
            return;
        }
        /*
        if (ContextCompat.checkSelfPermission(cameraActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestWriteExternalStoragePermission();
        }
        */
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        //File dir = this.cameraActivity.getExternalFilesDir(Environment.DIRECTORY_DCIM);
        Log.i(TAG, "Environment dir: " + dir.getPath());
        Log.i(TAG, "Environment dir: " + dir.getAbsolutePath());
        Log.i(TAG, "Environment dir: " + dir.getName());
        cameraDir = new File(dir.getPath() + "/"+ folder);
        //File pictureDir = new File(dir.getPath() + "/"+ folder + "/" + pictureDirName);
        //File videoDir = new File(dir.getPath() + "/" + folder + "/" + videoDirName);
        //this.pictureDir = dir;
        if(!cameraDir.exists()) {
            Log.i(TAG, "This dir is not exist");
            if(!cameraDir.mkdir()) {
                Log.i(TAG, "this dir did not mkdir ");
            }
            Log.i(TAG, "picture dir: " + cameraDir.getPath());
            Log.i(TAG, "picture dir: " + cameraDir.getAbsolutePath());
            Log.i(TAG, "picture dir: " + cameraDir.getName());
        }
    }

    public void capture() {
        CameraCharacteristics characteristics = null;
        try {
            characteristics = cameraManager.getCameraCharacteristics(currentCameraId);
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            Log.i(TAG, "sensorOrientation: " + sensorOrientation);
            int displayRotation = this.cameraActivity.getWindowManager().getDefaultDisplay().getRotation();
            switch (displayRotation) {
                case Surface.ROTATION_0:
                    Log.i(TAG, "ROTATION_0");
                    break;
                case  Surface.ROTATION_90:
                    Log.i(TAG, "ROTATION_90");
                    break;
                case Surface.ROTATION_180:
                    Log.i(TAG, "ROTATION_180");
                    break;
                case Surface.ROTATION_270:
                    Log.i(TAG, "ROTATION_270");
                    break;
            }
            takePicture();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    public void takePicture() {
        lockFocus();
    }

    public void setPictureOrientation(int orientation){
        this.deviceOrientation = orientation;
    }

    public void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (cameraFeature.isFlashSupport()) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    private void createCameraPreview(CameraDevice device) {
        Log.i(TAG, "createCameraPreview");
        SurfaceTexture surfaceTexture = cameraTextureView.getSurfaceTexture();

        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        // This is the output Surface we need to start preview.
        Surface previewSurface = new Surface(surfaceTexture);
        try {
            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(previewSurface);
            // Here, we create a CameraCaptureSession for camera preview.
            device.createCaptureSession(Arrays.asList(previewSurface, imageReader.getSurface()), captureSessionStateCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCameraPreview CameraAccessException: " + e);
            e.printStackTrace();
        }

    }

    public void setListener(ImageListener listener) {
        imageListener = listener;
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
     * Stops the background thread and its Handler.
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

    private boolean shouldShowRequestPermissionsRationale(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(cameraActivity, permission))
                return true;
        }
        return false;
    }

    private void requestCameraPermission() {
        if(shouldShowRequestPermissionsRationale(CAMERA_PERMISSIONS)) {
            //new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            //first time, AndroidManifest must set user-permission
            ActivityCompat.requestPermissions(cameraActivity, CAMERA_PERMISSIONS, REQUEST_CAMERA);
            Log.v(TAG, "requestPermissions");
        }
    }


    private void requestWriteExternalStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(cameraActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            //new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            //first time, AndroidManifest must set user-permission
            ActivityCompat.requestPermissions(cameraActivity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
            Log.v(TAG, "requestPermissions");
        }
    }

    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            currentState = STATE_WAITING_LOCK;
            captureSession.capture(previewRequestBuilder.build(), capturePreviewCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(previewRequestBuilder);
            captureSession.capture(previewRequestBuilder.build(), capturePreviewCallback,
                    backgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            currentState = STATE_PREVIEW;
            captureSession.setRepeatingRequest(previewRequest, capturePreviewCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillPicture() {
        try {

            final Activity activity = this.cameraActivity;
            if (activity == null || currentCameraDevice == null) {
                return;
            }

            // This is the CaptureRequest.Builder that we use to take a picture.
            stillCaptureRequestBuilder =
                    currentCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            stillCaptureRequestBuilder.addTarget(imageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            stillCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            /*
            CameraCaptureSession.CaptureCallback stillCaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    //showToast("Saved: " + mFile);
                    Log.d(TAG, "still Capture completed");
                    unlockFocus();
                }
            };
            */
            // Orientation
            //int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            stillCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(deviceOrientation));

            captureSession.stopRepeating();
            captureSession.capture(stillCaptureRequestBuilder.build(), captureStillCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public File[] getFiles() {
        Log.i(TAG, "get files");
        return this.cameraDir.listFiles();
    }

    public void setThumbnailImgBtn(ImageButton thumbnailImgBtn) {
        this.thumbnailImgBtn = thumbnailImgBtn;
    }

    public void setThumbnail() {
        Log.i(TAG, this.cameraDir.getPath());
        String filePath = this.cameraDir.getPath() + "/" + this.cameraDir.list()[this.cameraDir.list().length - 1];
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);

        this.thumbnailImgBtn.setImageBitmap(bitmap);
    }
    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #captureCallback} from {@link #lockFocus()}.
     */
    /*private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            currentState = STATE_WAITING_PRECAPTURE;
            captureSession.capture(previewRequestBuilder.build(), captureCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }*/

    /**
     * Compares two {@code Size}s based on their areas.
     */
    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {
        private final String TAG = this.getClass().getSimpleName();
        /**
         * The JPEG image
         */
        private final Image image;
        /**
         * The file we save the image into.
         */
        private final File file;

        public ImageSaver(Image image, File file) {
            this.image = image;
            this.file = file;
            Log.i(TAG, "picture File: " + file.getPath());
        }

        @Override
        public void run() {
            Log.i(TAG, "ImageSaver run");
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(file);
                output.write(bytes);
                if(imageListener != null) {
                    imageListener.onImageByte(bytes);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                image.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
}
