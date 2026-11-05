package com.ideia.cdrviewer;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.widget.ImageView;

/** Gallery gestures: fit-to-screen, pinch/pan, double-tap zoom and horizontal page swipes. */
public class ZoomImageView extends ImageView {
  private final Matrix imageTransform = new Matrix();
  private final ScaleGestureDetector pinch;
  private final GestureDetector taps;
  private Runnable onTap;
  private java.util.function.IntConsumer onSwipe;
  private float zoom = 1f;
  private float downX, downY, lastX, lastY;
  private boolean multiTouch, moved, doubleTapped;
  private final int slop;

  public ZoomImageView(Context context) {
    super(context);
    setScaleType(ScaleType.MATRIX);
    slop = ViewConfiguration.get(context).getScaledTouchSlop();

    taps = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
      @Override public boolean onDown(MotionEvent e) { return true; }
      @Override public boolean onSingleTapConfirmed(MotionEvent e) {
        if (onTap != null) onTap.run();
        return true;
      }
      @Override public boolean onDoubleTap(MotionEvent e) {
        doubleTapped = true;
        if (zoom > 1.01f) resetZoom();
        else applyZoom(2.5f, e.getX(), e.getY());
        return true;
      }
    });
    pinch = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
      @Override public boolean onScale(ScaleGestureDetector d) {
        applyZoom(zoom * d.getScaleFactor(), d.getFocusX(), d.getFocusY());
        return true;
      }
    });
  }

  public void setGalleryGestures(Runnable tap, java.util.function.IntConsumer swipe) {
    onTap = tap;
    onSwipe = swipe;
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    post(this::resetZoom);
  }

  @Override public void setImageBitmap(android.graphics.Bitmap bitmap) {
    super.setImageBitmap(bitmap);
    post(this::resetZoom);
  }

  private void resetZoom() {
    Drawable d = getDrawable();
    if (d == null || getWidth() <= 0 || getHeight() <= 0 ||
        d.getIntrinsicWidth() <= 0 || d.getIntrinsicHeight() <= 0) return;
    float fit = Math.min((float) getWidth() / d.getIntrinsicWidth(),
                         (float) getHeight() / d.getIntrinsicHeight());
    imageTransform.reset();
    imageTransform.postScale(fit, fit);
    imageTransform.postTranslate((getWidth() - d.getIntrinsicWidth() * fit) / 2f,
                                 (getHeight() - d.getIntrinsicHeight() * fit) / 2f);
    zoom = 1f;
    setImageMatrix(imageTransform);
  }

  private void applyZoom(float requested, float x, float y) {
    if (getDrawable() == null) return;
    float target = Math.max(1f, Math.min(8f, requested));
    if (Math.abs(target - 1f) < .01f) { resetZoom(); return; }
    imageTransform.postScale(target / zoom, target / zoom, x, y);
    zoom = target;
    constrain();
    setImageMatrix(imageTransform);
  }

  private void constrain() {
    Drawable d = getDrawable();
    if (d == null) return;
    RectF rect = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
    imageTransform.mapRect(rect);
    float dx = rect.width() <= getWidth()
        ? (getWidth() - rect.width()) / 2f - rect.left
        : Math.max(getWidth() - rect.right, Math.min(0f, -rect.left));
    float dy = rect.height() <= getHeight()
        ? (getHeight() - rect.height()) / 2f - rect.top
        : Math.max(getHeight() - rect.bottom, Math.min(0f, -rect.top));
    imageTransform.postTranslate(dx, dy);
  }

  @Override public boolean onTouchEvent(MotionEvent event) {
    int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_DOWN) {
      downX = lastX = event.getX();
      downY = lastY = event.getY();
      multiTouch = moved = doubleTapped = false;
      if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
    } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
      multiTouch = true;
    }

    pinch.onTouchEvent(event);
    // Feed single-finger events to the detector for tap and double-tap recognition.
    if (!multiTouch && event.getPointerCount() == 1) taps.onTouchEvent(event);

    if (action == MotionEvent.ACTION_MOVE && event.getPointerCount() == 1 && !multiTouch) {
      float x = event.getX(), y = event.getY();
      if (Math.hypot(x - downX, y - downY) > slop) moved = true;
      if (zoom > 1.01f && moved) {
        imageTransform.postTranslate(x - lastX, y - lastY);
        constrain();
        setImageMatrix(imageTransform);
      }
      lastX = x; lastY = y;
    } else if (action == MotionEvent.ACTION_UP) {
      float dx = event.getX() - downX, dy = event.getY() - downY;
      if (!multiTouch && !doubleTapped && zoom <= 1.01f && onSwipe != null &&
          Math.abs(dx) > Math.max(slop * 4f, getWidth() * .12f) &&
          Math.abs(dx) > Math.abs(dy) * 1.35f) {
        onSwipe.accept(dx < 0 ? 1 : -1);
      }
      if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
    } else if (action == MotionEvent.ACTION_CANCEL) {
      if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
    }
    return true;
  }

  @Override public boolean performClick() {
    super.performClick();
    return true;
  }
}
