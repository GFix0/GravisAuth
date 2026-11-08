-- Player skins used by GravisAuth.
--
-- The website stores a direct HTTPS link to a 64x64 PNG and the arm model.
-- The mod reads both during authentication and pushes them to clients.
-- NULL skin_url means the default (Steve/Alex) skin.

ALTER TABLE gravis.users
  ADD COLUMN skin_url VARCHAR(512) NULL DEFAULT NULL AFTER last_login,
  ADD COLUMN skin_model ENUM('wide','slim') NOT NULL DEFAULT 'wide'
    AFTER skin_url;
