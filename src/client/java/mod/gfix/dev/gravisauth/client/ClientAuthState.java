package mod.gfix.dev.gravisauth.client;

import mod.gfix.dev.gravisauth.crypto.GravisCrypto;
import mod.gfix.dev.gravisauth.mixin.client.ClientCommonNetworkHandlerAccessor;
import mod.gfix.dev.gravisauth.network.GravisAuthNetworking;
import mod.gfix.dev.gravisauth.network.GravisAuthPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ClientConfigurationNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;

/**
 * Client half of the login handshake.
 *
 * When a saved-login token exists it is submitted automatically as soon as
 * the server sends its challenge. Passwords are only requested when there
 * is no token or the server rejects it.
 */
public final class ClientAuthState {

    private static PublicKey serverKey;
    private static KeyPair keyPair;

    /*
     * The configuration-phase connection. Kept so cancelling the login can
     * close it: until the socket goes away the server keeps the slot
     * reserved for the configured login timeout.
     */
    private static ClientConnection connection;

    private static String pendingUsername;
    private static String busyMessage;
    private static boolean tokenAttempt;

    private ClientAuthState() {
    }

    public static void handle(
            String message,
            ClientConfigurationNetworkHandler handler
    ) {
        if (handler != null) {
            connection = ((ClientCommonNetworkHandlerAccessor) handler)
                    .gravisAuth$getConnection();
        }

        if (message == null) {
            return;
        }

        if (message.startsWith("challenge:")) {
            beginChallenge(message.substring("challenge:".length()));
        } else if (message.equals("ok")) {
            finish(null);
        } else if (message.startsWith("ok:")) {
            finish(message.substring("ok:".length()));
        } else if (message.startsWith("fail:")) {
            fail(message.substring("fail:".length()));
        }
    }

    private static void beginChallenge(String publicKeyBase64) {
        reset();

        try {
            serverKey = GravisCrypto.decodePublicKey(publicKeyBase64);
            keyPair = GravisCrypto.generateKeyPair();
        } catch (Exception e) {
            GravisauthClient.LOGGER.error(
                    "Could not read the server challenge", e
            );
            return;
        }

        ClientLoginStore.SavedLogin saved = ClientLoginStore.load();
        MinecraftClient client = MinecraftClient.getInstance();

        if (saved != null && saved.username() != null
                && !saved.username().isBlank()) {
            pendingUsername = saved.username();
            tokenAttempt = true;
            busyMessage = "Signing in with your saved login\u2026";

            client.execute(() -> {
                client.setScreen(new GravisAuthScreen());
                submitToken(saved.username(), saved.token());
            });
            return;
        }

        client.execute(() -> client.setScreen(new GravisAuthScreen()));
    }

    private static void finish(String encryptedToken) {
        String username = pendingUsername;

        if (encryptedToken != null && !encryptedToken.isEmpty()) {
            try {
                String plaintext = new String(
                        GravisCrypto.decrypt(
                                sessionKey(),
                                encryptedToken
                        ),
                        StandardCharsets.UTF_8
                );

                int separator = plaintext.indexOf('\u0000');

                if (separator > 0) {
                    String name = plaintext.substring(0, separator);
                    String token = plaintext.substring(separator + 1);

                    if (!token.isBlank()) {
                        ClientLoginStore.save(name, token);
                        username = name;
                    }
                }
            } catch (Exception e) {
                GravisauthClient.LOGGER.warn(
                        "Could not read the saved-login token", e
                );
            }
        }

        busyMessage = null;
        tokenAttempt = false;
        pendingUsername = username;
        connection = null;

        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> {
            if (client.currentScreen instanceof GravisAuthScreen) {
                client.setScreen(null);
            }
        });
    }

    /**
     * Abandons the login: closes the connection and returns to the server
     * list. Closing the socket is what releases the slot the server reserves
     * for the login handshake.
     */
    public static void cancel() {
        ClientConnection active = connection;
        connection = null;

        GravisauthClient.LOGGER.info("Login cancelled by the player");

        reset();

        MinecraftClient client = MinecraftClient.getInstance();

        if (active != null) {
            active.disconnect(Text.literal("Login cancelled"));
        }

        client.setScreen(new MultiplayerScreen(new TitleScreen()));
    }

    private static void fail(String reason) {
        boolean rejectedToken = tokenAttempt
                && GravisAuthNetworking.TOKEN_INVALID.equals(reason);

        if (rejectedToken) {
            ClientLoginStore.clear();
        }

        tokenAttempt = false;
        busyMessage = null;

        String shown = rejectedToken
                ? "Saved login expired. Enter your password."
                : reason;

        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> {
            if (client.currentScreen instanceof GravisAuthScreen screen) {
                screen.setError(shown);
            }
        });
    }

    public static boolean isReady() {
        return serverKey != null && keyPair != null;
    }

    public static String pendingUsername() {
        return pendingUsername;
    }

    public static String busyMessage() {
        return busyMessage;
    }

    public static void submit(String username, String password) {
        tokenAttempt = false;
        pendingUsername = username;

        sendCredentials(
                "auth:",
                username + "\u0000" + password
        );
    }

    private static void submitToken(String username, String token) {
        tokenAttempt = true;
        pendingUsername = username;

        sendCredentials(
                "token:",
                username + "\u0000" + token
        );
    }

    private static void sendCredentials(
            String prefix,
            String credentials
    ) {
        if (!isReady() || connection == null || !connection.isOpen()) {
            showError("Connection to the server was lost. Press Esc to go back.");
            return;
        }

        try {
            String encrypted = GravisCrypto.encrypt(
                    sessionKey(),
                    credentials.getBytes(StandardCharsets.UTF_8)
            );

            String clientPublicKey =
                    GravisCrypto.encodePublicKey(keyPair.getPublic());

            ClientConfigurationNetworking.send(
                    new GravisAuthPayload(
                            prefix + clientPublicKey + ":" + encrypted
                    )
            );
        } catch (Exception e) {
            GravisauthClient.LOGGER.error(
                    "Could not send the authentication response", e
            );

            showError("Connection to the server was lost. Press Esc to go back.");
        }
    }

    private static SecretKeySpec sessionKey()
            throws GeneralSecurityException {

        return GravisCrypto.deriveKey(keyPair.getPrivate(), serverKey);
    }

    private static void reset() {
        serverKey = null;
        keyPair = null;
        pendingUsername = null;
        busyMessage = null;
        tokenAttempt = false;
    }

    private static void showError(String message) {
        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> {
            if (client.currentScreen instanceof GravisAuthScreen screen) {
                screen.setError(message);
            }
        });
    }
}
