package com.icmonroe.adaptablecircleview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

/**
 * Created by Ian Monroe on 10/24/2014.
 *
 * Creates a circle view that can be used as a plain circle,
 * as a binary pie chart, or a rounded image view.
 */
public class AdaptableCircleView extends View {

    public AdaptableCircleView(Context context) {
        super(context);
        sharedConstructor();
    }

    public AdaptableCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructor();
    }

    public AdaptableCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        sharedConstructor();
    }

    // Initially does not display percentage
    private float percentage = 0.0f;

    private Paint backgroundPaint;
    private Paint foregroundPaint;
    private Paint imagePaint;
    private RectF paddedRectF;
    private Rect originalRect;
    private BitmapShader shader;
    private float absoluteX;
    private float absoluteY;

    /**
     * Builds the paints for the background, pie fill foreground and image. Rectangles initialized.
     */
    private void sharedConstructor(){
        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Style.FILL);
        backgroundPaint.setColor(Color.WHITE);
        backgroundPaint.setAntiAlias(true);

        foregroundPaint = new Paint();
        foregroundPaint.setStyle(Style.FILL);
        foregroundPaint.setColor(Color.RED);
        foregroundPaint.setAntiAlias(true);

        imagePaint = new Paint();
        imagePaint.setDither(true);
        imagePaint.setAntiAlias(true);
        imagePaint.setFilterBitmap(true);
        imagePaint.setShader(shader);

        paddedRectF = new RectF(0,0,getWidth(),getHeight());
        originalRect = new Rect(0,0,getWidth(),getHeight());
    }

    /**
     * Few modification to onLayout being overridden. Essentially we adjust the rectangle for
     * layout changes and padding adjustments. We also process a user assigned image if here
     * so we can ensure width and height are set before hand.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if(changed){
            paddedRectF.set(
                    getPaddingLeft()+insetPadding,
                    getPaddingTop()+insetPadding,
                    getWidth() - getPaddingRight()-insetPadding,
                    getHeight() - getPaddingBottom()-insetPadding
            );
            if(imageBitmap!=null){
                absoluteX = (getWidth() - imageBitmap.getWidth()) / 2.0f;
                absoluteY = (getHeight() - imageBitmap.getHeight()) / 2.0f;

                shader = new BitmapShader(renderSquareImage(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                imagePaint.setShader(shader);
            }
            originalRect.set(0, 0, getWidth(), getHeight());
        }
    }

    public void setPercentage(float percentage){
        this.percentage = percentage;
        invalidate();
    }

    public void setBackgroundColor(int color){
        backgroundPaint.setColor(color);
        invalidate();
    }

    public void setForegroundColor(int color){
        foregroundPaint.setColor(color);
        invalidate();
    }

    public void setShadow(int radius,int dx, int dy, int color){
        setLayerType(radius==0 ? View.LAYER_TYPE_NONE : View.LAYER_TYPE_SOFTWARE,backgroundPaint);
        backgroundPaint.setShadowLayer(radius,dx,dy,color);
    }

    public static final int FILL_DIRECTION_CLOCKWISE = 1;
    public static final int FILL_DIRECTION_COUNTER_CLOCKWISE = -1;

    private int percentageDirection = FILL_DIRECTION_COUNTER_CLOCKWISE;

    public void setPercentageDirection(int dir){
        percentageDirection = dir;
        invalidate();
    }

    /**
     * Sets the percentage of the circle view with an animation transition for given time
     * @param newPercentage New percentage the pie should file (0.0f - 1.0f);
     * @param animationTime Time animation should take
     */
    public void setPercentage(float newPercentage, int animationTime){
        Animation animation = new PercentageAnimation(newPercentage);
        if(animationInterpolator!=null) animation.setInterpolator(animationInterpolator);
        animation.setDuration(animationTime);
        startAnimation(animation);
    }

    private class PercentageAnimation extends Animation {

        float oldPercent;
        float newPercent;
        float difference;

        public PercentageAnimation(float percent){
            newPercent = percent;
            oldPercent = percentage;
            difference = newPercent - oldPercent;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            setPercentage(oldPercent + difference * interpolatedTime);
        }

        @Override
        public boolean hasEnded() {
            setPercentage(newPercent);
            return super.hasEnded();
        }

    }

    private Interpolator animationInterpolator;

    /**
     * Set animation interpolator for when percentage is animated
     * @param interpolator Animation interpolator to use
     */
    public void setInterpolator(Interpolator interpolator){
        animationInterpolator = interpolator;
    }

    private Bitmap imageBitmap;
    private Bitmap squareImageBitmap;

    /**
     * Adds image into queue. We the view and ensure its height and width then it will be
     * processed.
     * @param image bitmap image you want viewed
     * @param round whether to round the image
     */
    public void setImageBitmap(Bitmap image,boolean round){
        imageBitmap = image;
        displayStoredBitmap();
    }

    public void clearImage() {
        imageBitmap = null;
        invalidate();
    }

    public static final int IMAGE_FILL = 1;
    public static final int IMAGE_CENTER_CROP = 2;
    public static final int IMAGE_ABSOLUTE = 3;

    int imageType = IMAGE_CENTER_CROP;

    public void setImageType(int type){
        imageType = type;
        shader = new BitmapShader(renderSquareImage(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        imagePaint.setShader(shader);
        invalidate();
    }

    private Bitmap renderSquareImage(){
        if(getWidth()>0 && getHeight()>0){
            squareImageBitmap = padBitmap(imageBitmap,imagePadding);
            return squareImageBitmap;
        }
        return imageBitmap;
    }

    private Bitmap padBitmap(Bitmap bitmap,int padding){
        Bitmap paddedBitmap = Bitmap.createBitmap(
                getWidth(),
                getHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(paddedBitmap);
        canvas.drawBitmap(
                bitmap,
                imageType==IMAGE_CENTER_CROP ? squareImageRect(bitmap) : new Rect(0,0,bitmap.getWidth(),bitmap.getHeight()),
                new RectF(padding,padding,getWidth()-padding,getHeight()-padding),
                new Paint(Paint.FILTER_BITMAP_FLAG));
        return paddedBitmap;
    }

    private Rect squareImageRect(Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int halfMin = Math.min(width,height) / 2;

        int left = (width/2) - halfMin;
        int top = (height/2) - halfMin;
        int right = (width/2) + halfMin;
        int bottom = (height/2) + halfMin;

        return new Rect(left,top,right,bottom);
    }

    private void displayStoredBitmap(){
        if(imageBitmap!=null){

            // Handle if scale type is FILL
            shader = new BitmapShader(renderSquareImage(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            imagePaint.setShader(shader);

            // Handle if scale type is NOT FILL
            absoluteX = (getWidth() - imageBitmap.getWidth()) / 2.0f;
            absoluteY = (getHeight() - imageBitmap.getHeight()) / 2.0f;

            invalidate();
        }
    }

    private int largestPadding(){
        return Math.max(Math.max(getPaddingLeft(),getPaddingRight()),Math.max(getPaddingTop(),getPaddingRight()));
    }

    int imagePadding = 0;

    public void setImagePadding(int imagePadding){
        this.imagePadding = imagePadding;
        invalidate();
    }

    private int insetPadding = 0;

    public void setInsetPadding(int padding){
        insetPadding = padding;
        paddedRectF.set(
                getPaddingLeft()+insetPadding,
                getPaddingTop()+insetPadding,
                getWidth() - getPaddingRight()-insetPadding,
                getHeight() - getPaddingBottom()-insetPadding
        );
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw background circle
        canvas.drawCircle(getWidth()/2,getHeight()/2,getWidth()/2 - largestPadding(),backgroundPaint);

        // Draw arc (pie thing) based on current percentage
        float sweepValue = percentageDirection * 360f * percentage;
        canvas.drawArc(paddedRectF,270f,sweepValue,true,foregroundPaint);

        // Draw image if need be
        if(imageBitmap!=null) {
            if(imageType!=IMAGE_ABSOLUTE) canvas.drawCircle(getWidth()/2 ,getHeight()/2,getWidth()/2 - largestPadding() - insetPadding, imagePaint);
            else canvas.drawBitmap(imageBitmap, absoluteX, absoluteY, imagePaint);
        }
    }

}
