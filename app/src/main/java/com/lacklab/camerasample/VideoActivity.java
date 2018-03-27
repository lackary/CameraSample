package com.lacklab.camerasample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.widget.MediaController;
import android.widget.VideoView;

import com.lackary.camerasample.R;

/**
 * Created by lackary on 2018/3/27.
 */

public class VideoActivity extends Activity {
    private VideoView videoView;
    private MediaController playerController;
    protected Uri uri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        Intent intent = getIntent();
        String filePath = intent.getStringExtra("filePath");
        videoView = (VideoView) findViewById(R.id.videoView);
        playerController = new MediaController(this);
        videoView.setMediaController(playerController);
        uri = Uri.parse(filePath);
        videoView.setVideoURI(uri);
        videoView.start();

    }
}
