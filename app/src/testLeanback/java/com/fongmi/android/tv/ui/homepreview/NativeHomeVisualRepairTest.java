package com.fongmi.android.tv.ui.homepreview;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import com.fongmi.android.tv.ui.home.NativeHomeBusinessTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, application=NativeHomeBusinessTest.FixtureApp.class, qualifiers="w960dp-h540dp-land-xhdpi")
public class NativeHomeVisualRepairTest {
    @Test public void drawerHasBoundedGeometryBeforeFirstShowAndOnEveryReopen() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
        ImageButton house=new ImageButton(activity);activity.setContentView(house);
        HomePreviewMenuDialog drawer=new HomePreviewMenuDialog(activity,new String[]{"A","B"},()->0,i->{},house);
        int screen=activity.getResources().getDisplayMetrics().widthPixels;
        for(int i=0;i<3;i++) {
            WindowManager.LayoutParams attrs=drawer.getWindow().getAttributes();
            assertTrue("before show must already be a narrow drawer",attrs.width>0&&attrs.width<=screen*.22f+1);
            assertEquals(Gravity.END|Gravity.TOP,attrs.gravity);
            assertEquals(WindowManager.LayoutParams.MATCH_PARENT,attrs.height);
            drawer.show();
            assertEquals(attrs.width,drawer.getWindow().getAttributes().width);
            drawer.dismiss();
        }
        activity.finish();
    }
    @Test public void ownedFocusSelectorsDisableAdditionalPlatformDecoration() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
        ImageButton house=new ImageButton(activity);TextView button=new TextView(activity);
        HomePreviewStyles.house(house);HomePreviewStyles.control(button);
        assertFalse(house.getDefaultFocusHighlightEnabled());
        assertFalse(button.getDefaultFocusHighlightEnabled());
        assertNotSame(house.getBackground(),button.getBackground());
        assertTrue(house.getBackground().isStateful());
        assertTrue(button.getBackground().isStateful());
        activity.finish();
    }
    @Test public void heroSlotsFitFontLinesAt1080And4kWithoutMovingRailForFilters() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();activity.setTheme(com.fongmi.android.tv.R.style.Theme_App);
        NativeHomeBrowseView view=new NativeHomeBrowseView(activity,4,3,()->{});activity.setContentView(view);
        for(int[] size:new int[][]{{1920,1080},{3840,2160}}) {
            view.setPadding(24,16,24,16);
            view.measure(View.MeasureSpec.makeMeasureSpec(size[0],View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(size[1],View.MeasureSpec.EXACTLY));view.layout(0,0,size[0],size[1]);
            TextView title=view.findViewById(com.fongmi.android.tv.R.id.nhp_title),meta=view.findViewById(com.fongmi.android.tv.R.id.nhp_meta),overview=view.findViewById(com.fongmi.android.tv.R.id.nhp_overview);
            View slot=view.findViewById(com.fongmi.android.tv.R.id.nhp_title_slot),hero=view.findViewById(com.fongmi.android.tv.R.id.nhp_hero),row=view.findViewById(com.fongmi.android.tv.R.id.nhp_category_row);
            assertTrue(slot.getHeight()+1>=title.getPaint().getFontSpacing()*2);
            assertTrue(meta.getHeight()>=meta.getPaint().getFontSpacing());
            assertTrue(overview.getHeight()+2>=overview.getPaint().getFontSpacing()*overview.getMaxLines());
            assertTrue(hero.getBottom()<=row.getTop());assertTrue(hero.getTop()>=16);
            View house=view.findViewById(com.fongmi.android.tv.R.id.nhp_home);
            assertTrue(house.getWidth()<size[0]*.06f);assertFalse(row.isFocusable());
        }
        view.release();activity.finish();
    }
    @Test public void houseKeepsFocusedBadgeAndTextUsesReadableOverlayWithoutLayoutGrowth() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();activity.setTheme(com.fongmi.android.tv.R.style.Theme_App);
        ImageButton house=new ImageButton(activity);android.widget.Button text=new android.widget.Button(activity);
        android.widget.LinearLayout parent=new android.widget.LinearLayout(activity);parent.addView(house,new android.widget.LinearLayout.LayoutParams(64,64));parent.addView(text,new android.widget.LinearLayout.LayoutParams(160,80));activity.setContentView(parent);
        text.setText("搜索");text.setPadding(20,0,20,0);HomePreviewStyles.house(house);HomePreviewStyles.function(text);
        parent.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));parent.layout(0,0,1920,1080);
        var tokens=com.fongmi.android.tv.theme.ThemeController.current();
        android.graphics.drawable.StateListDrawable states=(android.graphics.drawable.StateListDrawable)house.getBackground();
        states.setState(new int[]{});assertEquals(0,((android.graphics.drawable.GradientDrawable)states.getCurrent()).getColor().getDefaultColor());
        states.setState(new int[]{android.R.attr.state_focused});var badge=(android.graphics.drawable.GradientDrawable)states.getCurrent();
        assertEquals(android.graphics.drawable.GradientDrawable.OVAL,badge.getShape());assertEquals(tokens.colorPrimary(),badge.getColor().getDefaultColor());
        text.requestFocus();HomePreviewStyles.functionFocus(text,true);org.robolectric.shadows.ShadowLooper.idleMainLooper(150,java.util.concurrent.TimeUnit.MILLISECONDS);
        assertEquals(160,text.getWidth());assertEquals(1.08f,text.getScaleX(),.001f);assertEquals(tokens.colorPlayerControl(),text.getCurrentTextColor());
        assertTrue(text.getTextColors().isStateful());assertEquals(tokens.colorPlayerControlMuted(),text.getTextColors().getDefaultColor());
        android.graphics.drawable.StateListDrawable underline=(android.graphics.drawable.StateListDrawable)text.getBackground();underline.setState(new int[]{android.R.attr.state_focused});
        var focused=(android.graphics.drawable.LayerDrawable)underline.getCurrent();assertEquals(tokens.colorFocus(),((HomePreviewStyles.Underline)focused.getDrawable(1)).color());
        underline.setState(new int[]{});assertEquals(HomePreviewStyles.lightSurface(tokens,false),((android.graphics.drawable.GradientDrawable)underline.getCurrent()).getColor().getDefaultColor());
        activity.finish();
    }
}
