package com.lacklab.camera2tool.utility;

import android.util.Log;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.lacklab.camera2tool.module.ThumbnailInfo;

import java.io.File;

/**
 * Created by lackary on 2018/2/11.
 */

public class MediaManager {
    private final String TAG = this.getClass().getSimpleName();

    private Context context;

    private Cursor imageCursor;

    private Cursor videoCursor;

    private String[] imageThumbnailProject = new String[] {
            MediaStore.Images.Thumbnails._ID,
            MediaStore.Images.Thumbnails.IMAGE_ID,
            MediaStore.Images.Thumbnails.HEIGHT,
            MediaStore.Images.Thumbnails.WIDTH,
            MediaStore.Images.Thumbnails.DATA,
    };

    private String[] videoThumbnailProject = new String[] {
            MediaStore.Video.Thumbnails._ID,
            MediaStore.Video.Thumbnails.VIDEO_ID,
            MediaStore.Video.Thumbnails.HEIGHT,
            MediaStore.Video.Thumbnails.WIDTH,
            MediaStore.Video.Thumbnails.DATA
    };

    public MediaManager(Context context) {
        this.context = context;

    }

    public void getImageFiles(File file) {
        Cursor cursor =  this.context.getContentResolver().query(Uri.parse(file.getPath()),
                imageThumbnailProject, null, null, null);
    }

    public ThumbnailInfo getLastThumbnailInfo(File file) {
        Cursor cursor =  this.context.getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                imageThumbnailProject, null, null, null);
        ThumbnailInfo thumbnailInfo = new ThumbnailInfo();
        if (cursor.moveToLast()) {

            thumbnailInfo.set_id(cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Thumbnails._ID)));
            thumbnailInfo.setImage_id(cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID)));
            thumbnailInfo.setPath(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA)));
            thumbnailInfo.setHeight(cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Thumbnails.HEIGHT)));
            thumbnailInfo.setWidth(cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Thumbnails.WIDTH)));
        }

        return thumbnailInfo;
    }


}
