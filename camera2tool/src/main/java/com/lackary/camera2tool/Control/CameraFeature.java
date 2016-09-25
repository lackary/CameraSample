package com.lackary.camera2tool.Control;

import android.util.Range;
import android.util.Rational;
import android.util.Size;

/**
 * Created by lackary on 2016/8/28.
 */
public class CameraFeature {

    private String cameraId;
    private int deviceLevel;
    private Size[] JEPGFormatSizes;
    private boolean isFlashSupport;
    private Range<Integer> aeCompensationRange;
    private Rational aeCompensationStep;
    private int[] aeModes;
    private int[] afModes;
    private int[] awbModes;
    private float[] lensApertures;
    private Size largestPicture;

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public int getDeviceLevel() {
        return deviceLevel;
    }

    public void setDeviceLevel(int deviceLevel) {
        this.deviceLevel = deviceLevel;
    }

    public boolean isFlashSupport() {
        return isFlashSupport;
    }

    public void setFlashSupport(boolean flashSupport) {
        isFlashSupport = flashSupport;
    }

    public Size[] getJEPGFormatSizes() {
        return JEPGFormatSizes;
    }

    public void setJEPGFormatSizes(Size[] JEPGFormatSizes) {
        this.JEPGFormatSizes = JEPGFormatSizes;
    }

    public Range<Integer> getAeCompensationRange() {
        return aeCompensationRange;
    }

    public void setAeCompensationRange(Range<Integer> aeCompensationRange) {
        this.aeCompensationRange = aeCompensationRange;
    }

    public Rational getAeCompensationStep() {
        return aeCompensationStep;
    }

    public void setAeCompensationStep(Rational aeCompensationStep) {
        this.aeCompensationStep = aeCompensationStep;
    }

    public int[] getAeModes() {
        return aeModes;
    }

    public void setAeModes(int[] aeModes) {
        this.aeModes = aeModes;
    }

    public int[] getAfModes() {
        return afModes;
    }

    public void setAfModes(int[] afModes) {
        this.afModes = afModes;
    }

    public int[] getAwbModes() {
        return awbModes;
    }

    public void setAwbModes(int[] awbModes) {
        this.awbModes = awbModes;
    }

    public float[] getLensApertures() {
        return lensApertures;
    }

    public void setLensApertures(float[] lensApertures) {
        this.lensApertures = lensApertures;
    }

    public Size getLargestPicture() {
        return largestPicture;
    }

    public void setLargestPicture(Size largestPicture) {
        this.largestPicture = largestPicture;
    }
}
