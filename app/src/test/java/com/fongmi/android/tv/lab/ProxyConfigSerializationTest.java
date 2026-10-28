package com.fongmi.android.tv.lab;

import com.github.catvod.Init;
import com.github.catvod.bean.Proxy;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.net.InetSocketAddress;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, manifest=Config.NONE, application=MihomoRoutingIntegrationTest.FixtureApp.class)
public class ProxyConfigSerializationTest {
    @Test public void productionRuleParserAndRoundTripExcludeDerivedPlatformCaches() {
        Init.set(RuntimeEnvironment.getApplication());
        String json="[{\"name\":\"site\",\"hosts\":[\"*.example.test\"],\"urls\":[\"http://127.0.0.1:7890\"]}]";
        var rules=Proxy.arrayFrom(JsonParser.parseString(json));
        assertEquals(1,rules.size());
        Proxy rule=rules.get(0);
        String before=new Gson().toJson(rule);
        rule.init();
        assertEquals(1,rule.getProxies().size());
        assertEquals(java.net.Proxy.Type.HTTP,rule.getProxies().get(0).type());
        assertEquals(7890,((InetSocketAddress)rule.getProxies().get(0).address()).getPort());
        String after=new Gson().toJson(rule);
        assertEquals(before,after);
        assertEquals(Set.of("name","hosts","urls"),JsonParser.parseString(after).getAsJsonObject().keySet());
        Proxy copy=new Gson().fromJson(after,Proxy.class);
        copy.init();
        assertEquals(rule.getHosts(),copy.getHosts());
        assertEquals(rule.getUrls(),copy.getUrls());
        assertEquals(rule.getProxies(),copy.getProxies());
    }
    @Test public void serializedCacheFieldsCannotInjectRuntimeProxyState() {
        Proxy proxy=new Gson().fromJson("{\"hosts\":[\"*\"],\"urls\":[\"http://127.0.0.1:7891\"],\"proxies\":[{}],\"uris\":[{}],\"wildcard\":false}",Proxy.class);
        assertTrue(proxy.getProxies().isEmpty());
        proxy.init();
        assertEquals(1,proxy.getProxies().size());
        assertEquals(7891,((InetSocketAddress)proxy.getProxies().get(0).address()).getPort());
    }
}
