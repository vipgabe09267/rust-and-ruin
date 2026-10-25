package com.tetanusmod.world;
import net.minecraft.util.math.BlockPos;
/** Camp-specific home anchor; ordinary golems and wild variants never opt in. */
public interface CampGuardHome {int PATROL_RADIUS=8;void rustruin$campHome(BlockPos pos);}