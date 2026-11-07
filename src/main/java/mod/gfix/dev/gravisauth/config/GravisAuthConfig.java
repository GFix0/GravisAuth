package mod.gfix.dev.gravisauth.config;

public class GravisAuthConfig {

    /**
     * Master switch. When false the mod does nothing and the server
     * behaves like a normal offline-mode server.
     */
    public boolean enabled = false;

    /**
     * How long a player may stay in the configuration phase without
     * authenticating before being disconnected.
     */
    public int loginTimeoutSeconds = 120;

    /**
     * Failed password attempts allowed per connection.
     */
    public int maxAttempts = 3;

    /**
     * Failed attempts allowed per username across connections inside
     * the throttle window.
     */
    public int throttleAttempts = 10;

    public int throttleMinutes = 10;

    /**
     * Lifetime of a remembered-login token. Every use slides the expiry
     * forward by this many days.
     */
    public int tokenLifetimeDays = 30;

    public Database database = new Database();

    public static class Database {
        public String host = "127.0.0.1";
        public int port = 3306;
        public String database = "gravis";
        public String username = "gravisauth";
        public String password = "";
    }
}
