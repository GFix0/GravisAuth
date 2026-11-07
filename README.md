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

The vanilla login-phase "not whitelisted" result is ignored while the mod is
enabled, because at that point only the UUID the client advertised is known.
Bans are never relaxed. If the mod is missing, `white-list=true` still
applies as a fail-safe.

## Server setup

`config/gravisauth.json`:

```json
{
  "enabled": true,
  "loginTimeoutSeconds": 120,
  "maxAttempts": 3,
  "throttleAttempts": 10,
  "throttleMinutes": 10,
  "database": {
    "host": "127.0.0.1",
    "port": 3306,
    "database": "gravis",
    "username": "gravisauth",
    "password": "..."
  }
}
```

The database user only needs `SELECT` on `gravis.users`. The mod bundles the
MariaDB JDBC driver and bcrypt, so nothing else has to be installed.

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

> The login screen has to be filled in promptly: the vanilla client drops a
> connection that has not finished configuring after roughly 30 seconds.

## Building

```
./gradlew build
```

Output: `build/libs/gravisauth-1.0-SNAPSHOT.jar`. LuckPerms is `compileOnly`;
it must be present on the server, never bundled.
