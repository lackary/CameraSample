package com.lacklab.camera2tool.module;

import android.graphics.Bitmap;
import java.io.File;

/**
 * Created by lackary on 2018/3/26.
 */

public interface Thumbnail {
    void onShowThumbnail(Bitmap bitmap, String filePath);

}
