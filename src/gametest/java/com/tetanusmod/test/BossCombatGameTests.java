package com.tetanusmod.test;

import com.mojang.authlib.GameProfile;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.entity.BossAbilities;
import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.test.mixin.ServerPlayerTestAccessor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.UUID;

public final class BossCombatGameTests implements FabricGameTest {
    private static final String SPACE = "tetanusmod:test/camp_empty";
    private static final String BATCH = "tetanized_boss_combat";

    @GameTest(templateName = SPACE, batchId = BATCH)
    public void doublesDirectHitDragonBreathAndWitherExplosionDamage(TestContext ctx) {
        var world = ctx.getWorld();
        var wither = EntityType.WITHER.create(world);
        var dragon = EntityType.ENDER_DRAGON.create(world);
        var center = ctx.getAbsolute(new Vec3d(20.5, 32, 20.5));
        wither.setPosition(center);
        dragon.setPosition(center);
        var cloud = new AreaEffectCloudEntity(world, center.x, center.y, center.z);
        cloud.setOwner(dragon);
        float conversion = TetanusMod.CONFIG.mobConversionChance;
        try {
            TetanusMod.CONFIG.mobConversionChance = 0;
            assertHit(ctx, world.getDamageSources().mobAttack(wither), 4, 4);
            assertHit(ctx, world.getDamageSources().mobAttack(dragon), 4, 4);
            assertBreathDamage(ctx, dragon, cloud, 6);
            float normalExplosion = blastDamage(ctx, wither, center);
            ctx.assertTrue(normalExplosion > 0, "Vanilla Wither blast did not hit its test target");
            MobVariants.makePermanent(wither);
            MobVariants.makePermanent(dragon);
            assertHit(ctx, world.getDamageSources().mobAttack(wither), 4, 8);
            assertHit(ctx, world.getDamageSources().mobAttack(dragon), 4, 8);
            assertBreathDamage(ctx, dragon, cloud, 12);
            ctx.assertTrue(blastDamage(ctx, wither, center) == normalExplosion * 2,
                    "Boss-owned explosion damage was not doubled at the same distance");
            assertHit(ctx, world.getDamageSources().generic(), 4, 4);
        } finally {
            TetanusMod.CONFIG.mobConversionChance = conversion;
            wither.discard();
            dragon.discard();
            cloud.discard();
        }
        ctx.complete();
    }

    private void assertHit(TestContext ctx, DamageSource source, float amount, float expected) {
        var target = EntityType.IRON_GOLEM.create(ctx.getWorld());
        try {
            ctx.assertTrue(target.damage(source, amount) && target.getMaxHealth() - target.getHealth() == expected,
                    "Unarmored damage did not match its expected multiplier");
        } finally {
            target.discard();
        }
    }

    private void assertBreathDamage(TestContext ctx, EnderDragonEntity dragon, AreaEffectCloudEntity cloud, float expected) {
        var target = EntityType.IRON_GOLEM.create(ctx.getWorld());
        try {
            StatusEffects.INSTANT_DAMAGE.applyInstantEffect(cloud, dragon, target, 0, 1);
            ctx.assertTrue(target.getMaxHealth() - target.getHealth() == expected, "Dragon-owned instant breath received the wrong damage");
        } finally {
            target.discard();
        }
    }

