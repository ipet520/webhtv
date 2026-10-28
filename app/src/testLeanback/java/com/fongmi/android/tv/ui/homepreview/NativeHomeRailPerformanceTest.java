package com.fongmi.android.tv.ui.homepreview;

import android.app.Activity;
import android.view.*;
import android.widget.FrameLayout;
import androidx.leanback.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.presenter.HistoryPresenter;
import com.fongmi.android.tv.ui.home.NativeHomeBusinessTest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

/** Structural/event control only. Robolectric wall time is NOT device latency or frame timing. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
public class NativeHomeRailPerformanceTest {
    Activity activity;NativeHomeBrowseView immersive;
    @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();activity.setTheme(R.style.Theme_App);}
    @After public void close(){if(immersive!=null)immersive.release();activity.finish();}
    void layout(View view){view.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));view.layout(0,0,1920,1080);ShadowLooper.idleMainLooper(200,TimeUnit.MILLISECONDS);}
    @Test public void realHistoryPresenterAndImmersiveGridExposeTheirActualParameters(){
        var historyPresenter=new HistoryPresenter(new HistoryPresenter.OnClickListener(){public void onItemClick(History h){}public void onItemDelete(History h){}public boolean onLongClick(){return false;}});
        var objects=new ArrayObjectAdapter(historyPresenter);
        for(int i=0;i<24;i++){History h=new History(){@Override public String getSiteName(){return "本地测试";}};h.setKey("fixture@@@"+i);h.setVodName("本地影片"+i);h.setVodRemarks("测试");h.setVodPic("");objects.add(h);}
        CustomRowPresenter presenter=new CustomRowPresenter(16,FocusHighlight.ZOOM_FACTOR_SMALL,HorizontalGridView.FOCUS_SCROLL_ALIGNED);
        FrameLayout root=new FrameLayout(activity);activity.setContentView(root);
        Presenter.ViewHolder holder=presenter.onCreateViewHolder(root);root.addView(holder.view,new FrameLayout.LayoutParams(-1,-2));
        presenter.onBindViewHolder(holder,new ListRow(objects));
        presenter.setRowViewExpanded(holder,true);presenter.setRowViewSelected(holder,true);layout(root);
        HorizontalGridView classic=((ListRowPresenter.ViewHolder)presenter.getRowViewHolder(holder)).getGridView();
        assertEquals(24,classic.getAdapter().getItemCount());assertTrue(classic.getAdapter() instanceof ItemBridgeAdapter);
        List<Integer> classicSequence=navigate(classic,root);
        immersive=new NativeHomeBrowseView(activity,24,0,()->{});activity.setContentView(immersive);layout(immersive);
        HorizontalGridView rail=immersive.findViewById(R.id.nhp_posters);
        assertEquals(classic.getFocusScrollStrategy(),rail.getFocusScrollStrategy());
        assertEquals(classic.getWindowAlignment(),rail.getWindowAlignment());
        assertEquals(classic.getSmoothScrollSpeedFactor(),rail.getSmoothScrollSpeedFactor(),0);
        List<Integer> immersiveSequence=navigate(rail,immersive);
        assertEquals(List.of(1,2,1,2,1,0),classicSequence);assertEquals(classicSequence,immersiveSequence);
        System.out.println("NATIVE_RAIL_CONTROL classic="+parameters(classic)+" immersive="+parameters(rail));
        System.out.println("NATIVE_RAIL_DPAD classic="+classicSequence+" immersive="+immersiveSequence+" count=24; offline local fixtures, not hardware latency");
        presenter.onUnbindViewHolder(holder);
    }
    static String parameters(HorizontalGridView grid){return "strategy:"+grid.getFocusScrollStrategy()+",window:"+grid.getWindowAlignment()+",keyline:"+grid.getWindowAlignmentOffsetPercent()+",item:"+grid.getItemAlignmentOffsetPercent()+",speed:"+grid.getSmoothScrollSpeedFactor()+",pending:"+grid.getSmoothScrollMaxPendingMoves();}
    List<Integer> navigate(HorizontalGridView grid,View host) {
        grid.setSelectedPosition(0);grid.requestFocus();layout(host);List<Integer> result=new ArrayList<>();
        for(int code:new int[]{KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_LEFT}) {
            boolean handled=grid.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));
            // Robolectric does not perform ViewRootImpl's fallback directional focus search.
            if(!handled&&grid.findFocus()!=null){int direction=code==KeyEvent.KEYCODE_DPAD_RIGHT?View.FOCUS_RIGHT:View.FOCUS_LEFT;View next=grid.findFocus().focusSearch(direction);if(next!=null)next.requestFocus(direction);}
            grid.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));layout(host);result.add(grid.getSelectedPosition());
        }
        return result;
    }
    @Test public void repeatedInViewportFocusDoesNotRebindRemeasureOrRestoreAndLongRailPositionsAreConstantTime(){
        immersive=new NativeHomeBrowseView(activity,2000,0,()->{});activity.setContentView(immersive);layout(immersive);
        HorizontalGridView rail=immersive.findViewById(R.id.nhp_posters);
        View one=rail.findViewHolderForAdapterPosition(0).itemView,two=rail.findViewHolderForAdapterPosition(1).itemView;
        one.requestFocus();layout(immersive);var before=immersive.debugMetrics();int left=one.getLeft(),width=one.getWidth();
        for(int i=0;i<20;i++){two.requestFocus();one.requestFocus();}
        var after=immersive.debugMetrics();
        assertEquals(before.getInt("posterBinds"),after.getInt("posterBinds"));assertEquals(before.getInt("geometry"),after.getInt("geometry"));
        assertEquals(before.getInt("measures"),after.getInt("measures"));assertEquals(before.getInt("restorePosts"),after.getInt("restorePosts"));
        assertEquals(left,one.getLeft());assertEquals(width,one.getWidth());
        List<HomePreviewData.Card> cards=new ArrayList<>();List<Integer> positions=new ArrayList<>();
        // The standalone preview deliberately caps fixtures at 40; build a real 2000-item
        // adapter here rather than accidentally timing only the prototype's cap.
        for(int i=0;i<2000;i++)cards.add(new HomePreviewData.Card(i,"本地影片"+i,"","","","",R.drawable.nhp_poster_blue,R.drawable.nhp_cinema_demo,0));
        HomePreviewPosterAdapter adapter=new HomePreviewPosterAdapter(cards,(card,position)->positions.add(position));
        immersive.findViewById(R.id.nhp_home).requestFocus();rail.setAdapter(adapter);layout(immersive);
        rail.setSelectedPosition(1999,holder->holder.itemView.requestFocus());layout(immersive);layout(immersive);
        assertEquals(1999,(int)positions.get(positions.size()-1));
        System.out.println("NATIVE_RAIL_FOCUS 40 transitions extraBinds="+(after.getInt("posterBinds")-before.getInt("posterBinds"))+" extraMeasures="+(after.getInt("measures")-before.getInt("measures"))+" extraRestores="+(after.getInt("restorePosts")-before.getInt("restorePosts")));
    }
}
