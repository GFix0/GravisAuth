package mod.gfix.dev.gravisauth.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GravisAuthPayload(String message) implements CustomPayload {

    public static final Identifier ID =
            Identifier.of("gravisauth", "auth");

    public static final CustomPayload.Id<GravisAuthPayload> TYPE =
            new CustomPayload.Id<>(ID);

    public static final PacketCodec<PacketByteBuf, GravisAuthPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.string(32767),
                    GravisAuthPayload::message,
                    GravisAuthPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}