package mod.gfix.dev.gravisauth.auth;

import mod.gfix.dev.gravisauth.Gravisauth;
import mod.gfix.dev.gravisauth.crypto.GravisCrypto;
import mod.gfix.dev.gravisauth.mixin.ServerCommonNetworkHandlerAccessor;
import mod.gfix.dev.gravisauth.network.GravisAuthNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;

import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tracks the pending login of every connection in the configuration
 * phase.
 */
public final class AuthSessions {

    /**
     * Clients drop an idle connection after 30 seconds; stay well below it.
     */
    private static final long KEEP_ALIVE_SECONDS = 10L;

    /*
     * Weak keys: once a connection is gone nothing keeps its handler
     * alive, so abandoned entries are collected automatically.
     */
    private static final Map<ServerConfigurationNetworkHandler, AuthSession>
            SESSIONS = Collections.synchronizedMap(new WeakHashMap<>());

    private static final ScheduledExecutorService TIMEOUTS =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "gravisauth-timeout");
                thread.setDaemon(true);
                return thread;
            });

    private AuthSessions() {
    }

    public static AuthSession create(
            ServerConfigurationNetworkHandler handler
    ) throws GeneralSecurityException {

        AuthSession session = new AuthSession(
                GravisCrypto.generateKeyPair(),
                Gravisauth.CONFIG.maxAttempts
        );

        SESSIONS.put(handler, session);
        return session;
    }

    public static AuthSession get(ServerConfigurationNetworkHandler handler) {
        return SESSIONS.get(handler);
    }

    public static void remove(ServerConfigurationNetworkHandler handler) {
        AuthSession session = SESSIONS.remove(handler);

        if (session != null) {
            session.close();
        }
    }

    /**
     * Keeps an unauthenticated connection alive while the player types.
     *
     * The client applies a 30 second read timeout, and the server sends
     * nothing after the challenge, so the connection used to be dropped by
     * the client after half a minute. A tiny payload every few seconds
     * resets that timer.
     */
    public static void scheduleKeepAlive(
            ServerConfigurationNetworkHandler handler,
            AuthSession session
    ) {
        TIMEOUTS.scheduleAtFixedRate(() -> {
            if (session.isAuthenticated() || session.isClosed()) {
                return;
            }

            try {
                MinecraftServer server =
                        ((ServerCommonNetworkHandlerAccessor) handler)
                                .gravisAuth$getServer();

                server.execute(() -> {
                    if (session.isAuthenticated() || session.isClosed()) {
                        return;
                    }

                    try {
                        GravisAuthNetworking.sendKeepAlive(handler);
                    } catch (Exception ignored) {
                        // The connection is gone; the session cleanup will
                        // follow from the disconnect callback.
                    }
                });
            } catch (Exception ignored) {
                // Handler already unusable.
            }
        }, KEEP_ALIVE_SECONDS, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS);
    }

    public static void scheduleTimeout(
            ServerConfigurationNetworkHandler handler,
            AuthSession session
    ) {        int seconds = Math.max(15, Gravisauth.CONFIG.loginTimeoutSeconds);

        TIMEOUTS.schedule(() -> {
            if (session.isAuthenticated() || session.isClosed()) {
                return;
            }

            session.close();

            MinecraftServer server =
                    ((ServerCommonNetworkHandlerAccessor) handler)
                            .gravisAuth$getServer();

            server.execute(() -> {
                if (!session.isAuthenticated()) {
                    handler.disconnect(
                            net.minecraft.text.Text.literal(
                                    "GravisAuth: login timed out"
                            )
                    );
                }
            });
        }, seconds, TimeUnit.SECONDS);
    }
}
