package com.fongmi.android.tv.theme;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.view.ContextThemeWrapper;
import com.fongmi.android.tv.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, qualifiers="notnight",manifest=Config.NONE,application=com.fongmi.android.tv.lab.MihomoRoutingIntegrationTest.FixtureApp.class)
public class C55LocalUiPreservationTest {
    private Path root(){Path p=Path.of(System.getProperty("user.dir")).toAbsolutePath();while(p!=null&&!Files.exists(p.resolve("app/src/main/AndroidManifest.xml")))p=p.getParent();return p;}
    @Test public void updateCardsKeepStateBackgroundAndTextOnRepeatedPaletteChanges() {
        var ctx=new ContextThemeWrapper(RuntimeEnvironment.getApplication(),R.style.Theme_App);
        View root=LayoutInflater.from(ctx).inflate(R.layout.dialog_update,null);
        View row=root.findViewById(R.id.stableItem);
        TextView name=root.findViewById(R.id.stableName);
        ThemeProfile profile=ThemeProfile.defaultProfile();profile.light.primary="#284D8A";profile.light.focus="#C2185B";
        ThemeTokens custom=ThemeResolver.resolve(ThemeMode.LIGHT,ThemeSeed.NONE,0,0,profile,null,false);
        for(ThemeTokens tokens:new ThemeTokens[]{ThemeTokens.light(),ThemeTokens.dark(),custom,ThemeTokens.light()}) {
            ThemeBinder.bind(root,ThemeTokens.light(),tokens);
            for(int[] states:new int[][]{{android.R.attr.state_enabled},{android.R.attr.state_enabled,android.R.attr.state_focused},
                    {android.R.attr.state_enabled,android.R.attr.state_selected},{android.R.attr.state_enabled,android.R.attr.state_pressed},
                    {android.R.attr.state_enabled,android.R.attr.state_selected,android.R.attr.state_focused}}) {
                row.getBackground().setState(states);
                var shape=(GradientDrawable)row.getBackground().getCurrent();
                assertNotNull(shape.getColor());
                int fill=shape.getColor().getDefaultColor();
                int foreground=name.getTextColors().getColorForState(states,name.getCurrentTextColor());
                assertTrue("update text/fill ratio="+ThemeContrast.ratio(foreground,fill),ThemeContrast.ratio(foreground,fill)>=4.5);
            }
        }
    }
    @Test public void exitSecondaryForegroundMatchesDynamicFillInsteadOfFixedLightOverride() throws Exception {
        String background=Files.readString(root().resolve("app/src/leanback/res/drawable/selector_exit_confirm_secondary.xml"));
        String text=Files.readString(root().resolve("app/src/leanback/res/color/exit_confirm_secondary_text.xml"));
        assertTrue(background.contains("?attr/colorPrimaryContainer"));assertTrue(background.contains("?attr/colorSurfaceContainer"));
        assertTrue(text.contains("?attr/colorOnPrimaryContainer"));assertTrue(text.contains("?attr/colorOnSurfaceVariant"));
        for(ThemeTokens t:new ThemeTokens[]{ThemeTokens.light(),ThemeTokens.dark()}) {
            assertTrue(ThemeContrast.ratio(t.colorOnPrimaryContainer(),t.colorPrimaryContainer())>=4.5);
            assertTrue(ThemeContrast.ratio(t.colorOnSurfaceVariant(),t.colorSurfaceContainer())>=4.5);
        }
    }
    @Test public void mobileLogBandHasMatchedDynamicSurfaceAndReadableFilterInk() throws Exception {
        for(String flavor:new String[]{"mobile","leanback"}) {
            String xml=Files.readString(root().resolve("app/src/"+flavor+"/res/layout/dialog_ad_block_stats.xml"));
            int column=xml.indexOf("android:layout_height=\"88dp\"");
            assertTrue(column>0);
            String band=xml.substring(column,xml.indexOf("</HorizontalScrollView>",column));
            assertTrue(flavor,band.contains("android:background=\"?attr/colorSurfaceContainerHigh\""));
        }
        for(ThemeTokens t:new ThemeTokens[]{ThemeTokens.light(),ThemeTokens.dark()}) {
            assertTrue(ThemeContrast.ratio(t.colorPrimary(),t.colorSurfaceContainerHigh())>=4.5);
        }
    }
    @Test public void fixedPaleRatingBadgeKeepsReadableInkAfterDynamicPaletteBinding() {
        var ctx=new ContextThemeWrapper(RuntimeEnvironment.getApplication(),R.style.Theme_App);
        View root=LayoutInflater.from(ctx).inflate(R.layout.dialog_ai_recommendation_info,null);
        TextView rating=root.findViewById(R.id.doubanRating);
        for(ThemeTokens tokens:new ThemeTokens[]{ThemeTokens.light(),ThemeTokens.dark(),ThemeTokens.light().withColorFocus(0xFFE91E63)}) {
            ThemeBinder.bind(root,ThemeTokens.light(),tokens);
            int fill=((GradientDrawable)rating.getBackground()).getColor().getDefaultColor();
            assertEquals(0xFFEAF6EE,fill);
            assertEquals(0xFF146C2E,rating.getCurrentTextColor());
            assertTrue(ThemeContrast.ratio(rating.getCurrentTextColor(),fill)>=4.5);
        }
    }
    @Test public void fixedSurfaceCandidatesRemainThemePairedRatherThanGlobalFixedInk() throws Exception {
        for(String file:new String[]{"app/src/main/res/layout/adapter_custom_csp.xml","app/src/main/res/layout/dialog_mpv_config_create.xml",
                "app/src/leanback/res/drawable/selector_ad_stats_item.xml","app/src/leanback/res/drawable/shape_ad_stats_content.xml"}) {
            String source=Files.readString(root().resolve(file));
            assertFalse(file,source.contains("@color/fixed_light_text_muted"));
            assertTrue(file,source.contains("?attr/color") || source.contains("@color/webhtv_color"));
        }
    }
}
