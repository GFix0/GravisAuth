package mod.gfix.dev.gravisauth.config;

public class GravisAuthConfig {

    public Database database = new Database();

    public static class Database {
        public String host = "127.0.0.1";
        public int port = 3306;
        public String database = "gravis";
        public String username = "gravisauth";
        public String password = "";
    }
}