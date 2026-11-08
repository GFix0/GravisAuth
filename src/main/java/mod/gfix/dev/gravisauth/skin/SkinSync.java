package mod.gfix.dev.gravisauth.skin;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;

import java.util.ArrayList;
import java.util.List;

import mod.gfix.dev.gravisauth.Gravisauth;

/**
 * Pushes the skin table to clients as they enter the play phase.
 *
 * A joining player receives every online player, and the others receive
 * the newcomer, so all clients end up with the same table.
 */
public final class SkinSync {

    private static boolean payloadRegistered;

    private SkinSync() {
    }

    /**
     * Registers the S2C payload. Safe to call from both entry points.
     */
    public static synchronized void registerPayload() {
        if (payloadRegistered) {
            return;
        }

        PayloadTypeRegistry.playS2C().register(
                GravisSkinPayload.TYPE,
                GravisSkinPayload.CODEC
        );

        payloadRegistered = true;
    }

    public static void initialize() {
        registerPayload();

        if (FabricLoader.getInstance().getEnvironmentType()
                != EnvType.SERVER) {
            return;
        }

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> sync(handler, server)
        );

        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> remove(handler)
        );
    }

    private static void sync(
            ServerPlayNetworkHandler handler,
            MinecraftServer server
    ) {
        ServerPlayerEntity player = handler.getPlayer();

        if (player == null) {
            return;
        }

        List<ServerPlayerEntity> online =
                new ArrayList<>(server.getPlayerManager().getPlayerList());

        /*
         * The JOIN event fires before the joining player is listed, so
         * add them explicitly; otherwise the snapshot is empty on the
         * first join and the player never receives their own skin.
         */
        if (online.stream().noneMatch(
                other -> other.getUuid().equals(player.getUuid())
        )) {
            online.add(player);
        }

        List<GravisSkinPayload.Entry> snapshot = new ArrayList<>();

        for (ServerPlayerEntity other : online) {
            GravisSkinPayload.Entry entry = entryOf(other);

            if (entry != null) {
                snapshot.add(entry);
            }
        }

        if (!snapshot.isEmpty()) {
            ServerPlayNetworking.send(
                    player,
                    new GravisSkinPayload(snapshot)
            );
        }

        Gravisauth.LOGGER.info(
                "Sent {} skin(s) to {} ({} online)",
                snapshot.size(),
                player.getGameProfile().name(),
                online.size()
        );

        /*
         * Always announce, even without a skin: a player who removed
         * their URL must clear the texture on clients that already
         * cached it. An empty URL means "use the default skin".
         */
        SkinRegistry.Skin skin = SkinRegistry.get(player.getUuid());

        GravisSkinPayload.Entry self = skin == null
                ? new GravisSkinPayload.Entry(player.getUuid(), "", "wide")
                : new GravisSkinPayload.Entry(
                        player.getUuid(),
                        skin.url(),
                        skin.model()
                );

        GravisSkinPayload announcement =
                new GravisSkinPayload(List.of(self));

        for (ServerPlayerEntity other : online) {
            if (other != player) {
                ServerPlayNetworking.send(other, announcement);
            }
        }

        Gravisauth.LOGGER.info(
                "Announced {}'s skin ({}) to {} player(s)",
                player.getGameProfile().name(),
                skin == null ? "default" : skin.url(),
                Math.max(0, online.size() - 1)
        );
    }

    private static void remove(ServerPlayNetworkHandler handler) {
        ServerPlayerEntity player = handler.getPlayer();

        if (player != null) {
            SkinRegistry.remove(player.getUuid());
        }
    }

    private static GravisSkinPayload.Entry entryOf(
            ServerPlayerEntity player
    ) {
        SkinRegistry.Skin skin = SkinRegistry.get(player.getUuid());

        if (skin == null) {
            return null;
        }

        return new GravisSkinPayload.Entry(
                player.getUuid(),
                skin.url(),
                skin.model()
        );
    }
}
