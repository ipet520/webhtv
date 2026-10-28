package com.fongmi.android.tv.ui.dialog;

import android.app.Dialog;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.ui.home.NativeHomeBusinessTest;
import com.fongmi.android.tv.ui.adapter.QuickAdapter;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
public class NativeHome08DialogsTest {
    AppCompatActivity activity;
    @Before public void setup(){activity=Robolectric.buildActivity(AppCompatActivity.class).setup().get();activity.setContentView(new FrameLayout(activity));}
    @After public void close(){activity.finish();}
    @Test public void customAppearanceShellHasFinalWidthBeforeWindowIsShown(){
        Dialog dialog=LightDialog.create(activity,"外观与语言",new LinearLayout(activity),"关闭",null,null,null);
        var before=dialog.getWindow().getAttributes();int expected=Math.min(Math.round(com.fongmi.android.tv.utils.ResUtil.getScreenWidth(activity)*.52f),com.fongmi.android.tv.utils.ResUtil.dp2px(560));
        assertEquals("initial window must never use theme wrap-content width",expected,before.width);
        assertEquals(WindowManager.LayoutParams.WRAP_CONTENT,before.height);dialog.show();
        org.robolectric.shadows.ShadowLooper.idleMainLooper();assertEquals(expected,dialog.getWindow().getAttributes().width);dialog.dismiss();
    }
    @Test public void emptyPopupConsumesEarlyAndLateBatchesAndSelection(){
        List<Vod> chosen=new ArrayList<>();QuickSearchDialog popup=QuickSearchDialog.create().listener(chosen::add);
        popup.show(activity);popup.addAll(List.of(vod("early")));
        activity.getSupportFragmentManager().executePendingTransactions();org.robolectric.shadows.ShadowLooper.idleMainLooper();
        androidx.recyclerview.widget.RecyclerView recycler=popup.getDialog().findViewById(R.id.recycler);
        assertEquals(1,recycler.getAdapter().getItemCount());assertEquals(View.VISIBLE,recycler.getVisibility());
        popup.setProgress(1,3,false);popup.addAll(List.of(vod("late")));org.robolectric.shadows.ShadowLooper.idleMainLooper();
        assertEquals(2,recycler.getAdapter().getItemCount());Vod item=((QuickAdapter)recycler.getAdapter()).get(1);
        popup.onItemClick(item);assertEquals("late",chosen.get(0).getId());org.robolectric.shadows.ShadowLooper.idleMainLooper();
        popup.addAll(List.of(vod("closed")));assertEquals(1,chosen.size());
    }
    @Test public void emptyFinishedPopupCanCloseAndNewSessionConsumesNewResults(){
        QuickSearchDialog popup=QuickSearchDialog.create();popup.show(activity);activity.getSupportFragmentManager().executePendingTransactions();
        popup.setProgress(2,2,true);org.robolectric.shadows.ShadowLooper.idleMainLooper();
        TextView loading=popup.getDialog().findViewById(R.id.loading);assertEquals(activity.getString(R.string.search_no_result),loading.getText().toString());
        popup.dismiss();activity.getSupportFragmentManager().executePendingTransactions();
        QuickSearchDialog retry=QuickSearchDialog.create().items(List.of(vod("retry")));retry.show(activity);activity.getSupportFragmentManager().executePendingTransactions();org.robolectric.shadows.ShadowLooper.idleMainLooper();
        androidx.recyclerview.widget.RecyclerView recycler=retry.getDialog().findViewById(R.id.recycler);assertEquals(1,recycler.getAdapter().getItemCount());retry.dismiss();
    }
    static Vod vod(String id){Vod v=new Vod();v.setId(id);v.setName(id);return v;}
}
