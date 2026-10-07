package mod.gfix.dev.gravisauth.account;

import mod.gfix.dev.gravisauth.database.GravisDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GravisAccountManager {

    public GravisAccount findByUsername(String username) {

        String sql = """
                SELECT id, username, email_verified
                FROM users
                WHERE username = ?
                LIMIT 1
                """;

        try (
                Connection connection = GravisDatabase.connect();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    return null;
                }

                int id = result.getInt("id");
                String databaseUsername = result.getString("username");
                boolean verified = result.getBoolean("email_verified");

                return new GravisAccount(
                        id,
                        databaseUsername,
                        verified,
                        GravisUuid.fromUserId(id)
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to look up Gravis account: " + username,
                    e
            );
        }
    }
}