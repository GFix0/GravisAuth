package mod.gfix.dev.gravisauth.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/**
 * Mod Menu configuration screen.
 *
 * GravisAuth has no settings to tune, so this screen exists purely to manage
 * the saved login from inside the game:
 *
 * - "Log out & reconnect" forgets the saved token, leaves the server and
 *   immediately joins it again, which reopens the sign-in screen. This is
 *   how a player switches from one Gravis account to another.
 * - "Log out & disconnect" forgets the token and returns to the server list.
 * - "Forget saved login" does the same while not connected.
 * - "Open server list" is the login path from the title screen: joining any
 *   Gravis server opens the sign-in screen when no token is stored.
 */
public final class GravisModMenuScreen extends Screen {

    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;

    private final Screen parent;

    private ClientLoginStore.SavedLogin saved;
    private ServerInfo serverInfo;
    private boolean connected;
    private String notice;

    public GravisModMenuScreen(Screen parent) {
        super(Text.literal("GravisAuth"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MinecraftClient client = this.client;

        this.saved = ClientLoginStore.load();
        this.connected = client != null && client.getNetworkHandler() != null;
        this.serverInfo = client == null
                ? null
                : client.getCurrentServerEntry();

        int centerX = this.width / 2;
        int y = this.height / 2 - 20;

        if (this.connected) {
            if (this.canReconnect()) {
                this.addDrawableChild(
                        ButtonWidget.builder(
                                        Text.literal("Log out & reconnect"),
                                        button -> this.logOutAndReconnect()
                                )
                                .dimensions(
                                        centerX - BUTTON_WIDTH / 2,
                                        y,
                                        BUTTON_WIDTH,
                                        BUTTON_HEIGHT
                                )
                                .tooltip(Tooltip.of(
                                        Text.literal(
                                                "Forget the saved login and rejoin "
                                                        + "this server to sign in again."
                                        )
                                ))
                                .build()
                );

                y += BUTTON_SPACING;
            }

            this.addDrawableChild(
                    ButtonWidget.builder(
                                    Text.literal("Log out & disconnect"),
                                    button -> this.logOutAndDisconnect()
                            )
                            .dimensions(
                                    centerX - BUTTON_WIDTH / 2,
                                    y,
                                    BUTTON_WIDTH,
                                    BUTTON_HEIGHT
                            )
                            .build()
            );

            y += BUTTON_SPACING;
        } else {
            ButtonWidget forget = ButtonWidget.builder(
                            Text.literal("Forget saved login"),
                            button -> this.forgetSavedLogin()
                    )
                    .dimensions(
                            centerX - BUTTON_WIDTH / 2,
                            y,
                            BUTTON_WIDTH,
                            BUTTON_HEIGHT
                    )
                    .build();

            forget.active = this.saved != null;
            this.addDrawableChild(forget);

            y += BUTTON_SPACING;

            this.addDrawableChild(
                    ButtonWidget.builder(
                                    Text.literal("Open server list"),
                                    button -> this.openServerList()
                            )
                            .dimensions(
                                    centerX - BUTTON_WIDTH / 2,
                                    y,
                                    BUTTON_WIDTH,
                                    BUTTON_HEIGHT
                            )
                            .build()
            );

            y += BUTTON_SPACING;
        }

        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("Done"),
                                button -> this.close()
                        )
                        .dimensions(
                                centerX - BUTTON_WIDTH / 2,
                                y,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );
    }

    private boolean canReconnect() {
        return this.serverInfo != null
                && !this.serverInfo.isLocal()
                && !this.serverInfo.isRealm()
                && this.serverInfo.address != null
                && ServerAddress.isValid(this.serverInfo.address);
    }

    private void logOutAndReconnect() {
        MinecraftClient client = this.client;
        ServerInfo info = this.serverInfo;

        if (client == null || info == null) {
            this.close();
            return;
        }

        ClientLoginStore.clear();

        client.disconnect(Text.literal("Logged out of Gravis."));

        ConnectScreen.connect(
                new MultiplayerScreen(new TitleScreen()),
                client,
                ServerAddress.parse(info.address),
                info,
                false,
                null
        );
    }

    private void logOutAndDisconnect() {
        ClientLoginStore.clear();

        MinecraftClient client = this.client;

        if (client != null && client.getNetworkHandler() != null) {
            client.disconnect(Text.literal(
                    "Logged out of Gravis. Join a server to sign in again."
            ));
            return;
        }

        this.notice = "Saved login forgotten.";
        this.clearAndInit();
    }

    private void forgetSavedLogin() {
        ClientLoginStore.clear();
        this.notice = "Saved login forgotten.";
        this.clearAndInit();
    }

    private void openServerList() {
        if (this.client != null) {
            this.client.setScreen(new MultiplayerScreen(new TitleScreen()));
        }
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int top = this.height / 2 - 94;

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("GravisAuth"),
                centerX,
                top,
                0xFFFFFF
        );

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(
                        this.saved == null
                                ? "No saved login on this computer"
                                : "Saved login: " + this.saved.username()
                ),
                centerX,
                top + 18,
                this.saved == null ? 0xA0A0A0 : 0x55FF55
        );

        String connection;

        if (!this.connected) {
            connection = "Not connected";
        } else if (this.serverInfo != null
                && this.serverInfo.name != null
                && !this.serverInfo.name.isBlank()) {
            connection = "Connected to " + this.serverInfo.name;
        } else {
            connection = "Connected to a local world";
        }

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(connection),
                centerX,
                top + 32,
                0xA0A0A0
        );

        if (this.notice != null) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal(this.notice),
                    centerX,
                    top + 50,
                    0xFFFF55
            );
        }

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(
                        this.connected
                                ? "Logging out ends the current Gravis session."
                                : "Join a Gravis server to sign in."
                ),
                centerX,
                this.height - 28,
                0x808080
        );
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(
                    this.parent == null ? new TitleScreen() : this.parent
            );
        }
    }
}
