package com.fongmi.android.tv.ui.homepreview;

import android.app.Activity;
import android.view.*;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.setting.Setting;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=com.fongmi.android.tv.ui.home.NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeHistoryBrowseTest {
    public static class Host extends androidx.appcompat.app.AppCompatActivity {
        @Override protected void onCreate(android.os.Bundle state){setTheme(R.style.Theme_App);super.onCreate(state);}
    }
    Host activity;NativeHistoryBrowseView view;List<History> opened=new ArrayList<>(),details=new ArrayList<>(),deleted=new ArrayList<>();int manages,reports,longs;
    @Before public void setup(){com.github.catvod.utils.Prefers.getPrefers().edit().clear().commit();Setting.putTmdbConfig("{}");activity=Robolectric.buildActivity(Host.class).setup().visible().get();com.fongmi.android.tv.theme.ThemeController.applyFromPreferences(activity);view=new NativeHistoryBrowseView(activity,new NativeHistoryBrowseView.Actions(){public void open(History item){opened.add(item);}public void details(History item){details.add(item);}public void delete(History item,int p){deleted.add(item);}public boolean longClick(){longs++;return true;}public void manage(){manages++;}public void report(){reports++;}});activity.setContentView(view);layout();}
    @After public void close(){view.release();activity.finish();}
    static History item(String key){History h=new History();h.setKey("site@@@"+key);h.setVodName("Film "+key);h.setVodRemarks("E12");h.setPosition(60000);h.setDuration(240000);return h;}
    void layout(){view.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));view.layout(0,0,1920,1080);ShadowLooper.idleMainLooper(350,TimeUnit.MILLISECONDS);}
    void settle()throws Exception{for(int i=0;i<30;i++){Thread.sleep(5);layout();}}
    void key(int code){view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));layout();}
    @Test public void detailsAndPosterAreSeparateAndProgressUsesNativeMilliseconds()throws Exception{
        History h=item("one");view.submit(List.of(h),false);settle();assertTrue(view.rail.hasFocus());assertEquals(2500,view.progress.getProgress());assertEquals("E12",view.episode.getText().toString());assertEquals(h.getPlaybackTimeText(),view.time.getText().toString());
        view.resume.performClick();view.rail.findViewHolderForAdapterPosition(0).itemView.performClick();assertEquals(1,opened.size());assertEquals(1,details.size());assertSame(h,opened.get(0));assertSame(h,details.get(0));assertTrue(deleted.isEmpty());assertEquals(activity.getString(R.string.native_history_view_details),view.resume.getText().toString());
        var holder=(NativeHistoryPosterAdapter.Holder)view.rail.findViewHolderForAdapterPosition(0);assertEquals("E12",holder.details.getText().toString());
        for(int i=0;i<holder.card.poster.getChildCount();i++)assertFalse(holder.card.poster.getChildAt(i) instanceof android.widget.ProgressBar);
    }
    @Test public void topResumeRailNavigationAndHistoryBackRemainSeparate()throws Exception{
        view.submit(List.of(item("one"),item("two")),false);settle();key(KeyEvent.KEYCODE_DPAD_UP);assertTrue(view.resume.hasFocus());key(KeyEvent.KEYCODE_DPAD_UP);assertTrue(view.deleteButton.hasFocus());key(KeyEvent.KEYCODE_DPAD_RIGHT);assertTrue(view.reportButton.hasFocus());key(KeyEvent.KEYCODE_DPAD_LEFT);assertTrue(view.deleteButton.hasFocus());key(KeyEvent.KEYCODE_DPAD_DOWN);assertTrue(view.resume.hasFocus());key(KeyEvent.KEYCODE_DPAD_DOWN);assertTrue(view.rail.hasFocus());
        view.rail.setSelectedPosition(1);layout();assertFalse(view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK)));assertEquals(1,view.rail.getSelectedPosition());
        view.deleteButton.performClick();view.reportButton.performClick();assertEquals(1,manages);assertEquals(1,reports);
    }
    @Test public void reorderDeleteAndEmptyRestoreByOriginalDisplayIdentity()throws Exception{
        History a=item("a"),b=item("b"),c=item("c");view.submit(List.of(a,b,c),false);settle();view.restore(1);layout();assertEquals("Film b",view.title.getText().toString());
        Object rail=view.rail,adapter=view.rail.getAdapter();view.submit(List.of(c,a,b),false);settle();assertEquals(2,view.rail.getSelectedPosition());assertTrue(view.rail.findViewHolderForAdapterPosition(2).itemView.hasFocus());
        view.submit(List.of(c,a),false);settle();assertEquals(1,view.rail.getSelectedPosition());assertEquals("Film a",view.title.getText().toString());assertSame(rail,view.rail);assertSame(adapter,view.rail.getAdapter());
        view.submit(List.of(),false);settle();assertEquals("",view.title.getText().toString());assertEquals(0,view.progress.getProgress());assertFalse(view.resume.isEnabled());assertFalse(view.deleteButton.isEnabled());assertEquals(View.VISIBLE,view.empty.getVisibility());assertTrue(view.reportButton.hasFocus());
    }
    @Test public void deleteStateOnlyUsesOriginalDeleteCallbackAndLongPress()throws Exception{
        History h=item("a");view.submit(List.of(h),false);settle();View card=view.rail.findViewHolderForAdapterPosition(0).itemView;card.performLongClick();assertEquals(1,longs);view.submit(List.of(h),true);settle();card.performClick();assertEquals(List.of(h),deleted);assertTrue(opened.isEmpty());
        var holder=(NativeHistoryPosterAdapter.Holder)view.rail.findViewHolderForAdapterPosition(0);assertEquals(View.VISIBLE,holder.remove.getVisibility());view.submit(List.of(h),false);settle();assertEquals(View.GONE,holder.remove.getVisibility());
    }
    @Test public void progressUnknownAndInvalidDurationNeverInventPercentage()throws Exception{
        History h=item("unknown");h.setDuration(0);view.submit(List.of(h),false);settle();assertEquals(View.INVISIBLE,view.progress.getVisibility());assertTrue(view.time.getText().toString().endsWith("--:--"));
        h=h.copy();h.setDuration(100);h.setPosition(Long.MAX_VALUE);assertEquals(10000,NativeHistoryPosterAdapter.progress(h));h.setPosition(-1);assertEquals(0,NativeHistoryPosterAdapter.progress(h));
    }
    @Test public void insetsOnlyMoveContentAndThemeManagementUsesHomeUnderline()throws Exception{
        view.submit(List.of(item("a")),false);settle();int baseline=view.top.getLeft();view.dispatchApplyWindowInsets(new WindowInsetsCompat.Builder().setSystemWindowInsets(Insets.of(60,24,0,30)).build().toWindowInsets());layout();assertTrue(view.top.getLeft()>baseline);assertEquals(0,view.leftShade.getLeft());assertEquals(1920,view.leftShade.getRight());assertEquals(1080,view.bottomShade.getBottom());assertEquals(0,view.getPaddingLeft());
        view.deleteButton.requestFocus();view.refreshTheme();assertTrue(((android.graphics.drawable.LayerDrawable)((android.graphics.drawable.StateListDrawable)view.deleteButton.getBackground()).getStateDrawable(0)).getDrawable(1) instanceof HomePreviewStyles.Underline);
    }
    @Test public void everyEntryReturnsToFirstRatherThanRestoringOldPoster()throws Exception{
        List<History> list=List.of(item("a"),item("b"),item("c"));view.submit(list,false);settle();view.restore(2);layout();assertEquals(2,view.rail.getSelectedPosition());
        view.active(false);view.enter();view.submit(list,false);settle();assertEquals(0,view.rail.getSelectedPosition());assertTrue(view.rail.findViewHolderForAdapterPosition(0).itemView.hasFocus());assertEquals("Film a",view.title.getText().toString());
    }
    @Test public void snapshotResumeAndRailHaveSeparateBoundsAtTelevisionSize()throws Exception{
        List<History> list=new ArrayList<>();for(int i=0;i<12;i++)list.add(item("layout-"+i));view.submit(list,false);settle();
        assertTrue(view.resumeRow.getBottom()<=view.rail.getTop());assertTrue(view.top.getBottom()<view.hero.getTop());assertEquals(view.episode.getParent(),view.time.getParent());assertTrue(view.progress.getHeight()<=8);
        String folder=System.getenv("NATIVE_HISTORY_SCREENSHOT_DIR");if(folder!=null){
            android.widget.FrameLayout parent=(android.widget.FrameLayout)view.getParent();
            var wall=new com.fongmi.android.tv.ui.custom.CustomWallView(activity,null);parent.addView(wall,0,new android.widget.FrameLayout.LayoutParams(-1,-1));wall.acquireContentBackground(view);wall.showContentBackground(view,activity.getDrawable(R.drawable.nhp_cinema_demo),()->{});
            for(int i=0;i<view.rail.getChildCount();i++)if(view.rail.getChildViewHolder(view.rail.getChildAt(i)) instanceof NativeHistoryPosterAdapter.Holder holder)holder.card.image.setImageResource(new int[]{R.drawable.nhp_poster_amber,R.drawable.nhp_poster_blue,R.drawable.nhp_poster_green}[i%3]);
            parent.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));parent.layout(0,0,1920,1080);
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(1920,1080,android.graphics.Bitmap.Config.ARGB_8888);parent.draw(new android.graphics.Canvas(bitmap));try(var out=new java.io.FileOutputStream(new java.io.File(folder,"native-history-layout.png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();wall.releaseContentBackground(view);
        }
    }
}
