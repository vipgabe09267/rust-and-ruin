package com.tetanusmod.entity;

import java.util.Map;
import java.util.UUID;

/** Saved combat cooldown and bounded leases for one Tetanized Wither's summoned guard. */
public interface WitherBossState {
    int rustruin$minionCooldown();
    void rustruin$minionCooldown(int ticks);
    Map<UUID,Long> rustruin$minionLeases();
}
