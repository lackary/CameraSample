package tw.com.hgdata.www.camerasample;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
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

    private void initView() {
        
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraTextureView = (CameraTextureView) findViewById(R.id.camera_preview);
        captureBtn = (Button) findViewById(R.id.btn_capture);
        captureBtn.setOnClickListener(this);



        cameraInstant = Camera2Instant.getInstance();
        cameraInstant.setCameraActivity(this);
        cameraInstant.setCameraTextureView(cameraTextureView);
        //private path /storage/...
        //cameraInstant.setPicturePath(getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        cameraInstant.setPicturePath("FullCamera");
        cameraInstant.initCamera(CameraCharacteristics.LENS_FACING_BACK);


        DeviceOrientationListener deviceOrientationListener = new DeviceOrientationListener(this) {

            @Override
            public void onDeviceOrientationChanged(int orientation) {
                Log.i(TAG, "orientation: " + orientation);
                MainActivity.this.cameraInstant.setPictureOrientation(orientation);
            }
        };

        deviceOrientationListener.enable();


    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
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
        switch (v.getId()) {
            case R.id.btn_capture:
                cameraInstant.capture();
                break;
            case R.id.img_btn_switch_camera:
                cameraInstant.closeCamera();
                cameraInstant.initCamera(CameraCharacteristics.LENS_FACING_FRONT);

                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

}
