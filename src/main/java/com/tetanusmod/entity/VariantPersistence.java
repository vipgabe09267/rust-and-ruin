package com.tetanusmod.entity;

import com.tetanusmod.expansion.Rabies;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/** Repair only the persistence flag forced by pre-fix mutations, once per saved mob. */
public final class VariantPersistence {
    public static final String RULES_KEY = "RustRuinDespawnRules";
    public static final int RULES_VERSION = 1;

    private VariantPersistence() { }

    public static boolean repairLegacy(MobEntity mob, NbtCompound nbt) {
        if (nbt.getInt(RULES_KEY) >= RULES_VERSION || !nbt.getBoolean("PersistenceRequired")
                || (!MobVariants.permanent(mob) && !Rabies.rabid(mob))) return false;
        // These are explicit vanilla reasons to keep a creature. Leash/passenger data may
        // not yet have resolved to entities while custom NBT is being read.
        if (mob.hasCustomName() || nbt.contains("CustomName") || nbt.contains("Leash")
                || nbt.contains("Passengers") || mob.hasVehicle() || mob.hasPassengers()
                || nbt.getBoolean("Tame") || nbt.contains("Owner") || nbt.contains("OwnerUUID")
                || nbt.getBoolean("FromBucket") || nbt.getBoolean("NoAI")) return false;
        // Picking up equipment sets both PersistenceRequired and a guaranteed drop chance.
        for (String key : new String[]{"HandDropChances", "ArmorDropChances"}) {
            var chances = nbt.getList(key, NbtElement.FLOAT_TYPE);
            for (int i = 0; i < chances.size(); i++) if (chances.getFloat(i) > 1.0F) return false;
        }
        return true;
    }
}
