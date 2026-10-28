package com.fongmi.android.tv.ui.detail;

/** Geometry only: no player, input scheduling or Android lifecycle ownership. */
public final class FusionFloatingLayout {
    private FusionFloatingLayout() {}
    public record Area(int left,int top,int right,int bottom) {
        public int width(){return Math.max(0,right-left);}
        public int height(){return Math.max(0,bottom-top);}
    }
    public record Box(int x,int y,int width,int height) {}
    public record Position(float x,float y) {}
    public static boolean floating(boolean current,boolean allowed,boolean ready,boolean loading,
                                   int spacerTop,int spacerBottom,Area viewport) {
        if(!allowed || viewport.width()==0 || viewport.height()==0 || spacerBottom<=spacerTop)return false;
        if(!ready && !(current&&loading))return false;
        float visible=Math.max(0,Math.min(spacerBottom,viewport.bottom)-Math.max(spacerTop,viewport.top))/(float)(spacerBottom-spacerTop);
        if(current)return visible<.30f;
        return visible<=.05f && spacerBottom<=viewport.top+(spacerBottom-spacerTop)*.05f;
    }
    public static Box box(Area safe,float density,Position saved) {
        int width=Math.min(safe.width(),Math.round(Math.min(480*density,Math.max(Math.min(180*density,safe.width()*.4f),safe.width()*.24f))));
        int height=Math.round(width*9/16f);
        if(height>safe.height()){height=safe.height();width=Math.round(height*16/9f);}
        return at(safe,new Box(0,0,width,height),saved);
    }
    public static Box at(Area safe,Box size,Position position) {
        return new Box(safe.left+Math.round(Math.max(0,safe.width()-size.width)*unit(position.x)),
                safe.top+Math.round(Math.max(0,safe.height()-size.height)*unit(position.y)),size.width,size.height);
    }
    public static Box clamp(Area safe,Box box) {
        return new Box(Math.max(safe.left,Math.min(box.x,safe.right-box.width)),Math.max(safe.top,Math.min(box.y,safe.bottom-box.height)),box.width,box.height);
    }
    public static Box snap(Area safe,Box original) {
        Box b=clamp(safe,original);
        int left=b.x-safe.left,right=safe.right-b.x-b.width,top=b.y-safe.top,bottom=safe.bottom-b.y-b.height;
        int nearest=Math.min(Math.min(left,right),Math.min(top,bottom));
        if(nearest==left)return new Box(safe.left,b.y,b.width,b.height);
        if(nearest==right)return new Box(safe.right-b.width,b.y,b.width,b.height);
        if(nearest==top)return new Box(b.x,safe.top,b.width,b.height);
        return new Box(b.x,safe.bottom-b.height,b.width,b.height);
    }
    public static Position position(Area safe,Box b) {
        return new Position(safe.width()>b.width?unit((b.x-safe.left)/(float)(safe.width()-b.width)):1,
                safe.height()>b.height?unit((b.y-safe.top)/(float)(safe.height()-b.height)):0);
    }
    private static float unit(float f){return Float.isFinite(f)?Math.max(0,Math.min(1,f)):0;}
}
