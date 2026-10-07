package mod.gfix.dev.gravisauth.mixin;

import mod.gfix.dev.gravisauth.Gravisauth;
import mod.gfix.dev.gravisauth.network.GravisAuthNetworking;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConfigurationNetworkHandler.class)
public class ServerConfigurationNetworkHandlerMixin {

    @Inject(
            method = "sendConfigurations",
            at = @At("TAIL")
    )
    private void gravisauth$sendAuthenticationRequest(
            CallbackInfo ci
    ) {
        if (!Gravisauth.CONFIG.enabled) {
            return;
        }

        Gravisauth.LOGGER.info(
                "[GravisAuth] Sending authentication request."
        );

        GravisAuthNetworking.requestAuthentication(
                (ServerConfigurationNetworkHandler) (Object) this
        );
    }
}