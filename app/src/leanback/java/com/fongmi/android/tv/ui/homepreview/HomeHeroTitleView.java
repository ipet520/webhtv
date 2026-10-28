package com.fongmi.android.tv.ui.homepreview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/** Native marquee selection belongs to visible text, never to DPAD focus. */
public final class HomeHeroTitleView extends androidx.appcompat.widget.AppCompatTextView {
    private boolean active=true;
    private Long filmIdentity;
    public HomeHeroTitleView(Context context,AttributeSet attributes){super(context,attributes);setFocusable(false);}
    public void setMarqueeActive(boolean active){this.active=active;updateMarquee();}
    /** Metadata callbacks cannot restart the current film's native marquee. */
    public void bindTitle(long identity,CharSequence text){
        if(filmIdentity!=null&&filmIdentity==identity&&android.text.TextUtils.equals(getText(),text))return;
        filmIdentity=identity;
        super.setSelected(false);scrollTo(0,0);super.setText(text,TextView.BufferType.NORMAL);updateMarquee();
    }
    @Override public void setText(CharSequence text,TextView.BufferType type){
        if(type==TextView.BufferType.NORMAL&&text instanceof String&&android.text.TextUtils.equals(getText(),text))return;
        filmIdentity=null;
        super.setSelected(false);scrollTo(0,0);super.setText(text,type);updateMarquee();
    }
    private void updateMarquee(){
        boolean selected=active&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==View.VISIBLE
            &&getLayout()!=null&&getLayout().getLineCount()==1
            &&android.text.Layout.getDesiredWidth(getText(),getPaint())>getWidth()-getCompoundPaddingLeft()-getCompoundPaddingRight();
        if(isSelected()!=selected)super.setSelected(selected);
        if(!selected)scrollTo(0,0);
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){super.onLayout(changed,left,top,right,bottom);updateMarquee();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();updateMarquee();}
    @Override protected void onDetachedFromWindow(){super.setSelected(false);scrollTo(0,0);super.onDetachedFromWindow();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);updateMarquee();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);updateMarquee();}
}
