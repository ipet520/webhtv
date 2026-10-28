package com.fongmi.android.tv.lab;

import com.fongmi.android.tv.App;
import com.github.catvod.Init;
import com.github.catvod.net.OkProxySelector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.net.*;
import java.nio.file.*;
import java.security.KeyStore;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.SocketFactory;
import javax.net.ssl.*;
import okhttp3.*;
import mockwebserver3.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, manifest=Config.NONE, application=MihomoStopThreadTest.FixtureApp.class)
public class MihomoStopThreadTest {
    public static class FixtureApp extends App { @Override public void onCreate() { Init.set(this); } }

    @Test public void mainStopRestoresRoutingBeforeBackgroundHttpsSocketCloseAndRestartWins() throws Exception {
        Init.set(RuntimeEnvironment.getApplication());
        Path file=Files.createTempDirectory("mihomo-tls").resolve("fixture.p12");
        String keytool=Path.of(System.getProperty("java.home"),"bin","keytool.exe").toString();
        Process keygen=new ProcessBuilder(keytool,"-genkeypair","-alias","test","-keyalg","RSA","-keystore",file.toString(),"-storepass","testpass","-keypass","testpass","-dname","CN=localhost","-ext","SAN=dns:localhost,ip:127.0.0.1","-validity","1").redirectErrorStream(true).start();
        keygen.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
        assertEquals(0,keygen.waitFor());
        KeyStore keys=KeyStore.getInstance("PKCS12");
        try(var in=Files.newInputStream(file)){keys.load(in,"testpass".toCharArray());}
        KeyManagerFactory km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(keys,"testpass".toCharArray());
        TrustManagerFactory tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(keys);
        java.security.Provider[] providers=java.security.Security.getProviders();
        for(var provider:providers) if(provider.getClass().getName().startsWith("org.conscrypt.")) java.security.Security.removeProvider(provider.getName());
        SSLContext ssl=SSLContext.getInstance("TLS", "SunJSSE");ssl.init(km.getKeyManagers(),tm.getTrustManagers(),null);
        Thread caller=Thread.currentThread();
        assertSame(android.os.Looper.getMainLooper().getThread(), caller);
java.net.Proxy first=new java.net.Proxy(java.net.Proxy.Type.HTTP,new InetSocketAddress("127.0.0.1",7890));
            java.net.Proxy next=new java.net.Proxy(java.net.Proxy.Type.HTTP,new InetSocketAddress("127.0.0.1",7891));
        OkProxySelector selector=new OkProxySelector();
        selector.setRuntimeProxy(first,Set.of());
        CountDownLatch cleanupGate=new CountDownLatch(1), cleanupEntered=new CountDownLatch(1);
        ConnectionPool pool=selector.registerRuntimePool(new ConnectionPool());
        OkHttpClient client=new OkHttpClient.Builder().addInterceptor(selector.runtimeCleanupInterceptor()).connectionPool(pool).proxy(java.net.Proxy.NO_PROXY).dns(host -> java.util.List.of(InetAddress.getByName("127.0.0.1"))).protocols(java.util.List.of(Protocol.HTTP_1_1)).callTimeout(5,TimeUnit.SECONDS)
                .sslSocketFactory(ssl.getSocketFactory(),(X509TrustManager)tm.getTrustManagers()[0]).build();
        try(MockWebServer server=new MockWebServer()) {
            server.useHttps(ssl.getSocketFactory());server.start();server.enqueue(new MockResponse.Builder().body("ok").build());
            CompletableFuture.runAsync(()->{try(Response response=client.newCall(new Request.Builder().url(server.url("/")).build()).execute()){assertEquals("ok",response.body().string());}catch(Exception e){throw new CompletionException(e);}}).get(10,TimeUnit.SECONDS);
            assertEquals(1,pool.idleConnectionCount());

            var cleanerField=OkProxySelector.class.getDeclaredField("POOL_CLEANER");
            cleanerField.setAccessible(true);
            ExecutorService cleaner=(ExecutorService)cleanerField.get(null);
            cleaner.execute(() -> {cleanupEntered.countDown();try{cleanupGate.await(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
            assertTrue(cleanupEntered.await(5,TimeUnit.SECONDS));


            selector.setRuntimeProxy(null,Set.of());
            assertNull(selector.getRuntimeProxy());
            assertEquals(java.net.Proxy.NO_PROXY,selector.select(URI.create("https://example.com")).get(0));
            selector.setRuntimeProxy(next,Set.of());
            assertEquals(next,selector.getRuntimeProxy());
            server.enqueue(new MockResponse.Builder().body("after-stop").addHeader("Connection", "close").build());
            CompletableFuture<String> pendingRequest=CompletableFuture.supplyAsync(() -> {try(Response response=client.newCall(new Request.Builder().url(server.url("/after-stop")).build()).execute()){return response.body().string();}catch(Exception e){throw new CompletionException(e);}});
            try {pendingRequest.get(150,TimeUnit.MILLISECONDS);fail("request must wait for pending cleanup");}catch(TimeoutException expected) {assertEquals(1,server.getRequestCount());}
            cleanupGate.countDown();
            assertEquals("after-stop",pendingRequest.get(5,TimeUnit.SECONDS));
            Thread cleanupThread=cleaner.submit(Thread::currentThread).get(5,TimeUnit.SECONDS);
            assertNotSame(caller,cleanupThread);
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(pool.idleConnectionCount()!=0 && System.nanoTime()<deadline) Thread.sleep(10);
            assertEquals(0,pool.idleConnectionCount());
            assertEquals(next,selector.getRuntimeProxy());
            assertEquals(java.net.Proxy.NO_PROXY,selector.select(URI.create("http://127.0.0.1/test")).get(0));
        } finally {cleanupGate.countDown();for(int i=0;i<providers.length;i++) if(java.security.Security.getProvider(providers[i].getName())==null) java.security.Security.insertProviderAt(providers[i],i+1);selector.setRuntimeProxy(null,Set.of());client.dispatcher().executorService().shutdown();Files.deleteIfExists(file);Files.deleteIfExists(file.getParent());}
    }
}
