package com.fongmi.android.tv.ui.homepreview;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.TextView;
import com.fongmi.android.tv.theme.ThemeController;
import com.fongmi.android.tv.theme.ThemeTokens;
import androidx.core.graphics.ColorUtils;

/** Owned native drawables consume the sole App token snapshot, never a separate palette. */
final class HomePreviewStyles {
    private HomePreviewStyles() {}
    static int alpha(int color, float alpha) { return ColorUtils.setAlphaComponent(color, Math.round(255 * Math.max(0, Math.min(1, alpha)))); }
    static void control(TextView view) { control(view, ThemeController.current()); }
    static void control(TextView view, ThemeTokens t) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, shape(view, alpha(t.colorSurfaceContainerHigh(), .92f), t.colorFocus(), 6));
        states.addState(new int[]{}, shape(view, Color.TRANSPARENT, Color.TRANSPARENT, 6));
        view.setBackground(states);
        // Our state-list background is the sole focus decoration, including window handover.
        if (android.os.Build.VERSION.SDK_INT >= 26) view.setDefaultFocusHighlightEnabled(false);
        view.setFocusable(true); view.setFocusableInTouchMode(true); view.setStateListAnimator(null);
        view.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}}, new int[]{t.colorOnSurface(), t.colorOnSurfaceVariant()}));
        view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }
    static void house(View view) { house(view, ThemeController.current()); }
    static void house(View view, ThemeTokens t) {
        StateListDrawable states = new StateListDrawable();
        GradientDrawable focused=shape(view,t.colorPrimary(),t.colorFocus(),0);
        focused.setShape(GradientDrawable.OVAL);
        focused.setStroke(HomePreviewPosterAdapter.dp(view.getContext(),2),t.colorFocus());
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{}, shape(view, Color.TRANSPARENT, Color.TRANSPARENT, 6));
        view.setBackground(states); view.setFocusable(true); view.setFocusableInTouchMode(true); view.setStateListAnimator(null);
        if (android.os.Build.VERSION.SDK_INT >= 26) view.setDefaultFocusHighlightEnabled(false);
        if (view instanceof android.widget.ImageView image) image.setImageTintList(new ColorStateList(
            new int[][]{{android.R.attr.state_focused},{}},new int[]{t.colorOnPrimary(),t.colorOnSurface()}));
    }
    static void function(TextView view) { function(view,ThemeController.current()); }
    static void function(TextView view,ThemeTokens t) {
        StateListDrawable states=new StateListDrawable();
        // Share the App's light overlay palette rather than painting dark button blocks.
        int normal=lightSurface(t,false),focused=lightSurface(t,true);
        states.addState(new int[]{android.R.attr.state_focused},new android.graphics.drawable.LayerDrawable(
            new android.graphics.drawable.Drawable[]{shape(view,focused,Color.TRANSPARENT,20),new Underline(view,t.colorFocus())}));
        states.addState(new int[]{},shape(view,normal,Color.TRANSPARENT,20));
        view.setBackground(states);view.setBackgroundTintList(null);view.setStateListAnimator(null);
        view.setFocusable(true);view.setFocusableInTouchMode(true);
        if(android.os.Build.VERSION.SDK_INT>=26)view.setDefaultFocusHighlightEnabled(false);
        // Global overlay foregrounds remain readable over both light and dark film imagery.
        view.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_focused},{}},
            new int[]{t.colorPlayerControl(),t.colorPlayerControlMuted()}));
        view.setShadowLayer(HomePreviewPosterAdapter.dp(view.getContext(),2),0,1,t.colorShadow());
        view.setScaleX(view.hasFocus()?1.08f:1);view.setScaleY(view.hasFocus()?1.08f:1);
    }
    static int lightSurface(ThemeTokens t,boolean focused) {
        float opacity=(t.colorOverlayLight()>>>24)/255f;
        return alpha(t.colorOverlayLight(),Math.min(1,opacity*(focused?2:1)));
    }
    static void categoryFocus(View view,boolean focused) {
        view.animate().cancel();view.animate().scaleX(focused?1.05f:1).scaleY(focused?1.05f:1).setDuration(120).start();
    }
    static void functionFocus(View view,boolean focused) {
        view.animate().cancel();view.animate().scaleX(focused?1.08f:1).scaleY(focused?1.08f:1).setDuration(120).start();
    }
    /** WOGG uses a centered 18px marker, not a text-length underline. */
    static final class Underline extends android.graphics.drawable.Drawable {
        private final TextView owner;
        private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        Underline(TextView owner,int color){this.owner=owner;paint.setColor(color);}
        int color(){return paint.getColor();}
        @Override public void draw(android.graphics.Canvas canvas){
            android.graphics.Rect b=getBounds();int thickness=HomePreviewPosterAdapter.dp(owner.getContext(),2);
            float available=Math.max(0,b.width()-owner.getCompoundPaddingLeft()-owner.getCompoundPaddingRight());
            android.text.Layout layout=owner.getLayout();
            float textWidth=0;
            if(layout!=null)for(int line=0;line<layout.getLineCount();line++)textWidth=Math.max(textWidth,layout.getLineMax(line));
            else {
                CharSequence text=owner.getText();
                if(owner.getTransformationMethod()!=null)text=owner.getTransformationMethod().getTransformation(text,owner);
                textWidth=android.text.Layout.getDesiredWidth(text,owner.getPaint());
            }
            float width=Math.min(available,Math.min(HomePreviewPosterAdapter.dp(owner.getContext(),12),textWidth*.45f));
            float center=(b.left+owner.getCompoundPaddingLeft()+b.right-owner.getCompoundPaddingRight())*.5f;
            canvas.drawRect(center-width*.5f,b.bottom-thickness*2,center+width*.5f,b.bottom-thickness,paint);
        }
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
        @Override public void setColorFilter(android.graphics.ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
    }
    static void pill(TextView view, boolean primary) { pill(view, primary, ThemeController.current()); }
    static void pill(TextView view, boolean primary, ThemeTokens t) {
        control(view, t);
        if(primary){category(view,t);return;}
        StateListDrawable states = new StateListDrawable();
        GradientDrawable selectedFocus = shape(view, t.colorPrimaryContainer(), t.colorFocus(), 20);
        selectedFocus.setStroke(HomePreviewPosterAdapter.dp(view.getContext(), 2), t.colorFocus());
        states.addState(new int[]{android.R.attr.state_selected, android.R.attr.state_focused}, selectedFocus);
        states.addState(new int[]{android.R.attr.state_focused}, shape(view, t.colorSurfaceContainerHigh(), t.colorFocus(), 20));
        states.addState(new int[]{android.R.attr.state_selected}, shape(view, primary ? t.colorPrimaryContainer() : t.colorPrimary(), primary ? t.colorPrimary() : Color.TRANSPARENT, 20));
        states.addState(new int[]{}, shape(view, alpha(t.colorSurfaceContainer(), primary ? .30f : .78f), Color.TRANSPARENT, 20));
        view.setBackground(states);
        view.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_selected, android.R.attr.state_focused}, {android.R.attr.state_selected}, {}},
                new int[]{t.colorOnPrimaryContainer(), primary ? t.colorOnPrimaryContainer() : t.colorOnPrimary(), t.colorOnSurface()}));
    }
    private static void category(TextView view,ThemeTokens t) {
        view.setBackgroundTintList(null);
        int normal=alpha(t.colorSurfaceContainer(),.30f),selected=alpha(t.colorPrimary(),.28f);
        StateListDrawable states=new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_selected,android.R.attr.state_focused},categoryOutline(view,selected,t));
        states.addState(new int[]{android.R.attr.state_focused},categoryOutline(view,normal,t));
        states.addState(new int[]{android.R.attr.state_selected},shape(view,selected,Color.TRANSPARENT,20));
        states.addState(new int[]{},shape(view,normal,Color.TRANSPARENT,20));
        view.setBackground(states);
        view.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_focused},{android.R.attr.state_selected},{}},
            new int[]{t.colorPlayerControl(),t.colorPlayerControl(),t.colorPlayerControlMuted()}));
        view.setShadowLayer(HomePreviewPosterAdapter.dp(view.getContext(),2),0,1,t.colorShadow());
        view.setScaleX(view.hasFocus()?1.05f:1);view.setScaleY(view.hasFocus()?1.05f:1);
    }
    private static android.graphics.drawable.LayerDrawable categoryOutline(View view,int fill,ThemeTokens t) {
        // A bright neutral outline is independent from the selected theme fill. A thin
        // dark outside rim keeps it distinguishable against a light film backdrop too.
        GradientDrawable contrast=shape(view,fill,alpha(t.colorShadow(),.8f),20);
        contrast.setStroke(HomePreviewPosterAdapter.dp(view.getContext(),3),alpha(t.colorShadow(),.8f));
        GradientDrawable bright=shape(view,Color.TRANSPARENT,t.colorPlayerControl(),20);
        bright.setStroke(HomePreviewPosterAdapter.dp(view.getContext(),2),t.colorPlayerControl());
        var result=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{contrast,bright});
        int inset=Math.max(1,HomePreviewPosterAdapter.dp(view.getContext(),.5f));result.setLayerInset(1,inset,inset,inset,inset);
        return result;
    }
    static GradientDrawable shape(View view, int color, int stroke, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color); drawable.setCornerRadius(HomePreviewPosterAdapter.dp(view.getContext(), radius));
        drawable.setStroke(HomePreviewPosterAdapter.dp(view.getContext(), 1), stroke);
        return drawable;
    }
}
