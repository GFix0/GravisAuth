package mod.gfix.dev.gravisauth.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerCommonNetworkHandler.class)
public interface ServerCommonNetworkHandlerAccessor {

    @Accessor("server")
    MinecraftServer gravisAuth$getServer();

    @Accessor("connection")
    ClientConnection gravisAuth$getConnection();
}
