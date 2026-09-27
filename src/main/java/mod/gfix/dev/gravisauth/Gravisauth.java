package mod.gfix.dev.gravisauth;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import mod.gfix.dev.gravisauth.config.GravisAuthConfig;
import mod.gfix.dev.gravisauth.config.GravisAuthConfigManager;

public class Gravisauth implements ModInitializer {

    public static final String MOD_ID = "gravisauth";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static GravisAuthConfig CONFIG;

    @Override
    public void onInitialize() {
        LOGGER.info("GravisAuth has loaded!");

        CONFIG = GravisAuthConfigManager.load();
    }
}
