package com.tetanusmod.command;

import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.expansion.Rabies;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** Read-only diagnostics, restricted by the operator command root. Never loads chunks. */
public final class MobInspector {
    public static String reason(MobEntity mob) {
        if (mob.hasCustomName()) return "named; vanilla persistence applies";
        if (mob instanceof TameableEntity tame && tame.isTamed()) return "tamed; vanilla ownership rules apply";
        if (mob.isLeashed()) return "leashed; vanilla persistence applies";
        if (mob.hasVehicle() || mob.hasPassengers()) return "riding or carrying passengers";
        if (mob.isPersistent()) return "PersistenceRequired flag (equipment pickup or explicit persistence)";
        if (mob.cannotDespawn()) return "species/state prevents natural despawning";
        if (!mob.canImmediatelyDespawn(16385)) return "species keeps its normal distance-despawn rules";
        return "eligible for vanilla distance despawning; nearest-player distance still applies";
    }
    public static int inspect(ServerCommandSource source, Entity target) {
        if (!(target instanceof MobEntity mob)) {
            source.sendError(Text.translatable("commands.tetanusmod.inspect_mob")); return 0;
        }
        source.sendFeedback(() -> Text.literal(Registries.ENTITY_TYPE.getId(mob.getType()) + " " + mob.getUuid()
                + " | tetanized=" + MobVariants.permanent(mob) + ", rabid=" + Rabies.rabid(mob)
                + ", persistent=" + mob.isPersistent() + ", cannotDespawn=" + mob.cannotDespawn()
                + " | " + reason(mob)), false);
        return 1;
    }
    public static int population(ServerCommandSource source, int radius) {
        var origin = source.getPosition();
        var box = new net.minecraft.util.math.Box(origin, origin).expand(radius);
        var mobs = source.getWorld().getEntitiesByClass(MobEntity.class, box,
                mob -> mob.squaredDistanceTo(origin) <= (double)radius * radius);
        long tetanized = mobs.stream().filter(MobVariants::permanent).count();
        long rabid = mobs.stream().filter(Rabies::rabid).count();
        long animals = mobs.stream().filter(MobVariants::isAnimal).count();
        long persistent = mobs.stream().filter(mob -> mob.isPersistent() || mob.cannotDespawn()).count();
        source.sendFeedback(() -> Text.translatable("commands.tetanusmod.population", radius, mobs.size(), tetanized, rabid, animals, persistent), false);
        return mobs.size();
    }
}