# DrawCropAndroid

This library helps you crop images by hand.

![preview](README.assets/preview.gif)

## What library do.

- Prevent user touch coordinates from going outside the image.
- Dynamic zoom is provided to zoom in on the part the user touches.
- If the user's image is larger than the view, resizing it looks good.

## You should be careful.

We basically resize the image to fit the view size, so the resulting image is all based on the resized image. In other words, if you try to crop a high resolution image (e.g., an image larger than the view size), it will return a resized low resolution image.

## Importing Library

**Step 1.** Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2.** Add the dependency

```gradle
dependencies {
	    implementation 'com.github.gomsang:DrawCropAndroid:0.0.3'
}
```

## How to use

Place the DrawCropView in the desired location.

```xml
<com.gomsang.drawcropandroid.library.DrawCropView
    android:id="@+id/cropView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Cast the added view and specify a bitmap image as follows:

```java
final DrawCropView drawCropView = findViewById(R.id.cropView);

final BitmapDrawable drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.mango);
final Bitmap bitmap = drawable.getBitmap();

drawCropView.setImageBitmap(bitmap);
drawCropView.setOnCropListener(result -> {
    //do what you want.
});
```

It also works with Kotlin code that corresponds to that Java code.

## Additional Settings

```java
// Whether to use a magnifier, the default is true.
setMaginfierEnabled(true);
// Number of coordinates needed to crop image, default value: 12
setMinimumPositions(12);
// Magnifier size, default value: 200
setMagnifierSize(200);
// Size of the part to be enlarged (e.g. the size of the magnifying glass divided by the size of the part to be enlarged), the default value: 40
setMagnifyPartSize(40);
// The maximum distance at which the start and end points are encountered, default: 100
setDistanceCloser(100);

// Paint of touch record line in magnifier, You can customize Paint.
getLinePaint();
// Paint of touch record line in DrawCropView, You can customize Paint.
getLineWithDashPaint();
```

## Reference

https://stackoverflow.com/a/18459072/12141040

I used that answer to design a library base. 