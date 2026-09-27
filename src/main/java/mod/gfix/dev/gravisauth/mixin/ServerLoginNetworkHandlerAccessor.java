package mod.gfix.dev.gravisauth.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLoginNetworkHandler.class)
public interface ServerLoginNetworkHandlerAccessor {

    @Accessor("profile")
    void gravisAuth$setProfile(GameProfile profile);

    @Accessor("profile")
    GameProfile gravisAuth$getProfile();
}