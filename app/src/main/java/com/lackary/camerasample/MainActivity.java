package com.lackary.camerasample;

import android.app.Activity;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.lackary.camera2tool.Control.Camera2Instant;
import com.lackary.camera2tool.utility.CameraTextureView;
import com.lackary.camera2tool.utility.DeviceOrientationListener;

public class MainActivity extends Activity implements View.OnClickListener, View.OnLongClickListener{
    private final String TAG = this.getClass().getSimpleName();

    private Camera2Instant cameraInstant;

    private CameraTextureView cameraTextureView;

    private Button captureBtn;

    private ImageButton switchImgBtn;

    private int fromRotation = 0;

    boolean currentFacing = true;

    private void initView() {
        cameraTextureView = (CameraTextureView) findViewById(R.id.camera_preview);
        captureBtn = (Button) findViewById(R.id.btn_capture);
        switchImgBtn = (ImageButton) findViewById(R.id.img_btn_switch_camera);
    }

    private void setView () {
        captureBtn.setOnClickListener(this);
        captureBtn.setOnLongClickListener(this);
        switchImgBtn.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        //camera setting
        cameraInstant = Camera2Instant.getInstance();
        cameraInstant.setCameraActivity(this);
        cameraInstant.setCameraTextureView(cameraTextureView);
        //private path /storage/...
        //cameraInstant.setPicturePath(getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        cameraInstant.setPicturePath("FullCamera");
        cameraInstant.initCamera(CameraCharacteristics.LENS_FACING_BACK);
        //Log.i(TAG, "not currentFacing:" + ~currentFacing);


        DeviceOrientationListener deviceOrientationListener = new DeviceOrientationListener(this) {

            @Override
            public void onDeviceOrientationChanged(int orientation, int clockwise) {
                Log.i(TAG, "orientation: " + orientation);
                MainActivity.this.cameraInstant.setPictureOrientation(orientation);
                int toRotation;
                switch(orientation) {
                    case Surface.ROTATION_0:
                        toRotation = 0;
                        break;
                    case Surface.ROTATION_90:
                        toRotation = 270;
                        break;
                    case Surface.ROTATION_180:
                        toRotation = 180;
                        break;
                    case Surface.ROTATION_270:
                        toRotation = 90;
                        break;
                    default:
                        toRotation = 0;
                }
                Log.i(TAG, "form:" + fromRotation + "to:" + toRotation);
                Log.i(TAG, "clockwise:" + clockwise);
                switch (clockwise) {
                    case ANTI_CLOCKWISE:
                        if(toRotation == Surface.ROTATION_0) {
                            toRotation += 360;
                        }
                        break;
                    case CLOCKWISE:
                        if(fromRotation == Surface.ROTATION_0) {
                            fromRotation += 360;
                        }
                        break;
                    default:
                        break;
                }

                final RotateAnimation rotateAnim = new RotateAnimation(
                        fromRotation, toRotation, switchImgBtn.getWidth()/2, switchImgBtn.getHeight()/2);
                rotateAnim.setDuration(500); // Use 0 ms to rotate instantly
                rotateAnim.setFillAfter(true); // Must be true or the animation will reset
                switchImgBtn.startAnimation(rotateAnim);
                fromRotation = toRotation;

            }
        };

        deviceOrientationListener.enable();


    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        setView();
        cameraInstant.resumeCamera();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        cameraInstant.pauseCamera();
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "capture button click");
        switch (v.getId()) {
            case R.id.btn_capture:
                cameraInstant.capture();
                break;
            case R.id.img_btn_switch_camera:
                cameraInstant.closeCamera();
                cameraInstant.initCamera(currentFacing?CameraCharacteristics.LENS_FACING_FRONT:CameraCharacteristics.LENS_FACING_BACK);
                cameraInstant.resumeCamera();
                currentFacing = !currentFacing;
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        Log.i(TAG, "capture button long click");
        switch (v.getId()) {
            case R.id.btn_capture:
                break;

        }
        return false;
    }

}
