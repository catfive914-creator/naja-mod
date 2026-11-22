package com.catfive.naga;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class NagaMod implements ModInitializer {
    public static final String ID = "catfive_naga";

    public static final AttachmentType<NagaState> STATE = AttachmentRegistry.<NagaState>builder()
        .persistent(NagaState.CODEC).copyOnDeath().buildAndRegister(Identifier.of(ID, "state"));

    public static final EntityType<NagaBodyEntity> BODY = Registry.register(Registries.ENTITY_TYPE,
        Identifier.of(ID, "naga_body"),
        EntityType.Builder.<NagaBodyEntity>create(NagaBodyEntity::new, SpawnGroup.MISC)
            .dimensions(0.5f, 0.5f).maxTrackingRange(16).trackingTickInterval(1).disableSaving().build("naga_body"));

    @Override public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(Net.Action.ID, Net.Action.CODEC);
        PayloadTypeRegistry.playS2C().register(Net.Sync.ID, Net.Sync.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Net.Action.ID, (payload, ctx) -> NagaLogic.handle(ctx.player(), payload.id()));

        ServerPlayConnectionEvents.JOIN.register((h, sender, server) -> NagaLogic.sync(h.player));
        ServerPlayConnectionEvents.DISCONNECT.register((h, server) -> NagaLogic.revert(h.player));

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, src, amount) -> {
            if (entity instanceof ServerPlayerEntity p) NagaLogic.revert(p);
            return true;
        });
        // ฆ่ามอนสเตอร์ = +1 เลเวล
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killed) -> {
            if (entity instanceof ServerPlayerEntity p) NagaLogic.addLevel(p, 1);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) NagaLogic.tick(p);
        });
    }
}
