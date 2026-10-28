package com.fongmi.android.tv.ui.detail;
import org.junit.Test;
import static com.fongmi.android.tv.ui.detail.FusionFloatingLayout.*;
import static org.junit.Assert.*;
public class FusionFloatingLayoutTest {
    final Area viewport=new Area(0,0,1920,1080);
    @Test public void onlyScrolledAboveAndNearlyInvisibleEntersWithHysteresis(){
        assertFalse(floating(false,true,true,false,100,600,viewport));
        assertFalse(floating(false,true,true,false,1100,1600,viewport));
        assertTrue(floating(false,true,true,false,-490,10,viewport));
        assertTrue(floating(true,true,true,false,-400,100,viewport));
        assertFalse(floating(true,true,true,false,-340,160,viewport));
        for(int i=0;i<30;i++)assertTrue(floating(true,true,true,false,-450,50+i%2,viewport));
    }
    @Test public void eligibilityPauseLoadingAndInvalidGeometry(){
        assertFalse(floating(false,true,false,true,-500,0,viewport));
        assertTrue(floating(true,true,false,true,-500,0,viewport));
        assertFalse(floating(true,false,true,true,-500,0,viewport));
        assertFalse(floating(false,true,true,false,0,0,viewport));
        assertFalse(floating(true,true,true,false,-500,0,new Area(0,0,0,0)));
        // There is deliberately no isPlaying input: an established paused video keeps its layout.
        assertTrue(floating(true,true,true,false,-500,0,viewport));
    }
    @Test public void allFourEdgesKeepFreeAlongEdgeCoordinates(){
        Area safe=new Area(24,24,1896,1056);
        assertEquals(new Box(24,333,400,225),snap(safe,new Box(30,333,400,225)));
        assertEquals(new Box(1496,333,400,225),snap(safe,new Box(1490,333,400,225)));
        assertEquals(new Box(555,24,400,225),snap(safe,new Box(555,30,400,225)));
        assertEquals(new Box(555,831,400,225),snap(safe,new Box(555,825,400,225)));
    }
    @Test public void defaultAndRelativePositionSurviveResizeAndBadSavedValues(){
        Position p=new Position(1,.42f);
        for(Area safe:new Area[]{viewport,new Area(80,48,3760,2100),new Area(20,20,400,250)}){
            Box b=box(safe,2,p);assertTrue(b.x()>=safe.left());assertTrue(b.y()>=safe.top());assertTrue(b.x()+b.width()<=safe.right());assertTrue(b.y()+b.height()<=safe.bottom());
            assertEquals(16/9f,b.width()/(float)b.height(),.015f);
            assertEquals(.42f,position(safe,b).y(),.02f);
        }
        Box initial=box(viewport,2,new Position(1,0));assertEquals(1920,initial.x()+initial.width());assertEquals(0,initial.y());
        Box bad=box(viewport,2,new Position(Float.NaN,Float.POSITIVE_INFINITY));assertEquals(0,bad.x());assertEquals(0,bad.y());
    }
}
