package com.lacklab.camerasample;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import com.lackary.camerasample.R;

/**
 * Created by lackary on 2018/3/27.
 */

public class PhotoActivity extends Activity {
    private ImageView imageView;
    protected Uri uri;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one_photo);
        Intent intent = getIntent();
        String filePath = intent.getStringExtra("filePath");
        uri = Uri.parse(filePath);
        imageView = (ImageView) findViewById(R.id.img_photo);
        imageView.setImageURI(this.uri);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }
}
