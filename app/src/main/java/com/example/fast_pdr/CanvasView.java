package com.example.fast_pdr;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
public class CanvasView extends View{
    public int width;
    public int height;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    Context context;
    private Paint mPaint;
    private float mX, mY;
    private static final float TOLERANCE = 5;
    boolean initialized = false;
    private PointF mPoint = new PointF(5,5);
    private List<PointF> historyPoints = new ArrayList<PointF>();

    // for scaling
    private float scaleFactor = 1.f;
    private ScaleGestureDetector scaleDetector;

    public CanvasView(Context c, AttributeSet attrs) {
        super(c, attrs);
        context = c;

        // we set a new Path
        mPath = new Path();

        // and we set a new Paint with the desired attributes
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(4f);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        // 创建ScaleGestureDetector对象
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    // override onSizeChanged
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        // your Canvas will draw onto the defined Bitmap
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // 让ScaleGestureDetector处理触摸事件
        scaleDetector.onTouchEvent(ev);
        return true;
    }

    // override onDraw
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 在绘制内容之前，缩放画布
        canvas.save();
        canvas.scale(scaleFactor, scaleFactor);
        
        {
            mPaint.setColor(Color.BLACK);
            mPath.moveTo(0, height/2);
            mPath.lineTo(width,height/2);
            mPath.moveTo(width/2, 0);
            mPath.lineTo(width/2, height);
            mPath.moveTo(width/2 + 50, height/2);
            mPath.lineTo(width/2 + 50, height/2 - 4);
            // draw the mPath with the mPaint on the canvas when onDraw
            canvas.drawPath(mPath, mPaint);
            mPaint.setStrokeWidth(1f);
            canvas.drawText("5m", width/2 + 45, height/2 + 14, mPaint);
            mPaint.setStrokeWidth(10);

            float centerX = width / 2f;
            float centerY = height / 2f;

            initialized = true;
            mPaint.setColor(Color.RED);
            canvas.drawPoint(centerX, centerY, mPaint);
            
            // set color to green then
            mPaint.setColor(Color.GREEN);
            for (PointF p : historyPoints){
                canvas.drawPoint(p.x, p.y, mPaint);
            }
        }

        // 在绘制内容之后，恢复画布
        canvas.restore();
        
    }

    public void drawPoint(float x, float y) {
        // mPoint.x = x;
        // mPoint.y = y;
        // 求该点相对于中心点的坐标
        x = (x) + width / 2;
        y = height / 2 - (y);
        historyPoints.add(new PointF(x, y));
        //invalidate();
        postInvalidate();
    }


    public void clearCanvas() {
        mPath.reset();
        historyPoints.clear();
        //invalidate();
        postInvalidate();
    }

    private class ScaleListener extends SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));

            // invalidate();
            postInvalidate();
            return true;
        }
    }


    public void testDraw() {
        if (initialized) {
            drawPoint(5, 5);
            drawPoint(10, 10);
            drawPoint(15, 15);
            drawPoint(20, 20);
            drawPoint(25, 25);
            drawPoint(30, 30);
        }
    }
}
