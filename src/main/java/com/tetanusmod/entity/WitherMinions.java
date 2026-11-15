package com.tetanusmod.entity;

import com.tetanusmod.TetanusMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.*;
import net.minecraft.registry.*;

/** Summoned only by the boss; no natural spawn entries or spawn restrictions are replaced. */
public final class WitherMinions {
    public static final EntityType<WitherMinionEntity> TYPE=Registry.register(Registries.ENTITY_TYPE,TetanusMod.id("tetanized_wither_minion"),
            FabricEntityTypeBuilder.<WitherMinionEntity>create(SpawnGroup.MONSTER,WitherMinionEntity::new)
                    .dimensions(EntityDimensions.fixed(0.35F,1.2F)).trackRangeBlocks(64).trackedUpdateRate(3).fireImmune().build());
    public static void initialize(){FabricDefaultAttributeRegistry.register(TYPE,WitherMinionEntity.attributes());}
    private WitherMinions(){}
}
