package nl.lucmulder.watt.lib;


import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import nl.lucmulder.watt.R;

/**
 * Simple single android view component that can be used to showing a round progress bar.
 * It can be customized with size, stroke size, colors and text etc.
 * Progress change will be animated.
 * Created by Kristoffer, http://kmdev.se
 */
public class CircularProgressBar extends View {

    private int mViewWidth;
    private int mViewHeight;

    private final float mStartAngle = -90;      // Always start from top (default is: "3 o'clock on a watch.")
    private float mSweepAngle = 0;              // How long to sweep from mStartAngle
    private float mMaxSweepAngle = 360;         // Max degrees to sweep = full circle
    private int mStrokeWidth = 20;              // Width of outline
    private int mAnimationDuration = 400;       // Animation duration for progress change
    private int mMaxProgress = 100;             // Max progress to use
    private boolean mDrawText = true;
    private boolean mDrawImage = true;  // Set to true if progress text should be drawn
    private boolean mRoundedCorners = true;     // Set to true if rounded corners should be applied to outline ends
    private int mProgressColor = Color.rgb(0, 0, 0); // Outline color
    private int mTextColor = Color.rgb(0, 0, 0);    // Progress text color
    private int mImage = R.drawable.flash;
    private int mProgress = 0;
    private String text = "";                   // Progress text
    private Context context = null;

    private final String TAG = "CircularProgressBar";

    private int offsetY = 13;

    private final Paint mPaint;                 // Allocate paint outside onDraw to avoid unnecessary object creation

    public CircularProgressBar(Context context) {
        this(context, null);
    }

    public CircularProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        initMeasurments();
        drawOutlineArc(canvas);

        if (mDrawImage && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            drawImage(canvas);
        } else {
            mDrawText = true;
        }

        if (mDrawText) {
            drawText(canvas);
        }

