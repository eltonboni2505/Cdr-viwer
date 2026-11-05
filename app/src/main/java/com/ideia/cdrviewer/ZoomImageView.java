package com.ideia.cdrviewer;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class ZoomImageView extends ImageView {
  private final Matrix matrix=new Matrix();
  private final ScaleGestureDetector detector;
  private float scale=1f,lastX,lastY;
  private int pointers=0;
  public ZoomImageView(Context context){
    super(context);setScaleType(ScaleType.MATRIX);
    detector=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
      @Override public boolean onScale(ScaleGestureDetector d){
        float next=Math.max(1f,Math.min(8f,scale*d.getScaleFactor()));
        float factor=next/scale;scale=next;
        matrix.postScale(factor,factor,d.getFocusX(),d.getFocusY());
        constrain();setImageMatrix(matrix);return true;
      }
    });
  }
  @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);reset();}
  @Override public void setImageBitmap(android.graphics.Bitmap bitmap){super.setImageBitmap(bitmap);post(this::reset);}
  private void reset(){
    Drawable d=getDrawable();if(d==null||getWidth()==0||getHeight()==0)return;
    float fit=Math.min((float)getWidth()/d.getIntrinsicWidth(),(float)getHeight()/d.getIntrinsicHeight());
    matrix.reset();matrix.postScale(fit,fit);
    matrix.postTranslate((getWidth()-d.getIntrinsicWidth()*fit)/2f,(getHeight()-d.getIntrinsicHeight()*fit)/2f);
    scale=1f;setImageMatrix(matrix);
  }
  private void constrain(){
    Drawable d=getDrawable();if(d==null)return;
    android.graphics.RectF rect=new android.graphics.RectF(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());matrix.mapRect(rect);
    float dx=rect.width()<=getWidth()?(getWidth()-rect.width())/2-rect.left:Math.max(getWidth()-rect.right,Math.min(0,-rect.left));
    float dy=rect.height()<=getHeight()?(getHeight()-rect.height())/2-rect.top:Math.max(getHeight()-rect.bottom,Math.min(0,-rect.top));
    matrix.postTranslate(dx,dy);
  }
  @Override public boolean onTouchEvent(MotionEvent event){
    detector.onTouchEvent(event);
    int action=event.getActionMasked();
    if(action==MotionEvent.ACTION_DOWN){lastX=event.getX();lastY=event.getY();pointers=1;return true;}
    if(action==MotionEvent.ACTION_POINTER_DOWN){pointers=2;return true;}
    if(action==MotionEvent.ACTION_MOVE&&!detector.isInProgress()&&event.getPointerCount()==1&&pointers==1){
      if(scale>1.001f){matrix.postTranslate(event.getX()-lastX,event.getY()-lastY);constrain();setImageMatrix(matrix);}
      lastX=event.getX();lastY=event.getY();return true;
    }
    if(action==MotionEvent.ACTION_POINTER_UP){pointers=2;}
    if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){pointers=0;performClick();}
    return true;
  }
  @Override public boolean performClick(){super.performClick();return true;}
}
