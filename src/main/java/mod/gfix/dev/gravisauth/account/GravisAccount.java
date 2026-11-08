package mod.gfix.dev.gravisauth.account;

import java.util.UUID;

public class GravisAccount {

    private final int id;
    private final String username;
    private final boolean verified;
    private final UUID uuid;
    private final String skinUrl;
    private final String skinModel;

    public GravisAccount(
            int id,
            String username,
            boolean verified,
            UUID uuid,
            String skinUrl,
            String skinModel
    ) {
        this.id = id;
        this.username = username;
        this.verified = verified;
        this.uuid = uuid;
        this.skinUrl = skinUrl;
        this.skinModel = skinModel;
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

    /**
     * @return the player-chosen skin URL, or null when the default is used
     */
    public String getSkinUrl() {
        return skinUrl;
    }

    /**
     * @return "slim" or "wide"
     */
    public String getSkinModel() {
        return skinModel;
    }
}
