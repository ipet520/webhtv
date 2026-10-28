package com.fongmi.android.tv.ui.homepreview;


import android.graphics.drawable.*;
import android.view.*;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.theme.ThemeTokens;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=com.fongmi.android.tv.ui.home.NativeHomeBusinessTest.FixtureApp.class)
public class NativeHistoryUiFixTest {
    @Test public void deletionUsesTheClassicViewAndOnlyVisibilityChanges() {
        var activity=Robolectric.buildActivity(NativeHistoryBrowseTest.Host.class).setup().get();
        var parent=new android.widget.FrameLayout(activity);
        var holder=new NativeHistoryPosterAdapter.Holder(parent);
        var classic=AdapterVodBinding.inflate(LayoutInflater.from(activity)).delete;
        assertEquals(classic.getClass(),holder.remove.getClass());
        assertEquals(R.id.delete,holder.remove.getId());
        assertEquals(View.GONE,holder.remove.getVisibility());
        assertTrue(holder.remove instanceof android.widget.ImageView);
        assertEquals(((android.widget.ImageView)classic).getDrawable().getIntrinsicWidth(),((android.widget.ImageView)holder.remove).getDrawable().getIntrinsicWidth());
        assertEquals(classic.getBackground().getConstantState().getClass(),holder.remove.getBackground().getConstantState().getClass());
        assertSame(holder.card.poster,holder.remove.getParent());
        assertFalse(holder.remove.isFocusable());
        activity.finish();
    }
    @Test public void lightDarkAndAccentRefreshUseLightOverlayAndShadowWithoutChangingGeometry() {
        var activity=Robolectric.buildActivity(NativeHistoryBrowseTest.Host.class).setup().get();
        TextView button=new TextView(activity);button.setText("Delete");
        var params=new android.widget.FrameLayout.LayoutParams(120,48);button.setLayoutParams(params);
        for(var tokens:new ThemeTokens[]{ThemeTokens.light(),ThemeTokens.dark(),ThemeTokens.light().withColorFocus(0xffff00ff)}) {
            HomePreviewStyles.function(button,tokens);
            var states=(StateListDrawable)button.getBackground();
            var normal=(GradientDrawable)states.getStateDrawable(1);
            int scrim=normal.getColor().getDefaultColor();
            assertTrue((scrim>>>24)>0&&(scrim>>>24)<255);
            assertEquals(HomePreviewStyles.lightSurface(tokens,false),scrim);
            assertTrue("light fill must remain translucent",(scrim>>>24)<=56);
            assertTrue("no dark button block",ColorUtils.calculateLuminance(scrim|0xff000000)>.8);
            assertEquals(tokens.colorShadow(),button.getShadowColor());assertTrue(button.getShadowRadius()>0);
            var focused=(LayerDrawable)states.getStateDrawable(0);
            assertEquals(tokens.colorFocus(),((HomePreviewStyles.Underline)focused.getDrawable(1)).color());
            assertSame(params,button.getLayoutParams());assertEquals(120,params.width);assertEquals(48,params.height);
        }
        activity.finish();
    }
}
