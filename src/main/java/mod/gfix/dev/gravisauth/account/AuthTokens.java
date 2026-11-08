package mod.gfix.dev.gravisauth.account;

import mod.gfix.dev.gravisauth.Gravisauth;
import mod.gfix.dev.gravisauth.database.GravisDatabase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Long-lived, revocable login tokens.
 *
 * The client stores one token per installation so a returning player does
 * not have to type the password again. Only the SHA-256 hash of a token is
 * persisted, so a database leak does not expose usable credentials.
 *
 * Tokens are bound to the user id, not to the username, so renaming the
 * Gravis account does not invalidate saved logins. Every use slides the
 * expiry forward.
 */
public final class AuthTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int TOKEN_BYTES = 32;
    private static final int MAX_TOKENS_PER_USER = 10;
    private static final int MAX_TOKEN_LENGTH = 512;

    private AuthTokens() {
    }

    /**
     * Creates a new token for the account.
     *
     * @return the plaintext token, or null when it could not be stored
     */
    public static String issue(int userId) {
        byte[] random = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(random);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(random);

        String sql = """
                INSERT INTO auth_tokens (user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                """;

        try (
                Connection connection = GravisDatabase.connect();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, userId);
            statement.setString(2, hash(token));
            statement.setTimestamp(3, expiry());

            statement.executeUpdate();
        } catch (SQLException e) {
            Gravisauth.LOGGER.error(
                    "Could not store a login token for account #{}",
                    userId,
                    e
            );

            return null;
        }

        prune(userId);
        return token;
    }

    /**
     * Validates a token and slides its expiry forward.
     *
     * @return the account the token belongs to, or null when it is unknown,
     *         expired or belongs to an unverified account
     */
    public static GravisAccount verify(String token) throws SQLException {
        if (token == null
                || token.isEmpty()
                || token.length() > MAX_TOKEN_LENGTH) {
            return null;
        }

        String tokenHash = hash(token);

        String select = """
                SELECT users.id, users.username,
                       users.skin_url, users.skin_model
                FROM auth_tokens
                JOIN users ON users.id = auth_tokens.user_id
                WHERE auth_tokens.token_hash = ?
                  AND auth_tokens.expires_at > NOW()
                  AND users.email_verified = 1
                LIMIT 1
                """;

        try (Connection connection = GravisDatabase.connect()) {
            int userId;
            String username;
            String skinUrl;
            String skinModel;

            try (PreparedStatement statement =
                         connection.prepareStatement(select)) {
                statement.setString(1, tokenHash);

                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return null;
                    }

                    userId = result.getInt("id");
                    username = result.getString("username");
                    skinUrl = result.getString("skin_url");
                    skinModel = result.getString("skin_model");
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    """
                    UPDATE auth_tokens
                    SET last_used_at = NOW(), expires_at = ?
                    WHERE token_hash = ?
                    """
            )) {
                statement.setTimestamp(1, expiry());
                statement.setString(2, tokenHash);
                statement.executeUpdate();
            }

            return new GravisAccount(
                    userId,
                    username,
                    true,
                    GravisUuid.fromUserId(userId),
                    skinUrl,
                    skinModel
            );
        }
    }

    /**
     * Best-effort housekeeping: drop expired rows and keep only the newest
     * few tokens per account. Failures are logged but never block a login.
     */
    private static void prune(int userId) {
        try (Connection connection = GravisDatabase.connect()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM auth_tokens WHERE expires_at <= NOW()"
            )) {
                statement.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    """
                    DELETE FROM auth_tokens
                    WHERE user_id = ?
                      AND id NOT IN (
                        SELECT id FROM (
                          SELECT id FROM auth_tokens
                          WHERE user_id = ?
                          ORDER BY id DESC
                          LIMIT ?
                        ) keep
                      )
                    """
            )) {
                statement.setInt(1, userId);
                statement.setInt(2, userId);
                statement.setInt(3, MAX_TOKENS_PER_USER);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            Gravisauth.LOGGER.debug(
                    "Could not prune login tokens for account #{}",
                    userId,
                    e
            );
        }
    }

    private static Timestamp expiry() {
        int days = Math.max(1, Gravisauth.CONFIG.tokenLifetimeDays);

        return Timestamp.from(Instant.now().plus(days, ChronoUnit.DAYS));
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
