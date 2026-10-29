package com.fongmi.android.tv.ui.dialog;
import android.os.Bundle;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.Player;
import androidx.media3.common.MediaMetadata;
import androidx.media3.ui.danmaku.DanmakuController;
import com.fongmi.android.tv.*;
import com.fongmi.android.tv.api.DanmakuApi;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.bean.DanmakuMatchCache;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.engine.PlaySpec;
import com.fongmi.android.tv.setting.*;
import com.fongmi.android.tv.title.MediaTitleRequest;
import com.github.catvod.Init;
import com.github.catvod.utils.Prefers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.shadow.api.Shadow;
import java.util.*;
import java.lang.reflect.Proxy;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=DanmakuDynamicEnableTest.FixtureApp.class,shadows=DanmakuDynamicEnableTest.RendererShadow.class)
public class DanmakuDynamicEnableTest {
 public static class FixtureApp extends App {@Override public void onCreate(){Init.set(this);}}
 public static class Host extends AppCompatActivity {@Override public void onCreate(Bundle b){setTheme(R.style.Theme_App);super.onCreate(b);}}
 @Implements(DanmakuController.class) public static class RendererShadow {
  Player player;boolean enabled;List<String> urls=new ArrayList<>();List<Long> positions=new ArrayList<>();
  @Implementation protected void setPlayer(Player value){player=value;}
  @Implementation protected void setEnabled(boolean value){enabled=value;}
  @Implementation protected void setDataSource(Uri uri){urls.add(uri.toString());positions.add(player.getCurrentPosition());}
 }
 Host host;PlayerManager manager;PlaySpec spec;Player video;DanmakuDialog dialog;RendererShadow render;long position=120000;int seeks,mediaChanges;
 @Before public void before(){Prefers.getPrefers().edit().clear().commit();host=Robolectric.buildActivity(Host.class).setup().get();}
 @After public void after(){DanmakuApi.cancel();if(dialog!=null&&dialog.isAdded())dialog.dismissNow();host.finish();}
 void session(boolean load,boolean source){
  DanmakuSetting.putLoad(load);DanmakuSetting.putShow(load);
  String payload="{\"url\":\"https://fixture.invalid/video.m3u8\",\"header\":{\"X-Fixture\":\"retained\"}"+(source?",\"danmaku\":[{\"name\":\"site\",\"url\":\"https://fixture.invalid/site.xml\"}]":"")+"}";
  Result original=Result.objectFrom(payload);spec=PlaySpec.from(original,"site@@@vod@@@E1",new MediaMetadata.Builder().setTitle("Film").build());
  try {
   var unsafeType=java.lang.Class.forName("sun.misc.Unsafe");var field=unsafeType.getDeclaredField("theUnsafe");field.setAccessible(true);
   manager=(PlayerManager)unsafeType.getMethod("allocateInstance",java.lang.Class.class).invoke(field.get(null),PlayerManager.class);
  } catch(ReflectiveOperationException e){throw new AssertionError(e);}
  video=(Player)Proxy.newProxyInstance(Player.class.getClassLoader(),new Class[]{Player.class},(proxy,method,args)->{
   switch(method.getName()){
    case "getCurrentPosition":return position;
    case "seekTo":seeks++;return null;
    case "setMediaItem":case "setMediaItems":mediaChanges++;return null;
    case "getPlaybackState":return Player.STATE_READY;
    case "isPlaying":return true;
    case "equals":return proxy==args[0];case "hashCode":return System.identityHashCode(proxy);case "toString":return "SamePlayer";
   }
   if(method.getReturnType()==boolean.class)return false;if(method.getReturnType()==long.class)return 0L;if(method.getReturnType()==int.class)return 0;return null;
  });
  var trace=new com.fongmi.android.tv.player.PlaybackTrace();trace.begin();ReflectionHelpers.setField(manager,"playbackTrace",trace);
  ReflectionHelpers.setField(manager,"spec",spec);ReflectionHelpers.setField(manager,"player",video);ReflectionHelpers.setField(manager,"userPlaybackSpeed",1.25f);
  var controller=Shadow.newInstanceOf(DanmakuController.class);controller.setPlayer(video);render=Shadow.extract(controller);ReflectionHelpers.setField(manager,"danmakuController",controller);
 }
 void open(){dialog=DanmakuDialog.create().player(manager).identity("site","vod","Film","E1");dialog.show(host);host.getSupportFragmentManager().executePendingTransactions();}
 void load(boolean value){var binding=(com.fongmi.android.tv.databinding.DialogDanmakuBinding)ReflectionHelpers.getField(dialog,"binding");binding.load.setChecked(value);}
 void stable(){assertEquals(120000,position);assertEquals(0,seeks);assertEquals(0,mediaChanges);assertSame(video,ReflectionHelpers.getField(manager,"player"));assertSame(spec,ReflectionHelpers.getField(manager,"spec"));assertEquals("retained",manager.getHeaders().get("X-Fixture"));assertEquals(1.25f,manager.getSpeed(),0);}
 void cached(){var cache=new DanmakuMatchCache();cache.put("site","vod","E1","Film","Film",Danmaku.from("https://fixture.invalid/auto.xml"));Setting.putDanmakuMatchCache(cache);}
 MediaTitleRequest request(){return MediaTitleRequest.builder().siteKey("site").vodId("vod").rawTitle("Film").episodeName("E1").build();}
 @Test public void beforePlaybackAndDynamicEnableReachSameExistingLoaderAtCurrentTime(){
  session(true,true);manager.setDanmaku(spec.getDanmakus().get(0));assertEquals(List.of("https://fixture.invalid/site.xml"),render.urls);
  session(false,true);assertTrue(spec.getDanmakus().isEmpty());open();load(true);
  assertEquals(List.of("https://fixture.invalid/site.xml"),render.urls);assertEquals(List.of(120000L),render.positions);assertTrue(render.enabled);assertTrue(manager.haveDanmaku());assertTrue(DanmakuSetting.isLoad());assertTrue(DanmakuSetting.isShow());assertFalse(DanmakuSetting.isAuto());stable();
 }
 @Test public void existingSourceAndOffOnReuseDataWithoutReparsingOrDuplicateLoading(){
  session(false,true);open();load(true);assertEquals(1,render.urls.size());load(false);assertFalse(render.enabled);load(true);assertTrue(render.enabled);assertEquals(1,render.urls.size());stable();
  dialog.dismissNow();session(false,true);open();load(true);assertEquals(1,render.urls.size());stable();
 }
 @Test public void noSiteSourceUsesOptedInAutoCacheButOffAndInvalidApiDoNotInventData(){
  session(false,false);DanmakuSetting.putAuto(true);DanmakuSetting.putApiUrl("https://fixture.invalid/api");cached();open();load(true);ShadowLooper.idleMainLooper();assertEquals(List.of("https://fixture.invalid/auto.xml"),render.urls);stable();
 }
 @Test public void autoOffAndNoApiKeepManualChoicesAndDoNotClaimLoaded(){
  for(boolean auto:new boolean[]{false,true}){session(false,false);DanmakuSetting.putAuto(auto);open();load(true);ShadowLooper.idleMainLooper();assertTrue(render.urls.isEmpty());assertFalse(manager.haveDanmaku());assertEquals(auto,DanmakuSetting.isAuto());assertTrue(render.enabled);dialog.dismissNow();}stable();
 }
 @Test public void cancelledEnableAndReenableRejectQueuedOldResult(){
  session(false,false);DanmakuSetting.putAuto(true);DanmakuSetting.putApiUrl("https://fixture.invalid/api");cached();open();load(true);load(false);ShadowLooper.idleMainLooper();assertTrue(render.urls.isEmpty());load(true);ShadowLooper.idleMainLooper();assertEquals(1,render.urls.size());stable();
 }
 @Test public void episodeChangeReleaseAndManualSourceRejectLateAuto(){
  for(int mode:new int[]{0,1,2}){session(false,false);DanmakuSetting.putAuto(true);DanmakuSetting.putApiUrl("https://fixture.invalid/api");cached();open();load(true);
   if(mode==0){ReflectionHelpers.setField(manager,"spec",PlaySpec.from("site@@@vod@@@E1","https://fixture.invalid/next.m3u8",Map.of(),null));com.fongmi.android.tv.player.PlaybackTrace trace=ReflectionHelpers.getField(manager,"playbackTrace");trace.begin();}
   if(mode==1)ReflectionHelpers.setField(manager,"player",null);
   if(mode==2)manager.setDanmaku(Danmaku.from("https://fixture.invalid/manual.xml"));
   ShadowLooper.idleMainLooper();assertEquals(mode==2?List.of("https://fixture.invalid/manual.xml"):List.of(),render.urls);dialog.dismissNow();}
 }
 @Test public void apiCancelAndReplacementInvalidateAlreadyPostedCacheCallbacks(){
  cached();List<Danmaku> results=new ArrayList<>();DanmakuApi.search(request(),results::add);DanmakuApi.cancel();ShadowLooper.idleMainLooper();assertTrue(results.isEmpty());
  DanmakuApi.search(request(),item->{throw new AssertionError("Old lookup delivered");});DanmakuApi.search(request(),results::add);ShadowLooper.idleMainLooper();assertEquals(1,results.size());
 }
}
