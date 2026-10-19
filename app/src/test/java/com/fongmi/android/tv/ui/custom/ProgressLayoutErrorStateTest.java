package com.fongmi.android.tv.ui.custom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.google.android.material.button.MaterialButton;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class)
public class ProgressLayoutErrorStateTest {

    public static class Host extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle state) {
            setTheme(R.style.Theme_App);
            super.onCreate(state);
        }
    }

    @Test
    public void errorShowsReasonAndRetryThenRestoresEmptyAndProgressChrome() {
        try (var host = Robolectric.buildActivity(Host.class).setup()) {
            ProgressLayout layout = new ProgressLayout(host.get(), null);
            AtomicBoolean retried = new AtomicBoolean();

            layout.showError(R.string.error_config_load, "Cannot connect", R.string.error_retry,
                    view -> retried.set(true));

            assertTrue(layout.isError());
            assertTrue(layout.getBackground() instanceof ColorDrawable);
            assertEquals(ContextCompat.getColor(host.get(), R.color.webhtv_color_overlay_dark),
                    ((ColorDrawable) layout.getBackground()).getColor());
            assertEquals("Cannot connect", ((TextView) layout.findViewById(R.id.description)).getText().toString());
            assertEquals(View.GONE, layout.findViewById(R.id.illustration).getVisibility());
            MaterialButton retry = layout.findViewById(R.id.action);
            assertEquals(View.VISIBLE, retry.getVisibility());
            assertEquals(host.get().getString(R.string.error_retry), retry.getText().toString());
            retry.performClick();
            assertTrue("retry action must invoke the original callback", retried.get());

            layout.showError(R.string.error_config_load, "Still unavailable", R.string.error_retry,
                    view -> retried.set(false));
            assertEquals("Still unavailable", ((TextView) layout.findViewById(R.id.description)).getText().toString());

            layout.showEmpty();
            assertFalse(layout.isError());
            assertEquals(View.VISIBLE, layout.findViewById(R.id.illustration).getVisibility());
            assertEquals(host.get().getString(R.string.error_empty),
                    ((TextView) layout.findViewById(R.id.text)).getText().toString());

            layout.showProgress();
            assertTrue("leaving ERROR/EMPTY must clear only the temporary scrim", layout.getBackground() == null);
        }
    }
}