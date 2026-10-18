package com.tetanusmod.beacon;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

public final class ModBeacons {
    public static final net.minecraft.registry.tag.TagKey<Block> BASES=net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.BLOCK,TetanusMod.id("tetanized_beacon_base_blocks"));
    public static final Block BLOCK=Registry.register(Registries.BLOCK,TetanusMod.id("tetanized_beacon"),new TetanizedBeaconBlock(FabricBlockSettings.copyOf(Blocks.BEACON)));
    public static final Item ITEM=ModItems.register("tetanized_beacon",new BlockItem(BLOCK,new Item.Settings().rarity(net.minecraft.util.Rarity.EPIC)));
    public static final BlockEntityType<TetanizedBeaconEntity> ENTITY=Registry.register(Registries.BLOCK_ENTITY_TYPE,TetanusMod.id("tetanized_beacon"),FabricBlockEntityTypeBuilder.create(TetanizedBeaconEntity::new,BLOCK).build());
    public static final ScreenHandlerType<TetanizedBeaconScreenHandler> SCREEN=Registry.register(Registries.SCREEN_HANDLER,TetanusMod.id("tetanized_beacon"),new ScreenHandlerType<>(TetanizedBeaconScreenHandler::new,FeatureFlags.VANILLA_FEATURES));
    public static void initialize() { }
}