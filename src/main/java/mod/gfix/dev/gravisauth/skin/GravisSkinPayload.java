package mod.gfix.dev.gravisauth.skin;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The skin table a client needs to render other Gravis players.
 *
 * Vanilla only knows the Mojang session server, which has never heard of
 * Gravis UUIDs, so the server pushes the URLs instead.
 */
public record GravisSkinPayload(List<Entry> entries) implements CustomPayload {

    public static final Identifier ID =
            Identifier.of("gravisauth", "skins");

    public static final CustomPayload.Id<GravisSkinPayload> TYPE =
            new CustomPayload.Id<>(ID);

    private static final PacketCodec<RegistryByteBuf, Entry> ENTRY_CODEC =
            PacketCodec.tuple(
                    Uuids.PACKET_CODEC,
                    Entry::uuid,
                    PacketCodecs.string(SkinUrls.MAX_LENGTH),
                    Entry::url,
                    PacketCodecs.string(8),
                    Entry::model,
                    Entry::new
            );

    private static final PacketCodec<RegistryByteBuf, List<Entry>> LIST_CODEC =
            PacketCodecs.collection(ArrayList::new, ENTRY_CODEC, 64);

    public static final PacketCodec<RegistryByteBuf, GravisSkinPayload> CODEC =
            LIST_CODEC.xmap(GravisSkinPayload::new, GravisSkinPayload::entries);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    public record Entry(UUID uuid, String url, String model) {
    }
}
