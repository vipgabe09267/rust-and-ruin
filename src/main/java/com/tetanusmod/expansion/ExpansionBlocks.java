package com.tetanusmod.expansion;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.*;
import net.minecraft.registry.*;

public final class ExpansionBlocks {
    public static final Block RUST_PILE=Registry.register(Registries.BLOCK,TetanusMod.id("rust"),new RustPileBlock(FabricBlockSettings.create().strength(0.1F).noCollision().nonOpaque().sounds(net.minecraft.sound.BlockSoundGroup.SAND)));
    public static final Block RUSTED_IRON_BLOCK=register("rusted_iron_block",new Block(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK)));
    public static final Block MEDIC_CABINET=register("medic_cabinet",new MedicineCabinetBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK)));
    public static final Block FIELD_MEDIC_CABINET=Registry.register(Registries.BLOCK,TetanusMod.id("field_medic_cabinet"),new MedicineCabinetBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK)));
    public static final Block BEAR_TRAP=register("bear_trap",new BearTrapBlock(false,FabricBlockSettings.create().strength(2).nonOpaque().sounds(net.minecraft.sound.BlockSoundGroup.METAL)));
    public static final Block RUSTED_BEAR_TRAP=register("rusted_bear_trap",new BearTrapBlock(true,FabricBlockSettings.create().strength(2).nonOpaque().sounds(net.minecraft.sound.BlockSoundGroup.METAL)));
    public static final BlockEntityType<BearTrapBlockEntity> BEAR_TRAP_ENTITY=Registry.register(Registries.BLOCK_ENTITY_TYPE,TetanusMod.id("bear_trap"),FabricBlockEntityTypeBuilder.create(BearTrapBlockEntity::new,BEAR_TRAP,RUSTED_BEAR_TRAP).build());
    private static Block register(String id,Block block){Registry.register(Registries.BLOCK,TetanusMod.id(id),block);ModItems.register(id,new BlockItem(block,new Item.Settings()));return block;}
    public static void initialize(){}
}
