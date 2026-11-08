package mod.gfix.dev.gravisauth.mixin.client;

import mod.gfix.dev.gravisauth.client.GravisSkinRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the local player their own Gravis skin.
 *
 * The player-list override cannot cover this case: the client renders
 * itself with the profile it logged in with, whose UUID is the offline
 * one, so no player-list entry matches it and vanilla falls back to the
 * default skin.
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void gravisAuth$useOwnSkin(
            CallbackInfoReturnable<SkinTextures> cir
    ) {
        AbstractClientPlayerEntity self =
                (AbstractClientPlayerEntity) (Object) this;

        if (MinecraftClient.getInstance().player != self) {
            return;
        }

        SkinTextures own = GravisSkinRegistry.getSelf();

        if (own != null) {
            cir.setReturnValue(own);
        }
    }
}
