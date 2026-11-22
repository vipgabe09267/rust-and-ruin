package com.tetanusmod.expansion;

import com.tetanusmod.TetanusMod;
import net.minecraft.entity.effect.*;
import net.minecraft.registry.*;

public final class ExpansionEffects {
    public static final StatusEffect RABIES=Registry.register(Registries.STATUS_EFFECT,TetanusMod.id("rabies"),new RabiesEffect());
    public static final StatusEffect RABIES_RESISTANCE=Registry.register(Registries.STATUS_EFFECT,TetanusMod.id("rabies_resistance"),new StatusEffect(StatusEffectCategory.BENEFICIAL,0xA87BD3){});
    public static final StatusEffect DISORIENTED=Registry.register(Registries.STATUS_EFFECT,TetanusMod.id("disoriented"),new DisorientedEffect());
    public static final StatusEffect SPLINTED = Registry.register(Registries.STATUS_EFFECT, TetanusMod.id("splinted"),
            new StatusEffect(StatusEffectCategory.BENEFICIAL, 0x52776C) {});
    public static void initialize(){}
}
