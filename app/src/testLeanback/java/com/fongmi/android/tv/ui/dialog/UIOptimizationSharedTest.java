package com.fongmi.android.tv.ui.dialog;
import android.os.Bundle;
import android.view.View;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaMetadata;
import com.fongmi.android.tv.*;
import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.api.DanmakuApi;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.setting.*;
import com.fongmi.android.tv.theme.*;
import com.fongmi.android.tv.ui.controller.VodPlayerControlController;
import com.fongmi.android.tv.title.MediaTitleRequest;
import com.github.catvod.Init;
import com.github.catvod.utils.Prefers;
import java.util.*;
import java.util.function.Consumer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.shadow.api.Shadow;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=UIOptimizationSharedTest.FixtureApp.class,shadows={UIOptimizationSharedTest.PlayerShadow.class,UIOptimizationSharedTest.ApiShadow.class})
public class UIOptimizationSharedTest {
 public static class FixtureApp extends App {@Override public void onCreate(){Init.set(this);}}
 public static class Host extends AppCompatActivity {@Override public void onCreate(Bundle state){setTheme(R.style.Theme_App);super.onCreate(state);}}
 @Implements(PlayerManager.class) public static class PlayerShadow {
  List<Danmaku> sources=new ArrayList<>();boolean enabled,released;String key="site@@@vod@@@E1";Danmaku loaded;int loads;
  @Implementation protected boolean isEmpty(){return false;}
  @Implementation protected boolean isReleased(){return released;}
  @Implementation protected String getKey(){return key;}
  @Implementation protected String getPlaybackTraceId(){return key;}
  @Implementation protected List<Danmaku> getDanmakus(){return sources;}
  @Implementation protected boolean haveDanmaku(){return sources.stream().anyMatch(Danmaku::isSelected);}
  @Implementation protected MediaMetadata getMetadata(){return new MediaMetadata.Builder().setTitle("Fixture Movie").build();}
  @Implementation protected void setDanmakuEnabled(boolean value){enabled=value;}
  @Implementation protected void setDanmaku(Danmaku value){loaded=value;loads++;}
 }
 @Implements(DanmakuApi.class) public static class ApiShadow {
  static int calls;static Consumer<Danmaku> callback;static MediaTitleRequest request;
  @Implementation protected static void search(MediaTitleRequest input,Consumer<Danmaku> found){calls++;request=input;callback=found;}
 }
 Host host;PlayerManager player;PlayerShadow state;DanmakuDialog dialog;
 @Before public void create(){Prefers.getPrefers().edit().clear().commit();ApiShadow.calls=0;ApiShadow.callback=null;host=Robolectric.buildActivity(Host.class).setup().get();ThemeController.applyFromPreferences(host);player=Shadow.newInstanceOf(PlayerManager.class);state=Shadow.extract(player);}
 @After public void close(){if(dialog!=null&&dialog.isAdded())dialog.dismissNow();host.finish();}
 DanmakuDialog showDanmaku(){dialog=DanmakuDialog.create().player(player).identity("site","vod","Fixture Movie","E1");dialog.show(host);host.getSupportFragmentManager().executePendingTransactions();return dialog;}
 void load(boolean enabled){var b=(com.fongmi.android.tv.databinding.DialogDanmakuBinding)ReflectionHelpers.getField(dialog,"binding");b.load.setChecked(enabled);}
 @Test public void turningLoadOffReopeningAndEnablingReusesSourceAndPreservesButtonAndAutoSettings(){
  Danmaku source=Danmaku.from("https://fixture.invalid/a.xml");source.setSelected(true);state.sources.add(source);DanmakuSetting.putLoad(true);DanmakuSetting.putAuto(false);PlayerButtonSetting.putVisible(PlayerButtonSetting.DANMAKU,true);
  showDanmaku();load(false);assertFalse(DanmakuSetting.isLoad());assertFalse(DanmakuSetting.isShow());assertFalse(state.enabled);dialog.dismissNow();showDanmaku();load(true);
  assertTrue(DanmakuSetting.isLoad());assertTrue(DanmakuSetting.isShow());assertTrue(state.enabled);assertSame(source,state.loaded);assertEquals(1,state.loads);assertFalse(DanmakuSetting.isAuto());assertEquals(0,ApiShadow.calls);assertTrue(PlayerButtonSetting.isVisible(PlayerButtonSetting.DANMAKU));
  PlayerButtonSetting.putVisible(PlayerButtonSetting.DANMAKU,false);assertTrue(DanmakuSetting.isLoad());assertTrue(DanmakuSetting.isShow());
 }
 @Test public void noSourceUsesExistingAutoApiOnlyWhenOptedInAndRejectsStalePlayback(){
  DanmakuSetting.putAuto(true);DanmakuSetting.putApiUrl("https://fixture.invalid/api");showDanmaku();load(true);assertEquals(1,ApiShadow.calls);assertEquals("site",ApiShadow.request.getSiteKey());assertEquals("vod",ApiShadow.request.getVodId());
  ApiShadow.callback.accept(Danmaku.from("https://fixture.invalid/auto.xml"));assertEquals(1,state.loads);
  state.key="other-playback";ApiShadow.callback.accept(Danmaku.from("https://fixture.invalid/stale.xml"));assertEquals(1,state.loads);
  state.key="site@@@vod@@@E1";var manual=Danmaku.from("https://fixture.invalid/manual.xml");manual.setSelected(true);state.sources.add(manual);ApiShadow.callback.accept(Danmaku.from("https://fixture.invalid/late.xml"));assertEquals(1,state.loads);state.sources.clear();load(false);ApiShadow.callback.accept(Danmaku.from("https://fixture.invalid/off.xml"));assertEquals(1,state.loads);assertTrue(DanmakuSetting.isAuto());
 }
 @Test public void autoOffOrMissingApiKeepsManualEntryAndDoesNotInventSource(){
  for(boolean auto:new boolean[]{false,true}){DanmakuSetting.putAuto(auto);DanmakuSetting.putLoad(false);showDanmaku();load(true);assertEquals(0,ApiShadow.calls);assertEquals(0,state.loads);assertTrue(state.enabled);assertEquals(auto,DanmakuSetting.isAuto());dialog.dismissNow();}
 }
 @Test public void capabilityIsIndependentOfLoadAndShowButRespectsUserButtonFilter(){
  int[] shown={0};var control=new VodPlayerControlController(new VodPlayerControlController.Host(){public PlayerManager player(){return player;}public void showDanmakuDialog(){shown[0]++;}public void showPlayerInfoDialog(){}public void onDanmakuStateChanged(boolean show){}});
  DanmakuSetting.putLoad(false);DanmakuSetting.putShow(false);assertTrue(control.hasDanmakuControl());control.onDanmakuButton();assertEquals(1,shown[0]);assertFalse(DanmakuSetting.isLoad());
  View button=new View(host);var buttons=Map.of(PlayerButtonSetting.DANMAKU,button);PlayerButtonSetting.putVisible(PlayerButtonSetting.DANMAKU,false);PlayerButtonSetting.applyVisibility(buttons);assertEquals(View.GONE,button.getVisibility());assertFalse(DanmakuSetting.isShow());
  var source=Danmaku.from("https://fixture.invalid/a.xml");source.setSelected(true);state.sources.add(source);DanmakuSetting.putLoad(true);control.onDanmakuButton();assertTrue(state.enabled);assertTrue(DanmakuSetting.isShow());
 }
 @Test public void displayPanelHasReadableNormalFocusSelectedDisabledAndRefreshesTheme(){
  int[] changed={0};DisplayDialog.showPlayerOsd(host,()->changed[0]++);host.getSupportFragmentManager().executePendingTransactions();DisplayDialog display=(DisplayDialog)host.getSupportFragmentManager().getFragments().stream().filter(x->x instanceof DisplayDialog).findFirst().orElseThrow();
  var b=(com.fongmi.android.tv.databinding.DialogDisplayBinding)ReflectionHelpers.getField(display,"binding");assertEquals(View.VISIBLE,b.displayParams.getVisibility());
  for(int mode:new int[]{0,1,0}){Setting.putThemeMode(mode);ThemeController.applyFromPreferences(host);display.onResume();assertColors(b.displayTime);ThemeController.bindDialog(display.requireDialog());org.robolectric.shadows.ShadowLooper.idleMainLooper();assertColors(b.displayTime);}
  ThemeProfile profile=ThemeProfile.defaultProfile();profile.light.primary="#6750A4";profile.light.focus="#006B50";assertTrue(ThemeProfileStore.apply(profile).success());ThemeController.applyFromPreferences(host);display.onResume();assertColors(b.displayTime);
  boolean before=PlayerSetting.isOsdDiagnostics();b.displayParams.performClick();assertEquals(!before,PlayerSetting.isOsdDiagnostics());assertEquals(1,changed[0]);display.dismissNow();
 }
 void assertColors(android.widget.TextView view){
  view.setEnabled(true);view.setFocusableInTouchMode(true);assertTrue(view.requestFocus());view.setSelected(true);assertTrue(view.hasFocus());assertTrue(view.isSelected());
  var background=(StateListDrawable)view.getBackground();ColorStateList text=view.getTextColors();
  for(int[] state:new int[][]{{android.R.attr.state_enabled},{android.R.attr.state_enabled,android.R.attr.state_focused},{android.R.attr.state_enabled,android.R.attr.state_selected},{android.R.attr.state_enabled,android.R.attr.state_selected,android.R.attr.state_focused}}){
   background.setState(state);var shape=(GradientDrawable)background.getCurrent();int fill=shape.getColor().getDefaultColor(),color=text.getColorForState(state,0);assertTrue("contrast "+androidx.core.graphics.ColorUtils.calculateContrast(color,fill),androidx.core.graphics.ColorUtils.calculateContrast(color,fill)>=4.5);
  }
  background.setState(new int[]{android.R.attr.state_enabled,android.R.attr.state_selected,android.R.attr.state_focused});ColorStateList stroke=ReflectionHelpers.getField(background.getCurrent().getConstantState(),"mStrokeColors");assertNotNull(stroke);assertEquals(ThemeController.current().colorFocus(),stroke.getDefaultColor());
  assertTrue((text.getColorForState(new int[]{},0)>>>24)<255);view.setEnabled(false);assertFalse(view.isEnabled());view.setEnabled(true);
 }
}
