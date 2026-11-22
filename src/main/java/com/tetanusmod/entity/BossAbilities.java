package com.tetanusmod.entity;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/** Server-owned boss attacks with a visible, fixed target during the windup. */
public final class BossAbilities {
    private static final String TAG = "RustRuinBossAbilities";
    private static final int FIRST_CAST = 140;
    private static final int INTERRUPTED_REST = 80;
    private static final DustParticleEffect RUST = new DustParticleEffect(new Vector3f(0.85F, 0.32F, 0.10F), 1.3F);
    private static final DustParticleEffect VIOLET = new DustParticleEffect(new Vector3f(0.69F, 0.23F, 0.87F), 1.3F);

    private enum Attack {
        RUSTQUAKE("rustquake", 28, 5, RUST),
        SKULL_BARRAGE("skull_barrage", 24, 3, RUST),
        RUSTFIRE("rustfire", 32, 5, VIOLET),
        WINGSTORM("wingstorm", 30, 7, VIOLET);

        final String id;
        final int windup;
        final int radius;
        final ParticleEffect particles;

        Attack(String id, int windup, int radius, ParticleEffect particles) {
            this.id = id;
            this.windup = windup;
            this.radius = radius;
            this.particles = particles;
        }
    }

    public static final class State {
        private int rest = -1;
        private int windup;
        private boolean alternate;
        private boolean enraged;
        @Nullable private Attack pending;
        @Nullable private Vec3d point;

        public void cancelWindup() {
            if (pending == null) return;
            clearWindup();
            rest = INTERRUPTED_REST;
        }

        private void clearWindup() {
            pending = null;
            point = null;
            windup = 0;
        }

        public void write(NbtCompound nbt) {
            if (rest < 0 && !enraged) return;
            NbtCompound saved = new NbtCompound();
            // After a restart, give players time to see a fresh warning.
            saved.putInt("Rest", pending == null ? rest : INTERRUPTED_REST);
            saved.putBoolean("Alternate", alternate);
            saved.putBoolean("Enraged", enraged);
            nbt.put(TAG, saved);
        }

        public void read(NbtCompound nbt) {
            NbtCompound saved = nbt.getCompound(TAG);
            rest = saved.contains("Rest", NbtElement.INT_TYPE) ? MathHelper.clamp(saved.getInt("Rest"), -1, 360) : -1;
            alternate = saved.getBoolean("Alternate");
            enraged = saved.getBoolean("Enraged");
            clearWindup();
        }
    }

    private BossAbilities() {}

    public static void tick(MobEntity boss, State state) {
        boolean dragon = boss instanceof EnderDragonEntity;
        if (!dragon && !(boss instanceof WitherEntity)) return;
        if (!(boss.getWorld() instanceof ServerWorld world) || !boss.isAlive() || boss.getHealth() <= 0
                || !MobVariants.permanent(boss) || boss.isAiDisabled() || world.getDifficulty() == Difficulty.PEACEFUL
                || boss instanceof WitherEntity wither && wither.getInvulnerableTimer() > 0) {
            state.cancelWindup();
            return;
        }

        if (!state.enraged && boss.getHealth() <= boss.getMaxHealth() * 0.5F) {
            state.enraged = true;
            state.rest = state.rest < 0 ? 60 : Math.min(state.rest, 60);
            burst(world, boss.getPos().add(0, boss.getHeight() * 0.5, 0), dragon ? VIOLET : RUST, dragon ? 3 : 1);
            world.playSound(null, boss.getBlockPos(), dragon ? SoundEvents.ENTITY_ENDER_DRAGON_GROWL : SoundEvents.ENTITY_WITHER_AMBIENT,
                    SoundCategory.HOSTILE, 1.25F, 0.7F);
        }
        if (state.pending != null) {
            advance(world, boss, state, dragon);
            return;
        }
        if (state.rest < 0) state.rest = FIRST_CAST;
        if (state.rest > 0) {
            state.rest--;
            return;
        }

        int range = dragon ? 88 : 48;
        ServerPlayerEntity target = null;
        double closest = range * range;
        for (ServerPlayerEntity player : world.getPlayers()) {
            double distance = boss.squaredDistanceTo(player);
            if (combatant(player) && distance <= closest) {
                target = player;
                closest = distance;
            }
        }
        if (target == null) {
            state.rest = 20;
            return;
        }

        Attack attack = dragon ? (state.alternate ? Attack.WINGSTORM : Attack.RUSTFIRE)
                : (state.alternate ? Attack.SKULL_BARRAGE : Attack.RUSTQUAKE);
        state.pending = attack;
        state.point = attack == Attack.SKULL_BARRAGE ? target.getPos().add(0, 0.8, 0) : ground(world, target);
        state.windup = attack.windup;
        target.sendMessage(Text.translatable("message.tetanusmod.boss." + attack.id), true);
        ring(world, state.point, attack);
        world.playSound(null, BlockPos.ofFloored(state.point), dragon ? SoundEvents.ENTITY_ENDER_DRAGON_GROWL : SoundEvents.ENTITY_WITHER_HURT,
                SoundCategory.HOSTILE, 1.0F, 1.3F);
    }

