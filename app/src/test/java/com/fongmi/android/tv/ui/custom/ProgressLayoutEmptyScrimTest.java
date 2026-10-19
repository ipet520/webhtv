package com.fongmi.android.tv.ui.custom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class)
public class ProgressLayoutEmptyScrimTest {

    public static class Host extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle state) {
            setTheme(R.style.Theme_App);
            super.onCreate(state);
        }
    }

    @Test
    public void emptyStateUsesThemeOverlayAndLeavingItRemovesOnlyTheTemporaryScrim() {
        try (var host = Robolectric.buildActivity(Host.class).setup()) {
            ProgressLayout layout = new ProgressLayout(host.get(), null);
            int expected = ContextCompat.getColor(host.get(), R.color.webhtv_color_overlay_dark);

            layout.showEmpty();
            assertTrue("EMPTY must install a full-surface scrim", layout.getBackground() instanceof ColorDrawable);
            assertEquals(expected, ((ColorDrawable) layout.getBackground()).getColor());

            layout.showProgress();
            assertNull("leaving EMPTY for PROGRESS must remove the temporary scrim", layout.getBackground());
            layout.showEmpty();
            assertTrue(layout.getBackground() instanceof ColorDrawable);

            layout.showContent();
            assertNull("leaving EMPTY for CONTENT must remove the temporary scrim", layout.getBackground());
        }
    }
}