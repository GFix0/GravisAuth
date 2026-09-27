package mod.gfix.dev.gravisauth.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.packet.c2s.login.EnterConfigurationC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin {

    @Inject(method = "startVerify", at = @At("RETURN"))
    private void gravisauth$startVerify(GameProfile profile, CallbackInfo ci) {
        UUID gravisUuid = UUID.fromString("11111111-2222-3333-4444-555555555555");

        GameProfile gravisProfile = new GameProfile(
                gravisUuid,
                "GravisTest"
        );

        System.out.println("[GravisAuth] Mojang profile:");
        System.out.println("[GravisAuth]   Name: " + profile.name());
        System.out.println("[GravisAuth]   UUID: " + profile.id());

        System.out.println("[GravisAuth] Replacing with:");
        System.out.println("[GravisAuth]   Name: " + gravisProfile.name());
        System.out.println("[GravisAuth]   UUID: " + gravisProfile.id());

        //((ServerLoginNetworkHandlerAccessor) (Object) this).gravisAuth$setProfile(gravisProfile);
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