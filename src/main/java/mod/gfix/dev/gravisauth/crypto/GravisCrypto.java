package mod.gfix.dev.gravisauth.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Transport protection for the credential exchange.
 *
 * Offline-mode Minecraft connections are not encrypted, so the password
 * must never travel as plaintext. Both sides perform an ephemeral
 * X25519 key agreement and the credentials are sent inside AES-GCM.
 *
 * This protects against passive observers. It is not authenticated, so a
 * party that can actively intercept the TCP stream could substitute its
 * own keys. Players should only ever connect to the real server address.
 */
public final class GravisCrypto {

    private static final String KEY_ALGORITHM = "X25519";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private GravisCrypto() {
    }

    public static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        return generator.generateKeyPair();
    }

    public static String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static PublicKey decodePublicKey(String base64)
            throws GeneralSecurityException {
        byte[] encoded = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance(KEY_ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(encoded));
    }

    /**
     * Derive the symmetric key from the raw X25519 secret.
     */
    public static SecretKeySpec deriveKey(
            PrivateKey ownKey,
            PublicKey peerKey
    ) throws GeneralSecurityException {

        KeyAgreement agreement = KeyAgreement.getInstance(KEY_ALGORITHM);
        agreement.init(ownKey);
        agreement.doPhase(peerKey, true);

        byte[] secret = agreement.generateSecret();
        byte[] key = MessageDigest.getInstance("SHA-256").digest(secret);

        return new SecretKeySpec(key, "AES");
    }

    /**
     * Returns base64(iv || ciphertext || tag).
     */
    public static String encrypt(SecretKeySpec key, byte[] plaintext)
            throws GeneralSecurityException {

        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));

        byte[] encrypted = cipher.doFinal(plaintext);

        byte[] output = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, output, 0, iv.length);
        System.arraycopy(encrypted, 0, output, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(output);
    }

    public static byte[] decrypt(SecretKeySpec key, String base64)
            throws GeneralSecurityException {

        byte[] input = Base64.getDecoder().decode(base64);

        if (input.length <= IV_LENGTH) {
            throw new GeneralSecurityException("Ciphertext is too short");
        }

        byte[] iv = new byte[IV_LENGTH];
        byte[] encrypted = new byte[input.length - IV_LENGTH];

        System.arraycopy(input, 0, iv, 0, IV_LENGTH);
        System.arraycopy(input, IV_LENGTH, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));

        return cipher.doFinal(encrypted);
    }
}
