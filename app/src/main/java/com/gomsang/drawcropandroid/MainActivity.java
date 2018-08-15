package com.gomsang.drawcropandroid;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.gomsang.drawcropandroid.libs.DrawCropView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final DrawCropView drawCropView = findViewById(R.id.cropView);
        final ImageView imageView = findViewById(R.id.imageView);

        final BitmapDrawable drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.mango);
        final Bitmap bitmap = drawable.getBitmap();

        drawCropView.setImageBitmap(bitmap);
        drawCropView.setOnCropListener(result -> {
            imageView.bringToFront();
            drawCropView.setVisibility(View.GONE);
            imageView.setImageBitmap(result);
        });
    }
}