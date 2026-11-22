package com.catfive.naga;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class NagaLogic {
    private static final Map<UUID, NagaBodyEntity> BODIES = new HashMap<>();
    private static final Map<UUID, long[]> COOLDOWN = new HashMap<>();
    private static final Map<UUID, Integer> ULT = new HashMap<>();
    private static final Identifier HP_ID = Identifier.of(NagaMod.ID, "naga_health");
    private static final int[] CD = {80, 160, 100, 300, 1200};

    public static NagaState get(ServerPlayerEntity p) { return p.getAttachedOrElse(NagaMod.STATE, NagaState.DEFAULT); }
    public static void set(ServerPlayerEntity p, NagaState s) { p.setAttached(NagaMod.STATE, s); sync(p); }
    public static void sync(ServerPlayerEntity p) {
        NagaState s = get(p);
        ServerPlayNetworking.send(p, new Net.Sync(s.tribe(), s.level(), s.transformed()));
    }
    private static void msg(ServerPlayerEntity p, String t) { p.sendMessage(Text.literal(t), true); }
    private static ServerWorld w(ServerPlayerEntity p) { return p.getServerWorld(); }

    public static void handle(ServerPlayerEntity p, int id) {
        switch (id) {
            case 0 -> roll(p);
            case 1 -> toggle(p);
            case 2, 3, 4, 5 -> skill(p, id - 2);
            case 6 -> ult(p);
            default -> {}
        }
    }

    // ---------- สุ่มเผ่า ----------
    static void roll(ServerPlayerEntity p) {
        NagaState s = get(p);
        if (s.tribe() >= 0) return;
        int t = p.getRandom().nextInt(4);
        set(p, s.withTribe(t).withLevel(1));
        p.sendMessage(Text.literal("คุณได้รับพญานาคเผ่า" + NagaInfo.TRIBE[t] + "!"), false);
    }

    // ---------- เลเวล ----------
    static void addLevel(ServerPlayerEntity p, int n) {
        NagaState s = get(p);
        if (s.tribe() < 0 || s.level() >= 100) return;
        int oldHeads = s.heads();
        NagaState ns = s.withLevel(s.level() + n);
        set(p, ns);
        if (ns.heads() != oldHeads) {
            p.sendMessage(Text.literal("เลเวล " + ns.level() + "! พญานาคของคุณมี " + ns.heads() + " เศียรแล้ว"), false);
            NagaBodyEntity b = BODIES.get(p.getUuid());
            if (b != null) b.setHeads(ns.heads());
        }
    }

    // ---------- แปลงร่าง ----------
    static void toggle(ServerPlayerEntity p) {
        NagaState s = get(p);
        if (s.tribe() < 0) { msg(p, "ยังไม่ได้สุ่มเผ่า กด N เพื่อสุ่ม"); return; }
        if (s.transformed()) revert(p); else transform(p);
    }

    static void transform(ServerPlayerEntity p) {
        NagaState s = get(p);
        PlayerInventory inv = p.getInventory();
        List<ItemStack> stash = new ArrayList<>();
        for (int i = 0; i < inv.size(); i++) stash.add(inv.getStack(i).copy());
        inv.clear();
        set(p, s.with(true, stash));

        NagaBodyEntity b = new NagaBodyEntity(NagaMod.BODY, w(p));
        b.setup(p, s.tribe(), s.heads());
        w(p).spawnEntity(b);
        BODIES.put(p.getUuid(), b);

        p.getAbilities().allowFlying = true;
        p.sendAbilitiesUpdate();
        EntityAttributeInstance hp = p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        hp.removeModifier(HP_ID);
        hp.addTemporaryModifier(new EntityAttributeModifier(HP_ID, 20, EntityAttributeModifier.Operation.ADD_VALUE));
        p.setHealth(p.getMaxHealth());
        applyEffects(p);
        msg(p, s.heads() >= 2 ? "แปลงร่างเป็นราชานาค " + s.heads() + " เศียร!" : "แปลงร่างเป็นพญานาคเต็มตัว!");
        w(p).spawnParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1, p.getZ(), 3, 1, 1, 1, 0);
    }

    public static void revert(ServerPlayerEntity p) {
        NagaState s = get(p);
        if (!s.transformed()) return;
        PlayerInventory inv = p.getInventory();
        inv.clear();
        for (int i = 0; i < Math.min(inv.size(), s.stash().size()); i++) inv.setStack(i, s.stash().get(i).copy());
        set(p, s.with(false, List.of()));
        NagaBodyEntity b = BODIES.remove(p.getUuid());
        if (b != null) b.discard();
        ULT.remove(p.getUuid());
        boolean can = p.isCreative() || p.isSpectator();
        p.getAbilities().allowFlying = can;
        if (!can) p.getAbilities().flying = false;
        p.sendAbilitiesUpdate();
        p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).removeModifier(HP_ID);
        p.removeStatusEffect(StatusEffects.INVISIBILITY);
        p.removeStatusEffect(StatusEffects.WATER_BREATHING);
        p.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
        msg(p, "กลับคืนร่างมนุษย์");
    }

    static void applyEffects(ServerPlayerEntity p) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 60, 0, false, false, false));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 60, 0, false, false, false));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 60, 0, false, false, false));
    }

    public static void tick(ServerPlayerEntity p) {
        if (!get(p).transformed()) return;
        ultTick(p);
        if (p.age % 20 == 0) {
            applyEffects(p);
            if (p.isTouchingWater()) p.heal(1.0f); // ฟื้นฟูพลังในน้ำ
        }
        NagaBodyEntity b = BODIES.get(p.getUuid());
        if (b == null || b.isRemoved()) { // ร่างหายไป (เช่น ย้ายมิติ) สร้างใหม่
            NagaState s = get(p);
            NagaBodyEntity nb = new NagaBodyEntity(NagaMod.BODY, w(p));
            nb.setup(p, s.tribe(), s.heads());
            w(p).spawnEntity(nb);
            BODIES.put(p.getUuid(), nb);
        }
    }

    // ---------- สกิล ----------
    private static boolean ready(ServerPlayerEntity p, int slot) {
        long now = w(p).getTime();
        long[] a = COOLDOWN.computeIfAbsent(p.getUuid(), k -> new long[5]);
        if (now < a[slot]) { msg(p, "สกิลยังไม่พร้อม รออีก " + ((a[slot] - now + 19) / 20) + " วินาที"); return false; }
        a[slot] = now + CD[slot];
        return true;
    }

    private static ParticleEffect fx(int tribe) {
        return switch (tribe) {
            case 0 -> ParticleTypes.SNEEZE;
            case 1 -> ParticleTypes.FLAME;
            case 2 -> ParticleTypes.SPLASH;
            default -> ParticleTypes.ELECTRIC_SPARK;
        };
    }

    static void skill(ServerPlayerEntity p, int slot) {
        NagaState s = get(p);
        if (!s.transformed()) { msg(p, "ต้องแปลงร่างก่อน (กด G)"); return; }
        if (ULT.containsKey(p.getUuid()) || !ready(p, slot)) return;
        int t = s.tribe();
        float dmg = 6 + s.level() * 0.15f;
        msg(p, NagaInfo.SKILL[t][slot]);
        switch (slot) {
            case 0 -> beam(p, 0, 14, dmg, t, true);
            case 1 -> {
                if (t == 2) whirlpool(p, 10, dmg);
                else if (t == 3) chain(p, 12, dmg);
                else ring(p, 8, dmg, t);
            }
            case 2 -> dash(p, dmg + 2);
            case 3 -> {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 2));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 3));
                if (t == 1) p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 400, 0));
                if (t == 2) { p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 2)); p.heal(10); }
                if (t == 3) p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 2));
                w(p).spawnParticles(fx(t), p.getX(), p.getY() + 1, p.getZ(), 60, 0.8, 1.0, 0.8, 0.05);
            }
        }
    }

    private static List<LivingEntity> near(ServerPlayerEntity p, Vec3d c, double r) {
        return w(p).getEntitiesByClass(LivingEntity.class, new Box(c.subtract(r, r, r), c.add(r, r, r)), e -> e != p && e.isAlive());
    }
    private static void hurt(ServerPlayerEntity p, LivingEntity e, float d) {
        e.damage(w(p).getDamageSources().playerAttack(p), d);
    }
    private static void affect(ServerPlayerEntity p, LivingEntity e, int t, Vec3d dir) {
        switch (t) {
            case 0 -> e.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 1));
            case 1 -> e.setOnFireFor(5);
            case 2 -> { e.addVelocity(dir.x * 1.2, 0.35, dir.z * 1.2); e.velocityModified = true; }
            default -> e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2));
        }
    }

    /** พ่นจากปาก; yawOff = องศาเบี่ยงซ้ายขวา */
    static void beam(ServerPlayerEntity p, double yawOff, double range, float dmg, int t, boolean extra) {
        ServerWorld world = w(p);
        Vec3d dir = p.getRotationVec(1f).rotateY((float) Math.toRadians(yawOff));
        Vec3d o = p.getEyePos().add(dir.multiply(1.2));
        Set<LivingEntity> hit = new HashSet<>();
        for (double d = 0; d <= range; d += 0.75) {
            Vec3d pos = o.add(dir.multiply(d));
            world.spawnParticles(fx(t), pos.x, pos.y, pos.z, 4, 0.25, 0.25, 0.25, 0.02);
            for (LivingEntity e : near(p, pos, 1.3)) if (hit.add(e)) { hurt(p, e, dmg); if (extra) affect(p, e, t, dir); }
        }
    }

    static void ring(ServerPlayerEntity p, double r, float dmg, int t) {
        ServerWorld world = w(p);
        for (int i = 0; i < 64; i++) {
            double a = i * Math.PI * 2 / 64;
            world.spawnParticles(fx(t), p.getX() + Math.cos(a) * r, p.getY() + 0.5, p.getZ() + Math.sin(a) * r, 3, 0.2, 0.4, 0.2, 0.02);
        }
        for (LivingEntity e : near(p, p.getPos(), r)) { hurt(p, e, dmg); affect(p, e, t, e.getPos().subtract(p.getPos()).normalize()); }
    }

    static void whirlpool(ServerPlayerEntity p, double r, float dmg) {
        ServerWorld world = w(p);
        for (int k = 0; k < 60; k++) {
            double a = k * 0.5, rr = r * (1 - k / 70.0);
            world.spawnParticles(ParticleTypes.SPLASH, p.getX() + Math.cos(a) * rr, p.getY() + 0.3 + k * 0.03, p.getZ() + Math.sin(a) * rr, 3, 0.1, 0.1, 0.1, 0.1);
        }
        for (LivingEntity e : near(p, p.getPos(), r)) {
            Vec3d v = p.getPos().subtract(e.getPos()).normalize().multiply(1.0);
            e.addVelocity(v.x, 0.3, v.z); e.velocityModified = true; hurt(p, e, dmg * 0.6f);
        }
    }

    static void strike(ServerPlayerEntity p, Vec3d at) {
        LightningEntity l = EntityType.LIGHTNING_BOLT.create(w(p));
        if (l == null) return;
        l.refreshPositionAfterTeleport(at);
        l.setChanneler(p);
        w(p).spawnEntity(l);
    }
    static void chain(ServerPlayerEntity p, double r, float dmg) {
        List<LivingEntity> l = near(p, p.getPos(), r);
        l.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)));
        for (int i = 0; i < Math.min(5, l.size()); i++) { strike(p, l.get(i).getPos()); hurt(p, l.get(i), dmg); }
    }

    static void dash(ServerPlayerEntity p, float dmg) {
        Vec3d d = p.getRotationVec(1f);
        p.setVelocity(d.x * 2.2, Math.max(d.y * 1.5, 0.25), d.z * 2.2);
        p.velocityModified = true;
        for (LivingEntity e : near(p, p.getPos().add(d.multiply(2.5)), 4)) {
            hurt(p, e, dmg);
            e.addVelocity(d.x * 1.0, 0.4, d.z * 1.0); e.velocityModified = true;
        }
        w(p).spawnParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + d.x * 2, p.getY() + 1, p.getZ() + d.z * 2, 5, 1, 0.5, 1, 0);
    }

    // ---------- อันติเมต: ทุกเศียรพ่นพลังพร้อมกัน ----------
    static void ult(ServerPlayerEntity p) {
        NagaState s = get(p);
        if (!s.transformed()) { msg(p, "ต้องแปลงร่างก่อน (กด G)"); return; }
        if (ULT.containsKey(p.getUuid()) || !ready(p, 4)) return;
        ULT.put(p.getUuid(), 80);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 85, 6, false, false, false));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 90, 4, false, false, false));
        p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(Text.literal(NagaInfo.ULT[s.tribe()])));
        w(p).playSound(null, p.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_ENDER_DRAGON_GROWL, net.minecraft.sound.SoundCategory.PLAYERS, 3f, 0.6f);
    }

    private static void ultTick(ServerPlayerEntity p) {
        Integer left = ULT.get(p.getUuid());
        if (left == null) return;
        NagaState s = get(p);
        int t = 80 - left, h = s.heads(), tr = s.tribe();
        ServerWorld world = w(p);
        float dmg = 5 + s.level() * 0.2f;
        if (t >= 20 && t % 4 == 0) {
            for (int i = 0; i < h; i++) {
                double off = h == 1 ? 0 : (i - (h - 1) / 2.0) * (90.0 / (h - 1));
                beam(p, off, 28, dmg, tr, false);
            }
        }
        if (t % 5 == 0) {
            double r = 2 + t * 0.25;
            for (int i = 0; i < 80; i++) {
                double a = i * Math.PI * 2 / 80;
                world.spawnParticles(fx(tr), p.getX() + Math.cos(a) * r, p.getY() + 0.3, p.getZ() + Math.sin(a) * r, 2, 0.1, 0.3, 0.1, 0.02);
            }
        }
        if (left == 1) {
            world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), p.getY() + 1, p.getZ(), 6, 6, 1, 6, 0);
            for (LivingEntity e : near(p, p.getPos(), 20)) { hurt(p, e, 12 + s.level() * 0.3f); affect(p, e, tr, e.getPos().subtract(p.getPos()).normalize()); if (tr == 3) strike(p, e.getPos()); }
            ULT.remove(p.getUuid());
        } else ULT.put(p.getUuid(), left - 1);
    }
    private NagaLogic() {}
}
