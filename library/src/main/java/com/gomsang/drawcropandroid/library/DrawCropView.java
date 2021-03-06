package com.gomsang.drawcropandroid.library;

import android.app.AlertDialog;
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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DrawCropView extends View implements View.OnTouchListener {
    private boolean maginfierEnabled = true;

    private int SIZE_MAGNIFIER = 200;
    private int SIZE_PART_MAGNIFY = 40;

    private int DISTANCE_CONSIDER_CLOSER = 100;
    private int POSITIONS_MINIMUM = 12;

    private Paint lineWithDashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int viewCanvasWidth, viewCanvasHeight;

    private Bitmap targetOriginalBitmap;
    private Bitmap actualVisibleBitmap;

    private ArrayList<Coordinate> drawCoordinates = new ArrayList<>();

    private OnCropListener onCropListener;

    // --- library UserSide ---
    public void setMagnifierSize(int SIZE_MAGNIFIER) {
        this.SIZE_MAGNIFIER = SIZE_MAGNIFIER;
    }

    public void setMagnifyPartSize(int SIZE_PART_MAGNIFY) {
        this.SIZE_PART_MAGNIFY = SIZE_PART_MAGNIFY;
    }

    public void setMaginfierEnabled(boolean maginfierEnabled) {
        this.maginfierEnabled = maginfierEnabled;
    }
    public void setDistanceCloser(int DISTANCE_CONSIDER_CLOSER) {
        this.DISTANCE_CONSIDER_CLOSER = DISTANCE_CONSIDER_CLOSER;
    }

    public void setMinimumPositions(int POSITIONS_MINIMUM) {
        this.POSITIONS_MINIMUM = POSITIONS_MINIMUM;
    }

    public Paint getLineWithDashPaint() {
        return lineWithDashPaint;
    }

    public Paint getLinePaint() {
        return linePaint;
    }
    // --- library UserSide ---

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
        lineWithDashPaint.setStyle(Paint.Style.STROKE);
        lineWithDashPaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
        lineWithDashPaint.setStrokeWidth(5);
        lineWithDashPaint.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFF000000);
        lineWithDashPaint.setColor(Color.WHITE);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3);
        linePaint.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFF000000);
        linePaint.setColor(Color.WHITE);

        drawCoordinates.clear();
        setOnTouchListener(this);
        invalidate();
    }

    public void setImageBitmap(Bitmap imageBitmap) {
        targetOriginalBitmap = imageBitmap;
        actualVisibleBitmap = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        viewCanvasWidth = canvas.getWidth();
        viewCanvasHeight = canvas.getHeight();

        if (targetOriginalBitmap != null) {
            // If target's original bitmap is bigger than view size, adjust size for fit
            if (actualVisibleBitmap == null)
                actualVisibleBitmap = scaleBitmapAndKeepRation(targetOriginalBitmap, canvas.getHeight(), canvas.getWidth());
            canvas.drawBitmap(actualVisibleBitmap, viewCanvasWidth / 2 - actualVisibleBitmap.getWidth() / 2,
                    viewCanvasHeight / 2 - actualVisibleBitmap.getHeight() / 2, null);
        } else {
            Paint textPaint = new Paint();
            canvas.drawPaint(textPaint);
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(16);
            canvas.drawText("Please set image bitmap for process", viewCanvasWidth, viewCanvasHeight, textPaint);
        }

        if (drawCoordinates.size() > 0) {
            // crop parts for magnify
//            canvas.drawBitmap(getMagnifierPart(drawCoordinates.get(drawCoordinates.size() - 1)), 0, 0, null);
            if (maginfierEnabled) {
                canvas.drawBitmap(Bitmap.createScaledBitmap(getMagnifierPart(drawCoordinates.get(drawCoordinates.size() - 1)), SIZE_MAGNIFIER, SIZE_MAGNIFIER, false)
                        , 0, 0, null);
                canvas.drawPoint(SIZE_MAGNIFIER / 2, SIZE_MAGNIFIER / 2, lineWithDashPaint);
            }
            canvas.drawPath(genPathByCoordinate(drawCoordinates, 1), lineWithDashPaint);
        }
    }

    public Bitmap getMagnifierPart(Coordinate touchedPoint) {
        // adjust view coordinate to bitmap side coordinate
        Coordinate touchedPointOnBitmap = new Coordinate();
        touchedPointOnBitmap.x = touchedPoint.x - (viewCanvasWidth / 2 - actualVisibleBitmap.getWidth() / 2);
        touchedPointOnBitmap.y = touchedPoint.y - (viewCanvasHeight / 2 - actualVisibleBitmap.getHeight() / 2);


        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        Bitmap bmp = Bitmap.createBitmap(actualVisibleBitmap.getWidth() + SIZE_PART_MAGNIFY * 2,
                actualVisibleBitmap.getHeight() + SIZE_PART_MAGNIFY * 2, conf);


        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.BLACK);


        Rect dst = new Rect(canvas.getWidth() / 2 - actualVisibleBitmap.getWidth() / 2,
                canvas.getHeight() / 2 - actualVisibleBitmap.getHeight() / 2,
                canvas.getWidth() / 2 + actualVisibleBitmap.getWidth() / 2,
                canvas.getHeight() / 2 + actualVisibleBitmap.getHeight() / 2);

        canvas.drawBitmap(actualVisibleBitmap, null, dst, null);
        canvas.drawPath(genPathByCoordinate(genCoordinatesForMagnifier(drawCoordinates), 1), linePaint);


        Bitmap magnifyPart =
                Bitmap.createBitmap(bmp, (int) touchedPointOnBitmap.x,
                        (int) touchedPointOnBitmap.y, SIZE_PART_MAGNIFY * 2, SIZE_PART_MAGNIFY * 2);
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
                if (!(measureDistance(drawCoordinates.get(0), currentCoordinate) < DISTANCE_CONSIDER_CLOSER)) {
                    Toast.makeText(getContext(), "Please put up your hands in closer with first position", Toast.LENGTH_SHORT).show();
                    drawCoordinates.clear();
                    this.invalidate();
                    break;
                }
                if (drawCoordinates.size() < POSITIONS_MINIMUM) {
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
    private Path genPathByCoordinate(ArrayList<Coordinate> coordinates, int interval) {
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

    private ArrayList<Coordinate> genCoordinatesForMagnifier(ArrayList<Coordinate> coordinates) {
        ArrayList<Coordinate> additionCoordinates = new ArrayList<>();

        for (Coordinate coordinate : coordinates) {
            Coordinate modifiedCoordinate = new Coordinate();
            modifiedCoordinate.x = coordinate.x;
            modifiedCoordinate.y = coordinate.y;
            modifiedCoordinate = convertToBitmapSideCoordinate(modifiedCoordinate);
            modifiedCoordinate.x += SIZE_PART_MAGNIFY;
            modifiedCoordinate.y += SIZE_PART_MAGNIFY;

            additionCoordinates.add(modifiedCoordinate);
        }

        return additionCoordinates;
    }

    private Coordinate adjustCoordinateForFit(Coordinate targetCoordinate) {
        // if selected coordinate is over the actual visible bitmap's area, adjust it.
        final int targetWidth = actualVisibleBitmap.getWidth();
        final int targetHeight = actualVisibleBitmap.getHeight();
        final int targetStartWidth = (viewCanvasWidth - targetWidth) / 2;
        final int targetStartHeight = (viewCanvasHeight - targetHeight) / 2;

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

        Coordinate bitmapSideCoordinate = new Coordinate();
        bitmapSideCoordinate.x = targetCoordinate.x - (viewCanvasWidth / 2 - actualVisibleBitmap.getWidth() / 2);
        bitmapSideCoordinate.y = targetCoordinate.y - (viewCanvasHeight / 2 - actualVisibleBitmap.getHeight() / 2);
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
        Bitmap resultImage = Bitmap.createBitmap(viewCanvasWidth, viewCanvasHeight, actualVisibleBitmap.getConfig());

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
        resultCanvas.drawPath(genPathByCoordinate(drawCoordinates, 1), resultPaint);
        resultPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        Rect dst = new Rect(viewCanvasWidth / 2 - actualVisibleBitmap.getWidth() / 2,
                viewCanvasHeight / 2 - actualVisibleBitmap.getHeight() / 2,
                viewCanvasWidth / 2 + actualVisibleBitmap.getWidth() / 2,
                viewCanvasHeight / 2 + actualVisibleBitmap.getHeight() / 2);

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