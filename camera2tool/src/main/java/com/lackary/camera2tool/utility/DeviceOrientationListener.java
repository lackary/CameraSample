package com.lackary.camera2tool.utility;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by lackary on 2016/10/9.
 */

public abstract class DeviceOrientationListener extends OrientationEventListener {
    private final String TAG = this.getClass().getSimpleName();

    public static final int CONFIGURATION_ORIENTATION_UNDEFINED = Configuration.ORIENTATION_UNDEFINED;
    public int prevOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private Context context;
    private ReentrantLock lock = new ReentrantLock(true);
    private volatile int defaultScreenOrientation = CONFIGURATION_ORIENTATION_UNDEFINED;

    private int formOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private int relayOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private int toOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;

    private final int ORIENTATION_0 = 0;
    private final int ORIENTATION_90 = 90;
    private final int ORIENTATION_180 = 180;
    private final int ORIENTATION_270 = 270;

    public final int CLOCKWISE = 1;
    public final int ANTI_CLOCKWISE = 0;
    public final int UNKNOWN_CLOCKWISE = -1;

    private int clockwise = -1;

    public DeviceOrientationListener(Context context) {
        super(context);
        this.context = context;
    }

    public DeviceOrientationListener(Context context, int rate) {
        super(context, rate);
        this.context = context;
    }

    @Override
    public void onOrientationChanged(final int orientation) {
        int currentOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;

        toOrientation = orientation;
        if (orientation >= 330 || orientation < 30) {
            currentOrientation = Surface.ROTATION_0;
        } else if (orientation >= 60 && orientation < 120) {
            currentOrientation = Surface.ROTATION_90;
        } else if (orientation >= 150 && orientation < 210) {
            currentOrientation = Surface.ROTATION_180;
        } else if (orientation >= 240 && orientation < 300) {
            currentOrientation = Surface.ROTATION_270;
        } else {
            currentOrientation = prevOrientation;
        }
        if (prevOrientation != currentOrientation && orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            prevOrientation = currentOrientation;

            Log.i(TAG, "clockwise: " + formOrientation + " " + relayOrientation + " " + toOrientation);
            if (currentOrientation != OrientationEventListener.ORIENTATION_UNKNOWN)
                //reportOrientationChanged(currentOrientation);
                //Log.i(TAG, "defaultOrientation:" + getDeviceDefaultOrientation());
                if(toOrientation > formOrientation) {
                    clockwise = CLOCKWISE;
                } else if(toOrientation < formOrientation) {
                    clockwise = ANTI_CLOCKWISE;
                } else {
                    clockwise = UNKNOWN_CLOCKWISE;
                }
                onDeviceOrientationChanged(currentOrientation, clockwise);
                //oldOrientation = currentOrientation;
        }
        formOrientation  = orientation;


    }

    private void reportOrientationChanged(final int currentOrientation) {

        int defaultOrientation = getDeviceDefaultOrientation();
        Log.i(TAG, "defaultOrientation" + defaultOrientation);

        int orthogonalOrientation = defaultOrientation == Configuration.ORIENTATION_LANDSCAPE ? Configuration.ORIENTATION_PORTRAIT
                : Configuration.ORIENTATION_LANDSCAPE;

        int toReportOrientation;

        if (currentOrientation == Surface.ROTATION_0 || currentOrientation == Surface.ROTATION_180)
            toReportOrientation = defaultOrientation;
        else
            toReportOrientation = orthogonalOrientation;

       onDeviceOrientationChanged(toReportOrientation, clockwise);
    }

    /**
     * Must determine what is default device orientation (some tablets can have default landscape). Must be initialized when device orientation is defined.
     *
     * @return value of {@link Configuration#ORIENTATION_LANDSCAPE} or {@link Configuration#ORIENTATION_PORTRAIT}
     */
    private int getDeviceDefaultOrientation() {
        if (defaultScreenOrientation == CONFIGURATION_ORIENTATION_UNDEFINED) {
            lock.lock();
            defaultScreenOrientation = initDeviceDefaultOrientation(context);
            lock.unlock();
        }
        return defaultScreenOrientation;
    }

    /**
     * Provides device default orientation
     *
     * @return value of {@link Configuration#ORIENTATION_LANDSCAPE} or {@link Configuration#ORIENTATION_PORTRAIT}
     */
    private int initDeviceDefaultOrientation(Context context) {

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Configuration config = context.getResources().getConfiguration();
        int rotation = windowManager.getDefaultDisplay().getRotation();

        boolean isLand = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
        boolean isDefaultAxis = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180;

        int result = CONFIGURATION_ORIENTATION_UNDEFINED;
        if ((isDefaultAxis && isLand) || (!isDefaultAxis && !isLand)) {
            result = Configuration.ORIENTATION_LANDSCAPE;
        } else {
            result = Configuration.ORIENTATION_PORTRAIT;
        }
        return result;
    }

    /**
     * Fires when orientation changes from landscape to portrait and vice versa.
     *
     * @param orientation value of {@link Configuration#ORIENTATION_LANDSCAPE} or {@link Configuration#ORIENTATION_PORTRAIT}
     */
    public abstract void onDeviceOrientationChanged(int orientation, int clockwise);

}
