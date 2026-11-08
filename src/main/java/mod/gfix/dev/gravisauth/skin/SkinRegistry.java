package mod.gfix.dev.gravisauth.skin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skins of the players that authenticated on this server, keyed by the
 * Gravis UUID that is also the in-game identity.
 */
public final class SkinRegistry {

    public record Skin(String url, String model) {
    }

    private static final Map<UUID, Skin> SKINS = new ConcurrentHashMap<>();

    private SkinRegistry() {
    }

    public static void put(UUID uuid, String url, String model) {
        if (uuid == null) {
            return;
        }

        String normalized = SkinUrls.normalize(url);

        if (normalized == null || normalized.isEmpty()) {
            SKINS.remove(uuid);
            return;
        }

        SKINS.put(
                uuid,
                new Skin(
                        normalized,
                        SkinUrls.isSlim(model) ? "slim" : "wide"
                )
        );
    }

    public static Skin get(UUID uuid) {
        return uuid == null ? null : SKINS.get(uuid);
    }

    public static void remove(UUID uuid) {
        if (uuid != null) {
            SKINS.remove(uuid);
        }
    }
}
