package mod.gfix.dev.gravisauth.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPermsProvider;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * LuckPerms loads permission data during the login phase, keyed by the
 * UUID the client advertises. GravisAuth replaces that UUID with the
 * Gravis UUID while the connection is in the configuration phase, so the
 * user record LuckPerms needs at play time is not the one it loaded.
 *
 * Loading it again here, before the configuration phase ends, keeps
 * "permissions data was not loaded during the pre-login stage" from
 * kicking every player.
 *
 * This class is only touched when LuckPerms is installed; it must not be
 * loaded otherwise (the API is a compile-only dependency).
 */
public final class LuckPermsCompat {

    private LuckPermsCompat() {
    }

    public static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded("luckperms");
    }

    public static CompletableFuture<Void> preload(
            UUID uuid,
            String username
    ) {
        try {
            return LuckPermsProvider.get()
                    .getUserManager()
                    .loadUser(uuid, username)
                    .thenApply(user -> (Void) null);
        } catch (Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }
}
