package mod.gfix.dev.gravisauth.mixin;

import mod.gfix.dev.gravisauth.Gravisauth;
import mod.gfix.dev.gravisauth.auth.AuthSession;
import mod.gfix.dev.gravisauth.auth.AuthSessions;
import mod.gfix.dev.gravisauth.crypto.GravisCrypto;
import mod.gfix.dev.gravisauth.network.GravisAuthNetworking;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.config.ReadyC2SPacket;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Holds every connection in the configuration phase until the client
 * has proven that it owns a verified Gravis account.
 *
 * The vanilla client has no way to answer the challenge, so it is
 * disconnected by the timeout. This is intentional: the server is in
 * offline mode and the mod is the only access control.
 */
@Mixin(ServerConfigurationNetworkHandler.class)
public abstract class ServerConfigurationNetworkHandlerMixin {

    @Inject(method = "sendConfigurations", at = @At("TAIL"))
    private void gravisauth$sendChallenge(CallbackInfo ci) {
        if (!Gravisauth.CONFIG.enabled) {
            return;
        }

        ServerConfigurationNetworkHandler handler =
                (ServerConfigurationNetworkHandler) (Object) this;

        try {
            AuthSession session = AuthSessions.create(handler);

            GravisAuthNetworking.sendChallenge(
                    handler,
                    GravisCrypto.encodePublicKey(
                            session.getKeyPair().getPublic()
                    )
            );

            AuthSessions.scheduleTimeout(handler, session);
            AuthSessions.scheduleKeepAlive(handler, session);

            GameProfile profile =
                    ((ServerConfigurationNetworkHandlerAccessor) handler)
                            .gravisAuth$getProfile();

            Gravisauth.LOGGER.info(
                    "Sent authentication challenge to {}",
                    profile == null ? "unknown" : profile.name()
            );
        } catch (Exception e) {
            Gravisauth.LOGGER.error(
                    "Failed to start the authentication challenge", e
            );

            handler.disconnect(
                    Text.literal("GravisAuth: authentication unavailable")
            );
        }
    }

    /**
     * The configuration phase only finishes after a successful login.
     * Until then the connection stays here, which keeps the player out
     * of the world entirely.
     */
    @Inject(
            method = "endConfiguration",
            at = @At("HEAD"),
            cancellable = true
    )
    private void gravisauth$blockUntilAuthenticated(CallbackInfo ci) {
        if (!Gravisauth.CONFIG.enabled) {
            return;
        }

        ServerConfigurationNetworkHandler handler =
                (ServerConfigurationNetworkHandler) (Object) this;

        AuthSession session = AuthSessions.get(handler);

        if (session == null || !session.isAuthenticated()) {
            ci.cancel();
        }
    }

    /**
     * A client can send ReadyC2SPacket itself; in vanilla that is only
     * sent after the server's JoinWorldTask ask for it. Refuse it until
     * the connection is authenticated so a hand-written client cannot
     * skip the gate.
     */
    @Inject(
            method = "onReady",
            at = @At("HEAD"),
            cancellable = true
    )
    private void gravisauth$blockReady(
            ReadyC2SPacket packet,
            CallbackInfo ci
    ) {
        if (!Gravisauth.CONFIG.enabled) {
            return;
        }

        ServerConfigurationNetworkHandler handler =
                (ServerConfigurationNetworkHandler) (Object) this;

        AuthSession session = AuthSessions.get(handler);

        if (session == null || !session.isAuthenticated()) {
            ci.cancel();
        }
    }
}
