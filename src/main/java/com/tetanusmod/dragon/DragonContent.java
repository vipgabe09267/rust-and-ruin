package com.tetanusmod.dragon;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.item.*;
import net.minecraft.recipe.*;
import net.minecraft.registry.*;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Rarity;

/** Registry entries are additive so existing saves keep their original identities. */
public final class DragonContent {
    private DragonContent(){}
    public static final Block FORGE=Registry.register(Registries.BLOCK,TetanusMod.id("tetanus_wither_forging_table"),new WitherForgingBlock(FabricBlockSettings.copyOf(Blocks.NETHERITE_BLOCK).strength(30,1200)));
    public static final Item FORGE_ITEM=ModItems.register("tetanus_wither_forging_table",new BlockItem(FORGE,new Item.Settings().fireproof()));
    public static final Block EGG=Registry.register(Registries.BLOCK,TetanusMod.id("tetanized_dragon_egg"),new DragonEggBlock(FabricBlockSettings.copyOf(Blocks.DRAGON_EGG)));
    public static final Item EGG_ITEM=ModItems.register("tetanized_dragon_egg",new BlockItem(EGG,new Item.Settings().rarity(Rarity.EPIC).fireproof()));
    public static final EntityType<TetanizedEndCrystalEntity> CRYSTAL_ENTITY=Registry.register(Registries.ENTITY_TYPE,TetanusMod.id("tetanized_end_crystal"),FabricEntityTypeBuilder.<TetanizedEndCrystalEntity>create(SpawnGroup.MISC,TetanizedEndCrystalEntity::new).dimensions(EntityDimensions.fixed(2,2)).trackRangeBlocks(256).trackedUpdateRate(1).build());
    public static final Item CRYSTAL=ModItems.register("tetanized_end_crystal",new TetanizedEndCrystalItem(new Item.Settings().rarity(Rarity.RARE)));
    public static final Item SWORD=ModItems.register("tetanus_overload_sword",new OverloadSwordItem());
    public static final ScreenHandlerType<WitherForgingScreenHandler> SCREEN=Registry.register(Registries.SCREEN_HANDLER,TetanusMod.id("wither_forging"),new ScreenHandlerType<>(WitherForgingScreenHandler::new,FeatureFlags.VANILLA_FEATURES));
    public static final RecipeType<CrystalForgingRecipe> RECIPE_TYPE=Registry.register(Registries.RECIPE_TYPE,TetanusMod.id("crystal_forging"),new RecipeType<>(){});
    public static final RecipeSerializer<CrystalForgingRecipe> RECIPE_SERIALIZER=Registry.register(Registries.RECIPE_SERIALIZER,TetanusMod.id("crystal_forging"),new CrystalForgingRecipe.Serializer());
    public static void initialize(){DragonEncounter.initialize();}
}
