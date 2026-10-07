package mod.gfix.dev.gravisauth.account;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class GravisUuid {

    private static final UUID GRAVIS_NAMESPACE =
            UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

    private GravisUuid() {
    }

    public static UUID fromUserId(int userId) {
        return uuidV5(
                GRAVIS_NAMESPACE,
                "gfix.dev:gravis:" + userId
        );
    }

    private static UUID uuidV5(UUID namespace, String name) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            ByteBuffer namespaceBytes = ByteBuffer.allocate(16);
            namespaceBytes.putLong(namespace.getMostSignificantBits());
            namespaceBytes.putLong(namespace.getLeastSignificantBits());

            digest.update(namespaceBytes.array());
            digest.update(name.getBytes(StandardCharsets.UTF_8));

            byte[] hash = digest.digest();

            // UUID version 5
            hash[6] &= 0x0F;
            hash[6] |= 0x50;

            // RFC 4122 variant
            hash[8] &= 0x3F;
            hash[8] |= 0x80;

            ByteBuffer uuidBytes = ByteBuffer.wrap(hash);

            return new UUID(
                    uuidBytes.getLong(),
                    uuidBytes.getLong()
            );

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-1 is not available",
                    e
            );
        }
    }
}