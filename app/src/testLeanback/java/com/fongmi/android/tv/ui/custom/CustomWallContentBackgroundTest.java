package com.fongmi.android.tv.ui.custom;

import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.home.NativeHomeBusinessTest;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CustomWallContentBackgroundTest {
    AppCompatActivity activity;FrameLayout root;CustomWallView wall;Object owner=new Object();ImageView image;
    @Before public void setup(){
        com.github.catvod.utils.Prefers.getPrefers().edit().clear().commit();
        activity=Robolectric.buildActivity(AppCompatActivity.class).setup().get();activity.setTheme(R.style.Theme_App);
        root=new FrameLayout(activity);wall=new CustomWallView(activity,null);root.addView(wall,new FrameLayout.LayoutParams(-1,-1));activity.setContentView(root);
        image=wall.acquireContentBackground(owner);root.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(180,View.MeasureSpec.EXACTLY));root.layout(0,0,320,180);
    }
    @After public void close(){wall.releaseContentBackground(owner);activity.finish();}
    void draw(){Bitmap bitmap=Bitmap.createBitmap(320,180,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));bitmap.recycle();}
    @Test public void drawableReadinessDoesNotAcknowledgeUntilNativeHostDrawsAndCancellationInvalidatesAck(){
        AtomicInteger callbacks=new AtomicInteger();int setting=Setting.getWall(),theme=Setting.getThemeColor();
        wall.showContentBackground(owner,new ColorDrawable(0xff00ff00),callbacks::incrementAndGet);
        assertEquals(0,callbacks.get());draw();assertEquals(1,callbacks.get());draw();assertEquals(1,callbacks.get());
        wall.showContentBackground(owner,new ColorDrawable(0xffff0000),callbacks::incrementAndGet);wall.clearContentBackground(owner);draw();assertEquals(1,callbacks.get());
        assertEquals(setting,Setting.getWall());assertEquals(theme,Setting.getThemeColor());
    }
    @Test public void newOwnerCannotBeClearedByReleasedOwnerAndConfigRestoresNormally(){
        wall.releaseContentBackground(owner);Object next=new Object();ImageView same=wall.acquireContentBackground(next);assertSame(image,same);
        ColorDrawable art=new ColorDrawable(0xff00ff00);wall.showContentBackground(next,art,()->{});
        wall.releaseContentBackground(owner);wall.clearContentBackground(owner);assertSame(art,image.getDrawable());assertTrue(wall.ownsContentBackground(next));
        Setting.putWall(Setting.WALL_AURORA_GLASS);ConfigEvent.wall();assertSame("wall config cannot replace active film art",art,image.getDrawable());
        wall.releaseContentBackground(next);assertFalse(wall.ownsContentBackground(next));assertNotSame(art,image.getDrawable());assertEquals(Setting.WALL_AURORA_GLASS,Setting.getWall());
        assertNull(image.getTag());
    }
}
