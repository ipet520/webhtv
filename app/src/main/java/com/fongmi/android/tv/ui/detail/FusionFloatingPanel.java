package com.fongmi.android.tv.ui.detail;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.theme.ThemeController;
import com.fongmi.android.tv.theme.ThemeTokens;
import com.github.catvod.utils.Prefers;
import com.google.android.material.card.MaterialCardView;
import static com.fongmi.android.tv.ui.detail.FusionFloatingLayout.*;

/** Presentation of the existing root panel only. Never owns a Player, Surface or playback request. */
public final class FusionFloatingPanel {
    private static final String X="tmdb_fusion_float_x",Y="tmdb_fusion_float_y";
    private final Activity activity;
    private final FrameLayout root;
    private final MaterialCardView panel;
    private final View spacer,scroll;
    private final Runnable stateChanged;
    private TextView handle;
    private boolean floating,moving,dragging,changing;
    private boolean originalFocusable,originalTouchFocusable,originalSpacerFocusable,originalSpacerTouchFocusable;
    private int originalDescendants,pointer=-1;
    private float originalRadius,originalZ,originalX,originalY,downX,downY;
    private FrameLayout.LayoutParams originalParams;
    private Box box,start;
    private Area safe;
    private Position position;
    private View detailFocus;
    private ThemeTokens styledTokens;
    private boolean styledFocus,styledMoving;
    private final ViewTreeObserver.OnGlobalFocusChangeListener focusMemory;

    public FusionFloatingPanel(Activity activity,FrameLayout root,MaterialCardView panel,View spacer,View scroll,Runnable stateChanged) {
        this.activity=activity;this.root=root;this.panel=panel;this.spacer=spacer;this.scroll=scroll;this.stateChanged=stateChanged;
        position=new Position(Prefers.getFloat(X,1),Prefers.getFloat(Y,0));
        focusMemory=(oldFocus,newFocus)->{if(floating&&isDetailTarget(newFocus))detailFocus=newFocus;};
        root.getViewTreeObserver().addOnGlobalFocusChangeListener(focusMemory);
    }
    public boolean isFloating(){return floating;}
    public boolean isMoving(){return moving;}
    public TextView handle(){return handle;}
    public Box bounds(){return box;}
    private int dp(float value){return Math.round(value*root.getResources().getDisplayMetrics().density);}

