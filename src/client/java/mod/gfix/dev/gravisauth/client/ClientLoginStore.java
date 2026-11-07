package mod.gfix.dev.gravisauth.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

/**
 * Stores the "remember me" token issued by the server.
 *
 * Only the token and the Gravis username are kept; the password is never
 * written to disk. The token is a bearer credential, so the file is
 * restricted to the owner on POSIX systems and can be deleted at any time
 * to force a password login.
 */
public final class ClientLoginStore {

    public record SavedLogin(String username, String token) {
    }

    private static final String FILE_NAME = "gravisauth-client.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private ClientLoginStore() {
    }

    private static final class Data {
        String username;
        String token;
    }

    private static Path file() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(FILE_NAME);
    }

    public static SavedLogin load() {
        Path path = file();

        if (!Files.isRegularFile(path)) {
            return null;
        }

        try {
            Data data = GSON.fromJson(
                    Files.readString(path, StandardCharsets.UTF_8),
                    Data.class
            );

            if (data == null || data.token == null || data.token.isBlank()) {
                return null;
            }

            return new SavedLogin(
                    data.username == null ? "" : data.username,
                    data.token
            );
        } catch (Exception e) {
            GravisauthClient.LOGGER.warn("Could not read {}", path, e);
            return null;
        }
    }

    public static void save(String username, String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        Data data = new Data();
        data.username = username;
        data.token = token;

        write(GSON.toJson(data));
    }

    public static void clear() {
        try {
            Files.deleteIfExists(file());
        } catch (IOException e) {
            GravisauthClient.LOGGER.warn("Could not delete {}", file(), e);
        }
    }

    private static void write(String json) {
        Path path = file();

        try {
            Path parent = path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path temporary = path.resolveSibling(
                    path.getFileName() + ".tmp"
            );

            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            restrict(temporary);

            try {
                Files.move(
                        temporary,
                        path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(
                        temporary,
                        path,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            restrict(path);
        } catch (IOException e) {
            GravisauthClient.LOGGER.warn("Could not save {}", path, e);
        }
    }

    private static void restrict(Path path) {
        try {
            Files.setPosixFilePermissions(
                    path,
                    EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE
                    )
            );
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows or a file system without POSIX permissions.
        }
    }
}
