package mod.gfix.dev.gravisauth.account;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class GravisAccount {

    private final String username;
    private final String originalUsername;
    private final UUID uuid;

    public GravisAccount(String username, String originalUsername) {
        this.username = username;
        this.originalUsername = originalUsername;

        this.uuid = UUID.nameUUIDFromBytes(
                ("Gravis:" + originalUsername).getBytes(StandardCharsets.UTF_8)
        );
    }

    public String getUsername() {
        return username;
    }

    public String getOriginalUsername() {
        return originalUsername;
    }

    public UUID getUuid() {
        return uuid;
    }
}