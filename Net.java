package com.catfive.naga;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class Net {
    /** 0=สุ่มเผ่า 1=แปลงร่าง 2-5=สกิล Z X C V 6=อันติ */
    public record Action(int id) implements CustomPayload {
        public static final Id<Action> ID = new Id<>(Identifier.of(NagaMod.ID, "action"));
        public static final PacketCodec<RegistryByteBuf, Action> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, Action::id, Action::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record Sync(int tribe, int level, boolean transformed) implements CustomPayload {
        public static final Id<Sync> ID = new Id<>(Identifier.of(NagaMod.ID, "sync"));
        public static final PacketCodec<RegistryByteBuf, Sync> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, Sync::tribe, PacketCodecs.VAR_INT, Sync::level, PacketCodecs.BOOL, Sync::transformed, Sync::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    private Net() {}
}
