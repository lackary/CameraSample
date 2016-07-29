package com.lackary.camera2tool.feature;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;

/**
 * Created by lackary on 2016/7/24.
 */
public class CameraUtility {
    private final String  TAG = this.getClass().getSimpleName();
    public Size outputSize = null;
    private int outputWidth = 0;
    private int outputHeight = 0;
    private Activity cameraActivity;
    private TextureView cameraTextureView;


    /*Camera2 Class*/
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread backgroundThread;

    /**
     * A Handler for running tasks in the background.
     */
    private Handler backgroundHandler;

    /**
     * An ImageReader that handles still image capture.
     */
    private ImageReader imageReader;


    /**
     * CameraDevice.StateCallback is called when CameraDevice changes its state.
     */
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {

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



    public CameraUtility(Activity activity, Size size, TextureView textureView) {
        this.outputSize = size;
        this.cameraActivity = activity;
        this.cameraTextureView = textureView;
        cameraManager = (CameraManager) cameraActivity.getSystemService(Context.CAMERA_SERVICE);
    }

    public void intiCamera() {

    }



    /*
    public int getCameraModules() {
        try {
            return cameraManager.getCameraIdList().length;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }*/


    private void setPreviewOutput(Size size) {

    }

    public void createCameraPreview(CameraDevice cameraDevice) {

    }

}
