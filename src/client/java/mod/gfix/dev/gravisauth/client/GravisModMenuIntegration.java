package mod.gfix.dev.gravisauth.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Entry point used by Mod Menu to add the GravisAuth configuration button.
 *
 * Mod Menu is optional: this class is only loaded when Mod Menu itself is
 * installed and asks for the "modmenu" entry point.
 */
public final class GravisModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return GravisModMenuScreen::new;
    }
}
