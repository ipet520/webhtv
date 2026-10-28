package com.fongmi.android.tv.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.databinding.ActivityVideoBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.adapter.*;
import com.fongmi.android.tv.ui.dialog.QuickSearchDialog;
import com.fongmi.android.tv.ui.home.NativeHomeBusinessTest;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeHome08SourceSearchTest {
    public static class Search extends SiteViewModel {
        int requests;String keyword;
        @Override public void searchContent(List<Site> sites,String keyword,boolean quick){requests++;this.keyword=keyword;}
    }
    public static class Host extends VideoActivity {
        ActivityVideoBinding binding;Search search=new Search();QuickAdapter quick;
        @Override protected androidx.viewbinding.ViewBinding getBinding(){
            // Exercise production search/controller methods without inflating the unrelated
            // Media3 playback surface (its packaged PNG icons do not decode in this JVM).
            try {
                var constructor=ActivityVideoBinding.class.getDeclaredConstructors()[0];constructor.setAccessible(true);
                Class<?>[] types=constructor.getParameterTypes();Object[] values=new Object[types.length];
                for(int i=0;i<types.length;i++){
                    Class<?> type=types[i];
                    if(type==com.fongmi.android.tv.ui.custom.ProgressLayout.class)values[i]=new com.fongmi.android.tv.ui.custom.ProgressLayout(this,null);
                    else if(type==com.google.android.material.textview.MaterialTextView.class)values[i]=new com.google.android.material.textview.MaterialTextView(this);
                    else if(type==androidx.appcompat.widget.AppCompatImageView.class)values[i]=new androidx.appcompat.widget.AppCompatImageView(this);
                    else if(type==androidx.leanback.widget.HorizontalGridView.class)values[i]=new androidx.leanback.widget.HorizontalGridView(this);
                }
                binding=(ActivityVideoBinding)constructor.newInstance(values);set(this,"mBinding",binding);return binding;
            }catch(Exception error){throw new AssertionError(error);}
        }
        @Override protected boolean customWall(){return false;}
        @Override protected void initView(Bundle state){
            binding=ReflectionHelpers.getField(this,"mBinding");binding.name.setText("影片");binding.part.setTag("影片");
            set(this,"mBroken",new ArrayList<String>());set(this,"mViewModel",search);
            set(this,"mQuickAdapter",quick=new QuickAdapter(this));set(this,"mArrayAdapter",new ArrayAdapter(null));
            set(this,"mEpisodeAdapter",new EpisodeAdapter(null));set(this,"mFlagAdapter",new FlagAdapter(null));set(this,"mPartAdapter",new PartAdapter(null));
        }
        @Override protected void initEvent(){}
        @Override protected boolean shouldBindPlaybackService(){return false;}
    }
    Host host;
    @Before public void setup(){
        host=Robolectric.buildActivity(Host.class,new Intent().putExtra("key","current").putExtra("id","same")).create().get();
        ReflectionHelpers.callInstanceMethod(host.getSupportFragmentManager(),"dispatchActivityCreated");
    }
    @Test public void manualSwitchUsesExistingCandidatesWithoutAutoSelectionOrAnotherSearch() throws Exception {
        host.quick.addAll(List.of(vod("current","same"),vod("other","candidate")));
        set(host,"autoMode",true);set(host,"initAuto",true);call(host,"onChange");
        assertFalse((boolean)ReflectionHelpers.getField(host,"autoMode"));assertFalse((boolean)ReflectionHelpers.getField(host,"initAuto"));assertEquals(0,host.search.requests);
        host.getSupportFragmentManager().executePendingTransactions();QuickSearchDialog popup=ReflectionHelpers.getField(host,"mQuickSearchDialog");assertNotNull(popup);
        popup.onStart();org.robolectric.shadows.ShadowLooper.idleMainLooper();
        androidx.recyclerview.widget.RecyclerView recycler=popup.getDialog().findViewById(com.fongmi.android.tv.R.id.recycler);
        assertEquals(1,recycler.getAdapter().getItemCount());assertEquals("other",((QuickAdapter)recycler.getAdapter()).get(0).getSiteKey());
        call(host,"onChange");org.robolectric.shadows.ShadowLooper.idleMainLooper();assertEquals(1,recycler.getAdapter().getItemCount());popup.dismiss();
    }
    @Test public void firstManualSwitchStartsOneManualRequestAndAcceptsLateResults() throws Exception {
        call(host,"onChange");assertEquals(1,host.search.requests);assertEquals("影片",host.search.keyword);assertFalse((boolean)ReflectionHelpers.getField(host,"initAuto"));
        host.getSupportFragmentManager().executePendingTransactions();QuickSearchDialog popup=ReflectionHelpers.getField(host,"mQuickSearchDialog");popup.onStart();
        Result result=Result.objectFrom("{\"list\":[{\"vod_id\":\"new\",\"vod_name\":\"影片\",\"siteKey\":\"other\"}]}");
        result.getList().get(0).setSite(Site.get("other","其他站源"));var method=VideoActivity.class.getDeclaredMethod("setSearch",Result.class);method.setAccessible(true);method.invoke(host,result);
        org.robolectric.shadows.ShadowLooper.idleMainLooper();androidx.recyclerview.widget.RecyclerView recycler=popup.getDialog().findViewById(com.fongmi.android.tv.R.id.recycler);
        assertEquals(1,recycler.getAdapter().getItemCount());assertEquals(1,host.search.requests);popup.dismiss();
    }
    @Test public void manualAndAutomaticEligibilityRemainSeparate() throws Exception {
        Site site=Site.get("manual-only","手动站源");site.setSearchable(1);site.setChangeable(0);
        var pass=VideoActivity.class.getDeclaredMethod("isPass",Site.class);pass.setAccessible(true);
        set(host,"autoMode",true);assertEquals(false,pass.invoke(host,site));
        set(host,"autoMode",false);assertEquals(true,pass.invoke(host,site));
        site.setSearchable(0);assertEquals(false,pass.invoke(host,site));
    }
    @Test public void emptyOrFailedSessionCanCloseAndRetryWithoutClearingCurrentDetail() throws Exception {
        host.getIntent().putExtra("tmdbMode",true); // Same entry when VideoActivity presents TMDB artwork.
        call(host,"onChange");host.getSupportFragmentManager().executePendingTransactions();
        QuickSearchDialog first=ReflectionHelpers.getField(host,"mQuickSearchDialog");first.onStart();
        var progress=VideoActivity.class.getDeclaredMethod("setSearchProgress",com.fongmi.android.tv.model.SearchProgress.class);progress.setAccessible(true);
        progress.invoke(host,new com.fongmi.android.tv.model.SearchProgress(0,0,true));org.robolectric.shadows.ShadowLooper.idleMainLooper();
        android.widget.TextView empty=first.getDialog().findViewById(com.fongmi.android.tv.R.id.loading);
        assertEquals(host.getString(com.fongmi.android.tv.R.string.search_no_result),empty.getText().toString());
        first.dismiss();host.getSupportFragmentManager().executePendingTransactions();assertEquals("same",host.getIntent().getStringExtra("id"));
        call(host,"onChange");assertEquals(2,host.search.requests);host.getSupportFragmentManager().executePendingTransactions();
        QuickSearchDialog retry=ReflectionHelpers.getField(host,"mQuickSearchDialog");assertNotSame(first,retry);retry.dismiss();
    }
    static Vod vod(String site,String id){Vod v=new Vod();v.setId(id);v.setName("影片");v.setSite(Site.get(site,site));return v;}
    static void set(Object target,String name,Object value){ReflectionHelpers.setField(target,name,value);}
    static void call(Object target,String name) throws Exception {var method=VideoActivity.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(target);}
}
