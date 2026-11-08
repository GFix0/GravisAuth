package mod.gfix.dev.gravisauth.account;

import at.favre.lib.crypto.bcrypt.BCrypt;
import mod.gfix.dev.gravisauth.Gravisauth;
import mod.gfix.dev.gravisauth.database.GravisDatabase;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GravisAccountManager {

    public enum Status {
        OK,
        UNKNOWN,
        BAD_PASSWORD,
        NOT_VERIFIED,
        THROTTLED,
        ERROR
    }

    public record Result(Status status, GravisAccount account) {
        public static Result of(Status status) {
            return new Result(status, null);
        }
    }

    /**
     * A valid bcrypt hash used when the account does not exist, so that
     * a missing user costs the same time as a wrong password.
     */
    private static final String DUMMY_HASH =
            "$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi";

    private record Failures(int count, long windowStart) {
    }

    private static final Map<String, Failures> FAILURES =
            new ConcurrentHashMap<>();

    public Result authenticate(String username, String password) {
        String name = username == null ? "" : username.trim();

        if (name.isEmpty() || name.length() > 32) {
            return Result.of(Status.UNKNOWN);
        }

        String key = name.toLowerCase(Locale.ROOT);

        if (isThrottled(key)) {
            return Result.of(Status.THROTTLED);
        }

        String sql = """
                SELECT id, username, email_verified, password_hash,
                       skin_url, skin_model
                FROM users
                WHERE username = ?
                LIMIT 1
                """;

        try (
                Connection connection = GravisDatabase.connect();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, name);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    // Equalize the response time for unknown accounts.
                    BCrypt.verifyer().verify(
                            password.toCharArray(),
                            DUMMY_HASH
                    );

                    registerFailure(key);
                    return Result.of(Status.UNKNOWN);
                }

                String hash = result.getString("password_hash");

                boolean verified = hash != null
                        && !hash.isEmpty()
                        && BCrypt.verifyer()
                                .verify(password.toCharArray(), hash)
                                .verified;

                if (!verified) {
                    registerFailure(key);
                    return Result.of(Status.BAD_PASSWORD);
                }

                FAILURES.remove(key);

                if (!result.getBoolean("email_verified")) {
                    return Result.of(Status.NOT_VERIFIED);
                }

                int id = result.getInt("id");
                String databaseName = result.getString("username");

                return new Result(
                        Status.OK,
                        new GravisAccount(
                                id,
                                databaseName,
                                true,
                                GravisUuid.fromUserId(id),
                                result.getString("skin_url"),
                                result.getString("skin_model")
                        )
                );
            }

        } catch (SQLException e) {
            Gravisauth.LOGGER.error(
                    "Database error while authenticating {}", name, e
            );

            return Result.of(Status.ERROR);
        }
    }

    private boolean isThrottled(String key) {
        Failures failures = FAILURES.get(key);

        if (failures == null) {
            return false;
        }

        long window = Gravisauth.CONFIG.throttleMinutes * 60_000L;

        if (System.currentTimeMillis() - failures.windowStart() > window) {
            FAILURES.remove(key);
            return false;
        }

        return failures.count() >= Gravisauth.CONFIG.throttleAttempts;
    }

    private void registerFailure(String key) {
        long now = System.currentTimeMillis();

        FAILURES.compute(key, (ignored, existing) -> {
            if (existing == null
                    || now - existing.windowStart() > 600_000L) {
                return new Failures(1, now);
            }

            return new Failures(existing.count() + 1, existing.windowStart());
        });
    }
}
