package mod.gfix.dev.gravisauth.mixin;

import com.mojang.authlib.GameProfile;
import mod.gfix.dev.gravisauth.auth.LoginWhitelistPhase;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The login phase runs before GravisAuth knows the account, so the
 * vanilla whitelist check can only see the UUID the client sent. Mark
 * that phase; {@link PlayerManagerMixin} lets it through.
 */
@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {

    @Inject(method = "tickVerify", at = @At("HEAD"))
    private void gravisauth$enterLoginPhase(
            GameProfile profile,
            CallbackInfo ci
    ) {
        LoginWhitelistPhase.enter();
    }

    @Inject(method = "tickVerify", at = @At("RETURN"))
    private void gravisauth$leaveLoginPhase(
            GameProfile profile,
            CallbackInfo ci
    ) {
        LoginWhitelistPhase.exit();
    }
}
