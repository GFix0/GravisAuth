package mod.gfix.dev.gravisauth.client;

import mod.gfix.dev.gravisauth.skin.GravisSkinPayload;
import mod.gfix.dev.gravisauth.skin.SkinUrls;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom skins received from the server.
 *
 * Textures are downloaded and registered by the client itself because
 * vanilla only asks the Mojang session server, which does not know the
 * Gravis UUIDs. Until a download finishes the vanilla skin is used.
 */
public final class GravisSkinRegistry {

    private static final Map<UUID, Entry> SKINS =
            new ConcurrentHashMap<>();

    private GravisSkinRegistry() {
    }

    public static void apply(List<GravisSkinPayload.Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getTextureManager() == null) {
            return;
        }

        GravisauthClient.LOGGER.info(
                "Received {} custom skin(s) from the server",
                entries.size()
        );

        for (GravisSkinPayload.Entry entry : entries) {
            if (entry == null || entry.uuid() == null) {
                continue;
            }

            String url = SkinUrls.normalize(entry.url());

            if (url == null || url.isEmpty()) {
                SKINS.remove(entry.uuid());
                continue;
            }

            String hash = sha1(url);

            Identifier texture = Identifier.of(
                    "gravisauth",
                    "skins/"
                            + entry.uuid().toString().replace("-", "")
                            + "_"
                            + hash.substring(0, 16)
            );

            Entry existing = SKINS.get(entry.uuid());

            if (existing != null
                    && existing.texture().equals(texture)
                    && existing.url().equals(url)) {
                continue;
            }

            Entry skin = new Entry(
                    entry.uuid(),
                    url,
                    SkinUrls.isSlim(entry.model()),
                    texture
            );

            SKINS.put(entry.uuid(), skin);
            download(client, skin, hash);
        }
    }

    /**
     * @return the custom skin, or null while the default should be used
     */
    public static SkinTextures get(UUID uuid) {
        Entry entry = uuid == null ? null : SKINS.get(uuid);

        return entry == null ? null : entry.textures();
    }

    public static void clear() {
        SKINS.clear();
    }

    private static void download(
            MinecraftClient client,
            Entry skin,
            String hash
    ) {
        Path directory = client.runDirectory.toPath()
                .resolve("gravisauth-skins");
        Path file = directory.resolve(hash + ".png");

        PlayerSkinTextureDownloader downloader =
                new PlayerSkinTextureDownloader(
                        client.getNetworkProxy(),
                        client.getTextureManager(),
                        client
                );

        downloader.downloadAndRegisterTexture(
                        skin.texture(),
                        file,
                        skin.url(),
                        true
                )
                .thenAccept(ignored -> skin.markReady())
                .exceptionally(error -> {
                    GravisauthClient.LOGGER.warn(
                            "Could not download a Gravis skin", error
                    );

                    return null;
                });
    }

    private static String sha1(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1")
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is unavailable", e);
        }
    }

    private static final class Entry {

        private final UUID uuid;
        private final String url;
        private final boolean slim;
        private final Identifier texture;
        private volatile SkinTextures textures;

        private Entry(
                UUID uuid,
                String url,
                boolean slim,
                Identifier texture
        ) {
            this.uuid = uuid;
            this.url = url;
            this.slim = slim;
            this.texture = texture;
        }

        private String url() {
            return url;
        }

        private Identifier texture() {
            return texture;
        }

        private SkinTextures textures() {
            return textures;
        }

        private void markReady() {
            textures = new SkinTextures(
                    new AssetInfo.SkinAssetInfo(texture, url),
                    null,
                    null,
                    slim ? PlayerSkinType.SLIM : PlayerSkinType.WIDE,
                    false
            );

            GravisauthClient.LOGGER.info(
                    "Registered the custom skin for {}",
                    uuid
            );
        }
    }
}
