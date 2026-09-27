package mod.gfix.dev.gravisauth;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Gravisauth implements ModInitializer {

    public static final String MOD_ID = "gravisauth";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("GravisAuth has loaded!");
    }
}
