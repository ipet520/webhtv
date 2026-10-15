package com.fongmi.android.tv.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class)
public class PlayerOsdThemeContractTest {

    @Test
    public void osdIconStatesPairWithTheirThemeSurfaces() {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();
            View root = LayoutInflater.from(context).inflate(R.layout.adapter_player_osd, null, false);
            ImageView visible = root.findViewById(R.id.visible);
            ColorStateList tint = visible.getImageTintList();
            assertEquals(tokens.colorOnSurfaceVariant(), tint.getDefaultColor());
            assertEquals(tokens.colorOnPrimaryContainer(),
                    tint.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));
            assertEquals(tokens.colorOnPrimaryContainer(),
                    tint.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed}, 0));
            assertEquals(tokens.colorOnSecondaryContainer(),
                    tint.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_selected}, 0));
            assertTrue(ThemeContrast.ratio(tokens.colorOnPrimaryContainer(), tokens.colorPrimaryContainer()) >= 4.5);
            assertTrue(ThemeContrast.ratio(tokens.colorOnSecondaryContainer(), tokens.colorSecondaryContainer()) >= 4.5);
        }
    }

    @Test
    public void rawOsdDialogsJoinTheThemeRebindContract() throws Exception {
        for (String path : new String[]{
                "src/leanback/java/com/fongmi/android/tv/ui/dialog/PlayerOsdDialog.java",
                "src/mobile/java/com/fongmi/android/tv/ui/dialog/PlayerOsdDialog.java"}) {
            String source = read(path);
            assertTrue(source.contains("ThemeController.bindDialog(dialog)"));
        }
    }

    @Test
    public void osdUsesComponentResourcesInsteadOfTheSharedLightCard() throws Exception {
        for (String path : new String[]{
                "src/leanback/res/layout/adapter_player_osd.xml",
                "src/mobile/res/layout/adapter_player_osd.xml"}) {
            String source = read(path);
            assertTrue(source.contains("@drawable/selector_player_osd_row"));
            assertTrue(source.contains("@drawable/selector_player_osd_switch"));
            assertTrue(source.contains("@color/player_osd_icon_tint"));
        }
    }

    @Test
    public void sharedCardCallersRemainIsolatedFromTheOsdMigration() throws Exception {
        String playerConfig = read("src/main/res/layout/adapter_player_button_config.xml");
        String shellProxy = read("src/main/res/layout/adapter_shell_proxy_rule.xml");
        assertTrue(playerConfig.contains("@drawable/selector_settings_card"));
        assertTrue(shellProxy.contains("@drawable/selector_settings_card"));
        assertTrue(read("src/main/res/drawable/selector_git_cloud_card.xml").contains("?attr/colorSurfaceContainer"));
        assertTrue(read("src/main/res/drawable/selector_settings_card.xml").contains("?attr/colorSecondaryContainer"));
        assertTrue(read("src/leanback/res/layout/adapter_ad_rule.xml").contains("@drawable/selector_dialog_switch"));
        assertTrue(read("src/leanback/res/layout/adapter_player_osd.xml").contains("@drawable/selector_player_osd_switch"));
        assertTrue(read("src/mobile/res/layout/adapter_player_osd.xml").contains("@drawable/selector_player_osd_switch"));
    }

    private static Context themed(boolean dark) {
        Configuration configuration = new Configuration(
                RuntimeEnvironment.getApplication().getResources().getConfiguration());
        configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (dark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        Context context = RuntimeEnvironment.getApplication().createConfigurationContext(configuration);
        context.setTheme(R.style.Theme_App);
        return context;
    }

    private static String read(String relative) throws Exception {
        Path root = Files.exists(Path.of("src")) ? Path.of("") : Path.of("app");
        return Files.readString(root.resolve(relative), StandardCharsets.UTF_8);
    }
}
