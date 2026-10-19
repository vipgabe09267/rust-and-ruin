package com.tetanusmod.expansion;

import com.tetanusmod.TetanusMod;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.*;
import net.minecraft.registry.*;
import net.minecraft.text.Text;

public final class ModDamage {
    public static final RegistryKey<DamageType> TETANUS=key("tetanus");
    public static final RegistryKey<DamageType> RABIES=key("rabies");
    public static final RegistryKey<DamageType> RUSTED_NAIL=key("rusted_nail");
    public static final RegistryKey<DamageType> BEAR_TRAP=key("bear_trap");
    public static final RegistryKey<DamageType> BARBED_WIRE=key("barbed_wire");
    public static final RegistryKey<DamageType> DISORIENTATION=key("disorientation");
    private static RegistryKey<DamageType> key(String id){return RegistryKey.of(RegistryKeys.DAMAGE_TYPE,TetanusMod.id(id));}
    public static DamageSource source(LivingEntity target,RegistryKey<DamageType> key){return new DamageSource(target.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(key));}
    public static DamageSource nail(Entity projectile,Entity owner) {
        return new DamageSource(projectile.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(RUSTED_NAIL),projectile,owner) {
            @Override public Text getDeathMessage(LivingEntity victim) {
                if(victim==owner)return Text.translatable("death.attack.tetanusmod.returned_nail",victim.getDisplayName());
                return super.getDeathMessage(victim);
            }
        };
    }
}