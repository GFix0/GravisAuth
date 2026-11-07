package mod.gfix.dev.gravisauth.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

/**
 * Login screen shown while the server holds the connection in the
 * configuration phase.
 */
public class GravisAuthScreen extends Screen {

    private TextFieldWidget usernameField;
    private TextFieldWidget passwordField;
    private ButtonWidget loginButton;

    private String error;

    public GravisAuthScreen() {
        super(Text.literal("Gravis Login"));
    }

    @Override
    protected void init() {
        int fieldWidth = 220;
        int centerX = this.width / 2;

        this.usernameField = new TextFieldWidget(
                this.textRenderer,
                centerX - fieldWidth / 2,
                this.height / 2 - 50,
                fieldWidth,
                20,
                Text.literal("Gravis username")
        );

        this.usernameField.setMaxLength(32);
        this.usernameField.setPlaceholder(
                Text.literal("Gravis username")
        );

        String lastUsername = this.client != null
                ? this.client.getSession().getUsername()
                : "";

        if (lastUsername != null && !lastUsername.isBlank()) {
            this.usernameField.setText(lastUsername);
        }

        this.addDrawableChild(this.usernameField);

        this.passwordField = new TextFieldWidget(
                this.textRenderer,
                centerX - fieldWidth / 2,
                this.height / 2 - 20,
                fieldWidth,
                20,
                Text.literal("Password")
        );

        this.passwordField.setMaxLength(1024);
        this.passwordField.setPlaceholder(Text.literal("Password"));

        // Never render the real characters.
        this.passwordField.addFormatter(
                (string, firstCharacterIndex) ->
                        OrderedText.styledForwardsVisitedString(
                                "*".repeat(string.length()),
                                Style.EMPTY
                        )
        );

        this.addDrawableChild(this.passwordField);

        this.loginButton = ButtonWidget.builder(
                        Text.literal("Log in"),
                        button -> this.submit()
                )
                .dimensions(centerX - 50, this.height / 2 + 12, 100, 20)
                .build();

        this.addDrawableChild(this.loginButton);

        this.setInitialFocus(this.usernameField);
    }

    private void submit() {
        String username = this.usernameField.getText().trim();
        String password = this.passwordField.getText();

        if (username.isEmpty()) {
            this.setError("Enter your Gravis username");
            return;
        }

        if (password.isEmpty()) {
            this.setError("Enter your password");
            return;
        }

        this.setError(null);
        this.loginButton.active = false;

        ClientAuthState.submit(username, password);
    }

    public void setError(String error) {
        this.error = error;

        if (this.loginButton != null) {
            this.loginButton.active = true;
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 257 || input.key() == 335) { // Enter / keypad Enter
            this.submit();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Sign in to the Gravis server"),
                this.width / 2,
                this.height / 2 - 90,
                0xFFFFFF
        );

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(
                        "Use the username and password of your gfix.dev account"
                ),
                this.width / 2,
                this.height / 2 - 74,
                0xA0A0A0
        );

        if (this.error != null) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal(this.error),
                    this.width / 2,
                    this.height / 2 + 42,
                    0xFF5555
            );
        }
    }

    /**
     * The connection cannot continue without a login, so Escape must not
     * close the screen.
     */
    @Override
    public void close() {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
