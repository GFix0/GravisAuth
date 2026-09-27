package mod.gfix.dev.gravisauth.account;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GravisAccountManager {

    private final List<GravisAccount> accounts = new ArrayList<>();

    public GravisAccountManager() {
        accounts.add(new GravisAccount(
                "GFix0",
                "GFix"
        ));

        accounts.add(new GravisAccount(
                "TestPlayer",
                "TestPlayer"
        ));
    }

    public GravisAccount findByUsername(String username) {
        for (GravisAccount account : accounts) {
            if (account.getUsername().equals(username)) {
                return account;
            }
        }

        return null;
    }
}