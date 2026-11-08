# GravisAuth

Fabric server-side authentication for the Gravis SMP. The server runs in
offline mode; this mod replaces Mojang session auth with a Gravis account.

## How it works

1. The client connects and reaches the **configuration** phase. The server
   sends an X25519 challenge on `gravisauth:auth`.
2. The client shows a login screen. The player enters their Gravis
   **username** and website password.
3. The password is checked against the `gravis.users` table with bcrypt,
   and `email_verified` must be set.
4. On success the connection's profile is replaced with the Gravis UUID
   (`UUIDv5("gfix.dev:gravis:" + user_id)`) and the player name with the
   Gravis username, the LuckPerms user is pre-loaded, and only then is the
   configuration phase allowed to finish.
5. The whitelist is checked again on the Gravis UUID, so `whitelist.json`
   and `ops.json` are keyed by the Gravis UUID and survive renames.
6. On success the server also issues a **remembered-login token**, so the
   next connection signs in without asking for the password again.
7. In the play phase the server publishes the skin table; each client
   downloads the player-chosen skin itself (see below).

The vanilla login-phase "not whitelisted" result is ignored while the mod is
enabled, because at that point only the UUID the client advertised is known.
Bans are never relaxed. If the mod is missing, `white-list=true` still
applies as a fail-safe.

## Remembered login

After a password login the server creates a 256-bit random token and sends
it back encrypted with the same ephemeral key as the password. The client
stores it next to the Gravis username in `config/gravisauth-client.json`
(the password is never written to disk). On the next connection it is
submitted automatically as soon as the challenge arrives.

- Only the SHA-256 hash is stored in `gravis.auth_tokens`; the plaintext
  token exists client-side only.
- Tokens are bound to the user id, so a username change on the website
  keeps them valid. The current username is sent back on every login.
- Each use slides the expiry forward (`tokenLifetimeDays`, default 30).
- If the server rejects a token the client deletes it and falls back to the
  password screen; expired tokens are pruned from the table.
- Revoke a device by deleting its row from `auth_tokens`. Deleting the
  client file, or the whole table, is always safe.

## Skins

Players set their skin on the website (`users.skin_url`, `users.skin_model`).
The URL must be a direct HTTPS link to a 64x64 PNG; IP literals, localhost,
embedded credentials and non-standard ports are rejected by the website,
the server and the client.

When an authenticated player enters the play phase the server sends the
URL table on `gravisauth:skins`, together with the receiver's own Gravis
UUID. Every client downloads the textures itself (cached under
`gravisauth-skins/` in the game directory) and overrides both
`PlayerListEntry#getSkinTextures` and `AbstractClientPlayerEntity#getSkin`.
The first covers the player renderer, the tab list and the inventory for
other players; the second is needed for your own model, because the client
renders it with the profile it logged in with, which still carries the
offline UUID. A change applies on the next join; clearing the URL restores
the default skin. Add the columns with `docs/skins.sql`.

## Server setup

`config/gravisauth.json`:

```json
{
  "enabled": true,
  "loginTimeoutSeconds": 120,
  "maxAttempts": 3,
  "throttleAttempts": 10,
  "throttleMinutes": 10,
  "tokenLifetimeDays": 30,
  "database": {
    "host": "127.0.0.1",
    "port": 3306,
    "database": "gravis",
    "username": "gravisauth",
    "password": "..."
  }
}
```

The database user needs `SELECT` on `gravis.users` and
`SELECT, INSERT, UPDATE, DELETE` on `gravis.auth_tokens`. Create the table
with `docs/auth_tokens.sql`:

```sql
GRANT SELECT ON gravis.users TO 'gravisauth'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE
  ON gravis.auth_tokens TO 'gravisauth'@'localhost';
```

The mod bundles the MariaDB JDBC driver and bcrypt, so nothing else has to
be installed.

## Client setup

A vanilla client cannot join: the server requires Fabric Loader and Fabric
API, and the registry contents include modded entries. Install:

1. **Fabric Loader 1.21.11** (launcher profile).
2. **Fabric API** `0.141.6+1.21.11` in `.minecraft/mods`.
3. **`gravisauth-1.0-SNAPSHOT.jar`** in `.minecraft/mods` (the same jar the
   server uses).

The Minecraft account name no longer matters; the Gravis account decides the
in-game name and UUID. Rename yourself on the website and the change applies
the next time you join.

> The server sends a small keepalive every 10 seconds while a login is
> pending, so the connection no longer drops after the vanilla 30-second
> read timeout. Pressing Escape cancels the login and returns to the
> server list.

## Building

```
./gradlew build
```

Output: `build/libs/gravisauth-1.0-SNAPSHOT.jar`. LuckPerms is `compileOnly`;
it must be present on the server, never bundled.

### Testing two accounts locally

```
./gradlew runClient      # run/  , saved login for one account
./gradlew runClient2     # run2/ , saved login for a second account
```

Both clients need the same mod set; copy `run/mods/*.jar` into `run2/mods/`
the first time, otherwise the server's `carpet-shadow` registry entry is
unknown to the second client and it is disconnected.
