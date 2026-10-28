package com.fongmi.android.tv.ui.home;

import com.fongmi.android.tv.bean.*;
import com.fongmi.android.tv.setting.Setting;
import com.google.gson.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Metadata shape from the two read-only device cache records, not a TV display test. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class)
public class NativeHome10BackgroundTest {
    @Before public void setup(){com.github.catvod.utils.Prefers.getPrefers().edit().clear().commit();Setting.putTmdbConfig("{\"apiKey\":\"test-only\",\"language\":\"zh-CN\"}");}
    @Test public void twoActualCachedIdentitiesDoNotAcquireUnrelatedLandscape() throws Exception {
        for(int id:new int[]{336458,336036}){
            String name=id==336458?"谜林":"谎日沉影",vodId=id==336458?"/voddetail/133262.html":"/voddetail/132282.html";
            Vod vod=Vod.objectFrom("{\"vod_id\":\""+vodId+"\",\"vod_name\":\""+name+"\"}");
            TmdbItem item=new TmdbItem(id,"tv",name,"","","","");
            TmdbMatchCache cache=Setting.getTmdbMatchCache();cache.put("玩偶",vodId,name,item,"zh-CN");Setting.putTmdbMatchCache(cache);
            var service=new com.fongmi.android.tv.service.TmdbService(){
                @Override public JsonObject detail(TmdbItem matched,TmdbConfig config,boolean related){assertEquals(id,matched.getTmdbId());return JsonParser.parseString("{\"id\":"+id+",\"first_air_date\":\"2026-10-01\",\"backdrop_path\":null,\"poster_path\":\"/portrait.jpg\",\"images\":{\"backdrops\":[],\"logos\":[]}}").getAsJsonObject();}
            };
            var info=new NativeHomeHero.ExistingTmdb(service).load(Site.get("玩偶","玩偶"),vod,TmdbConfig.effectiveCurrent());
            assertNotNull(info);assertEquals("",info.backdrop());assertTrue(info.poster().endsWith("/portrait.jpg"));
        }
    }
}
