package com.tetanusmod.registry;

import com.tetanusmod.TetanusMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;

/** Profession ambience is separate from disease announcements. */
public final class ModSounds {
    public static final SoundEvent MEDIC_WORK=Registry.register(Registries.SOUND_EVENT,
            TetanusMod.id("medic_work"),SoundEvent.of(TetanusMod.id("medic_work"),12));
    public static final SoundEvent DRAGON_DEFEATED=Registry.register(Registries.SOUND_EVENT,
            TetanusMod.id("dragon_defeated"),SoundEvent.of(TetanusMod.id("dragon_defeated"),32));
    private ModSounds(){}
}
