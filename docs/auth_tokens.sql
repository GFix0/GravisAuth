-- Remembered-login tokens used by GravisAuth.
--
-- Apply as a database administrator (the Minecraft server's DB account has
-- no DDL privileges):
--
--   mysql gravis < docs/auth_tokens.sql
--
-- Then grant the server account access to this table only:
--
--   GRANT SELECT, INSERT, UPDATE, DELETE
--     ON gravis.auth_tokens TO 'gravisauth'@'localhost';
--   FLUSH PRIVILEGES;
--
-- Only SHA-256 hashes are stored. Tokens are bound to the user id, so a
-- username change on the website does not invalidate saved logins.

CREATE TABLE IF NOT EXISTS `auth_tokens` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(10) unsigned NOT NULL,
  `token_hash` char(64) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_used_at` timestamp NULL DEFAULT NULL,
  `expires_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token_hash` (`token_hash`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `auth_tokens_ibfk_1`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
