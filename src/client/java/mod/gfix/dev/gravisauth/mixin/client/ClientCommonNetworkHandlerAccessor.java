package mod.gfix.dev.gravisauth.mixin.client;

import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the connection of the configuration-phase client handler.
 *
 * Vanilla has no public way to reach the connection from a screen, but the
 * login screen must be able to abandon it when the player presses Escape:
 * the server keeps the slot reserved until the connection goes away.
 */
@Mixin(ClientCommonNetworkHandler.class)
public interface ClientCommonNetworkHandlerAccessor {

    @Accessor("connection")
    ClientConnection gravisAuth$getConnection();
}
