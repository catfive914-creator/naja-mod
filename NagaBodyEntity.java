package com.catfive.naga;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/** ตัวพญานาคที่แสดงผล: ติดตามผู้เล่น (ผู้เล่นล่องหน) */
public class NagaBodyEntity extends Entity {
    private static final TrackedData<Integer> TRIBE = DataTracker.registerData(NagaBodyEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> HEADS = DataTracker.registerData(NagaBodyEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> OWNER = DataTracker.registerData(NagaBodyEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private UUID ownerUuid;

    public NagaBodyEntity(EntityType<? extends NagaBodyEntity> type, World world) { super(type, world); this.noClip = true; }

    @Override protected void initDataTracker(DataTracker.Builder b) { b.add(TRIBE, 0); b.add(HEADS, 1); b.add(OWNER, -1); }

    public void setup(PlayerEntity owner, int tribe, int heads) {
        ownerUuid = owner.getUuid();
        dataTracker.set(OWNER, owner.getId());
        dataTracker.set(TRIBE, tribe);
        dataTracker.set(HEADS, heads);
        setPosition(owner.getPos());
    }
    public int getTribe() { return dataTracker.get(TRIBE); }
    public int getHeads() { return dataTracker.get(HEADS); }
    public int getOwnerId() { return dataTracker.get(OWNER); }
    public void setHeads(int h) { dataTracker.set(HEADS, h); }

    @Override public void tick() {
        super.tick();
        if (!getWorld().isClient) {
            PlayerEntity o = ownerUuid == null ? null : getWorld().getPlayerByUuid(ownerUuid);
            if (o == null || !o.isAlive()) { discard(); return; }
            setPosition(o.getPos());
        }
    }
    @Override public boolean canHit() { return false; }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {}
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {}
}
