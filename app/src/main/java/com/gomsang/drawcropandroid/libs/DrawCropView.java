package com.gomsang.drawcropandroid.libs;

import android.app.AlertDialog;
import androidx.lifecycle.SingleGeneratedAdapterObserver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

public class DrawCropView extends View implements View.OnTouchListener {
    private final int SIZE_MAGNIFIER = 200;

    private final int DISTANCE_CONSIDER_CLOESR = 100;
    private final int DISTANCE_MINIMUM = 12;

    private int canvasWidth, canvasHeight;

    private Bitmap targetOriginalBitmap;
    private Bitmap actualVisibleBitmap;

    private ArrayList<Coordinate> drawCoordinates = new ArrayList<>();

    private Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private OnCropListener onCropListener;


    public DrawCropView(Context context) {
        super(context);
        init();
    }

    public DrawCropView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawCropView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        // construct paint for line (draw path)
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
        linePaint.setStrokeWidth(5);
        linePaint.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFF000000);
        linePaint.setColor(Color.WHITE);

        drawCoordinates.clear();
        setOnTouchListener(this);
        invalidate();
    }

    public void setImageBitmap(Bitmap imageBitmap) {
        targetOriginalBitmap = imageBitmap;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvasWidth = canvas.getWidth();
        canvasHeight = canvas.getHeight();

        if (targetOriginalBitmap != null) {
            // If target's original bitmap is bigger than view size, adjust size for fit
            if (actualVisibleBitmap == null)
                actualVisibleBitmap = scaleBitmapAndKeepRation(targetOriginalBitmap, canvas.getHeight(), canvas.getWidth());
            canvas.drawBitmap(actualVisibleBitmap, canvasWidth / 2 - actualVisibleBitmap.getWidth() / 2,
                    canvasHeight / 2 - actualVisibleBitmap.getHeight() / 2, null);
        } else {
            Paint textPaint = new Paint();
            canvas.drawPaint(textPaint);
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(16);
            canvas.drawText("Please set image bitmap for process", canvasWidth, canvasHeight, textPaint);
        }

        if (drawCoordinates.size() > 0) {
            // crop parts for magnify
            canvas.drawBitmap(getMagnifierPart(drawCoordinates.get(drawCoordinates.size() - 1)), 0, 0, null);
            canvas.drawPoint(SIZE_MAGNIFIER / 2, SIZE_MAGNIFIER / 2, linePaint);

            canvas.drawPath(generatePathByCoordinate(drawCoordinates, 1), linePaint);
        }
    }

    public Bitmap getMagnifierPart(Coordinate touchedPoint) {
        Coordinate lastBitmapCoordinate = convertToBitmapSideCoordinate(touchedPoint);

        final Bitmap visibleBitmapWithBorder =
                Bitmap.createBitmap((actualVisibleBitmap.getWidth() + SIZE_MAGNIFIER / 4) * 2, (actualVisibleBitmap.getHeight() + SIZE_MAGNIFIER / 4) * 2, actualVisibleBitmap.getConfig());
        Canvas canvas = new Canvas(visibleBitmapWithBorder);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(actualVisibleBitmap, SIZE_MAGNIFIER / 4, SIZE_MAGNIFIER / 4, null);


        Bitmap magnifyPart = Bitmap.createBitmap(visibleBitmapWithBorder, (int) lastBitmapCoordinate.x, (int) lastBitmapCoordinate.y, SIZE_MAGNIFIER / 2, SIZE_MAGNIFIER / 2);
        magnifyPart = Bitmap.createScaledBitmap(magnifyPart, SIZE_MAGNIFIER, SIZE_MAGNIFIER, true);

        return magnifyPart;
    }

    public static Bitmap scaleBitmapAndKeepRation(Bitmap TargetBmp, int reqHeightInPixels, int reqWidthInPixels) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, TargetBmp.getWidth(), TargetBmp.getHeight()), new RectF(0, 0, reqWidthInPixels, reqHeightInPixels), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(TargetBmp, 0, 0, TargetBmp.getWidth(), TargetBmp.getHeight(), m, true);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // 터치된 좌표를 데이터화 함
        final Coordinate currentCoordinate = new Coordinate();
        currentCoordinate.x = (int) event.getX();
        currentCoordinate.y = (int) event.getY();
        adjustCoordinateForFit(currentCoordinate);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                drawCoordinates.add(currentCoordinate);
                invalidate();
                if (drawCoordinates.size() < 24) break;
                this.invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!(measureDistance(drawCoordinates.get(0), currentCoordinate) < DISTANCE_CONSIDER_CLOESR)) {
                    Toast.makeText(getContext(), "Please put up your hands in closer with first position", Toast.LENGTH_SHORT).show();
                    drawCoordinates.clear();
                    this.invalidate();
                    break;
                }
                if (drawCoordinates.size() < DISTANCE_MINIMUM) {
                    Toast.makeText(getContext(), "Please draw more positions for create sticker", Toast.LENGTH_SHORT).show();
                    drawCoordinates.clear();
                    this.invalidate();
                    break;
                }
                showProduceDialog();
                this.invalidate();
                break;
            default:
                break;
        }

        return true;
    }

    // generate path by coordinates arr
    private Path generatePathByCoordinate(ArrayList<Coordinate> coordinates, int interval) {
        Path path = new Path();
        boolean first = true;
        for (int ci = 0; ci < coordinates.size(); ci += interval) {
            Coordinate coordinateData = coordinates.get(ci);
            if (first) {
                first = false;
                path.moveTo(coordinateData.x, coordinateData.y);
            } else if (ci < coordinates.size() - 1) {
                Coordinate next = coordinates.get(ci + 1);
                path.quadTo(coordinateData.x, coordinateData.y, next.x, next.y);
            } else {
                coordinateData = coordinates.get(ci);
                path.lineTo(coordinateData.x, coordinateData.y);
            }
        }
        return path;
    }

    private int measureDistance(Coordinate targetA, Coordinate targetB) {
        double distance = Math.sqrt(Math.pow(targetA.x - targetB.x, 2) + Math.pow(targetA.y - targetB.y, 2));
        return (int) distance;
    }

    private Coordinate adjustCoordinateForFit(Coordinate targetCoordinate) {
        // if selected coordinate is over the actual visible bitmap's area, adjust it.
        final int targetWidth = actualVisibleBitmap.getWidth();
        final int targetHeight = actualVisibleBitmap.getHeight();
        final int targetStartWidth = (canvasWidth - targetWidth) / 2;
        final int targetStartHeight = (canvasHeight - targetHeight) / 2;

        if (targetCoordinate.x < targetStartWidth)
            targetCoordinate.x = targetStartWidth;
        if (targetCoordinate.x > targetStartWidth + targetWidth)
            targetCoordinate.x = targetStartWidth + targetWidth;
        if (targetCoordinate.y < targetStartHeight)
            targetCoordinate.y = targetStartHeight;
        if (targetCoordinate.y > targetStartHeight + targetHeight)
            targetCoordinate.y = targetStartHeight + targetHeight;

        return targetCoordinate;
    }

    // get coordinates on actual visible bitmap side.
    private Coordinate convertToBitmapSideCoordinate(Coordinate targetCoordinate) {
        targetCoordinate = adjustCoordinateForFit(targetCoordinate);

        final int targetWidth = actualVisibleBitmap.getWidth();
        final int targetHeight = actualVisibleBitmap.getHeight();
        final int targetStartWidth = (canvasWidth - targetWidth) / 2;
        final int targetStartHeight = (canvasHeight - targetHeight) / 2;

        Coordinate bitmapSideCoordinate = new Coordinate();
        bitmapSideCoordinate.x = targetCoordinate.x - targetStartWidth;
        bitmapSideCoordinate.y = targetCoordinate.y - targetStartHeight;
        return bitmapSideCoordinate;
    }

    private void showProduceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Do you proceed like this?");
        builder.setMessage("The image will be generated as you draw.");
        builder.setPositiveButton("Yes",
                (dialog, which) -> {
                    if (onCropListener != null) onCropListener.onCrop(produce());
                    init();
                });
        builder.setNegativeButton("No",
                (dialog, which) -> init());
        builder.show();
    }

    public void setOnCropListener(OnCropListener onCropListener) {
        this.onCropListener = onCropListener;
    }

    public Bitmap produce() {
        Bitmap resultImage = Bitmap.createBitmap(canvasWidth, canvasHeight, actualVisibleBitmap.getConfig());

        final Canvas resultCanvas = new Canvas(resultImage);
        final Paint resultPaint = new Paint();

        // struct paint for naturally
        resultPaint.setAntiAlias(true);
        resultPaint.setDither(true);                    // set the dither to true
        resultPaint.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
        resultPaint.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
        resultPaint.setPathEffect(new CornerPathEffect(10));
        resultPaint.setAntiAlias(true);                         // set anti alias so it smooths

        // struct paint for path-crop
        resultCanvas.drawPath(generatePathByCoordinate(drawCoordinates, 1), resultPaint);
        resultPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        Rect dst = new Rect(canvasWidth / 2 - actualVisibleBitmap.getWidth() / 2,
                canvasHeight / 2 - actualVisibleBitmap.getHeight() / 2,
                canvasWidth / 2 + actualVisibleBitmap.getWidth() / 2,
                canvasHeight / 2 + actualVisibleBitmap.getHeight() / 2);

        resultCanvas.drawBitmap(actualVisibleBitmap, null, dst, resultPaint);

        return BitmapUtil.cropBitmapToBoundingBox(resultImage, Color.TRANSPARENT);
    }

    public class Coordinate {
        float x, y;
    }

    public interface OnCropListener {
        void onCrop(Bitmap result);
    }
}