package mod.gfix.dev.gravisauth.client;

import mod.gfix.dev.gravisauth.network.GravisAuthPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GravisauthClient implements ClientModInitializer {

    public static final Logger LOGGER =
            LoggerFactory.getLogger("gravisauth.client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("GravisAuth client has loaded!");

        ClientConfigurationNetworking.registerGlobalReceiver(
                GravisAuthPayload.TYPE,
                (payload, context) -> ClientAuthState.handle(payload.message())
        );
    }
}
