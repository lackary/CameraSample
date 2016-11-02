package tw.com.hgdata.www.camerasample;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.lackary.camera2tool.Control.Camera2Instant;
import com.lackary.camera2tool.utility.DeviceOrientationListener;

public class MainActivity extends Activity implements SensorEventListener2,  View.OnClickListener, View.OnLongClickListener{
    private final String TAG = this.getClass().getSimpleName();

    private static final int SENSOR_DELAY = 500 * 1000; // 500ms
    private static final int FROM_RADS_TO_DEGS = -57;
    private Camera2Instant cameraInstant;

    private TextureView cameraTextureView;

    private Button captureBtn;

    private TextView pitchTxt;

    private TextView rollTxt;

    private SensorManager sensorManager;

    private Sensor rotationSensor;
    /*
    private DeviceOrientationListener deviceOrientationListener = new DeviceOrientationListener(this) {

        @Override
        public void onDeviceOrientationChanged(int orientation) {
            Log.i(TAG, "orientation: " + orientation);
        }
    };
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraTextureView = (TextureView) findViewById(R.id.camera_preview);
        captureBtn = (Button) findViewById(R.id.btn_capture);
        captureBtn.setOnClickListener(this);
        pitchTxt =  (TextView) findViewById(R.id.txt_pitch);
        rollTxt = (TextView) findViewById(R.id.txt_roll);

        cameraInstant = Camera2Instant.getInstance();
        cameraInstant.setCameraActivity(this);
        cameraInstant.setCameraTextureView(cameraTextureView);
        //private path /storage/...
        //cameraInstant.setPicturePath(getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        cameraInstant.setPicturePath("FullCamera");
        cameraInstant.initCamera(CameraCharacteristics.LENS_FACING_FRONT);

        sensorManager = (SensorManager) getSystemService(Activity.SENSOR_SERVICE);

        rotationSensor =  sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        sensorManager.registerListener(this, rotationSensor, SENSOR_DELAY);

        //deviceOrientationListener.enable();


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
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor == rotationSensor) {
            if(event.values.length > 4) {
                float[] truncatedRotationVector = new float[4];
                System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
                update(truncatedRotationVector);
            } else {
                update(event.values);
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void update(float[] vectors) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors);
        int worldAxisX = SensorManager.AXIS_X;
        int worldAxisZ = SensorManager.AXIS_Z;
        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedRotationMatrix);
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);
        float pitch = orientation[1] + FROM_RADS_TO_DEGS;
        float roll = orientation[2] + FROM_RADS_TO_DEGS;
        pitchTxt.setText("Pitch: "+pitch);
        rollTxt.setText("Roll: "+roll);
    }

}
