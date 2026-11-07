package mod.gfix.dev.gravisauth.compat;

import mod.gfix.dev.gravisauth.Gravisauth;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PacketEvents (bundled inside GrimAC) builds its UUID to channel map from
 * the LOGIN_SUCCESS packet, i.e. from the UUID the client advertised during
 * the login phase. GravisAuth replaces that profile with the Gravis UUID in
 * the configuration phase, so by the time the play phase starts the lookup
 * PacketEvents/GrimAC performs for the native player's UUID returns null.
 *
 * GrimAC then fails to construct its platform player (the join-time
 * "PacketEvents caught an unhandled exception") and never caches it, which
 * makes every respawn fail with a NullPointerException inside copyFrom.
 *
 * Re-keying the existing channel under the Gravis UUID before the
 * configuration phase ends makes both lookups line up again. The user
 * profile is updated as well so anything reading it sees the same identity
 * as the server player.
 *
 * PacketEvents is an optional runtime dependency (it only exists when
 * GrimAC is installed) and its version may change, so everything here is
 * reflective and failure-tolerant: if this shim cannot apply, auth still
 * succeeds and the previous behaviour is preserved.
 */
public final class PacketEventsCompat {

    private static final AtomicBoolean FAILED = new AtomicBoolean();
    private static final AtomicBoolean FIRST_SUCCESS = new AtomicBoolean();

    private PacketEventsCompat() {
    }

    /**
     * @param channel  the connection's netty channel, may be null
     * @param uuid     the authenticated Gravis UUID
     * @param username the authenticated Gravis username
     */
    public static void rekey(Object channel, UUID uuid, String username) {
        if (channel == null) {
            warnOnce("Cannot re-key PacketEvents: connection has no channel");
            return;
        }

        try {
            Class<?> packetEvents = load(
                    "com.github.retrooper.packetevents.PacketEvents"
            );

            Object api = packetEvents.getMethod("getAPI").invoke(null);

            if (api == null) {
                warnOnce("Cannot re-key PacketEvents: API is not initialised");
                return;
            }

            Class<?> apiType = packetEvents.getMethod("getAPI")
                    .getReturnType();

            Object protocolManager = apiType
                    .getMethod("getProtocolManager")
                    .invoke(api);

            Class<?> protocolManagerType = apiType
                    .getMethod("getProtocolManager")
                    .getReturnType();

            protocolManagerType
                    .getMethod("setChannel", UUID.class, Object.class)
                    .invoke(protocolManager, uuid, channel);

            updateUserProfile(protocolManager, protocolManagerType, channel,
                    uuid, username);

            if (FIRST_SUCCESS.compareAndSet(false, true)) {
                Gravisauth.LOGGER.info(
                        "Re-keyed PacketEvents channel to the Gravis UUID"
                );
            } else {
                Gravisauth.LOGGER.debug(
                        "Re-keyed PacketEvents channel for {}", uuid
                );
            }
        } catch (Throwable t) {
            warnOnce("Could not re-key PacketEvents (GrimAC)", t);
        }
    }

    private static void updateUserProfile(
            Object protocolManager,
            Class<?> protocolManagerType,
            Object channel,
            UUID uuid,
            String username
    ) {
        try {
            Object user = protocolManagerType
                    .getMethod("getUser", Object.class)
                    .invoke(protocolManager, channel);

            if (user == null) {
                return;
            }

            Class<?> userType = load(
                    "com.github.retrooper.packetevents.protocol.player.User"
            );

            Object profile = userType.getMethod("getProfile").invoke(user);

            if (profile == null) {
                return;
            }

            Class<?> profileType = load(
                    "com.github.retrooper.packetevents.protocol.player.UserProfile"
            );

            profileType.getMethod("setUUID", UUID.class)
                    .invoke(profile, uuid);

            if (username != null) {
                profileType.getMethod("setName", String.class)
                        .invoke(profile, username);
            }
        } catch (Throwable t) {
            // The channel mapping is the part GrimAC needs; a profile update
            // failure must not discard it.
            Gravisauth.LOGGER.debug(
                    "Could not update the PacketEvents user profile", t
            );
        }
    }

    private static Class<?> load(String name) throws ClassNotFoundException {
        ClassLoader loader = PacketEventsCompat.class.getClassLoader();

        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException e) {
            ClassLoader context = Thread.currentThread()
                    .getContextClassLoader();

            if (context == null || context == loader) {
                throw e;
            }

            return Class.forName(name, false, context);
        }
    }

    private static void warnOnce(String message) {
        if (FAILED.compareAndSet(false, true)) {
            Gravisauth.LOGGER.warn(message);
        }
    }

    private static void warnOnce(String message, Throwable t) {
        if (FAILED.compareAndSet(false, true)) {
            Gravisauth.LOGGER.warn(message, t);
        }
    }
}
