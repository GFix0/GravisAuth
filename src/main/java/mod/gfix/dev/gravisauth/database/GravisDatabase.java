package mod.gfix.dev.gravisauth.database;

import mod.gfix.dev.gravisauth.Gravisauth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class GravisDatabase {

    static {
        /*
         * Register the driver explicitly. Relying on the JDBC ServiceLoader
         * can be fragile inside the Fabric nested-jar classloader, and a
         * missing driver should be an obvious startup error.
         */
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "MariaDB JDBC driver is not bundled in the mod jar",
                    e
            );
        }
    }

    public static Connection connect() throws SQLException {

        var config = Gravisauth.CONFIG.database;

        /*
         * Authentication runs on the server thread, so both timeouts are
         * short: a database that stops answering must never freeze ticks.
         */
        String url = String.format(
                "jdbc:mariadb://%s:%d/%s?connectTimeout=5000&socketTimeout=5000",
                config.host,
                config.port,
                config.database
        );

        return DriverManager.getConnection(
                url,
                config.username,
                config.password
        );
    }
}
