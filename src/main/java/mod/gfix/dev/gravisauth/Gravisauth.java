package mod.gfix.dev.gravisauth;

import net.fabricmc.api.ModInitializer;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import mod.gfix.dev.gravisauth.config.GravisAuthConfig;
import mod.gfix.dev.gravisauth.config.GravisAuthConfigManager;
import mod.gfix.dev.gravisauth.database.GravisDatabase;
import mod.gfix.dev.gravisauth.network.GravisAuthNetworking;
import mod.gfix.dev.gravisauth.skin.SkinSync;

public class Gravisauth implements ModInitializer {

    public static final String MOD_ID = "gravisauth";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static GravisAuthConfig CONFIG;

    @Override
    public void onInitialize() {
        LOGGER.info("GravisAuth has loaded!");

        CONFIG = GravisAuthConfigManager.load();

        LOGGER.info("GravisAuth server functionality enabled: {}", CONFIG.enabled);

        /*
         * Force the transformed classes to load now. Mixin applies its
         * injections at class-load time, so a bad injection point should
         * stop the server at startup instead of disconnecting players
         * during the first login.
         */
        try {
            ClassLoader loader = Gravisauth.class.getClassLoader();

            /*
             * Use class literals, not string names: Loom remaps literals to
             * the production intermediary names, while a hard-coded Yarn
             * name would not resolve on a real server.
             *
             * initialize=false keeps the check side-effect free.
             */
            Class.forName(
                    ServerConfigurationNetworkHandler.class.getName(),
                    false,
                    loader
            );
            Class.forName(
                    ServerCommonNetworkHandler.class.getName(),
                    false,
                    loader
            );
            LOGGER.info("GravisAuth mixin targets loaded successfully.");
        } catch (Throwable throwable) {
            LOGGER.error("GravisAuth mixins failed to apply.", throwable);
            throw new RuntimeException(
                    "GravisAuth mixins failed to apply",
                    throwable
            );
        }

        GravisAuthNetworking.initialize();
        SkinSync.initialize();

        if (!CONFIG.enabled) {
            LOGGER.info("GravisAuth server functionality is disabled.");
            return;
        }

        LOGGER.info(
                "Database config: {}:{} / {}",
                CONFIG.database.host,
                CONFIG.database.port,
                CONFIG.database.database
        );

        try {
            GravisDatabase.connect().close();
            LOGGER.info("Successfully connected to Gravis database!");
        } catch (Exception e) {
            LOGGER.error("Failed to connect to Gravis database!", e);
        }
    }
}
