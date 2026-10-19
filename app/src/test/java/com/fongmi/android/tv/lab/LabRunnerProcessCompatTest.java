package com.fongmi.android.tv.lab;

import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LabRunnerProcessCompatTest {

    @Test
    public void probeTreatsOnlyATerminatedProcessAsDead() {
        assertTrue(LabRunner.isAlive(fake(false)));
        assertFalse(LabRunner.isAlive(fake(true)));
        assertFalse(LabRunner.isAlive(null));
    }

    @Test
    public void labSourcesProbeThroughExitValueInsteadOfApi26Call() throws Exception {
        String runner = stripComments(read("app/src/main/java/com/fongmi/android/tv/lab/LabRunner.java"));
        String terminal = stripComments(read("app/src/main/java/com/fongmi/android/tv/lab/LabTerminalActivity.java"));
        assertTrue(runner.contains("process.exitValue()"));
        assertFalse(runner.contains(".isAlive()"));
        assertFalse(terminal.contains(".isAlive()"));
        assertTrue(terminal.contains("LabRunner.isAlive("));
    }

    private static Process fake(boolean terminated) {
        return new Process() {
            @Override public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
            @Override public InputStream getInputStream() { return InputStream.nullInputStream(); }
            @Override public InputStream getErrorStream() { return InputStream.nullInputStream(); }
            @Override public int waitFor() { return 0; }
            @Override public int exitValue() {
                if (terminated) return 0;
                throw new IllegalThreadStateException("still running");
            }
            @Override public void destroy() { }
        };
    }

    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//[^\\n]*", " ");
    }

    private static String read(String name) throws Exception {
        Path path = Path.of(name);
        if (!Files.exists(path)) path = Path.of("..").resolve(name).normalize();
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}