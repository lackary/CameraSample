package tw.com.hgdata.www.camerasample;

import android.graphics.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for(String cameraId :  cameraManager.getCameraIdList()) {
                Log.i(TAG, "Camera Id: " + cameraId);
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                List<CaptureRequest.Key<?>> captureRequestKeys = cameraCharacteristics.getAvailableCaptureRequestKeys();
                Log.i(TAG, "captureRequestKeys" + captureRequestKeys.toString());
                List<CaptureResult.Key<?>> captureResultKeys = cameraCharacteristics.getAvailableCaptureResultKeys();
                Log.i(TAG, "captureResultKeys:" + captureResultKeys.toString());
                List<CameraCharacteristics.Key<?>> cameraCharacteristicsKeys = cameraCharacteristics.getKeys();
                Log.i(TAG, "cameraCharacteristicsKeys: " +cameraCharacteristicsKeys.toString());

                int[] modes = cameraCharacteristics.get(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES);
                for(int i : modes) {
                    Log.i(TAG, "mode: " + i);
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
