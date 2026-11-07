package mod.gfix.dev.gravisauth.auth;

import java.security.KeyPair;

/**
 * Authentication state for one connection that is still in the
 * configuration phase.
 */
public final class AuthSession {

    private final KeyPair keyPair;
    private final long createdAt;
    private final int maxAttempts;

    private int attempts;
    private boolean authenticated;
    private boolean closed;

    public AuthSession(KeyPair keyPair, int maxAttempts) {
        this.keyPair = keyPair;
        this.maxAttempts = maxAttempts;
        this.createdAt = System.currentTimeMillis();
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * @return true when another attempt is still allowed
     */
    public boolean registerFailure() {
        attempts++;
        return attempts < maxAttempts;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        this.closed = true;
    }
}
