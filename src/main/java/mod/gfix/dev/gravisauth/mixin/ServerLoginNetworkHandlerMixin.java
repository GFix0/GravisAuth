package mod.gfix.dev.gravisauth.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import mod.gfix.dev.gravisauth.account.GravisAccount;
import mod.gfix.dev.gravisauth.account.GravisAccountManager;
import net.minecraft.network.packet.c2s.login.EnterConfigurationC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {

    @Shadow
    public abstract void disconnect(Text reason);

    @Inject(method = "startVerify", at = @At("RETURN"))
    private void gravisauth$startVerify(GameProfile profile, CallbackInfo ci) {

        GravisAccountManager accountManager = new GravisAccountManager();

        GravisAccount account = accountManager.findByUsername(profile.name());

        if (account != null) {
            System.out.println("[GravisAuth] Account recognized!");
            System.out.println("[GravisAuth]   Gravis Username: " + account.getUsername());
            System.out.println("[GravisAuth]   Gravis UUID: " + account.getUuid());

            GameProfile gravisProfile = new GameProfile(
                    account.getUuid(),
                    account.getUsername()
            );

            ((ServerLoginNetworkHandlerAccessor) (Object) this)
                    .gravisAuth$setProfile(gravisProfile);
        } else {
            System.out.println("[GravisAuth] Account NOT recognized!");

            this.disconnect(Text.literal(
                    "GravisAuth: Account " + profile.name() + " not recognized"
            ));
        }
    }

    @Inject(method = "onEnterConfiguration", at = @At("HEAD"))
    private void gravisauth$onEnterConfiguration(
            EnterConfigurationC2SPacket packet,
            CallbackInfo ci
    ) {
        System.out.println("[GravisAuth] Entering configuration");

        GameProfile profile = ((ServerLoginNetworkHandlerAccessor) (Object) this)
                .gravisAuth$getProfile();

        System.out.println("[GravisAuth] Current profile:");
        System.out.println("[GravisAuth]   Name: " + profile.name());
        System.out.println("[GravisAuth]   UUID: " + profile.id());
    }
}