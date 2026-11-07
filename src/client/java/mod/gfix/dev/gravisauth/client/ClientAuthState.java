package mod.gfix.dev.gravisauth.client;

import mod.gfix.dev.gravisauth.crypto.GravisCrypto;
import mod.gfix.dev.gravisauth.network.GravisAuthPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.minecraft.client.MinecraftClient;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;

/**
 * Client half of the login handshake.
 */
public final class ClientAuthState {

    private static PublicKey serverKey;
    private static KeyPair keyPair;

    private ClientAuthState() {
    }

    public static void handle(String message) {
        if (message == null) {
            return;
        }

        if (message.startsWith("challenge:")) {
            beginChallenge(message.substring("challenge:".length()));
        } else if (message.equals("ok")) {
            MinecraftClient client = MinecraftClient.getInstance();

            client.execute(() -> {
                if (client.currentScreen instanceof GravisAuthScreen) {
                    client.setScreen(null);
                }
            });
        } else if (message.startsWith("fail:")) {
            String reason = message.substring("fail:".length());
            GravisAuthScreen screen = currentScreen();

            if (screen != null) {
                screen.setError(reason);
            }
        }
    }

    private static void beginChallenge(String publicKeyBase64) {
        try {
            serverKey = GravisCrypto.decodePublicKey(publicKeyBase64);
            keyPair = GravisCrypto.generateKeyPair();
        } catch (Exception e) {
            GravisauthClient.LOGGER.error(
                    "Could not read the server challenge", e
            );
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> client.setScreen(new GravisAuthScreen()));
    }

    public static boolean isReady() {
        return serverKey != null && keyPair != null;
    }

    public static void submit(String username, String password) {
        if (!isReady()) {
            GravisAuthScreen screen = currentScreen();

            if (screen != null) {
                screen.setError("Not connected to the authentication server");
            }
            return;
        }

        try {
            SecretKeySpec key = GravisCrypto.deriveKey(
                    keyPair.getPrivate(),
                    serverKey
            );

            String credentials = username + "\u0000" + password;

            byte[] plaintext = credentials.getBytes(StandardCharsets.UTF_8);

            String encrypted = GravisCrypto.encrypt(key, plaintext);

            String clientPublicKey =
                    GravisCrypto.encodePublicKey(keyPair.getPublic());

            ClientConfigurationNetworking.send(
                    new GravisAuthPayload(
                            "auth:" + clientPublicKey + ":" + encrypted
                    )
            );
        } catch (Exception e) {
            GravisauthClient.LOGGER.error(
                    "Could not send the authentication response", e
            );

            GravisAuthScreen screen = currentScreen();

            if (screen != null) {
                screen.setError("Could not contact the server");
            }
        }
    }

    private static GravisAuthScreen currentScreen() {
        if (MinecraftClient.getInstance().currentScreen
                instanceof GravisAuthScreen screen) {
            return screen;
        }

        return null;
    }
}
