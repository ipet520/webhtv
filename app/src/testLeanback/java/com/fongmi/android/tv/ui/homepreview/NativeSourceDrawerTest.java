package com.fongmi.android.tv.ui.homepreview;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.setting.*;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.ui.home.NativeHomeBusinessTest;
import com.github.catvod.utils.Prefers;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;

import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
public class NativeSourceDrawerTest {
    Activity activity;Button house;HomePreviewMenuDialog drawer;List<Site> sites;List<Site> switched;
    @Before public void setup(){
        Prefers.getPrefers().edit().clear().commit();SiteDialog.rememberGroup("");GroupRuleConfig.setInterfaceRules(List.of());
        sites=new ArrayList<>(List.of(Site.get("a","[电影][盘] A"),Site.get("b","[剧集] B"),Site.get("c","[电影] C")));
        config(101);ReflectionHelpers.setField(VodConfig.get(),"sites",sites);switched=new ArrayList<>();
        activity=Robolectric.buildActivity(Activity.class).setup().get();house=new Button(activity);activity.setContentView(house);house.requestFocus();
    }
    void config(int id){Config c=new Config();c.setId(id);c.setUrl("https://fixture/config/"+id);ReflectionHelpers.setField(VodConfig.get(),"config",c);}
    @After public void finish(){if(drawer!=null)drawer.dismiss();activity.finish();}
    <T>T field(String name){return ReflectionHelpers.getField(drawer,name);}
    List<Button> options(){return field("options");}
    List<Button> groups(){return field("groupOptions");}
    Button selector(){return field("groupSelector");}
    Button group(String key){for(Button b:groups())if(key.equals(b.getTag()))return b;throw new AssertionError(key);}
    void show(int selected){drawer=new HomePreviewMenuDialog(activity,sites,()->selected,switched::add,house);drawer.show();layout();}
    void layout(){FrameLayout r=field("root");int w=drawer.getWindow().getAttributes().width;int h=activity.getResources().getDisplayMetrics().heightPixels;r.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));r.layout(0,0,w,h);ShadowLooper.idleMainLooper();}
    void open(){selector().performClick();layout();}
    void choose(String key){open();group(key).performClick();layout();}
    void key(int key){long t=SystemClock.uptimeMillis();drawer.dispatchKeyEvent(new KeyEvent(t,t,KeyEvent.ACTION_DOWN,key,0));drawer.dispatchKeyEvent(new KeyEvent(t,t+1,KeyEvent.ACTION_UP,key,0));layout();}
    void longKey(){long t=SystemClock.uptimeMillis();drawer.dispatchKeyEvent(new KeyEvent(t,t,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0));drawer.dispatchKeyEvent(new KeyEvent(t,t+600,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,1));drawer.dispatchKeyEvent(new KeyEvent(t,t+650,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER,0));layout();}

    @Test public void backToGroupThenDownRestoresBrowsedIdentityWithoutChangingActualSource(){
        show(0);key(KeyEvent.KEYCODE_DPAD_DOWN);options().get(2).requestFocus();
        ScrollView scroll=field("scroller");int y=scroll.getScrollY();
        key(KeyEvent.KEYCODE_BACK);assertSame(selector(),drawer.getCurrentFocus());assertEquals(y,scroll.getScrollY());
        key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(options().get(2),drawer.getCurrentFocus());
        assertTrue(options().get(0).isSelected());assertFalse(options().get(2).isSelected());assertTrue(switched.isEmpty());
        choose("电影");key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(options().get(1),drawer.getCurrentFocus());
        choose("剧集");key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(options().get(0),drawer.getCurrentFocus());
        choose("");key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(options().get(1),drawer.getCurrentFocus());
    }
    @Test public void systemBackUsesSameGroupListAndSortPriorityAsRemoteBack(){
        show(0);key(KeyEvent.KEYCODE_DPAD_DOWN);options().get(2).requestFocus();
        drawer.onBackPressed();assertTrue("System Back must not dismiss source list",drawer.isShowing());assertSame(selector(),drawer.getCurrentFocus());
        key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(options().get(2),drawer.getCurrentFocus());
        drawer.onBackPressed();open();group("电影").requestFocus();longKey();
        drawer.onBackPressed();assertNull(field("reorderingGroup"));assertNotNull(field("groupOverlay"));
        drawer.onBackPressed();assertNull(field("groupOverlay"));assertSame(selector(),drawer.getCurrentFocus());assertTrue(drawer.isShowing());
        drawer.onBackPressed();assertFalse(drawer.isShowing());assertTrue(house.hasFocus());
    }
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    public void compactButtonTextHasUsableHeightAndSymmetricInsetsAcrossFocus(){
        show(0);Button selector=selector();selector.setText("全部");layout();System.out.println("GROUP before/final h="+selector.getHeight()+" padding="+selector.getPaddingTop()+","+selector.getPaddingBottom()+" line="+selector.getLineHeight()+" baseline="+selector.getBaseline());
        for(Button view:new Button[]{selector,options().get(0)}){
            assertFalse(view.getIncludeFontPadding());assertEquals(0,view.getPaddingTop());assertEquals(0,view.getPaddingBottom());
            assertEquals(Gravity.CENTER_VERTICAL,view.getGravity()&Gravity.VERTICAL_GRAVITY_MASK);
            android.graphics.Rect glyph=new android.graphics.Rect();String text=view.getText().toString();view.getPaint().getTextBounds(text,0,text.length(),glyph);
            assertTrue("glyph top="+(view.getBaseline()+glyph.top),view.getBaseline()+glyph.top>=0);
            assertTrue("glyph bottom="+(view.getBaseline()+glyph.bottom),view.getBaseline()+glyph.bottom<=view.getHeight());
            assertEquals("optical center",view.getHeight()/2f,view.getBaseline()+glyph.exactCenterY(),6f);
            assertTrue(view.getHeight()-view.getCompoundPaddingTop()-view.getCompoundPaddingBottom()>=view.getLineHeight());
            int baseline=view.getBaseline(),h=view.getHeight();view.requestFocus();layout();assertEquals(baseline,view.getBaseline());assertEquals(h,view.getHeight());
        }
        selector.setText("这是非常非常非常非常长的测试分组名称");layout();assertEquals(1,selector.getLineCount());assertEquals("END",String.valueOf(selector.getEllipsize()));assertTrue(selector.getLayout().getEllipsisCount(0)>0);
        assertEquals(10*2,selector.getCompoundDrawablesRelative()[2].getBounds().height());assertEquals(32*2,selector.getHeight());
    }
    @Test
    @org.robolectric.annotation.Config(qualifiers="w960dp-h540dp-land-xxxhdpi")
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    public void fourKChineseGroupTextFitsSameCompactDpSlot(){
        show(0);selector().setText("全部");layout();float density=activity.getResources().getDisplayMetrics().density;
        assertEquals(3840,activity.getResources().getDisplayMetrics().widthPixels);assertEquals(Math.round(32*density),selector().getHeight());
        android.graphics.Rect glyph=new android.graphics.Rect();selector().getPaint().getTextBounds("全部",0,2,glyph);
        assertTrue(selector().getBaseline()+glyph.top>=0);assertTrue(selector().getBaseline()+glyph.bottom<=selector().getHeight());
        assertEquals(selector().getHeight()/2f,selector().getBaseline()+glyph.exactCenterY(),3*density);
        assertEquals(1,selector().getLineCount());assertFalse(selector().getIncludeFontPadding());
    }
    @Test public void drawerGlassUsesTranslucentThemeFillWithoutStackedOpaqueHeaderOrList(){
        show(0);LinearLayout body=field("body"),header=field("groupHeader");ScrollView list=field("scroller");
        assertTrue(body.getBackground() instanceof GradientDrawable);
        GradientDrawable glass=(GradientDrawable)body.getBackground();int[] colors=glass.getColors();assertEquals(3,colors.length);
        for(int color:colors)assertTrue((color>>>24)>=76&&(color>>>24)<160);
        assertEquals(0,(((android.graphics.drawable.ColorDrawable)header.getBackground()).getColor()>>>24));
        assertEquals(0,(((android.graphics.drawable.ColorDrawable)list.getBackground()).getColor()>>>24));
        assertTrue(drawer.getWindow().getAttributes().dimAmount<=.28f);
        for(Button option:options()){assertNotNull(option.getCompoundDrawablesRelative()[0]);assertTrue((option.getCurrentTextColor()>>>24)>200);}
        open();assertNotNull(field("groupOverlay"));key(KeyEvent.KEYCODE_BACK);assertTrue(drawer.isShowing());assertSame(selector(),drawer.getCurrentFocus());
    }
    @Test public void lightDarkAndCustomThemeRefreshGlassHueAndFocusWithoutLosingSourceIdentity(){
        show(0);options().get(2).requestFocus();
        for(int mode:new int[]{0,1,0}){
            Setting.putThemeMode(mode);com.fongmi.android.tv.theme.ThemeController.applyFromPreferences(null);drawer.refreshTheme();
            var tokens=com.fongmi.android.tv.theme.ThemeController.current();GradientDrawable glass=(GradientDrawable)this.<LinearLayout>field("body").getBackground();
            assertEquals(tokens.colorSurfaceContainer()&0xffffff,glass.getColors()[1]&0xffffff);assertTrue(options().get(0).isSelected());assertSame(options().get(2),drawer.getCurrentFocus());
            android.graphics.drawable.StateListDrawable states=(android.graphics.drawable.StateListDrawable)selector().getBackground();states.setState(new int[]{android.R.attr.state_focused});
            assertEquals(tokens.colorFocus(),org.robolectric.shadow.api.Shadow.<org.robolectric.shadows.ShadowGradientDrawable>extract(states.getCurrent()).getStrokeColor());
        }
        var custom=com.fongmi.android.tv.theme.ThemeProfile.defaultProfile();custom.light.surface="#FFF8F0";custom.light.focus="#006B50";
        assertTrue(com.fongmi.android.tv.theme.ThemeProfileStore.apply(custom).success());com.fongmi.android.tv.theme.ThemeController.applyFromPreferences(null);drawer.refreshTheme();
        assertEquals(com.fongmi.android.tv.theme.ThemeController.current().colorSurfaceContainer()&0xffffff,((GradientDrawable)this.<LinearLayout>field("body").getBackground()).getColors()[1]&0xffffff);
        assertTrue(options().get(0).isSelected());assertSame(options().get(2),drawer.getCurrentFocus());assertTrue(switched.isEmpty());
        Prefers.getPrefers().edit().clear().commit();com.fongmi.android.tv.theme.ThemeController.applyFromPreferences(null);
    }
    @Test public void groupPopupIsIndependentNarrowRightAlignedAndDoesNotResizeAnchorOrList(){
        show(0);int width=selector().getWidth(),top=this.<ScrollView>field("scroller").getTop();open();ScrollView popup=field("groupScroll");FrameLayout root=field("root");
        assertTrue(popup.getWidth()<width*.85f);assertEquals(root.getWidth()-24,popup.getRight());assertEquals(width,selector().getWidth());assertEquals(top,this.<ScrollView>field("scroller").getTop());
        assertTrue(popup.getTop()>=selector().getBottom());key(KeyEvent.KEYCODE_BACK);assertNull(field("groupOverlay"));assertSame(selector(),drawer.getCurrentFocus());
    }
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    public void expandedGroupsCenterActualTextInNormalFocusAndReorderWithoutChangingSelector(){
        sites=List.of(Site.get("a","[短][这是很长很长很长很长的分组名称] A"),Site.get("b","[另一个] B"));show(0);
        assertEquals(View.TEXT_ALIGNMENT_VIEW_START,selector().getTextAlignment());int selectorWidth=selector().getWidth();open();ScrollView popup=field("groupScroll");int width=popup.getWidth(),right=popup.getRight();
        for(Button item:groups()){
            assertEquals(View.TEXT_ALIGNMENT_CENTER,item.getTextAlignment());assertEquals(Gravity.CENTER,item.getGravity());assertEquals(item.getPaddingLeft(),item.getPaddingRight());
            int baseline=item.getBaseline();float center=item.getCompoundPaddingLeft()+(item.getLayout().getLineLeft(0)+item.getLayout().getLineRight(0))/2;
            assertEquals(item.getWidth()/2f,center,2f);item.requestFocus();layout();assertEquals(baseline,item.getBaseline());
            if(item.getText().length()>8)assertTrue(item.getLayout().getEllipsisCount(0)>0);
        }
        group("短").requestFocus();longKey();Button moving=group("短");assertEquals(View.TEXT_ALIGNMENT_CENTER,moving.getTextAlignment());assertEquals(moving.getWidth()/2f,moving.getCompoundPaddingLeft()+(moving.getLayout().getLineLeft(0)+moving.getLayout().getLineRight(0))/2,2f);
        key(KeyEvent.KEYCODE_BACK);assertNull(field("reorderingGroup"));assertEquals(width,popup.getWidth());assertEquals(right,popup.getRight());assertEquals(selectorWidth,selector().getWidth());key(KeyEvent.KEYCODE_BACK);assertSame(selector(),drawer.getCurrentFocus());
    }
    @Test public void preservesGeometryPaddingRowsAndDistinctCompactGroupRegion(){
        show(1);int width=drawer.getWindow().getAttributes().width;assertTrue(width<=activity.getResources().getDisplayMetrics().widthPixels*.22f+1);
        assertSame(selector(),drawer.getCurrentFocus());assertTrue(options().get(1).isSelected());
        assertEquals(40*2,options().get(0).getLayoutParams().height);assertEquals(9*2,((LinearLayout.LayoutParams)options().get(0).getLayoutParams()).bottomMargin);
        assertEquals(32*2,selector().getHeight());assertEquals("END",String.valueOf(selector().getEllipsize()));assertEquals(1,selector().getMaxLines());
        assertEquals(Gravity.START|Gravity.CENTER_VERTICAL,selector().getGravity());assertNotNull(selector().getCompoundDrawablesRelative()[2]);
        assertTrue(selector().getTextSize()<options().get(0).getTextSize());assertEquals(20*2,selector().getTop()+((View)selector().getParent()).getTop());
        assertEquals(-1,drawer.getWindow().getAttributes().height);assertEquals(Gravity.END|Gravity.TOP,drawer.getWindow().getAttributes().gravity);
        assertTrue(drawer.getWindow().getAttributes().dimAmount<.3);assertEquals(3,options().size());
    }
    @Test public void overlayDoesNotResizeOrMoveSiteListAndTrapsFocus(){
        show(1);ScrollView scroller=field("scroller");int top=scroller.getTop(),height=scroller.getHeight();Button original=options().get(0);open();
        assertEquals(top,scroller.getTop());assertEquals(height,scroller.getHeight());assertSame(original,options().get(0));
        LinearLayout body=field("body");assertEquals(ViewGroup.FOCUS_BLOCK_DESCENDANTS,body.getDescendantFocusability());assertFalse(original.requestFocus());
        assertNotNull(field("groupOverlay"));assertNotNull(field("groupScroll"));assertTrue(drawer.isShowing());
    }
    @Test public void focusOnlyDoesNotFilterOrSwitchAndConfirmUsesNativeMultiGroupRule(){
        show(0);open();Button original=options().get(0);group("剧集").requestFocus();assertSame(original,options().get(0));assertEquals("",SiteDialog.rememberedGroup(Site.getGroups(sites)));assertTrue(switched.isEmpty());
        group("电影").performClick();layout();assertEquals(2,options().size());assertEquals(List.of(sites.get(0),sites.get(2)),this.<List<Site>>field("visibleSites"));
        assertNull(field("groupOverlay"));assertSame(selector(),drawer.getCurrentFocus());assertTrue(switched.isEmpty());
        choose("盘");assertEquals(List.of(sites.get(0)),this.<List<Site>>field("visibleSites"));choose("");assertEquals(3,options().size());
    }
    @Test public void excludesCurrentGroupWithoutChangingActualSiteAndReturnsToSameIdentity(){
        show(2);assertSame(selector(),drawer.getCurrentFocus());key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(options().get(2),drawer.getCurrentFocus());choose("剧集");key(KeyEvent.KEYCODE_DPAD_DOWN);assertEquals(1,options().size());assertFalse(options().get(0).isSelected());assertSame(options().get(0),drawer.getCurrentFocus());
        choose("电影");assertSame(selector(),drawer.getCurrentFocus());assertTrue(options().get(1).isSelected());assertTrue(switched.isEmpty());
    }
    @Test public void currentSiteFarDownStillUsesOriginalScrollViewReveal(){
        sites.clear();for(int i=0;i<60;i++)sites.add(Site.get("s"+i,"[组] source "+i));show(55);layout();
        assertSame(selector(),drawer.getCurrentFocus());assertTrue(options().get(55).isSelected());assertTrue(this.<ScrollView>field("scroller").getScrollY()>0);
        assertTrue(selector().isShown());key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(options().get(55),drawer.getCurrentFocus());
    }
    @Test public void topUpAndBackFollowThreeLevelsAndRestoreHouse(){
        show(0);key(KeyEvent.KEYCODE_DPAD_UP);assertSame(selector(),drawer.getCurrentFocus());key(KeyEvent.KEYCODE_DPAD_CENTER);assertNotNull(field("groupOverlay"));
        group("电影").requestFocus();longKey();assertEquals("电影",field("reorderingGroup"));key(KeyEvent.KEYCODE_BACK);assertNull(field("reorderingGroup"));assertNotNull(field("groupOverlay"));
        key(KeyEvent.KEYCODE_BACK);assertNull(field("groupOverlay"));assertSame(selector(),drawer.getCurrentFocus());key(KeyEvent.KEYCODE_BACK);assertFalse(drawer.isShowing());assertTrue(house.hasFocus());
    }
    @Test public void longPressReleaseDoesNotSelectOrSaveAndVerticalReorderSavesNativeOrder(){
        show(0);open();group("电影").requestFocus();longKey();assertNotNull(field("reorderingGroup"));assertEquals("",SiteDialog.rememberedGroup(Site.getGroups(sites)));assertTrue(Prefers.getString("site_group_order_101","").isEmpty());
        key(KeyEvent.KEYCODE_DPAD_UP);assertEquals(List.of("电影","盘","剧集"),field("groups"));
        key(KeyEvent.KEYCODE_DPAD_DOWN);assertEquals(List.of("盘","电影","剧集"),field("groups"));assertSame(group("电影"),drawer.getCurrentFocus());
        key(KeyEvent.KEYCODE_DPAD_CENTER);assertNull(field("reorderingGroup"));assertNotNull(field("groupOverlay"));assertEquals(List.of("盘","电影","剧集"),SiteGroupOrderStore.sort(Site.getGroups(sites)));assertTrue(switched.isEmpty());
        drawer.dismiss();show(0);open();assertEquals("盘",groups().get(1).getTag());
    }
    @Test public void cancelReorderRestoresOrderAndDoesNotPersistOrApplyFilter(){
        show(1);open();group("盘").requestFocus();longKey();key(KeyEvent.KEYCODE_DPAD_DOWN);assertEquals(List.of("电影","剧集","盘"),field("groups"));
        key(KeyEvent.KEYCODE_BACK);assertEquals(List.of("电影","盘","剧集"),field("groups"));assertSame(group("盘"),drawer.getCurrentFocus());assertEquals(3,options().size());assertTrue(Prefers.getString("site_group_order_101","").isEmpty());
    }
    @Test public void groupOrderIsConfigurationScopedAndStaleGroupsAreIgnored(){
        show(0);open();group("盘").requestFocus();longKey();key(KeyEvent.KEYCODE_DPAD_UP);key(KeyEvent.KEYCODE_DPAD_CENTER);drawer.dismiss();
        config(202);assertEquals(List.of("电影","盘","剧集"),SiteGroupOrderStore.sort(Site.getGroups(sites)));config(101);assertEquals(List.of("盘","电影","剧集"),SiteGroupOrderStore.sort(Site.getGroups(sites)));
        sites.remove(0);assertEquals(List.of("电影","剧集"),SiteGroupOrderStore.sort(Site.getGroups(sites)));
    }
    @Test public void sharedMemorySurvivesReopenAndInvalidConfigGroupsFallBackToAll(){
        SiteDialog.rememberGroup("剧集");show(0);assertEquals("剧集",selector().getText().toString());assertEquals(1,options().size());drawer.dismiss();show(0);assertEquals(1,options().size());drawer.dismiss();
        config(202);sites.clear();sites.add(Site.get("x","[新组] X"));show(0);assertEquals("",SiteDialog.rememberedGroup(Site.getGroups(sites)));assertEquals(activity.getString(com.fongmi.android.tv.R.string.site_group_all),selector().getText().toString());
    }
    @Test public void emptyFilterKeepsSelectorAndDoesNotCloseOrSwitch(){
        show(0);open();for(Site s:sites)s.setName("not grouped");group("电影").performClick();layout();assertTrue(drawer.isShowing());assertEquals(0,options().size());assertSame(selector(),drawer.getCurrentFocus());assertTrue(this.<TextView>field("empty").isShown());assertTrue(switched.isEmpty());
        choose("");assertEquals(3,options().size());
    }
    @Test public void noGroupsStillHasAllAndCannotStartReorder(){
        sites=List.of(Site.get("x","plain"));show(0);open();assertEquals(1,groups().size());longKey();assertNull(field("reorderingGroup"));assertNotNull(field("groupOverlay"));assertTrue(switched.isEmpty());
    }
    @Test public void manyLongGroupsScrollLastIntoViewWithoutGrowingPanel(){
        sites.clear();for(int i=0;i<60;i++)sites.add(Site.get("g"+i,"[很长分组名称测试"+i+"] X"));show(0);int height=this.<ScrollView>field("scroller").getHeight();open();
        Button last=groups().get(groups().size()-1);last.requestFocus();layout();assertTrue(this.<ScrollView>field("groupScroll").getScrollY()>0);assertEquals("END",String.valueOf(last.getEllipsize()));assertEquals(height,this.<ScrollView>field("scroller").getHeight());key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(last,drawer.getCurrentFocus());
    }
    @Test public void nativeHealthColorUpdatesWithoutChangingSelectionAndUnknownIsReused(){
        show(0);Button b=options().get(1);int old=((GradientDrawable)b.getCompoundDrawablesRelative()[0]).getColor().getDefaultColor();assertEquals(SiteHealthStore.getColor(sites.get(1)),old);
        SiteHealthStore.recordHome(sites.get(1),false,0,10,"failure");drawer.refreshTheme();int next=((GradientDrawable)b.getCompoundDrawablesRelative()[0]).getColor().getDefaultColor();assertEquals(SiteHealthStore.getColor(sites.get(1)),next);assertNotEquals(old,next);
        b.requestFocus();assertFalse(b.isSelected());assertTrue(options().get(0).isSelected());assertTrue(switched.isEmpty());
    }
    List<Site> nativeOrder(){return new SiteAdapter(new SiteAdapter.OnClickListener(){public void onItemClick(Site s){}public boolean onItemKeyUp(int p){return false;}public boolean onItemKeyHorizontal(int p,boolean l){return false;}}).getItems();}
    @Test public void nativeHealthSortSettingIsConsumedExactlyOnceBeforeGroupFiltering(){
        SiteHealthStore.recordHome(sites.get(2),true,8,20,"");SiteHealthStore.recordHome(sites.get(0),false,0,20,"fail");Setting.putSiteHealthDialogSort(true);Setting.putSiteHealthSort(true);
        List<Site> expected=nativeOrder();sites=new ArrayList<>(expected);show(0);assertEquals(expected,field("visibleSites"));choose("电影");assertEquals(expected.stream().filter(s->s.inGroup("电影")).toList(),field("visibleSites"));drawer.dismiss();
        Setting.putSiteHealthDialogSort(false);SiteDialog.rememberGroup("");sites=new ArrayList<>(nativeOrder());show(0);assertEquals(sites,field("visibleSites"));
    }
    @Test public void actualSiteCallbackKeepsIdentityAndOnlyConfirmationClosesDrawer(){
        show(0);options().get(2).requestFocus();assertTrue(switched.isEmpty());assertTrue(drawer.isShowing());options().get(2).performClick();assertEquals(List.of(sites.get(2)),switched);assertFalse(drawer.isShowing());assertTrue(house.hasFocus());
    }
    @Test public void reorderLastBoundaryAndDismissCancelNeverPersist(){
        show(0);open();group("剧集").requestFocus();longKey();key(KeyEvent.KEYCODE_DPAD_DOWN);assertEquals(List.of("电影","盘","剧集"),field("groups"));
        key(KeyEvent.KEYCODE_DPAD_UP);assertSame(group("剧集"),drawer.getCurrentFocus());drawer.dismiss();assertTrue(Prefers.getString("site_group_order_101","").isEmpty());assertTrue(switched.isEmpty());
    }
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    public void renderCompactGroupRegionAndOverlayForLocalInspection() throws Exception {
        show(0);selector().setText("全部");layout();open();ScrollView popup=field("groupScroll");assertNotNull(popup);assertTrue(popup.isShown());assertTrue(popup.getWidth()>0);assertTrue(popup.getHeight()>0);
        System.out.println("POPUP "+popup.getWidth()+"x"+popup.getHeight()+" at "+popup.getLeft()+","+popup.getTop()+" overlay="+this.<FrameLayout>field("groupOverlay").getWidth()+"x"+this.<FrameLayout>field("groupOverlay").getHeight()+" focus="+drawer.getCurrentFocus());
        String dir=System.getenv("NATIVE_SOURCE_SCREENSHOT_DIR");if(dir==null)return;
        FrameLayout root=field("root");android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(root.getWidth(),root.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
        root.draw(new android.graphics.Canvas(bitmap));java.io.File file=new java.io.File(dir,"native-source-dropdown.png");file.getParentFile().mkdirs();
        try(java.io.FileOutputStream stream=new java.io.FileOutputStream(file)){assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream));}
    }
    @Test public void scriptChangeDoesNotTranslateRawGroupIdentity(){
        Site s=Site.get("raw","[采][盘][广] 樂園");sites=List.of(s);show(0);List<String> before=new ArrayList<>(Site.getGroups(sites));open();assertEquals(before,field("groups"));drawer.dismiss();
        org.robolectric.RuntimeEnvironment.setQualifiers("zh-rTW-w960dp-h540dp-land-xhdpi");show(0);open();assertEquals(before,field("groups"));assertEquals(List.of("采","盘","广"),field("groups"));
    }
    @Test public void downBackAndSelectorBackTakeShortestPathsWithoutSwitch(){
        show(2);assertSame(selector(),drawer.getCurrentFocus());assertTrue(options().get(2).isSelected());
        key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(options().get(2),drawer.getCurrentFocus());
        key(KeyEvent.KEYCODE_BACK);assertTrue(drawer.isShowing());assertSame(selector(),drawer.getCurrentFocus());assertTrue(options().get(2).isSelected());
        key(KeyEvent.KEYCODE_BACK);assertFalse(drawer.isShowing());assertTrue(house.hasFocus());assertTrue(switched.isEmpty());
    }
    @Test public void leftAndRightImmediatelyCloseFromSelectorSitesAndDropdownAndConsumeTail(){
        for(int direction:new int[]{KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT})for(int region=0;region<3;region++){
            show(1);if(region==1)key(KeyEvent.KEYCODE_DPAD_DOWN);if(region==2)open();
            long t=SystemClock.uptimeMillis();assertTrue(drawer.dispatchKeyEvent(new KeyEvent(t,t,KeyEvent.ACTION_DOWN,direction,0)));assertFalse(drawer.isShowing());
            assertTrue(drawer.dispatchKeyEvent(new KeyEvent(t,t+500,KeyEvent.ACTION_DOWN,direction,1)));
            assertTrue(drawer.dispatchKeyEvent(new KeyEvent(t,t+550,KeyEvent.ACTION_UP,direction,0)));
            assertNull(field("groupOverlay"));assertEquals(region==2?ViewGroup.FOCUS_AFTER_DESCENDANTS:ViewGroup.FOCUS_BEFORE_DESCENDANTS,this.<LinearLayout>field("body").getDescendantFocusability());assertTrue(house.hasFocus());assertTrue(switched.isEmpty());
        }
    }
    @Test public void sortingOwnsLrAndBackUntilCanceled(){
        show(0);open();group("电影").requestFocus();longKey();
        key(KeyEvent.KEYCODE_DPAD_LEFT);key(KeyEvent.KEYCODE_DPAD_RIGHT);assertTrue(drawer.isShowing());assertNotNull(field("reorderingGroup"));
        key(KeyEvent.KEYCODE_BACK);assertNull(field("reorderingGroup"));assertNotNull(field("groupOverlay"));
        key(KeyEvent.KEYCODE_DPAD_RIGHT);assertFalse(drawer.isShowing());assertTrue(house.hasFocus());assertTrue(Prefers.getString("site_group_order_101","").isEmpty());
    }
    @Test public void backDownRepeatAndUpOnlyTransferOnceAndCanceledUpDoesNothing(){
        show(1);key(KeyEvent.KEYCODE_DPAD_DOWN);Button current=options().get(1);long t=SystemClock.uptimeMillis();
        assertTrue(drawer.dispatchKeyEvent(new KeyEvent(t,t,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK,0)));
        assertTrue(drawer.dispatchKeyEvent(new KeyEvent(t,t+500,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK,1)));assertSame(current,drawer.getCurrentFocus());
        KeyEvent up=new KeyEvent(t,t+600,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,0);assertTrue(drawer.dispatchKeyEvent(KeyEvent.changeFlags(up,KeyEvent.FLAG_CANCELED)));assertSame(current,drawer.getCurrentFocus());
        key(KeyEvent.KEYCODE_BACK);assertSame(selector(),drawer.getCurrentFocus());assertTrue(drawer.isShowing());
    }
    @Test public void filteringPreparesCorrectDownTargetAndNeverStealsSelectorFocus(){
        show(2);choose("电影");assertSame(selector(),drawer.getCurrentFocus());assertTrue(options().get(1).isSelected());key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(options().get(1),drawer.getCurrentFocus());
        choose("剧集");assertSame(selector(),drawer.getCurrentFocus());assertFalse(options().get(0).isSelected());key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(options().get(0),drawer.getCurrentFocus());
        choose("");assertSame(selector(),drawer.getCurrentFocus());key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(options().get(1),drawer.getCurrentFocus());assertTrue(options().get(2).isSelected());assertTrue(switched.isEmpty());
    }
    @Test public void healthSortedListDownUsesCurrentIdentityAndSourceFirstUpReturnsSelector(){
        SiteHealthStore.recordHome(sites.get(2),true,8,20,"");SiteHealthStore.recordHome(sites.get(0),false,0,20,"fail");Setting.putSiteHealthDialogSort(true);Setting.putSiteHealthSort(true);
        sites=new ArrayList<>(nativeOrder());int selected=0;for(int i=0;i<sites.size();i++)if(sites.get(i).getKey().equals("b"))selected=i;
        show(selected);assertSame(selector(),drawer.getCurrentFocus());key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(options().get(selected),drawer.getCurrentFocus());
        options().get(0).requestFocus();key(KeyEvent.KEYCODE_DPAD_UP);assertSame(selector(),drawer.getCurrentFocus());assertTrue(switched.isEmpty());
    }
    @Test public void emptyGroupDownStaysOperableAndShortcutReopenHasNoHiddenFocus(){
        show(0);open();for(Site site:sites)site.setName("plain");group("电影").performClick();layout();key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(selector(),drawer.getCurrentFocus());assertTrue(drawer.isShowing());
        key(KeyEvent.KEYCODE_DPAD_LEFT);assertFalse(drawer.isShowing());show(0);assertSame(selector(),drawer.getCurrentFocus());assertNull(field("groupOverlay"));key(KeyEvent.KEYCODE_DPAD_DOWN);assertSame(options().get(0),drawer.getCurrentFocus());
    }

}