    private float blastDamage(TestContext ctx, WitherEntity wither, Vec3d at) {
        var world = ctx.getWorld();
        var target = EntityType.IRON_GOLEM.create(world);
        target.setPosition(at.add(10, 0, 0));
        target.setAiDisabled(true);
        try {
            ctx.assertTrue(world.spawnEntity(target) && world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(1),
                    entity -> entity == target).size() == 1, "Wither blast victim was not indexed");
            world.createExplosion(wither, at.x, at.y, at.z, 7, World.ExplosionSourceType.NONE);
            return target.getMaxHealth() - target.getHealth();
        } finally {
            target.discard();
        }
    }

    @GameTest(templateName = SPACE, batchId = BATCH)
    public void nativeSkullDoublesItsHitAndWitherPulseFrequency(TestContext ctx) throws Exception {
        var world = ctx.getWorld();
        var original = world.getDifficulty();
        var server = world.getServer();
        var hit = WitherSkullEntity.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
        hit.setAccessible(true);
        float conversion = TetanusMod.CONFIG.mobConversionChance;
        try {
            TetanusMod.CONFIG.mobConversionChance = 0;
            server.setDifficulty(Difficulty.NORMAL, true);
            int vanillaDuration = -1;
            for (boolean special : new boolean[]{false, true}) {
                var wither = EntityType.WITHER.create(world);
                if (special) MobVariants.makePermanent(wither);
                var skull = new WitherSkullEntity(world, wither, 1, 0, 0);
                var victim = EntityType.IRON_GOLEM.create(world);
                try {
                    hit.invoke(skull, new EntityHitResult(victim));
                    ctx.assertTrue(victim.getMaxHealth() - victim.getHealth() == (special ? 16 : 8),
                            "Native skull damage was " + (victim.getMaxHealth() - victim.getHealth()) + " for special=" + special);
                    var effect = victim.getStatusEffect(StatusEffects.WITHER);
                    ctx.assertTrue(effect != null && effect.getAmplifier() == (special ? 2 : 1), "Skull applied the wrong Wither strength");
                    if (special) {
                        ctx.assertTrue(effect.getDuration() == vanillaDuration, "Boss skull incorrectly extended Wither duration");
                        int normalPulses = witherPulses(vanillaDuration, 1);
                        ctx.assertTrue(witherPulses(vanillaDuration, 2) == normalPulses * 2, "Tetanized Wither effect did not double its pulses");
                    } else {
                        vanillaDuration = effect.getDuration();
                    }
                } finally {
                    skull.discard();
                    victim.discard();
                    wither.discard();
                }
            }
        } finally {
            TetanusMod.CONFIG.mobConversionChance = conversion;
            server.setDifficulty(original, true);
        }
        ctx.complete();
    }

    private int witherPulses(int ticks, int amplifier) {
        int count = 0;
        for (int duration = ticks; duration > 0; duration--) {
            if (StatusEffects.WITHER.canApplyUpdateEffect(duration, amplifier)) count++;
        }
        return count;
    }

    @GameTest(templateName = SPACE, batchId = BATCH)
    public void witherWindupCanBeDodgedAndEnrageSpeedsUpItsSkullBarrage(TestContext ctx) {
        var world = ctx.getWorld();
        var at = ctx.getAbsolute(new Vec3d(12.5, 24, 12.5));
        var scene = player(world, at);
        var actor = scene.player();
        var boss = EntityType.WITHER.create(world);
        boss.setPosition(at.add(-8, 3, 0));
        boss.setInvulTimer(0);
        try {
            var normal = ready(false);
            tick(boss, normal, 35);
            ctx.assertTrue(actor.getHealth() == actor.getMaxHealth(), "Vanilla Wither used a special attack");
            MobVariants.makePermanent(boss);
            var dodge = ready(false);
            tick(boss, dodge, 28);
            ctx.assertTrue(actor.getHealth() == actor.getMaxHealth(), "Rustquake hit before the visible warning ended");
            ctx.assertTrue(scene.connection().markerPackets >= 20 && scene.connection().witherWarnings == 1,
                    "Wither did not send its dust ring and warning sound to the player");
            actor.setPosition(at.add(7, 0, 0));
            BossAbilities.tick(boss, dodge);
            ctx.assertTrue(actor.getHealth() == actor.getMaxHealth(), "Rustquake moved with the dodging player");
            ctx.assertTrue(savedState(dodge).getInt("Rest") == 300, "Full-health Wither lost its rest time");

            actor.setPosition(at);
            var quake = ready(false);
            tick(boss, quake, 29);
            ctx.assertTrue(actor.getHealth() == 8 && actor.getVelocity().y > 0, "Rustquake did not apply its expected damage and lift");

            boss.setHealth(300);
            var barrage = ready(true);
            tick(boss, barrage, 25);
            var skulls = world.getEntitiesByClass(WitherSkullEntity.class, boss.getBoundingBox().expand(5), skull -> skull.getOwner() == boss);
            try {
                ctx.assertTrue(skulls.size() == 3, "Barrage did not create three native boss-owned skulls");
                var saved = savedState(barrage);
                ctx.assertTrue(saved.getBoolean("Enraged") && saved.getInt("Rest") == 180, "Low health did not shorten the Wither's recovery");
            } finally {
                skulls.forEach(WitherSkullEntity::discard);
            }
        } finally {
            boss.discard();
            clean(actor);
        }
        ctx.complete();
    }

    @GameTest(templateName = SPACE, batchId = BATCH)
    public void dragonRustfireUsesNativeDamageAndWingstormRespectsCover(TestContext ctx) {
        var world = ctx.getWorld();
        var at = ctx.getAbsolute(new Vec3d(15.5, 24, 15.5));
        var scene = player(world, at);
        var actor = scene.player();
        var dragon = EntityType.ENDER_DRAGON.create(world);
        dragon.setPosition(at.add(-10, 4, 0));
        var area = new Box(BlockPos.ofFloored(at)).expand(10);
        var wall = BlockPos.ofFloored(at.add(2, 0, 0));
        try {
            MobVariants.makePermanent(dragon);
            var fire = ready(false);
            BossAbilities.tick(dragon, fire);
            ctx.assertTrue(scene.connection().markerPackets >= 20 && scene.connection().dragonWarnings == 1,
                    "Dragon did not send its dust ring and warning sound to the player");
            var interrupted = new NbtCompound();
            fire.write(interrupted);
            var reloaded = new BossAbilities.State();
            reloaded.read(interrupted);
            tick(dragon, reloaded, 32);
            ctx.assertTrue(world.getEntitiesByClass(AreaEffectCloudEntity.class, area, cloud -> cloud.getOwner() == dragon).isEmpty(),
                    "Reload delivered an attack without a fresh warning");
            tick(dragon, fire, 32);
            var clouds = world.getEntitiesByClass(AreaEffectCloudEntity.class, area, cloud -> cloud.getOwner() == dragon);
            ctx.assertTrue(clouds.size() == 1 && clouds.get(0).getDuration() == 140, "Dragon did not leave a bounded Rustfire cloud");
            for (int i = 0; i < 5; i++) world.tickEntity(clouds.get(0));
            ctx.assertTrue(actor.getHealth() == 8, "Rustfire's native breath left " + actor.getHealth() + " health instead of 8");
            clouds.get(0).discard();

            actor.setHealth(actor.getMaxHealth());
            actor.timeUntilRegen = 0;
            dragon.setHealth(500);
            var storm = ready(true);
            BossAbilities.tick(dragon, storm);
            actor.setPosition(at.add(4, 0, 0));
            world.setBlockState(wall, Blocks.STONE.getDefaultState());
            tick(dragon, storm, 30);
            ctx.assertTrue(actor.getHealth() == actor.getMaxHealth(), "Wingstorm hit a player behind cover");
            ctx.assertTrue(savedState(storm).getInt("Rest") == 220, "Enraged dragon did not shorten its recovery");
            world.setBlockState(wall, Blocks.AIR.getDefaultState());
            actor.setPosition(at);
            actor.setVelocity(Vec3d.ZERO);
            var exposed = ready(true);
            BossAbilities.tick(dragon, exposed);
            tick(dragon, exposed, 30);
            ctx.assertTrue(actor.getHealth() == 10 && actor.getVelocity().x > 0.9, "Wingstorm failed to hit and push a player at its center");
        } finally {
            world.setBlockState(wall, Blocks.AIR.getDefaultState());
            world.getEntitiesByClass(AreaEffectCloudEntity.class, area, cloud -> cloud.getOwner() == dragon).forEach(AreaEffectCloudEntity::discard);
            dragon.discard();
            clean(actor);
        }
        ctx.complete();
    }

    private BossAbilities.State ready(boolean alternate) {
        var fixture = new NbtCompound();
        var saved = new NbtCompound();
        saved.putInt("Rest", 0);
        saved.putBoolean("Alternate", alternate);
        fixture.put("RustRuinBossAbilities", saved);
        var state = new BossAbilities.State();
        state.read(fixture);
        return state;
    }

    private NbtCompound savedState(BossAbilities.State state) {
        var nbt = new NbtCompound();
        state.write(nbt);
        return nbt.getCompound("RustRuinBossAbilities");
    }

    private void tick(MobEntity boss, BossAbilities.State state, int ticks) {
        for (int i = 0; i < ticks; i++) BossAbilities.tick(boss, state);
    }

    private record Actor(ServerPlayerEntity player, WarningConnection connection) {}

    private static final class WarningConnection extends ClientConnection {
        int markerPackets;
        int witherWarnings;
        int dragonWarnings;

        WarningConnection() { super(NetworkSide.SERVERBOUND); }

        @Override
        public void send(Packet<?> packet, PacketCallbacks callbacks) {
            if (packet instanceof ParticleS2CPacket particles && particles.getCount() == 1
                    && particles.getParameters() instanceof DustParticleEffect) markerPackets++;
            if (packet instanceof PlaySoundS2CPacket sound) {
                if (sound.getSound().value() == SoundEvents.ENTITY_WITHER_HURT) witherWarnings++;
                if (sound.getSound().value() == SoundEvents.ENTITY_ENDER_DRAGON_GROWL) dragonWarnings++;
            }
            super.send(packet, callbacks);
        }
    }

    private Actor player(ServerWorld world, Vec3d at) {
        var player = new ServerPlayerEntity(world.getServer(), world, new GameProfile(UUID.randomUUID(), "boss-combatant"));
        var connection = new WarningConnection();
        world.getServer().getPlayerManager().onPlayerConnect(connection, player);
        player.changeGameMode(GameMode.SURVIVAL);
        ((ServerPlayerTestAccessor) player).rustruin$joinInvulnerability(0);
        player.setNoGravity(true);
        player.setPosition(at);
        return new Actor(player, connection);
    }

    private void clean(ServerPlayerEntity player) {
        player.getServer().getPlayerManager().remove(player);
        player.discard();
    }
}
