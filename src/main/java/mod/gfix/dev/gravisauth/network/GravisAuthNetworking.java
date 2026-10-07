package mod.gfix.dev.gravisauth.network;

import mod.gfix.dev.gravisauth.Gravisauth;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;

public final class GravisAuthNetworking {

    private GravisAuthNetworking() {
    }

    public static void initialize() {

        PayloadTypeRegistry.configurationS2C()
                .register(
                        GravisAuthPayload.TYPE,
                        GravisAuthPayload.CODEC
                );

        PayloadTypeRegistry.configurationC2S()
                .register(
                        GravisAuthPayload.TYPE,
                        GravisAuthPayload.CODEC
                );

        ServerConfigurationNetworking.registerGlobalReceiver(
                GravisAuthPayload.TYPE,
                (payload, context) -> {

                    System.out.println(
                            "[GravisAuth] Received client response: "
                                    + payload.message()
                    );
                }
        );
    }

    public static void requestAuthentication(
            ServerConfigurationNetworkHandler handler
    ) {
        if (!Gravisauth.CONFIG.enabled) {
            return;
        }

        ServerConfigurationNetworking.send(
                handler,
                new GravisAuthPayload("request")
        );

        Gravisauth.LOGGER.info(
                "[GravisAuth] Sent authentication request to client."
        );
    }
}