package mod.gfix.dev.gravisauth.auth;

/**
 * Marks the thread while the vanilla login handler performs its
 * whitelist check.
 *
 * In offline mode the check uses the UUID the client advertises, which is
 * not the Gravis UUID yet. GravisAuth relaxes only that check; the
 * configuration phase checks the whitelist again once the real profile is
 * in place, so access control still happens on the authenticated UUID.
 */
public final class LoginWhitelistPhase {

    private static final ThreadLocal<Boolean> ACTIVE =
            ThreadLocal.withInitial(() -> false);

    private LoginWhitelistPhase() {
    }

    public static void enter() {
        ACTIVE.set(true);
    }

    public static void exit() {
        ACTIVE.remove();
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }
}
