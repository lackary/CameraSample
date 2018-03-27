package com.lacklab.camerasample;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.lackary.camerasample.R;

import java.io.File;

/**
 * Created by lackary on 2018/3/27.
 */

public class PhotoActivity extends Activity {
    private ImageView imageView;
    protected Uri uri;
    protected Context context;
    protected File imageFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one_photo);
        Intent intent = getIntent();
        String filePath = intent.getStringExtra("filePath");
        uri = Uri.parse(filePath);
        imageFile = new File(filePath);
        imageView = (ImageView) findViewById(R.id.img_photo);
        //imageView.setImageURI(this.uri);
        context = getApplicationContext();
        //

    }

    @Override
    protected void onStart() {
        super.onStart();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Glide.with(context).load(imageFile).into(imageView);
            }
        });
    }
}
