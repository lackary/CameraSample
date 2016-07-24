package com.lackary.camera2tool.utility;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * Created by lackary on 2016/7/10.
 */
public class AutoSetTextureView extends TextureView {

    private int ratioWidth;
    private int ratioHeight;

    public AutoSetTextureView(Context context) {
        super(context);
    }

    public AutoSetTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoSetTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AutoSetTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /***
     * To  set aspect ration the view
     * @param width
     * @param height
     */
    public void SetAspectRatio(int width, int height) {

        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }

        ratioWidth = width;
        ratioHeight = height;

        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = widthMeasureSpec;
        int height = heightMeasureSpec;

        if (widthMeasureSpec == 0 || heightMeasureSpec == 0) {
            setMeasuredDimension(width, height);
        } else  {
            if (width < height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth);
            } else {
                setMeasuredDimension(height * ratioWidth / ratioHeight, height);
            }
        }
    }
}
