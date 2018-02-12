package com.lacklab.camerasample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;

import com.lacklab.camera2tool.control.Camera2Instant;
import com.lacklab.camera2tool.module.ThumbnailInfo;
import com.lacklab.camera2tool.utility.CameraTextureView;
import com.lacklab.camera2tool.utility.DeviceOrientationListener;
import com.lackary.camerasample.R;
import com.lacklab.camera2tool.utility.MediaManager;

public class MainActivity extends Activity implements View.OnClickListener, View.OnLongClickListener{
    private final String TAG = this.getClass().getSimpleName();

    private Camera2Instant cameraInstant;

    private CameraTextureView cameraTextureView;

    private ImageButton captureImgBtn;

    private ImageButton switchImgBtn;

    private ImageButton thumbnailImgBtn;

    private int fromRotation = 0;

    boolean currentFacing = true;

    private void initView() {
        cameraTextureView = (CameraTextureView) findViewById(R.id.camera_preview);
        captureImgBtn = (ImageButton) findViewById(R.id.btn_capture);
        switchImgBtn = (ImageButton) findViewById(R.id.img_btn_switch_camera);
        thumbnailImgBtn = (ImageButton) findViewById(R.id.img_view_thumbnail);
    }

    private void setView () {
        captureImgBtn.setOnClickListener(this);
        captureImgBtn.setOnLongClickListener(this);
        switchImgBtn.setOnClickListener(this);
        thumbnailImgBtn.setOnClickListener(this);
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
        cameraInstant.setThumbnailImgBtn(thumbnailImgBtn);
        //private path /storage/...
        //cameraInstant.setPicturePath(getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        cameraInstant.setCameraPath("FullCamera");
        cameraInstant.initCamera(CameraCharacteristics.LENS_FACING_BACK);
        Log.i(TAG, "application file dir: " + this.getApplicationContext().getFilesDir());
        //Log.i(TAG, "application data dir: " + this.getApplicationContext().getDataDir());
        Log.i(TAG, "application cache dir: " + this.getApplicationContext().getCacheDir());
        getApplicationContext().getObbDir();
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

        //cameraInstant.getFiles();
        //cameraInstant.setThumbnail();
        //MediaManager fileManager = new MediaManager(this.getApplicationContext());
        //ThumbnailInfo thumbnailInfo = fileManager.getLastThumbnailInfo(cameraInstant.getCameraDir());
        //Bitmap bitmap = BitmapFactory.decodeFile(thumbnailInfo.getPath());
        //thumbnailImgBtn.setImageBitmap(bitmap);
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
