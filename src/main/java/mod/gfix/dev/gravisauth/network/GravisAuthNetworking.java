package mod.gfix.dev.gravisauth.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;

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
}