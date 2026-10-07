package mod.gfix.dev.gravisauth.account;

import java.util.UUID;

public class GravisAccount {

    private final int id;
    private final String username;
    private final boolean verified;
    private final UUID uuid;

    public GravisAccount(int id, String username, boolean verified, UUID uuid) {
        this.id = id;
        this.username = username;
        this.verified = verified;
        this.uuid = uuid;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public boolean isVerified() {
        return verified;
    }

    public UUID getUuid() {
        return uuid;
    }
}