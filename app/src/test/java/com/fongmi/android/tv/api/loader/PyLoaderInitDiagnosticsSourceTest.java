package com.fongmi.android.tv.api.loader;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PyLoaderInitDiagnosticsSourceTest {

    @Test
    public void pythonSpiderInitializationFailuresReachTheAppDiagnosticLogBeforeFallback() throws Exception {
        String source = read("src/main/java/com/fongmi/android/tv/api/loader/PyLoader.java");
        int log = source.indexOf("SpiderDebug.log(PY_TAG, e);");
        int fallback = source.indexOf("return new SpiderNull();", log);
        assertTrue("initialization exceptions must use the existing python-spider diagnostic category",
                source.contains("PY_TAG = \"python-spider\""));
        assertTrue("the full exception must reach the App diagnostic log before SpiderNull fallback",
                log >= 0 && fallback > log);
        assertFalse("unstructured stderr output must not be the only failure record",
                source.contains("e.printStackTrace();"));
    }

    private static String read(String relative) throws Exception {
        Path path = Path.of(relative);
        if (!Files.exists(path)) path = Path.of("app").resolve(relative);
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}