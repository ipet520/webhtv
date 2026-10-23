package com.fongmi.android.tv.ui.dialog;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/** Source contracts for preventing stale one-key-sync work from affecting a later run. */
public class OneKeySyncGenerationGuardTest {

    @Test
    public void preparationAndRequestUseTheCapturedDirectionAndGeneration() throws Exception {
        String source = code();
        String start = methodBody(source, "private void startSync()");

        assertTrue(start.contains("final boolean syncToRemote = toRemote"));
        assertTrue(start.contains("generation = ++syncGeneration"));
        assertTrue(start.contains("() -> isCurrentSync(generation)"));
        assertTrue(start.contains("buildBody(options, syncToRemote"));
        assertTrue(start.contains("if (!isCurrentSync(generation))"));
        assertTrue(start.contains("request(url, body, archive, mpvArchive, loginArchive, 0, generation)"));
    }

    @Test
    public void retryAndCallbacksCannotAffectANewerRun() throws Exception {
        String source = code();
        String retry = methodBody(source, "private void retry(");
        String callback = methodBody(source, "private Callback callback(");

        assertTrue(retry.contains("!isCurrentSync(generation)"));
        assertTrue(retry.contains("retry + 1, generation"));
        assertTrue(callback.contains("!isCurrentSync(generation)"));
        assertTrue(callback.contains("isCurrentGeneration(generation)"));
        assertTrue(callback.contains("deleteArchives(archive, mpvArchive, loginArchive)"));
    }

    @Test
    public void progressAndDestroyedViewAreGenerationGuarded() throws Exception {
        String source = code();
        for (String signature : new String[]{
                "private void updatePrepare(",
                "private void onPrepareProgress(",
                "private void updatePrepareProgress(",
                "private void updateUpload("}) {
            assertTrue(signature, methodBody(source, signature).contains("isCurrentSync(generation)"));
        }
        assertTrue(methodBody(source, "public void onDestroyView()").contains("++syncGeneration"));
        assertTrue(methodBody(source, "private void cancelSync()").contains("++syncGeneration"));
    }

    private static String code() throws Exception {
        Path relative = Path.of("app/src/main/java/com/fongmi/android/tv/ui/dialog/OneKeySyncDialog.java");
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve(relative))) root = root.getParent();
        if (root == null) throw new IllegalStateException("Cannot locate App repository root");
        return Files.readString(root.resolve(relative)).replace("\r\n", "\n");
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) throw new IllegalStateException("Method not found: " + signature);
        int open = source.indexOf('{', start);
        if (open < 0) throw new IllegalStateException("Method body not found: " + signature);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '{') depth++;
            else if (ch == '}' && --depth == 0) return source.substring(open, i + 1);
        }
        throw new IllegalStateException("Unclosed method body: " + signature);
    }
}
