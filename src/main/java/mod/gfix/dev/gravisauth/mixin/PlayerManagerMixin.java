package mod.gfix.dev.gravisauth.mixin;

import mod.gfix.dev.gravisauth.Gravisauth;
import mod.gfix.dev.gravisauth.auth.LoginWhitelistPhase;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

/**
 * Access control is keyed by the Gravis UUID, which only exists after the
 * client has authenticated. The login phase still runs the vanilla
 * whitelist check with the UUID the client advertised, so only the
 * "not whitelisted" outcome is dropped there.
 *
 * Bans are untouched, and the configuration phase checks the whitelist
 * again with the authenticated profile.
 */
@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    private static final String WHITELIST_KEY =
            "multiplayer.disconnect.not_whitelisted";

    @Inject(
            method = "checkCanJoin",
            at = @At("RETURN"),
            cancellable = true
    )
    private void gravisauth$deferWhitelistUntilAuthenticated(
            SocketAddress address,
            PlayerConfigEntry entry,
            CallbackInfoReturnable<Text> cir
    ) {
        if (!Gravisauth.CONFIG.enabled || !LoginWhitelistPhase.isActive()) {
            return;
        }

        Text result = cir.getReturnValue();

        if (result != null
                && result.getContent() instanceof TranslatableTextContent content
                && WHITELIST_KEY.equals(content.getKey())) {
            cir.setReturnValue(null);
        }
    }
}