    public boolean update(boolean allowed,boolean ready,boolean loading) {
        if(changing)return floating;
        Area viewport=viewport();
        int[] location=new int[2],origin=new int[2];spacer.getLocationInWindow(location);root.getLocationInWindow(origin);
        boolean next=FusionFloatingLayout.floating(floating,allowed,ready,loading,location[1]-origin[1],location[1]-origin[1]+spacer.getHeight(),viewport);
        // Do not revoke an explicitly focused/dragged movement target in the middle of its operation.
        if(floating && allowed && (ready||loading) && (moving||pointer!=-1||handle.hasFocus()))next=true;
        if(next!=floating){if(next)enter();else leave(true);}
        if(floating)refreshBounds();
        return floating;
    }
    private Area viewport() {
        int[] origin=new int[2];root.getLocationInWindow(origin);
        Rect visible=new Rect();
        if(!scroll.getGlobalVisibleRect(visible))return new Area(0,0,0,0);
        Area s=safeArea();
        return new Area(Math.max(s.left(),visible.left-origin[0]+scroll.getPaddingLeft()),Math.max(s.top(),visible.top-origin[1]+scroll.getPaddingTop()),
                Math.min(s.right(),visible.right-origin[0]-scroll.getPaddingRight()),Math.min(s.bottom(),visible.bottom-origin[1]-scroll.getPaddingBottom()));
    }
    private Area safeArea() {
        int l=root.getPaddingLeft(),t=root.getPaddingTop(),r=root.getPaddingRight(),b=root.getPaddingBottom();
        var insets=ViewCompat.getRootWindowInsets(root);
        if(insets!=null){var i=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            // Root may itself be inset in the window. Subtract that part before applying bars.
            int[] origin=new int[2];root.getLocationInWindow(origin);
            l=Math.max(l,Math.max(0,i.left-origin[0]));t=Math.max(t,Math.max(0,i.top-origin[1]));
            r=Math.max(r,i.right);b=Math.max(b,i.bottom);
        }
        int margin=dp(12);
        return new Area(l+margin,t+margin,Math.max(l+margin,root.getWidth()-r-margin),Math.max(t+margin,root.getHeight()-b-margin));
    }
    private void enter() {
        if(root.getWidth()==0||root.getHeight()==0)return;
        changing=true;
        originalParams=new FrameLayout.LayoutParams((FrameLayout.LayoutParams)panel.getLayoutParams());
        originalRadius=panel.getRadius();originalZ=panel.getTranslationZ();originalX=panel.getTranslationX();originalY=panel.getTranslationY();
        originalFocusable=panel.isFocusable();originalTouchFocusable=panel.isFocusableInTouchMode();originalDescendants=panel.getDescendantFocusability();
        originalSpacerFocusable=spacer.isFocusable();originalSpacerTouchFocusable=spacer.isFocusableInTouchMode();
        View current=activity.getCurrentFocus();if(current==null)current=root.findFocus();if(isDetailTarget(current))detailFocus=current;
        floating=true;
        panel.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);panel.setFocusable(false);panel.setFocusableInTouchMode(false);
        // Keep its geometry, but do not leave an empty/off-screen focus target while floating.
        spacer.setFocusable(false);spacer.setFocusableInTouchMode(false);
        // Preserve Surface attachment and avoid vendor-specific rounded Surface clipping.
        panel.setRadius(0);panel.setTranslationZ(0);
        ensureHandle();handle.setVisibility(View.VISIBLE);refreshStyle();
        safe=safeArea();box=FusionFloatingLayout.box(safe,root.getResources().getDisplayMetrics().density,position);apply();
        changing=false;stateChanged.run();
    }
    private void ensureHandle() {
        if(handle!=null)return;
        handle=new TextView(activity);handle.setId(View.generateViewId());handle.setGravity(Gravity.TOP|Gravity.END);handle.setTextSize(12);handle.setPadding(dp(6),dp(5),dp(6),0);
        handle.setFocusable(true);handle.setFocusableInTouchMode(true);handle.setContentDescription(activity.getString(R.string.fusion_float_move_hint));
        if(android.os.Build.VERSION.SDK_INT>=26)handle.setDefaultFocusHighlightEnabled(false);handle.setOnFocusChangeListener((v,focused)->{if(!focused&&moving)cancelMove();refreshStyle();});
        handle.setOnClickListener(v->{if(moving)confirm();else beginMove();});handle.setOnTouchListener(this::touch);
        // Above the existing panel, below later native control layers. No separate video node.
        root.addView(handle,root.indexOfChild(panel)+1,new FrameLayout.LayoutParams(1,1));
    }
    private void refreshStyle() {
        if(handle==null)return;
        var tokens=ThemeController.current();
        boolean focused=handle.hasFocus();
        if(tokens.equals(styledTokens)&&styledFocus==focused&&styledMoving==moving)return;
        styledTokens=tokens;styledFocus=focused;styledMoving=moving;
        GradientDrawable bg=new GradientDrawable();bg.setColor(0x00000000);
        bg.setStroke(dp(moving?3:2),handle.hasFocus()||moving?tokens.colorFocus():0x00000000);
        handle.setBackground(bg);handle.setTextColor(tokens.colorPlayerControl());handle.setShadowLayer(dp(2),0,1,tokens.colorShadow());
        handle.setText(moving?R.string.fusion_float_moving:R.string.fusion_float_move);
    }
    public void refreshBounds() {
        if(!floating||changing)return;
        Area next=safeArea();
        if(!next.equals(safe)){
            if(moving||pointer!=-1)cancelMove();
            safe=next;box=FusionFloatingLayout.box(safe,root.getResources().getDisplayMetrics().density,position);apply();
        }
        refreshStyle();
    }
    /** Repair only layout fields touched by the existing detail template/theme binder. */
    public void refreshLayout(){if(floating){refreshBounds();apply();}}
    private void apply() {
        if(box==null)return;
        var p=(FrameLayout.LayoutParams)panel.getLayoutParams();
        if(p.width!=box.width()||p.height!=box.height()||p.gravity!=(Gravity.TOP|Gravity.LEFT)||p.leftMargin!=0||p.topMargin!=0||p.getMarginStart()!=0||p.getMarginEnd()!=0){
            p=new FrameLayout.LayoutParams(box.width(),box.height(),Gravity.TOP|Gravity.LEFT);p.setMarginStart(0);p.setMarginEnd(0);panel.setLayoutParams(p);
        }
        panel.setTranslationX(box.x());panel.setTranslationY(box.y());
        var hp=(FrameLayout.LayoutParams)handle.getLayoutParams();
        if(hp.width!=box.width()||hp.height!=box.height()){hp=new FrameLayout.LayoutParams(box.width(),box.height(),Gravity.TOP|Gravity.LEFT);handle.setLayoutParams(hp);}
        handle.setTranslationX(box.x());handle.setTranslationY(box.y());
    }
    private void leave(boolean restoreLayout) {
        if(!floating)return;
        changing=true;moving=false;pointer=-1;dragging=false;floating=false;
        if(handle!=null)handle.setVisibility(View.GONE);
        panel.setFocusable(originalFocusable);panel.setFocusableInTouchMode(originalTouchFocusable);panel.setDescendantFocusability(originalDescendants);
        spacer.setFocusable(originalSpacerFocusable);spacer.setFocusableInTouchMode(originalSpacerTouchFocusable);
        panel.setRadius(originalRadius);panel.setTranslationZ(originalZ);
        if(restoreLayout){panel.setLayoutParams(originalParams);panel.setTranslationX(originalX);panel.setTranslationY(originalY);}
        changing=false;stateChanged.run();
    }
    /** Fullscreen already applies its own size immediately; do not resize twice or change the player. */
    public void suspend(){leave(false);}
    public void dispose(){
        root.getViewTreeObserver().removeOnGlobalFocusChangeListener(focusMemory);
        if(handle!=null)root.removeView(handle);handle=null;floating=false;moving=false;pointer=-1;
    }
    /** Only used after an explicit fullscreen operation, never by automatic layout restoration. */
    public void returnToDetails(){if(detailFocus!=null&&detailFocus.isShown())detailFocus.requestFocus();}
    public void cancelOperation(){if(moving||pointer!=-1)cancelMove();}
    private void beginMove(){if(!floating)return;start=box;moving=true;refreshStyle();}
    private void cancelMove(){if(start!=null)box=start;moving=false;pointer=-1;dragging=false;apply();refreshStyle();}
    private void confirm(){box=FusionFloatingLayout.snap(safe,box);position=FusionFloatingLayout.position(safe,box);Prefers.put(X,position.x());Prefers.put(Y,position.y());moving=false;pointer=-1;dragging=false;apply();refreshStyle();}
    public boolean key(KeyEvent event,View focus) {
        if(!floating||handle==null)return false;
        int key=event.getKeyCode();
        boolean arrow=key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN;
        boolean confirm=key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_NUMPAD_ENTER;
        if(moving){
            if(key==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)cancelMove();return true;}
            if(confirm){if(event.getAction()==KeyEvent.ACTION_UP)confirm();return true;}
            if(arrow){if(event.getAction()==KeyEvent.ACTION_DOWN){int step=dp(16);box=FusionFloatingLayout.clamp(safe,new Box(box.x()+(key==KeyEvent.KEYCODE_DPAD_RIGHT?step:key==KeyEvent.KEYCODE_DPAD_LEFT?-step:0),box.y()+(key==KeyEvent.KEYCODE_DPAD_DOWN?step:key==KeyEvent.KEYCODE_DPAD_UP?-step:0),box.width(),box.height()));apply();}return true;}
        }
        if(focus==handle){
            if(confirm){if(event.getAction()==KeyEvent.ACTION_UP)beginMove();return true;}
            if(arrow){if(event.getAction()==KeyEvent.ACTION_DOWN&&key!=KeyEvent.KEYCODE_DPAD_RIGHT&&detailFocus!=null&&detailFocus.isShown()){detailFocus.requestFocus();}return true;}
        }
        // Native horizontal neighbours keep precedence; the right edge/wrap connects to the move proxy.
        if(key==KeyEvent.KEYCODE_DPAD_RIGHT&&focus!=null&&inside(focus,scroll)){
            View next=focus.focusSearch(View.FOCUS_RIGHT);
            if(next==null||next==handle||inside(focus,next)||!inside(next,scroll)||!sameVisibleRow(focus,next)||centerX(next)<=centerX(focus)){
                if(event.getAction()==KeyEvent.ACTION_DOWN){detailFocus=focus;handle.requestFocus();}return true;
            }
        }
        return false;
    }
    private boolean isDetailTarget(View view){
        return view!=null && view!=scroll && view!=spacer && !inside(view,spacer)
                && inside(view,scroll) && view.isFocusable() && view.getGlobalVisibleRect(new Rect());
    }
    private boolean sameVisibleRow(View first,View second){
        Rect a=new Rect(),b=new Rect();
        return first.getGlobalVisibleRect(a)&&second.getGlobalVisibleRect(b)&&a.top<b.bottom&&b.top<a.bottom;
    }
    private int centerX(View view){int[] loc=new int[2];view.getLocationInWindow(loc);return loc[0]+view.getWidth()/2;}
    private static boolean inside(View child,View parent){for(View v=child;v!=null;v=v.getParent() instanceof View p?p:null)if(v==parent)return true;return false;}
    private boolean touch(View v,MotionEvent event) {
        if(!floating)return false;
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN -> {if(moving)cancelMove();start=box;pointer=event.getPointerId(0);downX=event.getRawX();downY=event.getRawY();dragging=false;return true;}
            case MotionEvent.ACTION_MOVE -> {
                if(pointer<0||event.getPointerId(0)!=pointer){cancelMove();return true;}
                float dx=event.getRawX()-downX,dy=event.getRawY()-downY;
                if(Math.hypot(dx,dy)>ViewConfiguration.get(activity).getScaledTouchSlop())dragging=true;
                if(dragging){box=FusionFloatingLayout.clamp(safe,new Box(start.x()+Math.round(dx),start.y()+Math.round(dy),box.width(),box.height()));apply();}return true;
            }
            case MotionEvent.ACTION_UP -> {pointer=-1;if(dragging)confirm();else handle.performClick();return true;}
            case MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_POINTER_DOWN -> {cancelMove();return true;}
            default -> {return true;}
        }
    }
}
