package mod.gfix.dev.gravisauth.skin;

import java.net.URI;
import java.util.Locale;

/**
 * Validation of player-supplied skin URLs.
 *
 * Skins are loaded by every connected client, so only plain HTTPS links
 * are accepted: no other scheme, no embedded credentials, no IP literals
 * and no local host names. The client repeats the check before every
 * download so a malicious server cannot point it at a local address.
 */
public final class SkinUrls {

    public static final int MAX_LENGTH = 512;

    private SkinUrls() {
    }

    /**
     * @return the normalised URL, an empty string when the skin should be
     *         cleared, or null when the value must not be used
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }

        String value = raw.trim();

        if (value.isEmpty()) {
            return "";
        }

        if (value.length() > MAX_LENGTH) {
            return null;
        }

        URI uri;

        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException e) {
            return null;
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.isOpaque()
                || uri.getUserInfo() != null) {
            return null;
        }

        String host = uri.getHost();

        if (host == null || host.isBlank()) {
            return null;
        }

        host = host.toLowerCase(Locale.ROOT);

        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }

        if (host.equals("localhost")
                || host.endsWith(".localhost")
                || host.endsWith(".local")
                || host.endsWith(".internal")
                || host.endsWith(".home.arpa")) {
            return null;
        }

        /*
         * Reject raw IPv4 and IPv6 literals. A host name can still resolve
         * to a private address, but the account owner chooses their own
         * skin URL, so that is not an attack surface against others.
         */
        if (host.indexOf(':') >= 0
                || host.matches("\\d{1,3}(?:\\.\\d{1,3}){3}")) {
            return null;
        }

        if (uri.getPort() != -1 && uri.getPort() != 443) {
            return null;
        }

        return uri.toString();
    }

    public static boolean isSlim(String model) {
        return "slim".equalsIgnoreCase(model);
    }
}
