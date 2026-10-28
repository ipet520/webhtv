package com.fongmi.android.tv.ui.homepreview;

import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.theme.ThemeTokens;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.ui.home.*;
import androidx.leanback.widget.HorizontalGridView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeHomeReleaseVisualTest {
    NativeHistoryBrowseTest.Host activity;
    NativeHomeBrowseView view;
    @Before public void setup(){activity=Robolectric.buildActivity(NativeHistoryBrowseTest.Host.class).setup().visible().get();}
    @After public void close(){if(view!=null)view.release();activity.finish();}
    TextView text(){TextView v=new TextView(activity);v.setText("动漫");v.setTextSize(16);v.setGravity(Gravity.CENTER);return v;}
    static int fill(Drawable drawable){if(drawable instanceof LayerDrawable layers)drawable=layers.getDrawable(0);return ((GradientDrawable)drawable).getColor().getDefaultColor();}
    @Test public void categoryFourStatesSeparateThemeFillFromBrightFocusAndKeepGeometry(){
        for(ThemeTokens tokens:new ThemeTokens[]{ThemeTokens.light(),ThemeTokens.dark(),ThemeTokens.dark().withColorFocus(0xffff00ff)}){
            TextView v=text();var parent=new FrameLayout(activity);parent.addView(v,new FrameLayout.LayoutParams(180,64));activity.setContentView(parent);HomePreviewStyles.pill(v,true,tokens);
            parent.measure(View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(200,View.MeasureSpec.EXACTLY));parent.layout(0,0,800,200);
            StateListDrawable states=(StateListDrawable)v.getBackground();
            assertTrue(states.getStateDrawable(0) instanceof LayerDrawable);assertTrue(states.getStateDrawable(1) instanceof LayerDrawable);
            int selected=HomePreviewStyles.alpha(tokens.colorPrimary(),.28f),normal=HomePreviewStyles.alpha(tokens.colorSurfaceContainer(),.30f);
            assertEquals(selected,fill(states.getStateDrawable(0)));assertEquals(normal,fill(states.getStateDrawable(1)));assertEquals(selected,fill(states.getStateDrawable(2)));assertEquals(normal,fill(states.getStateDrawable(3)));
            v.setSelected(true);v.requestFocus();HomePreviewStyles.categoryFocus(v,true);ShadowLooper.idleMainLooper(150,TimeUnit.MILLISECONDS);assertEquals(1.05f,v.getScaleX(),.001f);assertEquals(180,v.getWidth());
            parent.setFocusableInTouchMode(true);parent.requestFocus();HomePreviewStyles.categoryFocus(v,false);ShadowLooper.idleMainLooper(150,TimeUnit.MILLISECONDS);assertTrue(v.isSelected());assertFalse(v.hasFocus());assertEquals(1,v.getScaleX(),.001f);
        }
    }
    @Test public void categoryFocusHasBrightAndDarkEdgesOnBothLightAndDarkContent(){
        TextView v=text();HomePreviewStyles.pill(v,true,ThemeTokens.dark());var states=(StateListDrawable)v.getBackground();
        for(int state:new int[]{0,1}){
            Drawable d=states.getStateDrawable(state);d.setBounds(0,0,240,80);Bitmap bitmap=Bitmap.createBitmap(240,80,Bitmap.Config.ARGB_8888);d.draw(new Canvas(bitmap));
            boolean bright=false,dark=false;
            for(int x=0;x<18;x++){int p=bitmap.getPixel(x,40);int a=p>>>24,r=(p>>16)&255,g=(p>>8)&255,b=p&255;bright|=a>240&&r>220&&g>220&&b>220;dark|=a>120&&r<40&&g<40&&b<40;}
            assertTrue("neutral bright outline",bright);assertTrue("contrast rim over pale backdrop",dark);bitmap.recycle();
        }
    }
    @Test public void renderVisualContactSheetForLightDarkBackdrops()throws Exception{
        String folder=System.getenv("NATIVE_HOME_11_SCREENSHOT_DIR");if(folder==null)return;
        Bitmap sheet=Bitmap.createBitmap(1600,720,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(sheet);Paint label=new Paint(3);label.setTextSize(23);
        canvas.drawColor(0xff202020);int row=0;
        for(ThemeTokens tokens:new ThemeTokens[]{ThemeTokens.light(),ThemeTokens.dark()})for(int backdrop:new int[]{0xfff7f7f7,0xff15202b}){
            int y=row++*180;label.setColor(backdrop==0xfff7f7f7?0xff111111:0xffffffff);// Each row is composited over its own art brightness.
            canvas.drawRect(0,y,1600,y+180,new Paint(){{setColor(backdrop);}});
            canvas.drawText((tokens.equals(ThemeTokens.light())?"Light theme":"Dark theme")+" / "+(backdrop==0xfff7f7f7?"bright art":"dark art"),16,y+28,label);
            String[] captions={"Top normal","Top focused","Normal","Selected","Focused","Selected + focused"};
            for(int i=0;i<6;i++){
                TextView v=text();var host=new FrameLayout(activity);host.setFocusableInTouchMode(true);host.addView(v,new FrameLayout.LayoutParams(220,90));activity.setContentView(host);
                if(i<2)HomePreviewStyles.function(v,tokens);else HomePreviewStyles.pill(v,true,tokens);
                v.setSelected(i==3||i==5);boolean focused=i==1||i>=4;if(focused)v.requestFocus();else host.requestFocus();
                host.measure(View.MeasureSpec.makeMeasureSpec(240,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(100,View.MeasureSpec.EXACTLY));host.layout(0,0,240,100);
                canvas.save();canvas.translate(16+i*263,y+42);v.draw(canvas);canvas.restore();canvas.drawText(captions[i],16+i*263,y+162,label);
            }
        }
        try(var out=new java.io.FileOutputStream(new java.io.File(folder,"native-home-11-states.png"))){sheet.compress(Bitmap.CompressFormat.PNG,100,out);}sheet.recycle();
    }
    record Call(String type,Consumer<Result> ok){}
    final List<Call> calls=new ArrayList<>();final List<NativeHomeBrowseView.PreparationWindow> windows=new ArrayList<>();NativeHomeBrowseController controller;
    Result cards(String prefix,boolean home){StringBuilder j=new StringBuilder("{");if(home)j.append("\"class\":[{\"type_id\":\"A\",\"type_name\":\"电影\"},{\"type_id\":\"B\",\"type_name\":\"剧集\"},{\"type_id\":\"C\",\"type_name\":\"动漫\"}],");j.append("\"pagecount\":1,\"list\":[");for(int i=0;i<24;i++){if(i>0)j.append(',');j.append("{\"vod_id\":\"").append(prefix).append(i).append("\",\"vod_name\":\"film ").append(prefix).append(i).append("\"}");}return Result.objectFrom(j.append("]}").toString());}
    void layout(){view.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));view.layout(0,0,1920,1080);ShadowLooper.idleMainLooper(300,TimeUnit.MILLISECONDS);}
    void createBusiness(){
        view=new NativeHomeBrowseView(activity,0,0,()->{});activity.setContentView(view);layout();
        controller=new NativeHomeBrowseController(new NativeHomeBrowseController.Gateway(){public void home(Consumer<Result> ok,Consumer<Throwable> error){calls.add(new Call("home",ok));}public void category(String site,String type,int page,HashMap<String,String> extend,Consumer<Result> ok,Consumer<Throwable> error){calls.add(new Call(type,ok));}public void cancel(){}},r->{});
        view.setBusiness(controller,List::of,s->{},v->{},v->false,c->{});view.setPreparationWindowListener(windows::add);controller.attach("site",c->view.renderBusiness(c,"site"));calls.get(0).ok.accept(cards("H",true));layout();layout();
    }
    void assertWindow(String prefix){assertFalse(windows.isEmpty());var w=windows.get(windows.size()-1);assertSame(controller.state(),w.context());assertFalse(w.candidates().isEmpty());assertTrue(w.candidates().size()<=10);for(var c:w.candidates())assertTrue(c.vod().getId(),c.vod().getId().startsWith(prefix));}
    void category(String name){controller.focusCategory(name);controller.flushCategory();}
    @Test public void visibleWindowsAreRepublishedForSecondThirdAndCachedCategoriesWithoutRecreatingRail(){
        createBusiness();assertWindow("H");Object rail=view.findViewById(R.id.nhp_posters),adapter=((HorizontalGridView)rail).getAdapter();
        for(String name:new String[]{"A","B","C"}){category(name);calls.get(calls.size()-1).ok.accept(cards(name,false));layout();layout();assertWindow(name);}
        int requests=calls.size();category("A");layout();layout();assertWindow("A");assertEquals(requests,calls.size());category("B");layout();layout();assertWindow("B");assertEquals(requests,calls.size());assertSame(rail,view.findViewById(R.id.nhp_posters));assertSame(adapter,((HorizontalGridView)rail).getAdapter());controller.detach();
    }
    @Test public void delayedLayoutAndRapidABCABADiscardOldCategoryCallbacks(){
        createBusiness();category("A");Call a=calls.get(calls.size()-1);category("B");Call b=calls.get(calls.size()-1);category("C");Call c=calls.get(calls.size()-1);
        a.ok.accept(cards("A",false));b.ok.accept(cards("B",false));c.ok.accept(cards("C",false));ShadowLooper.idleMainLooper(); // intentionally defer Grid measurement
        layout();layout();assertWindow("C");
        category("A");Call nextA=calls.get(calls.size()-1);category("B");Call nextB=calls.get(calls.size()-1);category("A");Call finalA=calls.get(calls.size()-1);
        nextA.ok.accept(cards("A-old",false));nextB.ok.accept(cards("B-old",false));finalA.ok.accept(cards("A",false));layout();layout();assertWindow("A");controller.detach();
    }
}