        drawArrow(canvas);


    }

    private void initMeasurments() {
        mViewWidth = getWidth();
        mViewHeight = getHeight();
    }

    private void drawOutlineArc(Canvas canvas) {

        final int diameter = Math.min(mViewWidth, mViewHeight) - (mStrokeWidth * 2);

        final RectF outerOval = new RectF(mStrokeWidth, mStrokeWidth+offsetY, diameter, diameter);
        final RectF outerOval2 = new RectF(mStrokeWidth, mStrokeWidth+offsetY, diameter, diameter);

        mPaint.setColor(getResources().getColor(R.color.lightGrey));
        mPaint.setStrokeWidth(3);
        mPaint.setAntiAlias(true);

        canvas.drawArc(outerOval2, 0, 360, false, mPaint);

        mPaint.setColor(mProgressColor);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeCap(mRoundedCorners ? Paint.Cap.ROUND : Paint.Cap.BUTT);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(outerOval, mStartAngle, mSweepAngle, false, mPaint);
    }

    private void drawArrow(Canvas canvas){
        final int diameter = Math.min(mViewWidth, mViewHeight) - (mStrokeWidth * 2);

        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
        b = scaleBitmap(b, mStrokeWidth*3, (mStrokeWidth*3 * b.getHeight()) / b.getWidth(), mSweepAngle);
        int xPos = (int) ((canvas.getWidth() / 2) - (b.getWidth()/2));
        int yPos = (int) ((canvas.getHeight() / 2) - (b.getHeight()/2));

        PointF center = new PointF(xPos-mStrokeWidth/2, yPos-mStrokeWidth/2);
        Log.d(TAG, "Center y " +center.y);
        PointF position = getPosition(center, diameter/2 - mStrokeWidth/2 ,mSweepAngle-90);
        mPaint.setColorFilter(new PorterDuffColorFilter(mProgressColor, PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(b, position.x, position.y, mPaint);
        mPaint.setColorFilter(null);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void drawImage(Canvas canvas) {
        Bitmap b = BitmapFactory.decodeResource(getResources(), mImage);
        b = scaleBitmap(b, ((canvas.getHeight() / 2) * b.getWidth()) / b.getHeight(), (canvas.getHeight() / 2), 0);
        int xPos = (int) ((canvas.getWidth() / 2) - (b.getWidth()/2));
        int yPos = (int) ((canvas.getHeight() / 2) - (b.getHeight()/2));
        canvas.drawBitmap(b, xPos, yPos+offsetY, mPaint);
    }

    private void drawText(Canvas canvas) {
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), "Roboto-Thin.ttf");
        mPaint.setTypeface(typeface);
        float textSize = Math.min(mViewWidth, mViewHeight) / 10f;
        mPaint.setTextSize(textSize);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setStrokeWidth(0);
        mPaint.setColor(mTextColor);

        // Center text
        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) (canvas.getHeight()-(textSize+25));

        canvas.drawText(text, xPos, yPos+offsetY, mPaint);
    }

    private float calcSweepAngleFromProgress(int progress) {
        return (mMaxSweepAngle / mMaxProgress) * progress;
    }

    private int calcProgressFromSweepAngle(float sweepAngle) {
        return (int) ((sweepAngle * mMaxProgress) / mMaxSweepAngle);
    }

    /**
     * Set progress of the circular progress bar.
     *
     * @param progress progress between 0 and 100.
     */
    public void setProgress(int progress) {
        mProgress = progress;
        ValueAnimator animator = ValueAnimator.ofFloat(mSweepAngle, calcSweepAngleFromProgress(progress));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(mAnimationDuration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mSweepAngle = (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    public void setText(String text){
        this.text = text;
        invalidate();
    }

    public void setProgressColor(int color) {
        mProgressColor = color;
        invalidate();
    }

    public void setImage(int imageResource) {
        mImage = imageResource;
        invalidate();
    }

    public void setProgressWidth(int width) {
        mStrokeWidth = width;
        invalidate();
    }

    public void setTextColor(int color) {
        mTextColor = color;
        invalidate();
    }

    public void showProgressText(boolean show) {
        mDrawText = show;
        invalidate();
    }

    /**
     * Toggle this if you don't want rounded corners on progress bar.
     * Default is true.
     *
     * @param roundedCorners true if you want rounded corners of false otherwise.
     */
    public void useRoundedCorners(boolean roundedCorners) {
        mRoundedCorners = roundedCorners;
        invalidate();
    }


    /**
     * decodes a bitmap from a resource id. returns a mutable bitmap no matter what is the API level.<br/>
     * might use the internal storage in some cases, creating temporary file that will be deleted as soon as it isn't finished
     */
    public static Bitmap decodeMutableBitmapFromResourceId(final Context context, final int bitmapResId) {
        final BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            bitmapOptions.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), bitmapResId, bitmapOptions);
        if (!bitmap.isMutable())
            bitmap = convertToMutable(context, bitmap);
        return bitmap;
    }

    public static Bitmap convertToMutable(final Context context, final Bitmap imgIn) {
        final int width = imgIn.getWidth(), height = imgIn.getHeight();
        final Bitmap.Config type = imgIn.getConfig();
        File outputFile = null;
        final File outputDir = context.getCacheDir();
        try {
            outputFile = File.createTempFile(Long.toString(System.currentTimeMillis()), null, outputDir);
            outputFile.deleteOnExit();
            final RandomAccessFile randomAccessFile = new RandomAccessFile(outputFile, "rw");
            final FileChannel channel = randomAccessFile.getChannel();
            final MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes() * height);
            imgIn.copyPixelsToBuffer(map);
            imgIn.recycle();
            final Bitmap result = Bitmap.createBitmap(width, height, type);
            map.position(0);
            result.copyPixelsFromBuffer(map);
            channel.close();
            randomAccessFile.close();
            outputFile.delete();
            return result;
        } catch (final Exception e) {
        } finally {
            if (outputFile != null)
                outputFile.delete();
        }
        return null;
    }


    public static Bitmap scaleBitmap(Bitmap bitmapToScale, float newWidth, float newHeight, float angle) {
        if (bitmapToScale == null)
            return null;
//get the original width and height
        int width = bitmapToScale.getWidth();
        int height = bitmapToScale.getHeight();
// create a matrix for the manipulation
        Matrix matrix = new Matrix();

// resize the bit map

        matrix.setRotate(angle, newWidth/2, newHeight/2);
        matrix.postScale(newWidth / width, newHeight / height);

// recreate the new Bitmap and set it back
        return Bitmap.createBitmap(bitmapToScale, 0, 0, bitmapToScale.getWidth(), bitmapToScale.getHeight(), matrix, true);
    }

    private PointF getPosition(PointF center, float radius, float angle) {

        PointF p = new PointF((float) (center.x + radius * Math.cos(Math.toRadians(angle))),
                (float) (center.y + radius* Math.sin(Math.toRadians(angle))));

        return p;
    }
}