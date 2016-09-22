package mg.yra.lib.trackingring;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yvan on 18/08/15.
 */
public class TrackingRingView extends ImageView {
    private boolean haveShadow = true;
    private boolean haveIcon = true;
    private boolean haveColorGradient = true;

    public TrackingRingView(Context context) {
        super(context);
    }

    public TrackingRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TrackingRingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setProgess(float progess1, float progress2, float progress3) {
        ProgressDrawable drawable = new ProgressDrawable(progess1, progress2, progress3);
        setImageDrawable(drawable);
    }



//    public void setDataSet(DataSet dataSet) {
//        mDataSet = dataSet;
//        mDrawable = new ProgressDrawable();
//        setImageDrawable(mDrawable);
//    }

    private static class ProgressDrawable extends Drawable {
        private final static int PI = 180;
        private final static int START_ANGLE = -90;
        private final static float RING_SPACE_RATIO = 1f / 8f;
        private final static float DRAWABLE_PADDING_RATIO = 1f / 8f;
        private final static float TEXT_SIZE_RATIO = 1f / 2f;

        private final static int ANIMATION_DURATION = 800;

        /**
         * Inner circle scale
         */
        private float mInnerCircleScale;
        /**
         * Dat set
         */

        private SparseArray<Float> mProgressValues ;
        private RectF arcElements;
        private Paint paint;
        private float innerRadius;
        private float ringWidth;
        private float ringSpace;
        private Animator animator;

        private float[] progress;
        private Shader[] shaders;

        public ProgressDrawable(float p1, float p2, float p3) {
            mInnerCircleScale = 0.325f;
            progress = new float[3];
            progress[0] = p1;
            progress[1] = p2;
            progress[2] = p3;

            arcElements = new RectF();
            paint = new Paint();
            paint.setAntiAlias(true);
            mProgressValues = new SparseArray<>();
            initValues();
        }

        private void initShaders() {
            if (shaders == null) {
                shaders = new Shader[3];
                int centerX = getBounds().centerX();
                int centerY = getBounds().centerY();

                shaders[0] = new SweepGradient(centerX, centerY,
                        Color.parseColor("#01EDF4"), Color.parseColor("#2CFAAE"));
                shaders[1] = new SweepGradient(centerX, centerY,
                        Color.parseColor("#96FD00"), Color.parseColor("#D5FD35"));
                shaders[2] = new SweepGradient(centerX, centerY,
                        Color.parseColor("#BF0214"), Color.parseColor("#FA308B"));
                rotateShader(shaders[0]);
                rotateShader(shaders[1]);
                rotateShader(shaders[2]);
            }

        }

        private void rotateShader(Shader shader) {
            Matrix matrix = new Matrix();
            matrix.preRotate(START_ANGLE, getBounds().centerX(),  getBounds().centerY());
            shader.setLocalMatrix(matrix);
        }

        private static float getAngleFromProgress(float progress) {
            return 2f * PI / 100f * progress;
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            final boolean changed = super.setVisible(visible, restart);
            if (restart) {
                if (animator == null) {
                    AnimatorSet animatorSet = new AnimatorSet();
                    final List<Animator> animators = new ArrayList<>();
                    animators.add(prepareShowAnimation(0, progress[0]));
                    animators.add(prepareShowAnimation(1, progress[1]));
                    animators.add(prepareShowAnimation(2, progress[2]));
                    animatorSet.playTogether(animators);
                    animator = animatorSet;
                }
                animator.cancel();
                animator.start();
            }
            return changed;
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }

        public void animate() {
            setVisible(true, true);
        }

        @Override
        public void draw(Canvas canvas) {
            final Rect bounds = getBounds();
            initShaders();
            // Different component sizes computation
            final int size = Math.min(bounds.height(), bounds.width());
            innerRadius = size * mInnerCircleScale / 2;

            // Overlayed rings
            if (mProgressValues != null) {
                ringWidth = ((size - 2 * innerRadius) / mProgressValues.size()) / 2;
                ringSpace = ringWidth * RING_SPACE_RATIO;
                drawRingForDataEntry(canvas, 2, Color.parseColor("#66BF0214"), shaders[2], null, "2");
                drawRingForDataEntry(canvas, 1, Color.parseColor("#6697FE00"), shaders[1], null, "1");
                drawRingForDataEntry(canvas, 0, Color.parseColor("#6601EDF4"), shaders[0], null, "0");

            }

        }

        private void initValues() {
            mProgressValues.put(0, 0.0f);
            mProgressValues.put(1, 0.0f);
            mProgressValues.put(2, 0.0f);
        }

        private void drawRingForDataEntry(Canvas canvas, int position, int emptyColor, Shader fillColor,
                                          Drawable drawable, String text) {

            final Rect bounds = getBounds();

            final float arcX0 = bounds.centerX() - innerRadius / 2 - (position + 1) * ringWidth;
            final float arcY0 = bounds.centerY() - innerRadius / 2 - (position + 1) * ringWidth;
            final float arcX = bounds.centerX() + innerRadius / 2 + (position + 1) * ringWidth;
            final float arcY = bounds.centerY() + innerRadius / 2 + (position + 1) * ringWidth;

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(ringWidth);
            paint.setStrokeCap(Paint.Cap.ROUND);
            arcElements.set(arcX0, arcY0, arcX, arcY);


            // Inner plain ring
            paint.setColor(emptyColor);
            canvas.drawArc(arcElements, 0, 2 * PI, false, paint);

            // Inner progress ring
            paint.setShader(fillColor);
            if (position > 0 && position % 2 != 0) {
                paint.setStrokeWidth(ringWidth - ringSpace);
            }
            canvas.drawArc(arcElements, START_ANGLE, getAngleFromProgress(mProgressValues.get(position)), false, paint);
            paint.setShader(null);

            // Drawable
            if (drawable != null) {
                final int left = (int) (bounds.centerX() - ringWidth / 2 + ringWidth * DRAWABLE_PADDING_RATIO);
                final int top = (int) (bounds.centerY() - innerRadius / 2 - (position + 1) * ringWidth - ringWidth / 2 + ringWidth * DRAWABLE_PADDING_RATIO);
                final int right = (int) (left + ringWidth - ringWidth * DRAWABLE_PADDING_RATIO);
                final int bottom = (int) (top + ringWidth - ringWidth * DRAWABLE_PADDING_RATIO);
                drawable.setBounds(left, top, right, bottom);
                drawable.draw(canvas);
            }

            // Text
            if (!TextUtils.isEmpty(text)) {
                final int x = (int) (bounds.centerX() - ringWidth / 2 + ringWidth * DRAWABLE_PADDING_RATIO);
                final int y = (int) (bounds.centerY() - innerRadius / 2 - (position + 1) * ringWidth - ringWidth / 2 + ringWidth * DRAWABLE_PADDING_RATIO + ringWidth / 2);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(ringWidth * TEXT_SIZE_RATIO);
                canvas.drawText(text, x, y, paint);
            }

        }

        private Animator prepareShowAnimation(final int position, final float progress) {
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.0f, progress);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mProgressValues.put(position, (Float) animation.getAnimatedValue());
                    invalidateSelf();
                }
            });
            valueAnimator.setDuration(ANIMATION_DURATION);
            valueAnimator.setInterpolator(new AccelerateInterpolator());
            return valueAnimator;
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            paint.setColorFilter(cf);
        }
    }
}
