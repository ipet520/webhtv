package com.fongmi.android.tv.ui.homepreview;

import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/** Draw observation only. The existing image owner remains the title-state authority. */
public final class HomeHeroLogoView extends ImageView {
    private Drawable expected;
    private Runnable committed;
    public HomeHeroLogoView(Context context,AttributeSet attributes){super(context,attributes);}
    public void expectDraw(Drawable resource,Runnable callback){expected=resource;committed=callback;}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(committed!=null&&getDrawable()==expected&&expected!=null&&isAttachedToWindow()&&isShown()
                &&getWindowVisibility()==VISIBLE&&getAlpha()>0&&getWidth()>0&&getHeight()>0&&!canvas.getClipBounds().isEmpty()){
            Runnable callback=committed;committed=null;callback.run();
        }
    }
}
