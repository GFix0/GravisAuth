package mod.gfix.dev.gravisauth.client;

import mod.gfix.dev.gravisauth.network.GravisAuthPayload;
import mod.gfix.dev.gravisauth.skin.GravisSkinPayload;
import mod.gfix.dev.gravisauth.skin.SkinSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GravisauthClient implements ClientModInitializer {

    public static final Logger LOGGER =
            LoggerFactory.getLogger("gravisauth.client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("GravisAuth client has loaded!");

        SkinSync.registerPayload();

        ClientConfigurationNetworking.registerGlobalReceiver(
                GravisAuthPayload.TYPE,
                (payload, context) -> ClientAuthState.handle(
                        payload.message(),
                        context.networkHandler()
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                GravisSkinPayload.TYPE,
                (payload, context) -> GravisSkinRegistry.apply(
                        payload.entries()
                )
        );

        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> GravisSkinRegistry.clear()
        );
    }
}
