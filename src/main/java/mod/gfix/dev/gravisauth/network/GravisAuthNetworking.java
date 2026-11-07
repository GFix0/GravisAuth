package mod.gfix.dev.gravisauth.network;

import com.mojang.authlib.GameProfile;
import mod.gfix.dev.gravisauth.Gravisauth;
import mod.gfix.dev.gravisauth.account.GravisAccount;
import mod.gfix.dev.gravisauth.account.GravisAccountManager;
import mod.gfix.dev.gravisauth.auth.AuthSession;
import mod.gfix.dev.gravisauth.auth.AuthSessions;
import mod.gfix.dev.gravisauth.compat.LuckPermsCompat;
import mod.gfix.dev.gravisauth.crypto.GravisCrypto;
import mod.gfix.dev.gravisauth.mixin.ServerCommonNetworkHandlerAccessor;
import mod.gfix.dev.gravisauth.mixin.ServerConfigurationNetworkHandlerAccessor;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.text.Text;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

public final class GravisAuthNetworking {

    private static final String CHALLENGE_PREFIX = "challenge:";
    private static final String AUTH_PREFIX = "auth:";
    private static final String FAIL_PREFIX = "fail:";
    private static final String OK = "ok";

    private static final GravisAccountManager ACCOUNTS =
            new GravisAccountManager();

    private GravisAuthNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.configurationS2C().register(
                GravisAuthPayload.TYPE,
                GravisAuthPayload.CODEC
        );

        PayloadTypeRegistry.configurationC2S().register(
                GravisAuthPayload.TYPE,
                GravisAuthPayload.CODEC
        );

        ServerConfigurationNetworking.registerGlobalReceiver(
                GravisAuthPayload.TYPE,
                (payload, context) -> {
                    ServerConfigurationNetworkHandler handler =
                            context.networkHandler();

                    MinecraftServer server =
                            ((ServerCommonNetworkHandlerAccessor) handler)
                                    .gravisAuth$getServer();

                    // Packet handling must run on the server thread.
                    server.execute(
                            () -> handleResponse(handler, payload.message())
                    );
                }
        );
    }

    public static void sendChallenge(
            ServerConfigurationNetworkHandler handler,
            String publicKey
    ) {
        send(handler, CHALLENGE_PREFIX + publicKey);
    }

    private static void send(
            ServerConfigurationNetworkHandler handler,
            String message
    ) {
        ServerConfigurationNetworking.send(
                handler,
                new GravisAuthPayload(message)
        );
    }

    private static void handleResponse(
            ServerConfigurationNetworkHandler handler,
            String message
    ) {
        if (!Gravisauth.CONFIG.enabled) {
            return;
        }

        AuthSession session = AuthSessions.get(handler);

        if (session == null
                || session.isClosed()
                || session.isAuthenticated()) {
            return;
        }

        if (message == null || !message.startsWith(AUTH_PREFIX)) {
            return;
        }

        String[] parts = message.split(":", 3);

        if (parts.length != 3) {
            fail(handler, session, "Malformed authentication response");
            return;
        }

        String username;
        String password;

        try {
            PublicKey clientKey = GravisCrypto.decodePublicKey(parts[1]);

            SecretKeySpec key = GravisCrypto.deriveKey(
                    session.getKeyPair().getPrivate(),
                    clientKey
            );

            byte[] plaintext = GravisCrypto.decrypt(key, parts[2]);

            int separator = -1;

            for (int i = 0; i < plaintext.length; i++) {
                if (plaintext[i] == 0) {
                    separator = i;
                    break;
                }
            }

            if (separator < 0) {
                fail(handler, session, "Malformed credentials");
                return;
            }

            username = new String(
                    plaintext, 0, separator, StandardCharsets.UTF_8
            );

            password = new String(
                    plaintext,
                    separator + 1,
                    plaintext.length - separator - 1,
                    StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            Gravisauth.LOGGER.warn(
                    "Rejected authentication response", e
            );

            fail(handler, session, "Could not read credentials");
            return;
        }

        GravisAccountManager.Result result =
                ACCOUNTS.authenticate(username, password);

        switch (result.status()) {
            case OK -> succeed(handler, session, result.account());
            case NOT_VERIFIED -> fail(
                    handler,
                    session,
                    "Verify your email address on gfix.dev first"
            );
            case THROTTLED -> fail(
                    handler,
                    session,
                    "Too many failed attempts, try again later"
            );
            case ERROR -> {
                Gravisauth.LOGGER.error(
                        "Authentication database error for {}", username
                );
                fail(handler, session, "Authentication unavailable");
            }
            default -> fail(handler, session, "Invalid username or password");
        }
    }

    private static void succeed(
            ServerConfigurationNetworkHandler handler,
            AuthSession session,
            GravisAccount account
    ) {
        session.setAuthenticated(true);

        GameProfile profile = new GameProfile(
                account.getUuid(),
                account.getUsername()
        );

        ((ServerConfigurationNetworkHandlerAccessor) handler)
                .gravisAuth$setProfile(profile);

        send(handler, OK);

        Gravisauth.LOGGER.info(
                "Authenticated {} as Gravis account #{}",
                account.getUsername(),
                account.getId()
        );

        MinecraftServer server =
                ((ServerCommonNetworkHandlerAccessor) handler)
                        .gravisAuth$getServer();

        if (!LuckPermsCompat.isAvailable()) {
            finishConfiguration(handler);
            return;
        }

        /*
         * LuckPerms loaded its data during the login phase, keyed by the
         * UUID the client advertised. The profile now carries the Gravis
         * UUID instead, so the record it needs is a different one. Load it
         * before the connection is allowed to reach the play phase.
         */
        LuckPermsCompat.preload(account.getUuid(), account.getUsername())
                .orTimeout(10, TimeUnit.SECONDS)
                .whenComplete((ignored, error) ->
                        server.execute(() -> {
                            if (error != null) {
                                Gravisauth.LOGGER.warn(
                                        "Could not load LuckPerms data for {}",
                                        account.getUsername(),
                                        error
                                );

                                handler.disconnect(Text.literal(
                                        "GravisAuth: permissions are unavailable, "
                                                + "please try again later"
                                ));
                                return;
                            }

                            finishConfiguration(handler);
                        })
                );
    }

    private static void finishConfiguration(
            ServerConfigurationNetworkHandler handler
    ) {
        try {
            ((ServerConfigurationNetworkHandlerAccessor) handler)
                    .gravisAuth$endConfiguration();
        } catch (Exception e) {
            Gravisauth.LOGGER.warn(
                    "Could not finish the configuration phase", e
            );
        }
    }

    private static void fail(
            ServerConfigurationNetworkHandler handler,
            AuthSession session,
            String reason
    ) {
        boolean mayRetry = session.registerFailure();

        if (mayRetry) {
            send(handler, FAIL_PREFIX + reason);
        } else {
            session.close();
            handler.disconnect(
                    Text.literal("GravisAuth: " + reason)
            );
        }
    }
}
