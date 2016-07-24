package com.lackary.camera2tool.feature;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.support.v4.content.ContextCompat;
import android.util.Size;

/**
 * Created by lackary on 2016/7/24.
 */
public class CameraFeature {

    public Size outputSize = null;
    private int outputWidth = 0;
    private int outputHeight = 0;
    private Activity cameraActivity;
    private CameraManager cameraManager;

    public CameraFeature(Activity activity, Size size) {
        this.outputSize = size;
        this.cameraActivity = activity;
        cameraManager = (CameraManager) cameraActivity.getSystemService(Context.CAMERA_SERVICE);
    }

    public void intiCamera() {

    }

    public int getCameraModules() {
        try {
            return cameraManager.getCameraIdList().length;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setPreviewOutput(Size size) {

    }
}
