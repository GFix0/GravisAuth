package mod.gfix.dev.gravisauth.database;

import mod.gfix.dev.gravisauth.Gravisauth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class GravisDatabase {

    public static Connection connect() throws SQLException {

        var config = Gravisauth.CONFIG.database;

        String url = String.format(
                "jdbc:mariadb://%s:%d/%s",
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