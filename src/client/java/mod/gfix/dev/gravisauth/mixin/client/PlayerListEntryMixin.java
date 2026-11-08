package mod.gfix.dev.gravisauth.mixin.client;

import mod.gfix.dev.gravisauth.client.GravisSkinRegistry;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the vanilla (Steve/Alex) skin with the one the server sent.
 *
 * AbstractClientPlayerEntity#getSkin() delegates here, so the player
 * renderer, the tab list and the inventory all pick up the change.
 */
@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void gravisAuth$useCustomSkin(
            CallbackInfoReturnable<SkinTextures> cir
    ) {
        SkinTextures custom = GravisSkinRegistry.get(
                ((PlayerListEntry) (Object) this).getProfile().id()
        );

        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
