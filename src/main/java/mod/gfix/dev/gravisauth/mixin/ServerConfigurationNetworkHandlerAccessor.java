package mod.gfix.dev.gravisauth.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerConfigurationNetworkHandler.class)
public interface ServerConfigurationNetworkHandlerAccessor {

    @Mutable
    @Accessor("profile")
    void gravisAuth$setProfile(GameProfile profile);

    @Accessor("profile")
    GameProfile gravisAuth$getProfile();

    @Invoker("endConfiguration")
    void gravisAuth$endConfiguration();
}