    private static void advance(ServerWorld world, MobEntity boss, State state, boolean dragon) {
        Attack attack = state.pending;
        Vec3d point = state.point;
        BlockPos pos = BlockPos.ofFloored(point);
        if (!world.isChunkLoaded(pos) || !world.getWorldBorder().contains(pos)) {
            state.cancelWindup();
            return;
        }
        if (--state.windup > 0) {
            if (state.windup % 5 == 0) ring(world, point, attack);
            return;
        }
        state.clearWindup();
        state.alternate = !state.alternate;
        state.rest = dragon ? (state.enraged ? 220 : 360) : (state.enraged ? 180 : 300);
        burst(world, point, attack.particles, attack.radius * 0.25);
        world.playSound(null, pos, dragon ? SoundEvents.ENTITY_ENDER_DRAGON_FLAP : SoundEvents.BLOCK_BASALT_BREAK,
                SoundCategory.HOSTILE, 1.15F, 0.75F);
        switch (attack) {
            case SKULL_BARRAGE -> launchSkulls(world, (WitherEntity) boss, point);
            case RUSTFIRE -> rustfire(world, (EnderDragonEntity) boss, point);
            case RUSTQUAKE -> strikeArea(world, boss, point, attack.radius, 6, 0.45);
            case WINGSTORM -> strikeArea(world, boss, point, attack.radius, 5, 1.05);
        }
    }

    private static boolean combatant(ServerPlayerEntity player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative()
                && !player.getAbilities().invulnerable && !player.isSleeping()
                && !(player.getVehicle() instanceof EnderDragonEntity);
    }

    private static Vec3d ground(ServerWorld world, ServerPlayerEntity player) {
        Vec3d start = player.getPos().add(0, 0.25, 0);
        var trace = world.raycast(new RaycastContext(start, start.add(0, -3.5, 0),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        return trace.getType() == HitResult.Type.MISS ? player.getPos() : trace.getPos().add(0, 0.05, 0);
    }

    private static void ring(ServerWorld world, Vec3d point, Attack attack) {
        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2 * i / 20;
            world.spawnParticles(attack.particles, point.x + Math.cos(angle) * attack.radius, point.y + 0.10,
                    point.z + Math.sin(angle) * attack.radius, 1, 0, 0, 0, 0);
        }
    }

    private static void burst(ServerWorld world, Vec3d point, ParticleEffect particles, double width) {
        world.spawnParticles(particles, point.x, point.y + 0.5, point.z, 60, width, 0.6, width, 0.04);
    }

    private static void launchSkulls(ServerWorld world, WitherEntity wither, Vec3d point) {
        Vec3d origin = new Vec3d(wither.getX(), wither.getBodyY(0.8), wither.getZ());
        Vec3d aim = point.subtract(origin);
        Vec3d side = new Vec3d(-aim.z, 0, aim.x).normalize();
        if (side.lengthSquared() == 0) side = new Vec3d(1, 0, 0);
        for (int lane = -1; lane <= 1; lane++) {
            Vec3d launch = origin.add(side.multiply(lane * 0.65));
            Vec3d direction = point.add(side.multiply(lane * 1.8)).subtract(launch);
            var skull = new WitherSkullEntity(world, wither, direction.x, direction.y, direction.z);
            skull.setPosition(launch);
            world.spawnEntity(skull);
        }
    }

    private static void rustfire(ServerWorld world, EnderDragonEntity dragon, Vec3d point) {
        var cloud = new AreaEffectCloudEntity(world, point.x, point.y, point.z);
        cloud.setOwner(dragon);
        cloud.setParticleType(ParticleTypes.DRAGON_BREATH);
        cloud.setRadius(5);
        cloud.setDuration(140);
        cloud.setWaitTime(0);
        cloud.setRadiusGrowth(-0.018F);
        cloud.addEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 1));
        world.spawnEntity(cloud);
    }

    private static void strikeArea(ServerWorld world, MobEntity boss, Vec3d point, int radius, float damage, double force) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            Vec3d offset = player.getPos().subtract(point);
            var horizontal = new Vec3d(offset.x, 0, offset.z);
            if (!combatant(player) || Math.abs(offset.y) > 3 || horizontal.lengthSquared() > radius * radius) continue;
            var cover = world.raycast(new RaycastContext(point.add(0, 0.45, 0), player.getPos().add(0, 0.7, 0),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, boss));
            if (cover.getType() == HitResult.Type.MISS && player.damage(world.getDamageSources().mobAttack(boss), damage) && player.isAlive()) {
                if (horizontal.lengthSquared() < 0.01) {
                    // At the exact center, push along the direction the attack came from.
                    horizontal = new Vec3d(point.x - boss.getX(), 0, point.z - boss.getZ());
                    if (horizontal.lengthSquared() < 0.01) horizontal = Vec3d.fromPolar(0, boss.getYaw());
                }
                Vec3d push = horizontal.normalize().multiply(force);
                player.addVelocity(push.x, 0.35, push.z);
                player.velocityModified = true;
            }
        }
    }
}
