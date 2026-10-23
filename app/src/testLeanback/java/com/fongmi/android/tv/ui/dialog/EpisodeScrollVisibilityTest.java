package com.fongmi.android.tv.ui.dialog;

import android.app.Application;
import android.view.View;
import android.widget.FrameLayout;
import androidx.core.widget.NestedScrollView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class EpisodeScrollVisibilityTest {
    @Test public void realDescendantCoordinatesFollowVariableRowsAndDirectionChanges() {
        NestedScrollView scroll = new NestedScrollView(RuntimeEnvironment.getApplication());
        FrameLayout content = new FrameLayout(scroll.getContext());
        content.setMinimumHeight(40000);
        scroll.addView(content, new NestedScrollView.LayoutParams(600, 40000));
        View item = new View(scroll.getContext());
        content.addView(item, new FrameLayout.LayoutParams(280, 190));
        scroll.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY));
        scroll.layout(0, 0, 600, 720);
        for (int count : new int[]{20,40,100,160}) {
            for (int height : new int[]{40,57,190,206}) {
                for (int index : new int[]{0,count-1,Math.min(119,count-1),Math.min(122,count-1),2,0,count-1}) {
                    int y = 180 + index / 2 * (height + 8);
                    item.layout(0,y,280,y+height);
                    EpisodeScrollVisibility.reveal(scroll,item,8);
                    android.graphics.Rect r = new android.graphics.Rect();
                    item.getDrawingRect(r);
                    scroll.offsetDescendantRectToMyCoords(item,r);
                    assertTrue("top count="+count+" index="+index, r.top-8 >= scroll.getScrollY());
                    assertTrue("bottom count="+count+" index="+index+" rect="+r+" scroll="+scroll.getScrollY()+" content="+content.getHeight(), r.bottom+8 <= scroll.getScrollY()+720);
                    int before=scroll.getScrollY();
                    EpisodeScrollVisibility.reveal(scroll,item,8);
                    assertEquals(before,scroll.getScrollY());
                }
            }
        }
    }
    @Test public void visibleItemDoesNotJumpAndEdgesMoveOnlyRequiredDistance() {
        assertEquals(0,EpisodeScrollVisibility.delta(120,180,100,500));
        assertEquals(-8,EpisodeScrollVisibility.delta(92,180,100,500));
        assertEquals(12,EpisodeScrollVisibility.delta(420,512,100,500));
    }
}
