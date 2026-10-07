package mod.gfix.dev.gravisauth.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GravisauthClient implements ClientModInitializer {

    public static final Logger LOGGER =
            LoggerFactory.getLogger("gravisauth.client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("GravisAuth client has loaded!");
    }
}
