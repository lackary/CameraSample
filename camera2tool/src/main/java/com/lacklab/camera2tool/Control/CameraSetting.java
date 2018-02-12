package com.lacklab.camera2tool.control;

/**
 * Created by lackary on 2016/7/29.
 */
public class CameraSetting {
    private String cameraId;
    private String cameraTAG;
    private int picture;
    private String filePath;

    public CameraSetting(String id) {
        this.cameraId = id;
        switch (id) {

        }

    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
