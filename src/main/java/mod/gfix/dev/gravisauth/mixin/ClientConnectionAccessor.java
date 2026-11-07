package mod.gfix.dev.gravisauth.mixin;

import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The netty channel is what PacketEvents (bundled with GrimAC) uses as its
 * per-connection key. It has no getter, so reflection through an accessor
 * is the only way to re-key it after the profile swap.
 */
@Mixin(ClientConnection.class)
public interface ClientConnectionAccessor {

    @Accessor("channel")
    Channel gravisAuth$getChannel();
}
