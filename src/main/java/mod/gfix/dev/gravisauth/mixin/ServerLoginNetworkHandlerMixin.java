package mod.gfix.dev.gravisauth.mixin;

import com.mojang.authlib.GameProfile;
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

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {

    @Shadow
    public abstract void disconnect(Text reason);

    @Inject(method = "startVerify", at = @At("RETURN"))
    private void gravisauth$startVerify(
            GameProfile profile,
            CallbackInfo ci
    ) {
        GravisAccountManager accountManager = new GravisAccountManager();

        GravisAccount account = accountManager.findByUsername(profile.name());

        if (account == null) {
            System.out.println(
                    "[GravisAuth] Account NOT found: " + profile.name()
            );

            this.disconnect(Text.literal(
                    "GravisAuth: Account " + profile.name() + " not found"
            ));

            return;
        }

        System.out.println(
                "[GravisAuth] Account found: " + account.getUsername()
        );

        System.out.println(
                "[GravisAuth]   Gravis ID: " + account.getId()
        );

        System.out.println(
                "[GravisAuth]   Verified: " + account.isVerified()
        );

        if (!account.isVerified()) {
            System.out.println(
                    "[GravisAuth] Account is not verified!"
            );

            this.disconnect(Text.literal(
                    "GravisAuth: Please verify your email first"
            ));

            return;
        }

        System.out.println(
                "[GravisAuth]   Gravis UUID: " + account.getUuid()
        );

        GameProfile gravisProfile = new GameProfile(
                account.getUuid(),
                account.getUsername()
        );

        ((ServerLoginNetworkHandlerAccessor) (Object) this)
                .gravisAuth$setProfile(gravisProfile);

        System.out.println(
                "[GravisAuth] Profile replaced successfully!"
        );
    }

    @Inject(method = "onEnterConfiguration", at = @At("HEAD"))
    private void gravisauth$onEnterConfiguration(
            EnterConfigurationC2SPacket packet,
            CallbackInfo ci
    ) {
        System.out.println(
                "[GravisAuth] Entering configuration"
        );

        GameProfile profile =
                ((ServerLoginNetworkHandlerAccessor) (Object) this)
                        .gravisAuth$getProfile();

        if (profile == null) {
            System.out.println(
                    "[GravisAuth] Current profile is null!"
            );
            return;
        }

        System.out.println(
                "[GravisAuth] Current profile:"
        );

        System.out.println(
                "[GravisAuth]   Name: " + profile.name()
        );

        System.out.println(
                "[GravisAuth]   UUID: " + profile.id()
        );
    }
}