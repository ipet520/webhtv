package com.fongmi.android.tv.ui.homepreview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import com.fongmi.android.tv.theme.ThemeController;

/** Three solid dots. A single native animator runs only while this view is actually visible. */
public final class HomeBrowseLoadingView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ValueAnimator animator=ValueAnimator.ofFloat(0,1);
    private float phase;
    public HomeBrowseLoadingView(Context context) { this(context,null); }
    public HomeBrowseLoadingView(Context context,AttributeSet attrs) {
        super(context,attrs);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        animator.setDuration(900);animator.setRepeatCount(ValueAnimator.INFINITE);animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});refreshTheme();
    }
    public void refreshTheme(){paint.setColor(ThemeController.current().colorOnSurface());invalidate();}
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);float density=getResources().getDisplayMetrics().density;
        float radius=Math.min(5*density,getHeight()/7f),gap=radius*3.5f;
        for(int i=0;i<3;i++) {
            float p=(phase-i*.145f+1)%1;
            float bounce=p<.55f?(float)Math.sin(p/.55f*Math.PI):0;
            canvas.drawCircle(getWidth()/2f+(i-1)*gap,getHeight()/2f-bounce*radius*1.8f,radius,paint);
        }
    }
    private void sync(){if(isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE){if(!animator.isStarted())animator.start();}else animator.cancel();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();sync();}
    @Override protected void onDetachedFromWindow(){animator.cancel();super.onDetachedFromWindow();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(animator!=null)sync();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(animator!=null)sync();}
    public void stop(){animator.cancel();}
}
