package com.fongmi.android.tv.ui.activity;
import org.junit.Test;
import static org.junit.Assert.*;
public class HistoryFocusInsetTest {
    @Test public void firstRowFullFocusBoundsFitAcrossCardHeightsAndDensities() {
        for(float density:new float[]{1,1.5f,2,3}) for(int height:new int[]{180,240,320,480,640}) {
            int top=HistoryActivity.focusTopInset(height,density);
            float focusedTop=top-height*0.05f;
            assertTrue(focusedTop>=8*density);
            assertTrue(top<=height*0.05f+8*density+2);
            // Same inset for left/middle/right; focus changes do not mutate geometry.
            for(int column=0;column<6;column++) assertEquals(top,HistoryActivity.focusTopInset(height,density));
        }
    }
}
